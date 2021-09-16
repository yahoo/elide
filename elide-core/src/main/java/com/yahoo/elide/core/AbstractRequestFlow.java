package com.yahoo.elide.core;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.core.audit.AuditLogger;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.security.User;

import lombok.extern.slf4j.Slf4j;

import java.util.UUID;
import java.util.function.Supplier;

@Slf4j
public abstract class AbstractRequestFlow<T> {
    protected ElideSettings settings;

    AbstractRequestFlow(ElideSettings settings) {
        this.settings = settings;

        //TODO - Grab isVerbose from ElideSettings instead of request scope.
    }

    T processRequest(
            AuditLogger auditLogger,
            TransactionRegistry transactionRegistry,
            boolean isReadOnly,
            User user,
            Supplier<DataStoreTransaction> transaction,
            UUID requestId,
            QueryRunner<T> runner,
            ExceptionHandler<T> errorHandler
    ) {
        boolean isVerbose = false;
        try (DataStoreTransaction tx = transaction.get()) {
            transactionRegistry.addRunningTransaction(requestId, tx);
            RequestScope requestScope = createRequestScope(tx);
            T result = runner.runQuery(user, requestScope, tx);
            isVerbose = requestScope.getPermissionExecutor().isVerbose();
            tx.preCommit(requestScope);
            requestScope.runQueuedPreSecurityTriggers();
            requestScope.getPermissionExecutor().executeCommitChecks();
            if (!isReadOnly) {
                requestScope.saveOrCreateObjects();
            }
            requestScope.runQueuedPreFlushTriggers();
            tx.flush(requestScope);

            requestScope.runQueuedPreCommitTriggers();

            auditLogger.commit();
            tx.commit(requestScope);
            requestScope.runQueuedPostCommitTriggers();

            if (log.isTraceEnabled()) {
                requestScope.getPermissionExecutor().logCheckStats();
            }

            return result;
        } catch (Exception e) {
            return errorHandler.handleError(user, e, isVerbose);
        } finally {
            transactionRegistry.removeRunningTransaction(requestId);
            auditLogger.clear();
        }
    }

    protected abstract RequestScope createRequestScope(DataStoreTransaction tx);

    @FunctionalInterface
    public interface QueryRunner<T> {
        T runQuery(User user, RequestScope scope, DataStoreTransaction tx);
    }

    @FunctionalInterface
    public interface ExceptionHandler<T> {
        T handleError(User user, Exception e, boolean isVerbose);
    }
}
