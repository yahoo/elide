/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.parsers.state;

import com.yahoo.elide.core.exceptions.HttpStatusException;
import com.yahoo.elide.parsers.ormParser.RootCollectionLoadEntitiesContext;
import com.yahoo.elide.parsers.ormParser.RootCollectionLoadEntityContext;
import com.yahoo.elide.parsers.ormParser.RootCollectionRelationshipContext;
import com.yahoo.elide.parsers.ormParser.RootCollectionSubCollectionContext;
import com.yahoo.elide.parsers.ormParser.SubCollectionReadCollectionContext;
import com.yahoo.elide.parsers.ormParser.SubCollectionReadEntityContext;
import com.yahoo.elide.parsers.ormParser.SubCollectionRelationshipContext;
import com.yahoo.elide.parsers.ormParser.SubCollectionSubCollectionContext;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.tuple.Pair;

import java.util.function.Supplier;

/**
 * Base class for state information.
 */
public abstract class BaseState {

    /**
     * Handle void.
     *
     * @param state the state
     * @param ctx the ctx
     */
    public void handle (StateContext state, RootCollectionLoadEntitiesContext ctx) {
        throw new UnsupportedOperationException(this.getClass().toString());
    }

    /**
     * Handle void.
     *
     * @param state the state
     * @param ctx the ctx
     */
    public void handle (StateContext state, RootCollectionLoadEntityContext ctx) {
        throw new UnsupportedOperationException(this.getClass().toString());
    }

    /**
     * Handle void.
     *
     * @param state the state
     * @param ctx the ctx
     */
    public void handle (StateContext state, RootCollectionSubCollectionContext ctx) {
        throw new UnsupportedOperationException(this.getClass().toString());
    }

    /**
     * Handle void.
     *
     * @param state the state
     * @param ctx the ctx
     */
    public void handle(StateContext state, RootCollectionRelationshipContext ctx) {
        throw new UnsupportedOperationException(this.getClass().toString());
    }

    /**
     * Handle void.
     *
     * @param state the state
     * @param ctx the ctx
     */
    public void handle (StateContext state, SubCollectionReadCollectionContext ctx) {
        throw new UnsupportedOperationException(this.getClass().toString());
    }

    /**
     * Handle void.
     *
     * @param state the state
     * @param ctx the ctx
     */
    public void handle (StateContext state, SubCollectionReadEntityContext ctx) {
        throw new UnsupportedOperationException(this.getClass().toString());
    }

    /**
     * Handle void.
     *
     * @param state the state
     * @param ctx the ctx
     */
    public void handle (StateContext state, SubCollectionSubCollectionContext ctx) {
        throw new UnsupportedOperationException(this.getClass().toString());
    }


    /**
     * Handle void.
     *
     * @param state the state
     * @param ctx the ctx
     */
    public void handle(StateContext state, SubCollectionRelationshipContext ctx) {
        throw new UnsupportedOperationException(this.getClass().toString());
    }

    /**
     * We return a Function because we may have to perform post-commit operations. That is,
     * we may need to perform extra operations after having closed a transaction. As a result,
     * this method is invoked after committing a transaction in Elide.java.
     * @param state the state
     * @return the supplier
     * @throws HttpStatusException the http status exception
     */
    public Supplier<Pair<Integer, JsonNode>> handleGet(StateContext state) throws HttpStatusException {
        throw new UnsupportedOperationException(this.getClass().toString());
    }

    /**
     * Handle patch.
     *
     * @param state the state
     * @return the supplier
     * @throws HttpStatusException the http status exception
     */
    public Supplier<Pair<Integer, JsonNode>> handlePatch(StateContext state) throws HttpStatusException {
        throw new UnsupportedOperationException(this.getClass().toString());
    }

    /**
     * Handle post.
     *
     * @param state the state
     * @return the supplier
     * @throws HttpStatusException the http status exception
     */
    public Supplier<Pair<Integer, JsonNode>> handlePost(StateContext state) throws HttpStatusException {
        throw new UnsupportedOperationException(this.getClass().toString());
    }

    /**
     * Handle delete.
     *
     * @param state the state
     * @return the supplier
     * @throws HttpStatusException the http status exception
     */
    public Supplier<Pair<Integer, JsonNode>> handleDelete(StateContext state) throws HttpStatusException {
        throw new UnsupportedOperationException(this.getClass().toString());
    }
}
