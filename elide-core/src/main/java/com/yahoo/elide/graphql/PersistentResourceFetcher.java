/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

import com.google.common.collect.ImmutableList;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.HttpStatus;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.exceptions.InvalidAttributeException;
import com.yahoo.elide.core.exceptions.UnknownEntityException;
import graphql.language.Argument;
import graphql.language.Field;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLType;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.WebApplicationException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.yahoo.elide.graphql.ModelBuilder.ARGUMENT_DATA;
import static com.yahoo.elide.graphql.ModelBuilder.ARGUMENT_ID;
import static com.yahoo.elide.graphql.ModelBuilder.ARGUMENT_OPERATION;

/**
 * Interacts with {@link PersistentResource} to fetch data for GraphQL.
 */
@Slf4j
public class PersistentResourceFetcher implements DataFetcher {

    public static final List<Map<Object, Object>> EMPTY_DATA = Collections.singletonList(new HashMap<>());

    private static class Environment {
        public final RequestScope requestScope;
        public final String id;
        public final Object source;
        public final GraphQLType parentType;
        public final GraphQLType outputType;
        public final List<Field> fields;
        public final List<Map<String, Object>> data;

        public Environment(DataFetchingEnvironment environment) {
            requestScope = (RequestScope) environment.getContext();
            source = environment.getSource();
            parentType = environment.getParentType();
            outputType = environment.getFieldType();
            fields = ImmutableList.copyOf(environment.getFields());

            Map<String, Object> args = environment.getArguments();
            id = (String) args.get(ARGUMENT_ID);
            data = ImmutableList.copyOf((List<Map<String, Object>>) args.getOrDefault(ARGUMENT_DATA, EMPTY_DATA));
        }
    }

    @Override
    public Object get(DataFetchingEnvironment environment) {
        Map<String, Object> args = environment.getArguments();
        Environment context = new Environment(environment);
        RelationshipOp operation = (RelationshipOp) args.getOrDefault(OPERATION_ARGUMENT, RelationshipOp.FETCH);

        if (log.isDebugEnabled()) {
            logContext(operation, context);
        }

        switch (operation) {
            case FETCH:
                if (context.data != null && context.data.stream().filter(m -> !m.isEmpty()).count() > 0) {
                    throw new WebApplicationException("Data argument invalid on FETCH operation.",
                            HttpStatus.SC_BAD_REQUEST);
                }
                return fetchObject(context);

            case ADD:
                return createObject(context);

            case DELETE:
                return deleteObject(context);

            case REPLACE:
                return updateObject(context);
        }

        throw new UnsupportedOperationException("Not implemented.");
    }

    private void logContext(RelationshipOp operation, Environment environment) {
        List<String> requestedFields = environment.fields.stream().map(field -> {
            List<Field> children = field.getSelectionSet() != null
                    ? (List) field.getSelectionSet().getChildren()
                    : new ArrayList<>();
            return field.getName() + (
                    children.size() > 0
                            ? "(" + children.stream().map(Field::getName).collect(Collectors.toList()) + ")"
                            : ""
            );
        }).collect(Collectors.toList());
        GraphQLType parent = environment.parentType;
        log.debug("{} {} fields for {} with parent {}<{}>",
                operation, requestedFields, environment.source, parent.getClass().getSimpleName(), parent.getName());
    }

    private Object deleteObject(Environment request) {
        Set<Object> deleted = new HashSet<>();

        if (request.id != null && !request.id.isEmpty() && request.fields.size() > 1) {
            throw new WebApplicationException("Id argument specification with an additional list of id's to delete "
                    + "is unsupported", HttpStatus.SC_BAD_REQUEST);
        }

        EntityDictionary dictionary = request.requestScope.getDictionary();

        for (Field field : request.fields) {
            String loadType = field.getName();
            // TODO: This works at the root-level, but will it blow up in nested deletes?
            String idFieldName = dictionary.getIdFieldName(dictionary.getEntityClass(loadType));
            String loadId = field.getArguments().stream()
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
                    .orElse(request.id);

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
        }

        return deleted;
    }

    private Object updateObject(Environment request) {
        assertOneField(request.fields);

        Field field = request.fields.get(0);
        EntityDictionary dictionary = request.requestScope.getDictionary();

        PersistentResource updateObject = load(field.getName(), request.id, request.requestScope);

        if (request.outputType instanceof GraphQLList) {
            GraphQLObjectType objectType = (GraphQLObjectType) ((GraphQLList) request.outputType).getWrappedType();
            List<PersistentResource> container = new ArrayList<>();
            for (Map<String, Object> input : request.data) {
                Class<?> entityClass = dictionary.getEntityClass(objectType.getName());
                input.entrySet().stream()
                        .forEach(entry -> {
                            String fieldName = entry.getKey();
                            Object value = entry.getValue();
                            if (dictionary.isAttribute(entityClass, fieldName)) {
                                // Update attribute value
                                updateObject.updateAttribute(fieldName, value);
                            } else if (dictionary.isRelation(entityClass, fieldName)) {
                                // Replace relationship
                                String relName = dictionary.getJsonAliasFor(
                                        dictionary.getParameterizedType(entityClass, fieldName));
                                Set<PersistentResource> newRelationships = new HashSet<>();
                                if (value != null) {
                                    // TODO: For a to-many relationship this would be a list... Need to handle that case
                                    newRelationships.add(load(relName, value.toString(), request.requestScope));
                                }
                                updateObject.updateRelation(fieldName, newRelationships);
                            } else if (fieldName.equals(dictionary.getIdFieldName(entityClass))) {
                                // Set id value
                                if (value == null) {
                                    throw new WebApplicationException("Cannot set object identifier to null",
                                            HttpStatus.SC_BAD_REQUEST);
                                }
                                updateObject.setId(value.toString());
                            } else {
                                throw new WebApplicationException("Attempt to update unknown field: '"
                                        + fieldName + "'", HttpStatus.SC_BAD_REQUEST);
                            }
                        });
                container.add(updateObject);
            }
            return container;
        }
        throw new IllegalStateException("Not sure what to create " + request.outputType.getName());
    }

    private Object createObject(Environment request) {
        assertOneField(request.fields);

        Field field = request.fields.get(0);
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
                PersistentResource toCreate = PersistentResource.createObject(entityClass, request.requestScope,
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
        if (request.outputType instanceof GraphQLList) {
            GraphQLObjectType type = (GraphQLObjectType) ((GraphQLList) request.outputType).getWrappedType();
            String entityType = type.getName();
            if (request.id == null) {
                return loadCollectionOf(entityType, request.requestScope);
            }
            return Collections.singletonList(load(entityType, request.id, request.requestScope));

        } else if (request.outputType instanceof GraphQLObjectType) {
            return load(request.outputType.getName(), request.id, request.requestScope);

        } else if (request.outputType instanceof GraphQLScalarType) {
            return fetchProperty(request);

        }

        throw new IllegalStateException("WTF is a " + request.outputType.getClass().getName() + " mate?");
    }

    protected Object fetchProperty(Environment request) {
        assertOneField(request.fields);

        EntityDictionary dictionary = request.requestScope.getDictionary();
        PersistentResource resource = (PersistentResource) request.source;
        Class<?> sourceClass = resource.getResourceClass();
        Field field = request.fields.get(0);

        String fieldName = field.getName();
        if (dictionary.isAttribute(sourceClass, fieldName)) {
            return resource.getAttribute(fieldName);
        } else {
            log.debug("Tried to fetch property off of invalid loaded object.");
            throw new InvalidAttributeException(fieldName, resource.getType());
        }
    }

    private void assertOneField(List<Field> fields) {
        if (fields.size() != 1) {
            throw new WebApplicationException("Resource fetcher contract has changed");
        }
    }

    protected Collection<PersistentResource> loadCollectionOf(String type, RequestScope requestScope) {
        Class recordType = (Class) requestScope.getDictionary().getEntityClass(type);
        return PersistentResource.loadRecords(recordType, requestScope);
    }

    protected PersistentResource load(String type, String id, RequestScope requestScope) {
        Class<?> recordType = requestScope.getDictionary().getEntityClass(type);
        if (recordType == null) {
            throw new UnknownEntityException(type);
        }
        return PersistentResource.loadRecord(recordType, id, requestScope);
    }
}
