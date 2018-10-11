/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.parsers;

import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.generated.parsers.CoreBaseVisitor;
import com.yahoo.elide.generated.parsers.CoreParser.EntityContext;
import com.yahoo.elide.generated.parsers.CoreParser.IdContext;
import com.yahoo.elide.generated.parsers.CoreParser.QueryContext;
import com.yahoo.elide.generated.parsers.CoreParser.RootCollectionLoadEntitiesContext;
import com.yahoo.elide.generated.parsers.CoreParser.RootCollectionLoadEntityContext;
import com.yahoo.elide.generated.parsers.CoreParser.RootCollectionRelationshipContext;
import com.yahoo.elide.generated.parsers.CoreParser.RootCollectionSubCollectionContext;
import com.yahoo.elide.generated.parsers.CoreParser.StartContext;
import com.yahoo.elide.generated.parsers.CoreParser.SubCollectionReadCollectionContext;
import com.yahoo.elide.generated.parsers.CoreParser.SubCollectionReadEntityContext;
import com.yahoo.elide.generated.parsers.CoreParser.SubCollectionRelationshipContext;
import com.yahoo.elide.generated.parsers.CoreParser.SubCollectionSubCollectionContext;
import com.yahoo.elide.generated.parsers.CoreParser.TermContext;
import com.yahoo.elide.parsers.state.StartState;
import com.yahoo.elide.parsers.state.StateContext;

import com.fasterxml.jackson.databind.JsonNode;

import org.apache.commons.lang3.tuple.Pair;

import java.util.function.Supplier;

/**
 * Base request handler.
 */
public abstract class BaseVisitor extends CoreBaseVisitor<Supplier<Pair<Integer, JsonNode>>> {

    protected final StateContext state;

    public BaseVisitor(RequestScope requestScope) {
        state = new StateContext(new StartState(), requestScope);
    }

    public StateContext getState() {
        return state;
    }

    @Override
    public Supplier<Pair<Integer, JsonNode>> visitStart(StartContext ctx) {
        return super.visitStart(ctx);
    }

    @Override
    public Supplier<Pair<Integer, JsonNode>> visitRootCollectionLoadEntities(RootCollectionLoadEntitiesContext ctx) {
        state.handle(ctx);
        return super.visitRootCollectionLoadEntities(ctx);
    }

    @Override
    public Supplier<Pair<Integer, JsonNode>> visitRootCollectionLoadEntity(RootCollectionLoadEntityContext ctx) {
        state.handle(ctx);
        return super.visitRootCollectionLoadEntity(ctx);
    }

    @Override
    public Supplier<Pair<Integer, JsonNode>> visitRootCollectionSubCollection(RootCollectionSubCollectionContext ctx) {
        state.handle(ctx);
        return super.visitRootCollectionSubCollection(ctx);
    }

    @Override
    public Supplier<Pair<Integer, JsonNode>>
    visitRootCollectionRelationship(RootCollectionRelationshipContext ctx) {
        state.handle(ctx);
        return super.visitRootCollectionRelationship(ctx);
    }

    @Override
    public Supplier<Pair<Integer, JsonNode>> visitEntity(EntityContext ctx) {
        return super.visitEntity(ctx);
    }

    @Override
    public Supplier<Pair<Integer, JsonNode>> visitSubCollectionReadCollection(SubCollectionReadCollectionContext ctx) {
        state.handle(ctx);
        return super.visitSubCollectionReadCollection(ctx);
    }

    @Override
    public Supplier<Pair<Integer, JsonNode>> visitSubCollectionReadEntity(SubCollectionReadEntityContext ctx) {
        state.handle(ctx);
        return super.visitSubCollectionReadEntity(ctx);
    }

    @Override
    public Supplier<Pair<Integer, JsonNode>> visitSubCollectionSubCollection(SubCollectionSubCollectionContext ctx) {
        state.handle(ctx);
        return super.visitSubCollectionSubCollection(ctx);
    }

    @Override
    public Supplier<Pair<Integer, JsonNode>> visitSubCollectionRelationship(SubCollectionRelationshipContext ctx) {
        state.handle(ctx);
        return super.visitSubCollectionRelationship(ctx);
    }

    @Override
    public Supplier<Pair<Integer, JsonNode>> visitQuery(QueryContext ctx) {
        return super.visitQuery(ctx);
    }

    @Override
    public Supplier<Pair<Integer, JsonNode>> visitTerm(TermContext ctx) {
        return super.visitTerm(ctx);
    }

    @Override
    public Supplier<Pair<Integer, JsonNode>> visitId(IdContext ctx) {
        return super.visitId(ctx);
    }
}
