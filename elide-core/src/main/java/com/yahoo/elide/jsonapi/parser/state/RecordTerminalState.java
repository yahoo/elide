/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.jsonapi.parser.state;

import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.exceptions.HttpStatus;
import com.yahoo.elide.core.exceptions.InvalidEntityBodyException;
import com.yahoo.elide.core.exceptions.InvalidOperationException;
import com.yahoo.elide.jsonapi.models.Data;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.yahoo.elide.jsonapi.models.Relationship;
import com.yahoo.elide.jsonapi.models.Resource;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.tuple.Pair;

import lombok.ToString;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Record Found State.
 */
@ToString
public class RecordTerminalState extends BaseState {
    private final PersistentResource record;
    private final Optional<CollectionTerminalState> collectionTerminalState;

    public RecordTerminalState(PersistentResource record) {
        this(record, null);
    }

    // This constructor is to handle ToOne collection as a Record Terminal State
    public RecordTerminalState(PersistentResource record, CollectionTerminalState collectionTerminalState) {
        this.record = record;
        this.collectionTerminalState = Optional.ofNullable(collectionTerminalState);
    }

    @Override
    public Supplier<Pair<Integer, JsonApiDocument>> handleGet(StateContext state) {
        ObjectMapper mapper = state.getRequestScope().getMapper().getObjectMapper();
        return () -> Pair.of(HttpStatus.SC_OK, getResponseBody(record, state.getRequestScope()));
    }

    @Override
    public Supplier<Pair<Integer, JsonApiDocument>> handlePost(StateContext state) {
        return collectionTerminalState
                .orElseThrow(() -> new InvalidOperationException("Cannot POST to a record."))
                .handlePost(state);
    }

    @Override
    public Supplier<Pair<Integer, JsonApiDocument>> handlePatch(StateContext state) {
        JsonApiDocument jsonApiDocument = state.getJsonApiDocument();

        Data<Resource> data = jsonApiDocument.getData();

        if (data == null) {
            throw new InvalidEntityBodyException("Expected data but found null");
        }

        if (!data.isToOne()) {
            throw new InvalidEntityBodyException("Expected single element but found list");
        }

        Resource resource = data.getSingleValue();
        if (!record.matchesId(resource.getId())) {
            throw new InvalidEntityBodyException("Id in response body does not match requested id to update from path");
        }

        patch(resource, state.getRequestScope());
        return constructPatchResponse(record, state);
    }

    @Override
    public Supplier<Pair<Integer, JsonApiDocument>> handleDelete(StateContext state) {
        record.deleteResource();
        return () -> Pair.of(HttpStatus.SC_NO_CONTENT, null);
    }

    private boolean patch(Resource resource, RequestScope requestScope) {
        boolean isUpdated = false;

        // Update attributes first
        Map<String, Object> attributes = resource.getAttributes();
        if (attributes != null) {
            for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                String fieldName = entry.getKey();
                Object newVal = entry.getValue();
                isUpdated |= record.updateAttribute(fieldName, newVal);
            }
        }

        // Relations next
        Map<String, Relationship> relationships = resource.getRelationships();
        if (relationships != null) {
            for (Map.Entry<String, Relationship> entry : relationships.entrySet()) {
                String fieldName = entry.getKey();
                Relationship relationship = entry.getValue();
                Set<PersistentResource> resources = (relationship == null)
                                                    ? null
                                                    : relationship.toPersistentResources(requestScope);
                isUpdated |= record.updateRelation(fieldName, resources);
            }
        }

        return isUpdated;
    }
}
