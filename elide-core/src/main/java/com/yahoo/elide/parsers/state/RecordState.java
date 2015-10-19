/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.parsers.state;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.exceptions.InvalidAttributeException;
import com.yahoo.elide.core.exceptions.InvalidCollectionException;
import com.yahoo.elide.jsonapi.models.SingleElementSet;
import com.yahoo.elide.parsers.ormParser.SubCollectionReadCollectionContext;
import com.yahoo.elide.parsers.ormParser.SubCollectionReadEntityContext;
import com.yahoo.elide.parsers.ormParser.SubCollectionRelationshipContext;
import com.yahoo.elide.parsers.ormParser.SubCollectionSubCollectionContext;
import com.yahoo.elide.parsers.ormParser.SubCollectionToOneContext;

import com.google.common.base.Preconditions;

import java.util.Optional;
import java.util.Set;

/**
 * Record Read State.
 */
public class RecordState extends BaseState {
    private final PersistentResource resource;

    public RecordState(PersistentResource resource) {
        Preconditions.checkNotNull(resource);
        this.resource = resource;
    }

    @Override
    public void handle(StateContext state, SubCollectionReadCollectionContext ctx) {
        String subCollection = ctx.term().getText();
        EntityDictionary dictionary = state.getRequestScope().getDictionary();
        try {
            Set<PersistentResource> collection = resource.getRelation(subCollection); // Check if exists.
            String entityName =
                    dictionary.getBinding(dictionary.getParameterizedType(resource.getObject(), subCollection));
            Class<?> entityClass = dictionary.getBinding(entityName);
            if (entityClass == null) {
                throw new IllegalArgumentException("Unknown type " + entityName);
            }
            final BaseState nextState;
            if (collection instanceof SingleElementSet) {
                PersistentResource record = collection.iterator().next();
                nextState = new RecordTerminalState(subCollection, record);
            } else {
                nextState = new CollectionTerminalState(entityClass, Optional.of(resource), Optional.of(subCollection));
            }
            state.setState(nextState);
        } catch (InvalidAttributeException e) {
            throw new InvalidCollectionException(subCollection);
        }
    }

    @Override
    public void handle(StateContext state, SubCollectionReadEntityContext ctx) {
        String id = ctx.entity().id().getText();
        String subCollection = ctx.entity().term().getText();

        try {
            PersistentResource nextRecord = resource.getRelation(subCollection, id);
            state.setState(new RecordTerminalState(subCollection, nextRecord));
        } catch (InvalidAttributeException e) {
            throw new InvalidCollectionException(subCollection);
        }
    }

    @Override
    public void handle(StateContext state, SubCollectionSubCollectionContext ctx) {
        String id = ctx.entity().id().getText();
        String subCollection = ctx.entity().term().getText();
        try {
            state.setState(new RecordState(resource.getRelation(subCollection, id)));
        } catch (InvalidAttributeException e) {
            throw new InvalidCollectionException(subCollection);
        }
    }

    @Override
    public void handle(StateContext state, SubCollectionRelationshipContext ctx) {
        String id = ctx.entity().id().getText();
        String subCollection = ctx.entity().term().getText();

        PersistentResource childRecord;
        try {
            childRecord = resource.getRelation(subCollection, id);
        } catch (InvalidAttributeException e) {
            throw new InvalidCollectionException(subCollection);
        }

        String relationName = ctx.relationship().term().getText();
        try {
            childRecord.getRelation(relationName);
        } catch (InvalidAttributeException e) {
            throw new InvalidCollectionException(relationName);
        }

        state.setState(new RelationshipTerminalState(childRecord, relationName));
    }

    @Override
    public void handle(StateContext state, SubCollectionToOneContext ctx) {
        String subCollection = ctx.term().getText();
        Set<PersistentResource> single = resource.getRelation(subCollection);
        if (single instanceof SingleElementSet) {
            state.setState(new RecordState(single.iterator().next()));
        } else {
            throw new InvalidCollectionException("Expected single element but found list for '%s'", subCollection);
        }
    }
}
