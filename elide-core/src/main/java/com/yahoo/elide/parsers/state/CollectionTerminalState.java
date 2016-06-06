/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.parsers.state;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.yahoo.elide.core.HttpStatus;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.exceptions.ForbiddenAccessException;
import com.yahoo.elide.core.exceptions.InternalServerErrorException;
import com.yahoo.elide.core.exceptions.InvalidEntityBodyException;
import com.yahoo.elide.core.exceptions.InvalidObjectIdentifierException;
import com.yahoo.elide.jsonapi.JsonApiMapper;
import com.yahoo.elide.jsonapi.document.processors.DocumentProcessor;
import com.yahoo.elide.jsonapi.document.processors.IncludedProcessor;
import com.yahoo.elide.jsonapi.models.Data;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.yahoo.elide.jsonapi.models.Relationship;
import com.yahoo.elide.jsonapi.models.Resource;
import com.yahoo.elide.security.User;
import lombok.ToString;
import org.apache.commons.lang3.tuple.Pair;

import javax.ws.rs.core.MultivaluedMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Collection State.
 */
@ToString
public class CollectionTerminalState extends BaseState {
    private final Optional<PersistentResource> parent;
    private final Optional<String> relationName;
    private final Class<?> entityClass;
    private PersistentResource newObject;

    public CollectionTerminalState(Class<?> entityClass, Optional<PersistentResource> parent,
                                   Optional<String> relationName) {
        this.parent = parent;
        this.relationName = relationName;
        this.entityClass = entityClass;
    }

    @Override
    public Supplier<Pair<Integer, JsonNode>> handleGet(StateContext state) {
        JsonApiDocument jsonApiDocument = new JsonApiDocument();
        RequestScope requestScope = state.getRequestScope();
        ObjectMapper mapper = requestScope.getMapper().getObjectMapper();
        Optional<MultivaluedMap<String, String>> queryParams = requestScope.getQueryParams();

        Set<PersistentResource> collection = getResourceCollection(requestScope);
        // Set data
        jsonApiDocument.setData(getData(requestScope, collection));

        // Run include processor
        DocumentProcessor includedProcessor = new IncludedProcessor();
        includedProcessor.execute(jsonApiDocument, collection, queryParams);
        JsonNode responseBody = mapper.convertValue(jsonApiDocument, JsonNode.class);
        return () -> Pair.of(HttpStatus.SC_OK, responseBody);
    }

    @Override
    public Supplier<Pair<Integer, JsonNode>> handlePost(StateContext state) {
        RequestScope requestScope = state.getRequestScope();
        JsonApiMapper mapper = requestScope.getMapper();

        newObject = createObject(requestScope);
        if (parent.isPresent()) {
            parent.get().addRelation(relationName.get(), newObject);
        }
        return () -> {
            JsonApiDocument returnDoc = new JsonApiDocument();
            returnDoc.setData(new Data(newObject.toResource()));
            JsonNode responseBody = mapper.getObjectMapper().convertValue(returnDoc, JsonNode.class);
            return Pair.of(HttpStatus.SC_CREATED, responseBody);
        };
    }

    private Set<PersistentResource> getResourceCollection(RequestScope requestScope) {
        final Set<PersistentResource> collection;
        final boolean hasSortingOrPagination = !requestScope.getPagination().isDefaultInstance()
                || !requestScope.getSorting().isDefaultInstance();
        if (parent.isPresent()) {
            if (hasSortingOrPagination) {
                collection = parent.get().getRelationCheckedFilteredWithSortingAndPagination(relationName.get());
            } else {
                collection = parent.get().getRelationCheckedFiltered(relationName.get());
            }
        } else {
            if (hasSortingOrPagination) {
                collection = (Set) PersistentResource.loadRecordsWithSortingAndPagination(entityClass, requestScope);
            } else {
                collection = (Set) PersistentResource.loadRecords(entityClass, requestScope);
            }
        }

        return collection;
    }

    private Data getData(RequestScope requestScope, Set<PersistentResource> collection) {
        User user = requestScope.getUser();
        Preconditions.checkNotNull(collection);
        Preconditions.checkNotNull(user);

        List<Resource> resources = collection.stream().map(PersistentResource::toResource).collect(Collectors.toList());

        return new Data<>(resources);
    }

    private PersistentResource createObject(RequestScope requestScope)
        throws ForbiddenAccessException, InvalidObjectIdentifierException {
        JsonApiDocument doc = requestScope.getJsonApiDocument();
        JsonApiMapper mapper = requestScope.getMapper();

        Data<Resource> data = doc.getData();
        Collection<Resource> resources = data.get();

        Resource resource = (resources.size() == 1) ? resources.iterator().next() : null;
        if (resource == null) {
            try {
                throw new InvalidEntityBodyException(mapper.writeJsonApiDocument(doc));
            } catch (JsonProcessingException e) {
                throw new InternalServerErrorException(e);
            }
        }

        String id = resource.getId();
        PersistentResource pResource;
        if (parent.isPresent()) {
            pResource = PersistentResource.createObject(parent.get(), entityClass, requestScope, id);
        } else {
            pResource = PersistentResource.createObject(entityClass, requestScope, id);
        }

        assignId(pResource, id);

        Map<String, Object> attributes = resource.getAttributes();
        if (attributes != null) {
            for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                String key = entry.getKey();
                Object val = entry.getValue();
                pResource.updateAttribute(key, val);
            }
        }

        Map<String, Relationship> relationships = resource.getRelationships();
        if (relationships != null) {
            for (Map.Entry<String, Relationship> entry : relationships.entrySet()) {
                String fieldName = entry.getKey();
                Relationship relationship = entry.getValue();
                Set<PersistentResource> resourceSet = (relationship == null)
                                                    ? null
                                                    : relationship.toPersistentResources(requestScope);
                pResource.updateRelation(fieldName, resourceSet);
            }
        }

        return pResource;
    }

    /**
     * Assign provided id if id field is not generated.
     *
     * @param persistentResource resource
     * @param id resource id
     */
    private void assignId(PersistentResource persistentResource, String id) {

        //If id field is not a `@GeneratedValue` persist the provided id
        if (!persistentResource.isIdGenerated()) {
            if (id != null && !id.isEmpty()) {
                persistentResource.setId(id);
            } else {
                //If expecting id to persist and id is not present, throw exception
                throw new ForbiddenAccessException("No id provided, cannot persist " + persistentResource.getObject());
            }
        }
    }
}
