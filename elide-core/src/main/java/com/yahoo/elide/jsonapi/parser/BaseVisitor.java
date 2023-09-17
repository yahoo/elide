/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.jsonapi.parser;

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
import com.yahoo.elide.jsonapi.JsonApiRequestScope;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.yahoo.elide.jsonapi.parser.state.StartState;
import com.yahoo.elide.jsonapi.parser.state.StateContext;
import org.apache.commons.lang3.tuple.Pair;

import java.util.function.Supplier;

/**
 * Base request handler.
 */
public abstract class BaseVisitor extends CoreBaseVisitor<Supplier<Pair<Integer, JsonApiDocument>>> {

    protected final StateContext state;

    public BaseVisitor(JsonApiRequestScope requestScope) {
        state = new StateContext(new StartState(), requestScope);
    }

    public StateContext getState() {
        return state;
    }

    @Override
    public Supplier<Pair<Integer, JsonApiDocument>> visitStart(StartContext ctx) {
        return super.visitStart(ctx);
    }

    @Override
    public Supplier<Pair<Integer, JsonApiDocument>> visitRootCollectionLoadEntities(
            RootCollectionLoadEntitiesContext ctx
    ) {
        state.handle(ctx);
        return super.visitRootCollectionLoadEntities(ctx);
    }

    @Override
    public Supplier<Pair<Integer, JsonApiDocument>> visitRootCollectionLoadEntity(
            RootCollectionLoadEntityContext ctx
    ) {
        state.handle(ctx);
        return super.visitRootCollectionLoadEntity(ctx);
    }

    @Override
    public Supplier<Pair<Integer, JsonApiDocument>> visitRootCollectionSubCollection(
            RootCollectionSubCollectionContext ctx
    ) {
        state.handle(ctx);
        return super.visitRootCollectionSubCollection(ctx);
    }

    @Override
    public Supplier<Pair<Integer, JsonApiDocument>>
    visitRootCollectionRelationship(RootCollectionRelationshipContext ctx) {
        state.handle(ctx);
        return super.visitRootCollectionRelationship(ctx);
    }

    @Override
    public Supplier<Pair<Integer, JsonApiDocument>> visitEntity(EntityContext ctx) {
        return super.visitEntity(ctx);
    }

    @Override
    public Supplier<Pair<Integer, JsonApiDocument>> visitSubCollectionReadCollection(
            SubCollectionReadCollectionContext ctx
    ) {
        state.handle(ctx);
        return super.visitSubCollectionReadCollection(ctx);
    }

    @Override
    public Supplier<Pair<Integer, JsonApiDocument>> visitSubCollectionReadEntity(
            SubCollectionReadEntityContext ctx
    ) {
        state.handle(ctx);
        return super.visitSubCollectionReadEntity(ctx);
    }

    @Override
    public Supplier<Pair<Integer, JsonApiDocument>> visitSubCollectionSubCollection(
            SubCollectionSubCollectionContext ctx
    ) {
        state.handle(ctx);
        return super.visitSubCollectionSubCollection(ctx);
    }

    @Override
    public Supplier<Pair<Integer, JsonApiDocument>> visitSubCollectionRelationship(
            SubCollectionRelationshipContext ctx
    ) {
        state.handle(ctx);
        return super.visitSubCollectionRelationship(ctx);
    }

    @Override
    public Supplier<Pair<Integer, JsonApiDocument>> visitQuery(QueryContext ctx) {
        return super.visitQuery(ctx);
    }

    @Override
    public Supplier<Pair<Integer, JsonApiDocument>> visitTerm(TermContext ctx) {
        return super.visitTerm(ctx);
    }

    @Override
    public Supplier<Pair<Integer, JsonApiDocument>> visitId(IdContext ctx) {
        return super.visitId(ctx);
    }
}
