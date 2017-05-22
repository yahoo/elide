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
import java.util.stream.Stream;

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
    private final Class entityClass;
    private final PersistentResource resource;
    private final String idFieldName;

    public UpdateOperation(Environment environment) {
        field = environment.field;
        requestScope = environment.requestScope;
        dictionary = requestScope.getDictionary();
        id = environment.id.orElse(null);
        source = environment.source;
        outputType = environment.outputType;
        requestData = environment.data;
        isRoot = !(source instanceof PersistentResource);
        resource = (isRoot) ? null : (PersistentResource) source;
        entityClass = (isRoot)
                ? dictionary.getEntityClass(field.getName())
                : dictionary.getParameterizedType(((PersistentResource) source).getObject(), field.getName());
        idFieldName = dictionary.getIdFieldName(entityClass);
    }

    public Object execute() {
        Collection<PersistentResource> objectsToUpdate = fetchResourceCollection();

        if (outputType instanceof GraphQLList) {
            GraphQLObjectType objectType = (GraphQLObjectType) ((GraphQLList) outputType).getWrappedType();
            Set<PersistentResource> container = new HashSet<>();
            boolean replaceRoot = checkIsReplacingRoot();
            for (Map<String, Object> input : requestData) {
                // Find the id's that we're trying to update
                Set<String> objectIds = input.entrySet().stream()
                        .filter(entry -> ARGUMENT_ID.equalsIgnoreCase(entry.getKey()))
                        .map(entry -> (String) entry.getValue())
                        .collect(Collectors.toSet());
                if (replaceRoot) {
                    // Delete all the things we don't want to keep...
                    PersistentResource.loadRecords(entityClass, requestScope).stream()
                            .filter(p -> !objectIds.contains(p.getId()))
                            .forEach(PersistentResource::deleteResource);
                }
                processActions(objectsToUpdate, input);
                container.addAll(objectsToUpdate);
            }
            return container;
        }
        throw new IllegalStateException("Not sure what to create " + outputType.getName());
    }

    private void processActions(Collection<PersistentResource> objectsToUpdate, Map<String, Object> input) {
        String entityId = (String) input.get(idFieldName);
        boolean foundEntityId = entityId != null;
        if (!foundEntityId) {
            entityId = UUID.randomUUID().toString();
        }
        final String entityIdCapture = entityId;
        input.entrySet().stream()
                .forEach(entry -> {
                    String fieldName = entry.getKey();
                    Object value = entry.getValue();
                    // TODO: Should be able to reuse this logic for non-rootable objects as well. Perhaps store parent?
                    // This happens when we didn't load the object (i.e. not in our datastore).
                    if ((isRoot && !foundEntityId)
                            || (objectsToUpdate.stream().noneMatch(p -> p.matchesId(entityIdCapture)))) {
                        PersistentResource result =
                                PersistentResource.createObject(null, entityClass, requestScope, entityIdCapture);
                        objectsToUpdate.add(result);
                    }
                    Stream<PersistentResource> objects = objectsToUpdate.stream()
                            .filter(p -> (entityIdCapture == null) ? true : p.matchesId(entityIdCapture));
                    if (dictionary.isAttribute(entityClass, fieldName)) {
                        // Update attribute value
                        objects.forEach(o -> o.updateAttribute(fieldName, value));
                    } else if (dictionary.isRelation(entityClass, fieldName)) {
                        // Replace relationship
                        String relName = dictionary.getJsonAliasFor(
                                dictionary.getParameterizedType(entityClass, fieldName));
                        Set<PersistentResource> newRelationships = new HashSet<>();
                        if (value != null) {
                            // TODO: For a to-many relationship this would be a list... Need to handle that case
                            newRelationships.add(PersistentResourceFetcher.load(relName, value.toString(), requestScope));
                        }
                        objects.forEach(o -> o.updateRelation(fieldName, newRelationships));
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
                        objects.forEach(o -> o.setId(value.toString()));
                    } else {
                        throw new WebApplicationException("Attempt to update unknown field: '"
                                + fieldName + "'", HttpStatus.SC_BAD_REQUEST);
                    }
                });
    }

    /**
     * Check if request expects to replace a root collection.
     *
     * If all of the request bodies are applied to a specified id then root is being replaced. However,
     * if _any_ of the operations apply to the entire dataset as a whole it will be applied in order.
     *
     * @return True if replacing root, false otherwise.
     */
    private boolean checkIsReplacingRoot() {
        if (!isRoot) {
            return false;
        }

        for (Map<String, Object> input : requestData) {
            boolean isAppliedToId = false;
            for (String fieldName : input.keySet()) {
                if (fieldName.equals(idFieldName)) {
                    isAppliedToId = true;
                    break;
                }
            }
            if (!isAppliedToId) {
                return false;
            }
        }

        return true;
    }

    private Collection<PersistentResource> fetchResourceCollection() {
        if (isRoot) {
            return new ArrayList<>(fetchRootableResourceCollection());
        }
        return new ArrayList<>(fetchNonRootableResourceCollection());
    }

    private Collection<PersistentResource> fetchNonRootableResourceCollection() {
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
        return dataId
                .map(dId -> Collections.singleton(resource.getRelation(field.getName(), dId)))
                .orElseGet(() -> resource.getRelationCheckedFiltered(field.getName()));
    }

    private Collection<PersistentResource> fetchRootableResourceCollection() {
        return (id != null)
                ? Collections.singleton(PersistentResourceFetcher.load(field.getName(), id, requestScope))
                : PersistentResourceFetcher.loadCollectionOf(field.getName(), requestScope);
    }
}
