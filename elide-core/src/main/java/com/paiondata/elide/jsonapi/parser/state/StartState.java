/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.jsonapi.parser.state;

import com.paiondata.elide.core.PersistentResource;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.exceptions.InvalidCollectionException;
import com.paiondata.elide.core.request.EntityProjection;
import com.paiondata.elide.core.type.Type;
import com.paiondata.elide.generated.parsers.CoreParser.EntityContext;
import com.paiondata.elide.generated.parsers.CoreParser.RootCollectionLoadEntitiesContext;
import com.paiondata.elide.generated.parsers.CoreParser.RootCollectionLoadEntityContext;
import com.paiondata.elide.generated.parsers.CoreParser.RootCollectionRelationshipContext;
import com.paiondata.elide.generated.parsers.CoreParser.RootCollectionSubCollectionContext;

import java.util.Optional;

/**
 * Initial State.
 */
public class StartState extends BaseState {
    @Override
    public void handle(StateContext state, RootCollectionLoadEntitiesContext ctx) {
        String entityName = ctx.term().getText();
        EntityDictionary dictionary = state.getRequestScope().getDictionary();

        Type<?> entityClass = dictionary.getEntityClass(entityName, state.getRequestScope().getRoute().getApiVersion());

        state.setState(new CollectionTerminalState(entityClass, Optional.empty(), Optional.empty(),
                state.getRequestScope().getEntityProjection()));
    }

    @Override
    public void handle(StateContext state, RootCollectionLoadEntityContext ctx) {
        PersistentResource record = entityRecord(state, ctx.entity());
        state.setState(new RecordTerminalState(record));
    }

    @Override
    public void handle(StateContext state, RootCollectionSubCollectionContext ctx) {
        PersistentResource record = entityRecord(state, ctx.entity());

        state.setState(new RecordState(record, state.getRequestScope().getEntityProjection()));
    }

    @Override
    public void handle(StateContext state, RootCollectionRelationshipContext ctx) {
        PersistentResource record = entityRecord(state, ctx.entity());

        EntityProjection projection = state.getRequestScope().getEntityProjection();
        String relationName = ctx.relationship().term().getText();

        record.getRelationCheckedFiltered(projection.getRelationship(relationName)
                    .orElseThrow(IllegalStateException::new));

        state.setState(new RelationshipTerminalState(record, relationName, projection));
    }

    @Override
    public String toString() {
        return this.getClass().getName();
    }

    private PersistentResource<?> entityRecord(StateContext state, EntityContext entity) {
        String id = entity.id().getText();
        String entityName = entity.term().getText();
        EntityDictionary dictionary = state.getRequestScope().getDictionary();
        Type<?> entityClass = dictionary.getEntityClass(entityName, state.getRequestScope().getRoute().getApiVersion());

        if (entityClass == null || !dictionary.isRoot(entityClass)) {
            throw new InvalidCollectionException(entityName);
        }

        return PersistentResource.loadRecord(state.getRequestScope().getEntityProjection(),
                id, state.getRequestScope());
    }
}
