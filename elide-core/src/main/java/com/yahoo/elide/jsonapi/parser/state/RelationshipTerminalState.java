/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.jsonapi.parser.state;

import com.yahoo.elide.annotation.UpdatePermission;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.dictionary.RelationshipType;
import com.yahoo.elide.core.exceptions.ForbiddenAccessException;
import com.yahoo.elide.core.exceptions.HttpStatus;
import com.yahoo.elide.core.exceptions.InvalidEntityBodyException;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.jsonapi.document.processors.DocumentProcessor;
import com.yahoo.elide.jsonapi.document.processors.IncludedProcessor;
import com.yahoo.elide.jsonapi.document.processors.PopulateMetaProcessor;
import com.yahoo.elide.jsonapi.models.Data;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.yahoo.elide.jsonapi.models.Relationship;
import com.yahoo.elide.jsonapi.models.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

import javax.ws.rs.core.MultivaluedMap;

/**
 * State to handle relationships.
 */
public class RelationshipTerminalState extends BaseState {
    private final PersistentResource record;
    private final RelationshipType relationshipType;
    private final String relationshipName;

    /* The projection which loaded the resource which owns the relationship */
    private final EntityProjection parentProjection;

    public RelationshipTerminalState(PersistentResource record, String relationshipName,
                                     EntityProjection parentProjection) {
        this.record = record;
        this.parentProjection = parentProjection;

        this.relationshipType = record.getRelationshipType(relationshipName);
        this.relationshipName = relationshipName;
    }

    @Override
    public Supplier<Pair<Integer, JsonApiDocument>> handleGet(StateContext state) {
        JsonApiDocument doc = new JsonApiDocument();
        RequestScope requestScope = state.getRequestScope();
        MultivaluedMap<String, String> queryParams = requestScope.getQueryParams();

        Map<String, Relationship> relationships = record.toResource(parentProjection).getRelationships();
        if (relationships != null && relationships.containsKey(relationshipName)) {
            Relationship relationship = relationships.get(relationshipName);

            // Handle valid relationship

            // Set data
            Data<Resource> data = relationship.getData();
            doc.setData(data);

            // Run include processor
            DocumentProcessor includedProcessor = new IncludedProcessor();
            includedProcessor.execute(doc, requestScope, record, queryParams);

            return () -> Pair.of(HttpStatus.SC_OK, doc);
        }

        // Handle no data for relationship
        if (relationshipType.isToMany()) {
            doc.setData(new Data<>(new ArrayList<>()));
        } else if (relationshipType.isToOne()) {
            doc.setData(new Data<>((Resource) null));
        } else {
            throw new IllegalStateException("Failed to GET a relationship; relationship is neither toMany nor toOne");
        }

        PopulateMetaProcessor metaProcessor = new PopulateMetaProcessor();
        metaProcessor.execute(doc, requestScope, record, queryParams);

        return () -> Pair.of(HttpStatus.SC_OK, doc);
    }

    @Override
    public Supplier<Pair<Integer, JsonApiDocument>> handlePatch(StateContext state) {
        return handleRequest(state, this::patch);
    }

    @Override
    public Supplier<Pair<Integer, JsonApiDocument>> handlePost(StateContext state) {
        return handleRequest(state, this::post);
    }

    @Override
    public Supplier<Pair<Integer, JsonApiDocument>> handleDelete(StateContext state) {
        return handleRequest(state, this::delete);
    }

    /*
     * Base on the JSON API docs relationship updates MUST return 204 unless the server has made additional modification
     * to the relationship. http://jsonapi.org/format/#crud-updating-relationship-responses
     */
    private Supplier<Pair<Integer, JsonApiDocument>> handleRequest(StateContext state,
                                                            BiPredicate<Data<Resource>, RequestScope> handler) {
        Data<Resource> data = state.getJsonApiDocument().getData();
        handler.test(data, state.getRequestScope());
        // TODO: figure out if we've made modifications that differ from those requested by client
        return () -> Pair.of(HttpStatus.SC_NO_CONTENT, null);
    }

    private boolean patch(Data<Resource> data, RequestScope requestScope) {
        boolean isUpdated;

        if (relationshipType.isToMany() && data == null) {
            throw new InvalidEntityBodyException("Expected data but received null");
        }

        if (relationshipType.isToMany()) {
            if (data == null) {
                return false;
            }
            Collection<Resource> resources = data.get();
            if (resources == null) {
                return false;
            }
            if (!resources.isEmpty()) {
                isUpdated = record.updateRelation(relationshipName,
                        new Relationship(null, new Data<>(resources)).toPersistentResources(requestScope));
            } else {
                isUpdated = record.clearRelation(relationshipName);
            }
        } else if (relationshipType.isToOne()) {
            if (data != null) {
                Resource resource = data.getSingleValue();
                Relationship relationship = new Relationship(null, new Data<>(resource));
                isUpdated = record.updateRelation(relationshipName, relationship.toPersistentResources(requestScope));
            } else {
                isUpdated = record.clearRelation(relationshipName);
            }
        } else {
            throw new IllegalStateException("Bad relationship type");
        }

        return isUpdated;
    }

    private boolean post(Data<Resource> data, RequestScope requestScope) {
        if (data == null) {
            throw new InvalidEntityBodyException("Expected data but received null");
        }

        Collection<Resource> resources = data.get();
        if (resources == null) {
            return false;
        }
        resources.stream().forEachOrdered(resource ->
            record.addRelation(relationshipName, resource.toPersistentResource(requestScope)));
        return true;
    }

    private boolean delete(Data<Resource> data, RequestScope requestScope) {
        if (data == null) {
            throw new InvalidEntityBodyException("Expected data but received null");
        }

        Collection<Resource> resources = data.get();
        if (CollectionUtils.isEmpty(resources)) {
            // As per: http://jsonapi.org/format/#crud-updating-relationship-responses-403
            throw new ForbiddenAccessException(UpdatePermission.class);
        }
        resources.stream().forEachOrdered(resource ->
            record.removeRelation(relationshipName, resource.toPersistentResource(requestScope)));
        return true;
    }
}
