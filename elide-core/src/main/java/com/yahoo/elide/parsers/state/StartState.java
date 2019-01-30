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
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.generated.parsers.CoreParser.EntityContext;
import com.yahoo.elide.generated.parsers.CoreParser.RootCollectionLoadEntitiesContext;
import com.yahoo.elide.generated.parsers.CoreParser.RootCollectionLoadEntityContext;
import com.yahoo.elide.generated.parsers.CoreParser.RootCollectionRelationshipContext;
import com.yahoo.elide.generated.parsers.CoreParser.RootCollectionSubCollectionContext;

import java.util.Optional;

/**
 * Initial State.
 */
public class StartState extends BaseState {
    @Override
    public void handle(StateContext state, RootCollectionLoadEntitiesContext ctx) {
        String entityName = ctx.term().getText();
        EntityDictionary dictionary = state.getRequestScope().getDictionary();
        Class<?> entityClass = dictionary.getEntityClass(entityName);
        if (entityClass == null || !dictionary.isRoot(entityClass)) {
            throw new InvalidCollectionException(entityName);
        }
        state.setState(new CollectionTerminalState(entityClass, Optional.empty(), Optional.empty()));
    }

    @Override
    public void handle(StateContext state, RootCollectionLoadEntityContext ctx) {
        PersistentResource record = entityRecord(state, ctx.entity());
        state.setState(new RecordTerminalState(record));
    }

    @Override
    public void handle(StateContext state, RootCollectionSubCollectionContext ctx) {
        PersistentResource record = entityRecord(state, ctx.entity());
        state.setState(new RecordState(record));
    }

    @Override
    public void handle(StateContext state, RootCollectionRelationshipContext ctx) {
        PersistentResource record = entityRecord(state, ctx.entity());

        String relationName = ctx.relationship().term().getText();
        try {
            Optional<FilterExpression> filterExpression =
                    state.getRequestScope().getExpressionForRelation(record, relationName);
            record.getRelationCheckedFiltered(relationName, filterExpression, Optional.empty(), Optional.empty());
        } catch (InvalidAttributeException e) {
            throw new InvalidCollectionException(relationName);
        }

        state.setState(new RelationshipTerminalState(record, relationName));
    }

    @Override
    public String toString() {
        return this.getClass().getName();
    }

    private PersistentResource<?> entityRecord(StateContext state, EntityContext entity) {
        String entityName = entity.term().getText();
        String id = entity.id().getText();
        EntityDictionary dictionary = state.getRequestScope().getDictionary();
        Class<?> entityClass = dictionary.getEntityClass(entityName);
        if (entityClass == null || !dictionary.isRoot(entityClass)) {
            throw new InvalidCollectionException(entityName);
        }

        return PersistentResource.loadRecord(entityClass, id, state.getRequestScope());
    }
}
