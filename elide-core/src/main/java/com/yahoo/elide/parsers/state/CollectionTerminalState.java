/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.parsers.state;

import com.yahoo.elide.core.HttpStatus;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.exceptions.ForbiddenAccessException;
import com.yahoo.elide.core.exceptions.InternalServerErrorException;
import com.yahoo.elide.core.exceptions.InvalidEntityBodyException;
import com.yahoo.elide.core.exceptions.InvalidObjectIdentifierException;
import com.yahoo.elide.core.exceptions.InvalidValueException;
import com.yahoo.elide.core.exceptions.UnknownEntityException;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.pagination.Pagination;
import com.yahoo.elide.core.sort.Sorting;
import com.yahoo.elide.jsonapi.JsonApiMapper;
import com.yahoo.elide.jsonapi.document.processors.DocumentProcessor;
import com.yahoo.elide.jsonapi.document.processors.IncludedProcessor;
import com.yahoo.elide.jsonapi.models.Data;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.yahoo.elide.jsonapi.models.Meta;
import com.yahoo.elide.jsonapi.models.Relationship;
import com.yahoo.elide.jsonapi.models.Resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;

import org.apache.commons.lang3.tuple.Pair;

import lombok.ToString;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.ws.rs.core.MultivaluedMap;

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
        Optional<MultivaluedMap<String, String>> queryParams = requestScope.getQueryParams();

        Set<PersistentResource> collection = getResourceCollection(requestScope);
        // Set data
        jsonApiDocument.setData(getData(collection));

        // Run include processor
        DocumentProcessor includedProcessor = new IncludedProcessor();
        includedProcessor.execute(jsonApiDocument, collection, queryParams);

        // Add pagination meta data
        Pagination pagination = requestScope.getPagination();
        if (!pagination.isEmpty()) {

            Map<String, Number> pageMetaData = new HashMap<>();
            pageMetaData.put("number", (pagination.getOffset() / pagination.getLimit()) + 1);
            pageMetaData.put("limit", pagination.getLimit());

            // Get total records if it has been requested and add to the page meta data
            if (pagination.isGenerateTotals()) {
                Long totalRecords = pagination.getPageTotals();
                pageMetaData.put("totalPages", totalRecords / pagination.getLimit()
                        + ((totalRecords % pagination.getLimit()) > 0 ? 1 : 0));
                pageMetaData.put("totalRecords", totalRecords);
            }

            Map<String, Object> allMetaData = new HashMap<>();
            allMetaData.put("page", pageMetaData);

            Meta meta = new Meta(allMetaData);
            jsonApiDocument.setMeta(meta);
        }

        JsonNode responseBody = requestScope.getMapper().toJsonObject(jsonApiDocument);

        return () -> Pair.of(HttpStatus.SC_OK, responseBody);
    }

    @Override
    public Supplier<Pair<Integer, JsonNode>> handlePost(StateContext state) {
        RequestScope requestScope = state.getRequestScope();
        JsonApiMapper mapper = requestScope.getMapper();

        newObject = createObject(requestScope);
        parent.ifPresent(persistentResource -> persistentResource.addRelation(relationName.get(), newObject));
        return () -> {
            JsonApiDocument returnDoc = new JsonApiDocument();
            returnDoc.setData(new Data(newObject.toResource()));
            JsonNode responseBody = mapper.getObjectMapper().convertValue(returnDoc, JsonNode.class);
            return Pair.of(HttpStatus.SC_CREATED, responseBody);
        };
    }

    private Set<PersistentResource> getResourceCollection(RequestScope requestScope) {
        final Set<PersistentResource> collection;
        // TODO: In case of join filters, apply pagination after getting records
        // instead of passing it to the datastore

        Optional<Pagination> pagination = Optional.ofNullable(requestScope.getPagination());
        Optional<Sorting> sorting = Optional.ofNullable(requestScope.getSorting());

        if (parent.isPresent()) {
            Optional<FilterExpression> filterExpression =
                    requestScope.getExpressionForRelation(parent.get(), relationName.get());

            collection = parent.get().getRelationCheckedFiltered(
                    relationName.get(),
                    filterExpression,
                    sorting,
                    pagination);
        } else {
            Optional<FilterExpression> filterExpression = requestScope.getLoadFilterExpression(entityClass);

            collection = PersistentResource.loadRecords(
                entityClass,
                new ArrayList<>(), //Empty list of IDs
                filterExpression,
                sorting,
                pagination,
                requestScope);
        }

        return collection;
    }

    private Data getData(Set<PersistentResource> collection) {
        Preconditions.checkNotNull(collection);
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
        Class<?> newObjectClass = requestScope.getDictionary().getEntityClass(resource.getType());

        if (newObjectClass == null) {
            throw new UnknownEntityException("Entity " + resource.getType() + " not found");
        }
        if (!entityClass.isAssignableFrom(newObjectClass)) {
            throw new InvalidValueException("Cannot assign value of type: " + resource.getType()
                    + " to type: " + entityClass);
        }

        PersistentResource pResource = PersistentResource.createObject(
                parent.orElse(null), newObjectClass, requestScope, Optional.ofNullable(id));

        Map<String, Object> attributes = resource.getAttributes();
        if (attributes != null) {
            for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                String fieldName = entry.getKey();
                Object val = entry.getValue();
                pResource.updateAttribute(fieldName, val);
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
}
