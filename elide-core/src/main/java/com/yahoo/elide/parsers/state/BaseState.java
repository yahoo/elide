/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.parsers.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yahoo.elide.core.HttpStatus;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.exceptions.HttpStatusException;
import com.yahoo.elide.generated.parsers.CoreParser.RootCollectionLoadEntitiesContext;
import com.yahoo.elide.generated.parsers.CoreParser.RootCollectionLoadEntityContext;
import com.yahoo.elide.generated.parsers.CoreParser.RootCollectionRelationshipContext;
import com.yahoo.elide.generated.parsers.CoreParser.RootCollectionSubCollectionContext;
import com.yahoo.elide.generated.parsers.CoreParser.SubCollectionReadCollectionContext;
import com.yahoo.elide.generated.parsers.CoreParser.SubCollectionReadEntityContext;
import com.yahoo.elide.generated.parsers.CoreParser.SubCollectionRelationshipContext;
import com.yahoo.elide.generated.parsers.CoreParser.SubCollectionSubCollectionContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.yahoo.elide.jsonapi.document.processors.DocumentProcessor;
import com.yahoo.elide.jsonapi.document.processors.IncludedProcessor;
import com.yahoo.elide.jsonapi.models.Data;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.yahoo.elide.jsonapi.models.Resource;
import org.apache.commons.lang3.tuple.Pair;

import javax.ws.rs.core.MultivaluedMap;
import java.util.Optional;
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

    protected static Supplier<Pair<Integer, JsonNode>> constructResponse(
            PersistentResource record,
            StateContext stateContext,
            String requestType) {
        switch (requestType) {
            case "patch":
                int updateStatusCode = stateContext.getRequestScope().getUpdateStatusCode();
                return () -> Pair.of(
                        updateStatusCode,
                        updateStatusCode == HttpStatus.SC_NO_CONTENT
                                ? null
                                : getResponseBody(
                                        record,
                                        stateContext.getRequestScope(),
                                        stateContext.getRequestScope().getMapper().getObjectMapper()
                                )
                );
            default:
                return () -> Pair.of(HttpStatus.SC_NO_CONTENT, null);
        }
    }

    private static JsonNode getResponseBody(PersistentResource rec, RequestScope requestScope, ObjectMapper mapper) {
        Optional<MultivaluedMap<String, String>> queryParams = requestScope.getQueryParams();
        JsonApiDocument jsonApiDocument = new JsonApiDocument();

        //TODO Make this a document processor
        Data<Resource> data = rec == null ? null : new Data<>(rec.toResource());
        jsonApiDocument.setData(data);

        //TODO Iterate over set of document processors
        DocumentProcessor includedProcessor = new IncludedProcessor();
        includedProcessor.execute(jsonApiDocument, rec, queryParams);

        return mapper.convertValue(jsonApiDocument, JsonNode.class);
    }
}
