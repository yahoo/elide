/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.core.audit.AuditLogger;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.security.User;

import lombok.extern.slf4j.Slf4j;

import java.util.UUID;
import java.util.function.Supplier;

@Slf4j
public abstract class AbstractRequestFlow<QueryResult, QueryToken> {
    protected ElideSettings settings;

    public AbstractRequestFlow(ElideSettings settings) {
        this.settings = settings;

        //TODO - Grab isVerbose from ElideSettings instead of request scope.
    }

    public QueryResult processRequest(
            AuditLogger auditLogger,
            TransactionRegistry transactionRegistry,
            boolean isReadOnly,
            User user,
            Supplier<DataStoreTransaction> transaction,
            UUID requestId,
            RequestScopeFactory requestScopeMaker,
            QueryRunner<QueryToken> queryRunner
    ) {
        boolean isVerbose = false;
        try (DataStoreTransaction tx = transaction.get()) {
            transactionRegistry.addRunningTransaction(requestId, tx);
            RequestScope requestScope = requestScopeMaker.create(tx);
            QueryToken token = queryRunner.runQuery(user, requestScope, tx);
            isVerbose = requestScope.getPermissionExecutor().isVerbose();
            tx.preCommit(requestScope);
            requestScope.runQueuedPreSecurityTriggers();
            requestScope.getPermissionExecutor().executeCommitChecks();
            if (!isReadOnly) {

                //This will throw an exception if there is a problem prior to writing.
                validateBeforeWrite(token);
                requestScope.saveOrCreateObjects();
            }
            requestScope.runQueuedPreFlushTriggers();
            tx.flush(requestScope);

            requestScope.runQueuedPreCommitTriggers();

            QueryResult result = completeQuery(token);

            auditLogger.commit();
            tx.commit(requestScope);
            requestScope.runQueuedPostCommitTriggers();

            if (log.isTraceEnabled()) {
                requestScope.getPermissionExecutor().logCheckStats();
            }

            return result;
        } catch (Exception e) {
            return handleError(user, e, isVerbose);
        } finally {
            transactionRegistry.removeRunningTransaction(requestId);
            auditLogger.clear();
        }
    }

    protected abstract QueryResult completeQuery(QueryToken token);
    protected abstract QueryResult handleError(User user, Exception e, boolean isVerbose);

    protected void validateBeforeWrite(QueryToken token) {
        //NOOP
    }

    @FunctionalInterface
    public interface QueryRunner<QueryToken> {
        public QueryToken runQuery(User user, RequestScope scope, DataStoreTransaction tx);
    }

    @FunctionalInterface
    public interface RequestScopeFactory {
        public RequestScope create(DataStoreTransaction tx);
    }
}
