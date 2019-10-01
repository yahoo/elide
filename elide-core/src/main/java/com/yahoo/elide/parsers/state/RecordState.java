/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.parsers.state;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RelationshipType;
import com.yahoo.elide.generated.parsers.CoreParser.SubCollectionReadCollectionContext;
import com.yahoo.elide.generated.parsers.CoreParser.SubCollectionReadEntityContext;
import com.yahoo.elide.generated.parsers.CoreParser.SubCollectionRelationshipContext;
import com.yahoo.elide.generated.parsers.CoreParser.SubCollectionSubCollectionContext;
import com.yahoo.elide.jsonapi.models.SingleElementSet;
import com.yahoo.elide.request.EntityProjection;
import com.yahoo.elide.request.Relationship;

import com.google.common.base.Preconditions;

import java.util.Optional;
import java.util.Set;

/**
 * Record Read State.
 */
public class RecordState extends BaseState {
    private final PersistentResource resource;

    /* The projection which loaded this record */
    private final EntityProjection projection;

    public RecordState(PersistentResource resource, EntityProjection projection) {
        Preconditions.checkNotNull(resource);
        this.resource = resource;
        this.projection = projection;
    }

    @Override
    public void handle(StateContext state, SubCollectionReadCollectionContext ctx) {
        String subCollection = ctx.term().getText();
        EntityDictionary dictionary = state.getRequestScope().getDictionary();

        Class<?> entityClass;
        String entityName;

        RelationshipType type = dictionary.getRelationshipType(resource.getObject(), subCollection);

        Class<?> paramType = dictionary.getParameterizedType(resource.getObject(), subCollection);
        if (dictionary.isMappedInterface(paramType)) {
            entityName = EntityDictionary.getSimpleName(paramType);
            entityClass = paramType;
        } else {
            entityName = dictionary.getJsonAliasFor(paramType);
            entityClass = dictionary.getEntityClass(entityName);
        }
        if (entityClass == null) {
            throw new IllegalArgumentException("Unknown type " + entityName);
        }
        final BaseState nextState;
        final CollectionTerminalState collectionTerminalState =
                new CollectionTerminalState(entityClass, Optional.of(resource),
                        Optional.of(subCollection), projection);
        Set<PersistentResource> collection = null;
        if (type.isToOne()) {
            collection = resource.getRelationCheckedFiltered(projection.getRelationship(subCollection)
                    .orElseThrow(IllegalStateException::new));
        }
        if (collection instanceof SingleElementSet) {
            PersistentResource record = ((SingleElementSet<PersistentResource>) collection).getValue();
            nextState = new RecordTerminalState(record, collectionTerminalState);
        } else {
            nextState = collectionTerminalState;
        }
        state.setState(nextState);
    }

    @Override
    public void handle(StateContext state, SubCollectionReadEntityContext ctx) {
        String id = ctx.entity().id().getText();
        String subCollection = ctx.entity().term().getText();

        PersistentResource nextRecord = resource.getRelation(
                    projection.getRelationship(subCollection).orElseThrow(IllegalStateException::new), id);
        state.setState(new RecordTerminalState(nextRecord));
    }

    @Override
    public void handle(StateContext state, SubCollectionSubCollectionContext ctx) {
        String id = ctx.entity().id().getText();
        String subCollection = ctx.entity().term().getText();

        Relationship relationship = projection.getRelationship(subCollection)
                    .orElseThrow(IllegalStateException::new);

        state.setState(new RecordState(resource.getRelation(relationship, id), relationship.getProjection()));
    }

    @Override
    public void handle(StateContext state, SubCollectionRelationshipContext ctx) {
        String id = ctx.entity().id().getText();
        String subCollection = ctx.entity().term().getText();
        String relationName = ctx.relationship().term().getText();

        PersistentResource childRecord;

        Relationship childRelationship = projection.getRelationship(subCollection)
                .orElseThrow(IllegalStateException::new);

        childRecord = resource.getRelation(childRelationship , id);

        state.setState(new RelationshipTerminalState(childRecord, relationName, childRelationship.getProjection()));
    }
}
