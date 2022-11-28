/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.jsonapi.parser.state;

import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.exceptions.HttpStatus;
import com.yahoo.elide.core.exceptions.HttpStatusException;
import com.yahoo.elide.generated.parsers.CoreParser.RootCollectionLoadEntitiesContext;
import com.yahoo.elide.generated.parsers.CoreParser.RootCollectionLoadEntityContext;
import com.yahoo.elide.generated.parsers.CoreParser.RootCollectionRelationshipContext;
import com.yahoo.elide.generated.parsers.CoreParser.RootCollectionSubCollectionContext;
import com.yahoo.elide.generated.parsers.CoreParser.SubCollectionReadCollectionContext;
import com.yahoo.elide.generated.parsers.CoreParser.SubCollectionReadEntityContext;
import com.yahoo.elide.generated.parsers.CoreParser.SubCollectionRelationshipContext;
import com.yahoo.elide.generated.parsers.CoreParser.SubCollectionSubCollectionContext;
import com.yahoo.elide.jsonapi.document.processors.DocumentProcessor;
import com.yahoo.elide.jsonapi.document.processors.IncludedProcessor;
import com.yahoo.elide.jsonapi.document.processors.PopulateMetaProcessor;
import com.yahoo.elide.jsonapi.models.Data;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.yahoo.elide.jsonapi.models.Resource;
import org.apache.commons.lang3.tuple.Pair;

import java.util.function.Supplier;

import javax.ws.rs.core.MultivaluedMap;

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
    public Supplier<Pair<Integer, JsonApiDocument>> handleGet(StateContext state) throws HttpStatusException {
        throw new UnsupportedOperationException(this.getClass().toString());
    }

    /**
     * Handle patch.
     *
     * @param state the state
     * @return the supplier
     * @throws HttpStatusException the http status exception
     */
    public Supplier<Pair<Integer, JsonApiDocument>> handlePatch(StateContext state) throws HttpStatusException {
        throw new UnsupportedOperationException(this.getClass().toString());
    }

    /**
     * Handle post.
     *
     * @param state the state
     * @return the supplier
     * @throws HttpStatusException the http status exception
     */
    public Supplier<Pair<Integer, JsonApiDocument>> handlePost(StateContext state) throws HttpStatusException {
        throw new UnsupportedOperationException(this.getClass().toString());
    }

    /**
     * Handle delete.
     *
     * @param state the state
     * @return the supplier
     * @throws HttpStatusException the http status exception
     */
    public Supplier<Pair<Integer, JsonApiDocument>> handleDelete(StateContext state) throws HttpStatusException {
        throw new UnsupportedOperationException(this.getClass().toString());
    }

    /**
     * Construct PATCH response.
     *
     * @param record a resource that has been updated
     * @param stateContext a state that contains reference to request scope where we can get status code for update
     * @return a supplier of PATH response
     */
    protected static Supplier<Pair<Integer, JsonApiDocument>> constructPatchResponse(
            PersistentResource record,
            StateContext stateContext) {
        RequestScope requestScope = stateContext.getRequestScope();
        int updateStatusCode = requestScope.getUpdateStatusCode();
        return () -> Pair.of(
                updateStatusCode,
                updateStatusCode == HttpStatus.SC_NO_CONTENT ? null : getResponseBody(record, requestScope)
        );
    }

    protected static JsonApiDocument getResponseBody(PersistentResource resource, RequestScope requestScope) {
        MultivaluedMap<String, String> queryParams = requestScope.getQueryParams();
        JsonApiDocument jsonApiDocument = new JsonApiDocument();

        //TODO Make this a document processor
        Data<Resource> data = resource == null ? null : new Data<>(resource.toResource());
        jsonApiDocument.setData(data);

        //TODO Iterate over set of document processors
        DocumentProcessor includedProcessor = new IncludedProcessor();
        includedProcessor.execute(jsonApiDocument, requestScope, resource, queryParams);

        PopulateMetaProcessor metaProcessor = new PopulateMetaProcessor();
        metaProcessor.execute(jsonApiDocument, requestScope, resource, queryParams);

        return jsonApiDocument;
    }
}
