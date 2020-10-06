/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.export;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

import com.yahoo.elide.Elide;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.graphql.GraphQLRequestScope;
import com.yahoo.elide.request.EntityProjection;
import com.yahoo.elide.security.User;

import io.reactivex.Observable;

/**
 * Class for Table Export functionality.  
 */
public class TableExporter {

    private Elide elide;
    private String apiVersion;
    private User user;
    private GraphQLParser graphQLParser;

    public TableExporter(Elide elide, String apiVersion, User user) {
        this(elide, apiVersion, user, new GraphQLParser(elide, apiVersion));
    }

    public TableExporter(Elide elide, String apiVersion, User user, GraphQLParser graphQLParser) {
        this.elide = elide;
        this.apiVersion = apiVersion;
        this.user = user;
        this.graphQLParser = graphQLParser;
    }

    public Observable<PersistentResource> export(AsyncQuery query) {
        Observable<PersistentResource> results = Observable.empty();

        UUID requestId = UUID.fromString(query.getRequestId());

        try (DataStoreTransaction tx = elide.getDataStore().beginTransaction()) {
            elide.getTransactionRegistry().addRunningTransaction(requestId, tx);
            EntityProjection projection = graphQLParser.parse(query);

            //TODO - we need to add the baseUrlEndpoint to the queryObject.
            //TODO - Can we have projectionInfo as null?
            GraphQLRequestScope requestScope =
                    new GraphQLRequestScope("", tx, user, apiVersion, elide.getElideSettings(), null, requestId);

            if (projection != null) {
                results = PersistentResource.loadRecords(projection, Collections.emptyList(), requestScope);
            }

            tx.preCommit();
            requestScope.runQueuedPreSecurityTriggers();
            requestScope.getPermissionExecutor().executeCommitChecks();

            tx.flush(requestScope);

            requestScope.runQueuedPreCommitTriggers();

            elide.getAuditLogger().commit();
            tx.commit(requestScope);

            requestScope.runQueuedPostCommitTriggers();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            elide.getTransactionRegistry().removeRunningTransaction(requestId);
            elide.getAuditLogger().clear();
        }

        return results;
    }
}
