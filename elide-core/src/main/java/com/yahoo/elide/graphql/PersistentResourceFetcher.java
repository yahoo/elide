/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.exceptions.InvalidAttributeException;
import com.yahoo.elide.core.exceptions.UnknownEntityException;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.yahoo.elide.graphql.ModelBuilder.ID_ARGUMENT;
import static com.yahoo.elide.graphql.ModelBuilder.OPERATION_ARGUMENT;
import static com.yahoo.elide.graphql.ModelBuilder.RELATIONSHIP_ARGUMENT;

/**
 * Interacts with {@link PersistentResource} to fetch data for GraphQL.
 */
@Slf4j
public class PersistentResourceFetcher implements DataFetcher {

    @Override
    public Object get(DataFetchingEnvironment environment) {
        RequestScope requestScope = (RequestScope) environment.getContext();
        Object source = environment.getSource();
        GraphQLType parent = environment.getParentType();
        GraphQLType output = environment.getFieldType();
        List<Field> fields = environment.getFields();
        Map<String, Object> args = environment.getArguments();

        String id = (String) args.get(ID_ARGUMENT);
        List<Map<String, Object>> relationships = (List<Map<String, Object>>) args.getOrDefault(RELATIONSHIP_ARGUMENT, Collections.singletonList(new HashMap<>()));
        RelationshipOp operation = (RelationshipOp) args.getOrDefault(OPERATION_ARGUMENT, RelationshipOp.FETCH);

        if (log.isDebugEnabled()) {
            List<String> requestedFields = fields.stream().map(field -> {
                List<Field> children = field.getSelectionSet() != null
                        ? (List) field.getSelectionSet().getChildren()
                        : new ArrayList<>();
                return field.getName() + (
                        children.size() > 0
                                ? "(" + children.stream().map(Field::getName).collect(Collectors.toList()) + ")"
                                : ""
                );
            }).collect(Collectors.toList());
            log.debug("{} {} fields for {} with parent {}<{}>",
                    operation, requestedFields, source, parent.getClass().getSimpleName(), parent.getName());
        }


        switch (operation) {
            case FETCH:
                return fetchObject(requestScope, source, output, fields, id);

            case ADD:
                return createObject(requestScope, source, output, fields, id, relationships);

            case DELETE:

            case REPLACE:
        }
        throw new UnsupportedOperationException("Not implemented.");
    }

    private Object createObject(RequestScope requestScope, Object source,
                                GraphQLType output, List<Field> fields,
                                String id, List<Map<String, Object>> relationships) {
        assertOneField(fields);

        Field field = fields.get(0);
        EntityDictionary dictionary = requestScope.getDictionary();
        DataStoreTransaction transaction = requestScope.getTransaction();

        GraphQLObjectType objectType;
        if (output instanceof GraphQLObjectType) {
            objectType = (GraphQLObjectType) output;
            return transaction.createNewObject(dictionary.getEntityClass(objectType.getName()));

        } else if (output instanceof GraphQLList) {
            objectType = (GraphQLObjectType) ((GraphQLList) output).getWrappedType();
            List<PersistentResource> container = new ArrayList<>();
            for (Map<String, Object> input : relationships) {
                Class<?> entityClass = dictionary.getEntityClass(objectType.getName());
                PersistentResource toCreate = PersistentResource.createObject(entityClass, requestScope, UUID.randomUUID().toString());
                input.entrySet().stream()
                        .filter(entry -> dictionary.isAttribute(entityClass, entry.getKey()))
                        .forEach(entry -> toCreate.updateAttribute(entry.getKey(), entry.getValue()));
                container.add(toCreate);
            }
            return container;
        }
        throw new IllegalStateException("Not sure what to create " + output.getName());
    }

    private Object fetchObject(RequestScope requestScope, Object source,
                               GraphQLType output, List<Field> fields, String id) {
        assertOneField(fields);

        if (output instanceof GraphQLList) {
            GraphQLObjectType type = (GraphQLObjectType) ((GraphQLList) output).getWrappedType();
            String entityType = type.getName();
            if (id == null) {
                return loadCollectionOf(entityType, requestScope);
            }
            return Collections.singletonList(load(entityType, id, requestScope));

        } else if (output instanceof GraphQLObjectType) {
            return load(output.getName(), id, requestScope);

        } else if (output instanceof GraphQLScalarType) {
            return fetchProperty((PersistentResource) source, fields, requestScope);

        }

        throw new IllegalStateException("WTF is a " + output.getClass().getName() + " mate?");
    }

    protected Object fetchProperty(PersistentResource source, List<Field> fields, RequestScope requestScope) {
        assertOneField(fields);

        EntityDictionary dictionary = requestScope.getDictionary();
        Class<?> sourceClass = source.getResourceClass();
        Field field = fields.get(0);

        String fieldName = field.getName();
        if (dictionary.isAttribute(sourceClass, fieldName)) {
            return source.getAttribute(fieldName);
        } else {
            log.debug("Tried to fetch property off of invalid loaded object.");
            throw new InvalidAttributeException(fieldName, source.getType());
        }
    }

    private void assertOneField(List<Field> fields) {
        if (fields.size() != 1) {
            throw new WebApplicationException("Resource fetcher contract has changed");
        }
    }

    protected Collection<Object> loadCollectionOf(String type, RequestScope requestScope) {
        Class recordType = (Class) requestScope.getDictionary().getEntityClass(type);
        return PersistentResource.loadRecords(recordType, requestScope);
    }

    protected Object load(String type, String id, RequestScope requestScope) {
        Class<?> recordType = requestScope.getDictionary().getEntityClass(type);
        if (recordType == null) {
            throw new UnknownEntityException(type);
        }
        return PersistentResource.loadRecord(recordType, id, requestScope);
    }
}
