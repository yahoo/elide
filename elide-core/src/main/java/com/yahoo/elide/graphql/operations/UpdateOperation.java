/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql.operations;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.HttpStatus;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.graphql.Environment;
import com.yahoo.elide.graphql.PersistentResourceFetcher;
import graphql.language.Field;
import graphql.language.ObjectValue;
import graphql.language.StringValue;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLType;

import javax.ws.rs.WebApplicationException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.yahoo.elide.graphql.ModelBuilder.ARGUMENT_DATA;
import static com.yahoo.elide.graphql.ModelBuilder.ARGUMENT_ID;

public class UpdateOperation {
    private final Field field;
    private final RequestScope requestScope;
    private final EntityDictionary dictionary;
    private final String id;
    private final Object source;
    private final GraphQLType outputType;
    private final List<Map<String, Object>> requestData;
    private final boolean isRoot;

    public UpdateOperation(Environment environment) {
        field = environment.field;
        requestScope = environment.requestScope;
        dictionary = requestScope.getDictionary();
        id = environment.id.orElse(null);
        source = environment.source;
        outputType = environment.outputType;
        requestData = environment.data;
        isRoot = !(source instanceof PersistentResource);
    }

    public Object execute() {
        Collection<PersistentResource> objectsToUpdate;
        Class entityClass;

        if (!isRoot) {
            PersistentResource resource = (PersistentResource) source;
            entityClass = dictionary.getParameterizedType(resource.getObject(), field.getName());
            // NOTE: Currently only handles _single_ object update
            Optional<String> dataId = (id != null) ? Optional.of(id) : field.getArguments().stream()
                    .filter(arg -> ARGUMENT_DATA.equalsIgnoreCase(arg.getName()))
                    .findFirst()
                    .map(data -> {
                        // TODO: Handle lists?
                        if (data.getValue() instanceof ObjectValue) {
                            ObjectValue object = (ObjectValue) data.getValue();
                            return object.getObjectFields().stream()
                                    .filter(f -> "id".equalsIgnoreCase(f.getName()))
                                    .findFirst()
                                    .map(f -> ((StringValue) f.getValue()).getValue())
                                    .orElseGet(() -> {
                                        String uuid = UUID.randomUUID().toString();
                                        PersistentResource.createObject(resource, entityClass, requestScope, uuid);
                                        return uuid;
                                    });
                        }
                        return null;
                    });
            objectsToUpdate = dataId
                    .map(dId -> Collections.singleton(resource.getRelation(field.getName(), dId)))
                    .orElseGet(() -> resource.getRelationCheckedFiltered(field.getName()));
        } else {
            objectsToUpdate = (id != null)
                    ? Collections.singleton(PersistentResourceFetcher.load(field.getName(), id, requestScope))
                    : PersistentResourceFetcher.loadCollectionOf(field.getName(), requestScope);
            entityClass = dictionary.getEntityClass(field.getName());
        }

        if (outputType instanceof GraphQLList) {
            GraphQLObjectType objectType = (GraphQLObjectType) ((GraphQLList) outputType).getWrappedType();
            List<PersistentResource> container = new ArrayList<>();
            for (Map<String, Object> input : requestData) {
                // Find the id's that we're trying to update
                Set<String> objectIds = input.entrySet().stream()
                        .filter(entry -> ARGUMENT_ID.equalsIgnoreCase(entry.getKey()))
                        .map(entry -> (String) entry.getValue())
                        .collect(Collectors.toSet());
                if (isRoot) {
                    // Delete all the things we don't want to keep...
                    PersistentResource.loadRecords(entityClass, requestScope).stream()
                            .filter(p -> !objectIds.contains(p.getId()))
                            .forEach(PersistentResource::deleteResource);
                }
                input.entrySet().stream()
                        .forEach(entry -> {
                            String fieldName = entry.getKey();
                            Object value = entry.getValue();
                            if (dictionary.isAttribute(entityClass, fieldName)) {
                                // Update attribute value
                                objectsToUpdate.stream().forEach(o -> o.updateAttribute(fieldName, value));
                            } else if (dictionary.isRelation(entityClass, fieldName)) {
                                // Replace relationship
                                String relName = dictionary.getJsonAliasFor(
                                        dictionary.getParameterizedType(entityClass, fieldName));
                                Set<PersistentResource> newRelationships = new HashSet<>();
                                if (value != null) {
                                    // TODO: For a to-many relationship this would be a list... Need to handle that case
                                    newRelationships.add(PersistentResourceFetcher.load(relName, value.toString(), requestScope));
                                }
                                objectsToUpdate.stream().forEach(o -> o.updateRelation(fieldName, newRelationships));
                            } else if (fieldName.equals(dictionary.getIdFieldName(entityClass))) {
                                if (id == null) {
                                    // TODO: In case of list, should ensure we're modifying the correct object instance
                                    return; // Not updating id
                                }
                                // Set id value
                                if (value == null) {
                                    throw new WebApplicationException("Cannot set object identifier to null",
                                            HttpStatus.SC_BAD_REQUEST);
                                }
                                objectsToUpdate.stream().forEach(o -> o.setId(value.toString()));
                            } else {
                                throw new WebApplicationException("Attempt to update unknown field: '"
                                        + fieldName + "'", HttpStatus.SC_BAD_REQUEST);
                            }
                        });
                container.addAll(objectsToUpdate);
            }
            return container;
        }
        throw new IllegalStateException("Not sure what to create " + outputType.getName());
    }
}
