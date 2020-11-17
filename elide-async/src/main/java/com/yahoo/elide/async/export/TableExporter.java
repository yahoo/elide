/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.export;

import com.yahoo.elide.Elide;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.QueryType;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.exceptions.InvalidValueException;
import com.yahoo.elide.core.exceptions.TransactionException;
import com.yahoo.elide.graphql.GraphQLRequestScope;
import com.yahoo.elide.request.EntityProjection;
import com.yahoo.elide.security.User;

import io.reactivex.Observable;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

/**
 * Class for Table Export functionality.
 */
@Slf4j
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

    /**
     * Exports the Data based on AsyncQuery.
     * @param query AsyncQuery object.
     * @return Observable of PersistentResource.
     */
    public Observable<PersistentResource> export(AsyncQuery query) {
        Observable<PersistentResource> results = Observable.empty();

        UUID requestId = UUID.fromString(query.getRequestId());

        try (DataStoreTransaction tx = elide.getDataStore().beginTransaction()) {
            elide.getTransactionRegistry().addRunningTransaction(requestId, tx);

            EntityProjection projection = null;
            RequestScope requestScope = null;

            if (query.getQueryType().equals(QueryType.GRAPHQL_V1_0)) {
                projection = graphQLParser.parse(query);
                //TODO - we need to add the baseUrlEndpoint to the queryObject.
                //TODO - Can we have projectionInfo as null?
                requestScope = new GraphQLRequestScope("", tx, user, apiVersion, elide.getElideSettings(), null,
                        requestId);
            } else {
                //TODO - Add JSON Support
                throw new InvalidValueException("QueryType not supported");
            }

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
            log.error("IOException during TableExport", e);
            throw new TransactionException(e);
        } finally {
            elide.getTransactionRegistry().removeRunningTransaction(requestId);
            elide.getAuditLogger().clear();
        }

        return results;
    }
}
