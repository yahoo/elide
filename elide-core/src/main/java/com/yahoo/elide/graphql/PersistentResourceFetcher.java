/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

import com.google.common.collect.Sets;
import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.HttpStatus;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.exceptions.InvalidAttributeException;
import com.yahoo.elide.core.exceptions.UnknownEntityException;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.pagination.Pagination;
import com.yahoo.elide.core.sort.Sorting;
import com.yahoo.elide.graphql.operations.UpdateOperation;
import graphql.language.Argument;
import graphql.language.Field;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLType;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.WebApplicationException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.yahoo.elide.graphql.ModelBuilder.ARGUMENT_DATA;
import static com.yahoo.elide.graphql.ModelBuilder.ARGUMENT_OPERATION;

/**
 * Interacts with {@link PersistentResource} to fetch data for GraphQL.
 */
@Slf4j
public class PersistentResourceFetcher implements DataFetcher {
    private final ElideSettings settings;

    public PersistentResourceFetcher(ElideSettings settings) {
        this.settings = settings;
    }

    @Override
    public Object get(DataFetchingEnvironment environment) {
        Map<String, Object> args = environment.getArguments();
        Environment context = new Environment(environment);
        RelationshipOp operation = (RelationshipOp) args.getOrDefault(ARGUMENT_OPERATION, RelationshipOp.FETCH);

        if (log.isDebugEnabled()) {
            logContext(operation, context);
        }

        switch (operation) {
            case FETCH:
                return fetchObject(context);

            case ADD:
                return createObject(context);

            case DELETE:
                return deleteObject(context);

            case REPLACE:
                return new UpdateOperation(context).execute();
        }

        throw new UnsupportedOperationException("Unknown operation: " + operation);
    }

    private void logContext(RelationshipOp operation, Environment environment) {
        List<Field> children = environment.field.getSelectionSet() != null
                ? (List) environment.field.getSelectionSet().getChildren()
                : new ArrayList<>();
        String requestedFields = environment.field.getName() + (children.size() > 0
                ? "(" + children.stream().map(Field::getName).collect(Collectors.toList()) + ")" : "");
        GraphQLType parent = environment.parentType;
        log.debug("{} {} fields for {} with parent {}<{}>",
                operation, requestedFields, environment.source, parent.getClass().getSimpleName(), parent.getName());
    }

    private Object deleteObject(Environment request) {
        Set<Object> deleted = new HashSet<>();

        if (!request.id.isPresent() && request.data.size() > 1) {
            throw new WebApplicationException("Id argument specification with an additional list of id's to delete "
                    + "is unsupported", HttpStatus.SC_BAD_REQUEST);
        }

        EntityDictionary dictionary = request.requestScope.getDictionary();

        String loadType = request.field.getName();
        // TODO: This works at the root-level, but will it blow up in nested deletes?
        String idFieldName = dictionary.getIdFieldName(dictionary.getEntityClass(loadType));
        String loadId = request.field.getArguments().stream()
                .filter(arg -> ARGUMENT_DATA.equals(arg.getName()))
                .findFirst()
                .map(Argument::getChildren)
                // TODO: Iterate over children and determine which contains id.
                .map(List::toString) // TODO: place holder. remove this line.
//                    .map(arg -> {
//                        String specifiedId = arg.getValue().toString();
//                        if (id != null && !id.isEmpty() && !id.equals(specifiedId)) {
//                            throw new WebApplicationException("Specified non-matching id's as argument and data.",
//                                    HttpStatus.SC_BAD_REQUEST);
//                        }
//                        return specifiedId;
//                    })
                .orElseGet(request.id::get);

        if (loadId == null) {
            throw new WebApplicationException("Did not specify id of object type to delete.",
                    HttpStatus.SC_BAD_REQUEST);
        }

        PersistentResource deleteObject = load(loadType, loadId, request.requestScope);

        if (deleteObject == null || deleteObject.getObject() == null) {
            throw new WebApplicationException("Attempted to delete non-existent id.", HttpStatus.SC_BAD_REQUEST);
        }

        deleteObject.deleteResource();
        deleted.add(deleteObject);

        return deleted;
    }

    private Object createObject(Environment request) {
        EntityDictionary dictionary = request.requestScope.getDictionary();

        GraphQLObjectType objectType;
        String uuid = UUID.randomUUID().toString();
        if (request.outputType instanceof GraphQLObjectType) {
            // No parent
            // TODO: These UUID's should not be random. They should be whatever id's are specified by the user so they
            // can be referenced throughout the document
            objectType = (GraphQLObjectType) request.outputType;
            return PersistentResource.createObject(null, dictionary.getEntityClass(objectType.getName()),
                    request.requestScope, uuid);

        } else if (request.outputType instanceof GraphQLList) {
            // Has parent
            objectType = (GraphQLObjectType) ((GraphQLList) request.outputType).getWrappedType();
            List<PersistentResource> container = new ArrayList<>();
            for (Map<String, Object> input : request.data) {
                Class<?> entityClass = dictionary.getEntityClass(objectType.getName());
                // TODO: See above comment about UUID's.
                PersistentResource toCreate = PersistentResource.createObject(null, entityClass, request.requestScope,
                        uuid);
                input.entrySet().stream()
                        .filter(entry -> dictionary.isAttribute(entityClass, entry.getKey()))
                        .forEach(entry -> toCreate.updateAttribute(entry.getKey(), entry.getValue()));
                container.add(toCreate);
            }
            return container;
        }
        throw new IllegalStateException("Not sure what to create " + request.outputType.getName());
    }

    private Object fetchObject(Environment request) {
        if (!request.data.isEmpty()) {
            throw new WebApplicationException("FETCH must not include data.", HttpStatus.SC_BAD_REQUEST);
        }

        Optional<FilterExpression> filters = Optional.empty();
        Optional<Sorting> sorting = Optional.empty();
        Optional<Pagination> pagination = Optional.empty();

        RequestScope requestScope = request.requestScope;
        if (request.outputType instanceof GraphQLList) {
            if (request.id.isPresent() && request.filters.isPresent()) {
                throw new WebApplicationException("You may not filter when loading by id");
            }

            GraphQLObjectType graphQLType = (GraphQLObjectType) ((GraphQLList) request.outputType).getWrappedType();
            String entityType = graphQLType.getName();
            Class recordType = (Class) requestScope.getDictionary().getEntityClass(entityType);

            if (recordType == null) {
                throw new UnknownEntityException(entityType);
            }

            return request.id
                    .<Set>map((id) -> Sets.newHashSet(PersistentResource.loadRecord(recordType, id, requestScope)))
                    .orElse(PersistentResource.loadRecords(recordType, requestScope));
        } else if (request.outputType instanceof GraphQLObjectType) {
            if (request.parentResource == null) {
                throw new IllegalStateException("Do we have a singleton root object?");
            }

            // we are loading a toOne relationship
            DataStoreTransaction tx = request.requestScope.getTransaction();
            PersistentResource resource = request.parentResource;
            String relationName = request.field.getName();
            Object obj =
                    tx.getRelation(tx, resource.getObject(), relationName, filters, sorting, pagination, requestScope);
            return new PersistentResource(obj, resource, requestScope.getUUIDFor(obj), requestScope);

        } else if (request.outputType instanceof GraphQLScalarType) {
            return fetchProperty(request);
        } else if (request.outputType instanceof GraphQLEnumType) {
            return fetchProperty(request);
        }

        throw new IllegalStateException("WTF is a " + request.outputType.getClass().getName() + " mate?");
    }

    protected Object fetchProperty(Environment request) {
        EntityDictionary dictionary = request.requestScope.getDictionary();
        PersistentResource resource = (PersistentResource) request.parentResource;
        Class<?> sourceClass = resource.getResourceClass();

        String fieldName = request.field.getName();
        if (dictionary.isAttribute(sourceClass, fieldName)) {
            return resource.getAttribute(fieldName);
        } else {
            log.debug("Tried to fetch property off of invalid loaded object.");
            throw new InvalidAttributeException(fieldName, resource.getType());
        }
    }

    public static Collection<PersistentResource> loadCollectionOf(String type, RequestScope requestScope) {
        Class recordType = (Class) requestScope.getDictionary().getEntityClass(type);
        return PersistentResource.loadRecords(recordType, requestScope);
    }

    public static PersistentResource load(String type, String id, RequestScope requestScope) {
        Class<?> recordType = requestScope.getDictionary().getEntityClass(type);
        return PersistentResource.loadRecord(recordType, id, requestScope);
    }
}
