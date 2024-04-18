/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.jsonapi.parser.state;

import com.paiondata.elide.generated.parsers.CoreParser.RootCollectionLoadEntitiesContext;
import com.paiondata.elide.generated.parsers.CoreParser.RootCollectionLoadEntityContext;
import com.paiondata.elide.generated.parsers.CoreParser.RootCollectionRelationshipContext;
import com.paiondata.elide.generated.parsers.CoreParser.RootCollectionSubCollectionContext;
import com.paiondata.elide.generated.parsers.CoreParser.SubCollectionReadCollectionContext;
import com.paiondata.elide.generated.parsers.CoreParser.SubCollectionReadEntityContext;
import com.paiondata.elide.generated.parsers.CoreParser.SubCollectionRelationshipContext;
import com.paiondata.elide.generated.parsers.CoreParser.SubCollectionSubCollectionContext;
import com.paiondata.elide.jsonapi.JsonApiRequestScope;
import com.paiondata.elide.jsonapi.models.JsonApiDocument;
import org.apache.commons.lang3.tuple.Pair;

import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;

/**
 * Container for current state.
 */
@Slf4j
public class StateContext {
    private BaseState currentState;
    private final JsonApiRequestScope requestScope;

    public StateContext(BaseState initialState, JsonApiRequestScope requestScope) {
        currentState = initialState;
        this.requestScope = requestScope;
    }

    public JsonApiDocument getJsonApiDocument() {
        // TODO: We should really return an immutable copy of this object
        return requestScope.getJsonApiDocument();
    }

    public JsonApiRequestScope getRequestScope() {
        return requestScope;
    }

    void setState(BaseState nextState) {
        log.debug("State Transition - Current State: {} New State: {}", currentState, nextState);
        currentState = nextState;
    }

    public void handle(RootCollectionLoadEntitiesContext ctx) {
        if (log.isDebugEnabled()) {
            log.debug("{}", ctx.toStringTree());
        }
        currentState.handle(this, ctx);
    }

    public void handle(RootCollectionLoadEntityContext ctx) {
        if (log.isDebugEnabled()) {
            log.debug("{}", ctx.toStringTree());
        }
        currentState.handle(this, ctx);
    }

    public void handle(RootCollectionSubCollectionContext ctx) {
        if (log.isDebugEnabled()) {
            log.debug("{}", ctx.toStringTree());
        }
        currentState.handle(this, ctx);
    }

    public void handle(RootCollectionRelationshipContext ctx) {
        if (log.isDebugEnabled()) {
            log.debug("{}", ctx.toStringTree());
        }
        currentState.handle(this, ctx);
    }

    public void handle(SubCollectionReadCollectionContext ctx) {
        if (log.isDebugEnabled()) {
            log.debug("{}", ctx.toStringTree());
        }
        currentState.handle(this, ctx);
    }

    public void handle(SubCollectionReadEntityContext ctx) {
        if (log.isDebugEnabled()) {
            log.debug("{}", ctx.toStringTree());
        }
        currentState.handle(this, ctx);
    }

    public void handle(SubCollectionSubCollectionContext ctx) {
        if (log.isDebugEnabled()) {
            log.debug("{}", ctx.toStringTree());
        }
        currentState.handle(this, ctx);
    }

    public void handle(SubCollectionRelationshipContext ctx) {
        if (log.isDebugEnabled()) {
            log.debug("{}", ctx.toStringTree());
        }
        currentState.handle(this, ctx);
    }

    public Supplier<Pair<Integer, JsonApiDocument>> handleGet() {
        return currentState.handleGet(this);
    }

    public Supplier<Pair<Integer, JsonApiDocument>> handlePatch() {
        return currentState.handlePatch(this);
    }

    public Supplier<Pair<Integer, JsonApiDocument>> handlePost() {
        return currentState.handlePost(this);
    }

    public Supplier<Pair<Integer, JsonApiDocument>> handleDelete() {
        return currentState.handleDelete(this);
    }
}
