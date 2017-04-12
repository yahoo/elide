/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

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
        // TODO: May want to consider wrapping these all up in an object that gets passed to the functions.
        //       then a single helper method implemented in the object can take place of our many private methods.
        RequestScope requestScope = (RequestScope) environment.getContext();
        Object source = environment.getSource();
        GraphQLType parent = environment.getParentType();
        GraphQLType output = environment.getFieldType();
        List<Field> fields = environment.getFields();
        Map<String, Object> args = environment.getArguments();

        String id = (String) args.get(ID_ARGUMENT);
        List<HashMap<Object, Object>> empty = Collections.singletonList(new HashMap<>());
        List<Map<String, Object>> data =
                (List<Map<String, Object>>) args.getOrDefault(RELATIONSHIP_ARGUMENT, empty);
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
                if (args.containsKey(RELATIONSHIP_ARGUMENT)) {
                    throw new WebApplicationException("Data argument invalid on FETCH operation.",
                            HttpStatus.SC_BAD_REQUEST);
                }
                return fetchObject(requestScope, source, output, fields, id);

            case ADD:
                return createObject(requestScope, source, output, fields, id, data);

            case DELETE:
                return deleteObject(requestScope, source, fields, id, data);

            case REPLACE:
                return updateObject(requestScope, source, output, fields, id, data);
        }
        throw new UnsupportedOperationException("Not implemented.");
    }

    private Object deleteObject(RequestScope requestScope, Object source, List<Field> fields, String id,
                                List<Map<String, Object>> data) {
        Set<Object> deleted = new HashSet<>();

        if (id != null && !id.isEmpty() && fields.size() > 1) {
            throw new WebApplicationException("Id argument specification with an additional list of id's to delete " +
                    "is unsupported", HttpStatus.SC_BAD_REQUEST);
        }

        EntityDictionary dictionary = requestScope.getDictionary();

        for (Field field : fields) {
            String loadType = field.getName();
            // TODO: This works at the root-level, but will it blow up in nested deletes?
            String idFieldName = dictionary.getIdFieldName(dictionary.getEntityClass(loadType));
            String loadId = field.getArguments().stream()
                    .filter(arg -> RELATIONSHIP_ARGUMENT.equals(arg.getName()))
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
                    .orElse(id);

            if (loadId == null) {
                throw new WebApplicationException("Did not specify id of object type to delete.",
                        HttpStatus.SC_BAD_REQUEST);
            }

            PersistentResource deleteObject = load(loadType, loadId, requestScope);

            if (deleteObject == null || deleteObject.getObject() == null) {
                throw new WebApplicationException("Attempted to delete non-existent id.", HttpStatus.SC_BAD_REQUEST);
            }

            deleteObject.deleteResource();
            deleted.add(deleteObject);
        }

        return deleted;
    }

    private Object updateObject(RequestScope requestScope, Object source,
                                GraphQLType output, List<Field> fields,
                                String id, List<Map<String, Object>> relationships) {
        assertOneField(fields);

        Field field = fields.get(0);
        EntityDictionary dictionary = requestScope.getDictionary();

        PersistentResource updateObject = load(field.getName(), id, requestScope);

        if (output instanceof GraphQLList) {
            GraphQLObjectType objectType = objectType = (GraphQLObjectType) ((GraphQLList) output).getWrappedType();
            List<PersistentResource> container = new ArrayList<>();
            for (Map<String, Object> input : relationships) {
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
                                    newRelationships.add(load(relName, value.toString(), requestScope));
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
        throw new IllegalStateException("Not sure what to create " + output.getName());
    }

    private Object createObject(RequestScope requestScope, Object source,
                                GraphQLType output, List<Field> fields,
                                String id, List<Map<String, Object>> relationships) {
        assertOneField(fields);

        Field field = fields.get(0);
        EntityDictionary dictionary = requestScope.getDictionary();

        GraphQLObjectType objectType;
        String uuid = UUID.randomUUID().toString();
        if (output instanceof GraphQLObjectType) {
            // No parent
            // TODO: These UUID's should not be random. They should be whatever id's are specified by the user so they
            // can be referenced throughout the document
            objectType = (GraphQLObjectType) output;
            return PersistentResource.createObject(null, dictionary.getEntityClass(objectType.getName()), requestScope,
                    uuid);

        } else if (output instanceof GraphQLList) {
            // Has parent
            objectType = (GraphQLObjectType) ((GraphQLList) output).getWrappedType();
            List<PersistentResource> container = new ArrayList<>();
            for (Map<String, Object> input : relationships) {
                Class<?> entityClass = dictionary.getEntityClass(objectType.getName());
                // TODO: See above comment about UUID's.
                PersistentResource toCreate = PersistentResource.createObject(entityClass, requestScope,
                        uuid);
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

    protected PersistentResource load(String type, String id, RequestScope requestScope) {
        Class<?> recordType = requestScope.getDictionary().getEntityClass(type);
        if (recordType == null) {
            throw new UnknownEntityException(type);
        }
        return PersistentResource.loadRecord(recordType, id, requestScope);
    }
}
