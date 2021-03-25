/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.hooks;

import static com.yahoo.elide.annotation.LifeCycleHookBinding.Operation.CREATE;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.Operation.READ;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.TransactionPhase.POSTCOMMIT;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.TransactionPhase.PRESECURITY;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.annotation.LifeCycleHookBinding;
import com.yahoo.elide.async.models.AsyncAPI;
import com.yahoo.elide.async.models.AsyncAPIResult;
import com.yahoo.elide.async.models.QueryStatus;
import com.yahoo.elide.async.service.AsyncExecutorService;
import com.yahoo.elide.core.exceptions.InvalidOperationException;
import com.yahoo.elide.core.exceptions.InvalidValueException;
import com.yahoo.elide.core.lifecycle.LifeCycleHook;
import com.yahoo.elide.core.security.RequestScope;
import lombok.Data;

import java.security.Principal;
import java.util.concurrent.Callable;

/**
 * AsyncAPI Base Hook methods.
 * @param <T> Type of AsyncAPI.
 */
@Data
public abstract class AsyncAPIHook<T extends AsyncAPI> implements LifeCycleHook<T> {
    private final AsyncExecutorService asyncExecutorService;
    private final ElideSettings elideSettings;

    public AsyncAPIHook(AsyncExecutorService asyncExecutorService, ElideSettings elideSettings) {
        this.asyncExecutorService = asyncExecutorService;
        this.elideSettings = elideSettings;
    }

    /**
     * Validate the Query Options before executing.
     * @param query AsyncAPI type object.
     * @param requestScope RequestScope object.
     * @throws InvalidValueException InvalidValueException
     */
    protected void validateOptions(AsyncAPI query, RequestScope requestScope) {
        if (query.getAsyncAfterSeconds() > elideSettings.getMaxAsyncAfterSeconds()) {
            throw new InvalidValueException("Invalid Async After Seconds");
        }
    }

    /**
     * Execute the Hook.
     * @param query AsyncAPI type object.
     * @param requestScope RequestScope object.
     * @param queryWorker Thread to execute.
     * @throws InvalidOperationException InvalidOperationException
     */
    protected void executeHook(LifeCycleHookBinding.Operation operation, LifeCycleHookBinding.TransactionPhase phase,
            AsyncAPI query, RequestScope requestScope, Callable<AsyncAPIResult> queryWorker) {

        if (operation.equals(READ) && phase.equals(PRESECURITY)) {
            // AsyncAfterSeconds is a transient property. When we update the STATUS of an AsyncRequest for
            // CANCELLATION we do not have the property set. This fails the "validateOptions" method call.
            // Below code sets the value based on ElideSettings if its null.
            if (query.getAsyncAfterSeconds() == null) {
                query.setAsyncAfterSeconds(elideSettings.getMaxAsyncAfterSeconds());
            }

            validateOptions(query, requestScope);

            // Grapqhl AsyncRequest which were submitted with non-zero asyncAfterSeconds were not returning correct
            // Status. They came back with status QUEUED instead of COMPLETE.
            // Root Cause:
            // We populate the result object when the initial mutation is executed, and then even after executing
            // the hooks we return the same object back. https://github.com/yahoo/elide/blob/
            // d4901c6f07e57aa179c5afd640c9c67e90a8cdaf/elide-graphql/src/main/java/com/yahoo/elide/graphql/
            // QueryRunner.java#L190. In GraphQL, the only part of the body that is lazily returned is the ID.
            // Solution:
            // We decided to implement the execution flow as a ReadPreSecurityHook.
            // Those hooks get evaluated in line with the request processing.
            // Every time its read, we check if the STATUS is Queued AND Result is NULL, then we continue with the
            // Asynchronous execution call else we exit from the async flow.
            executeAsync(query, queryWorker);
            return;
        }
        if (operation.equals(CREATE)) {
            if (phase.equals(POSTCOMMIT)) {
                completeAsync(query, requestScope);
                return;
            }
            if (phase.equals(PRESECURITY)) {
                updatePrincipalName(query, requestScope);
                return;
            }
        }

        throw new InvalidOperationException("Invalid LifeCycle Hook Invocation");
    }

    /**
     * Call the completeQuery process in AsyncExecutorService.
     * @param query AsyncAPI object to complete.
     * @param requestScope RequestScope object.
     */
    protected void completeAsync(AsyncAPI query, RequestScope requestScope) {
        asyncExecutorService.completeQuery(query, requestScope.getUser(), requestScope.getApiVersion());
    }

    /**
     * Call the executeQuery process on AsyncExecutorService.
     * @param query AsyncAPI object to complete.
     * @param callable CallableThread instance.
     */
    protected void executeAsync(AsyncAPI query, Callable<AsyncAPIResult> callable) {
        if (query.getStatus() == QueryStatus.QUEUED && query.getResult() == null) {
            asyncExecutorService.executeQuery(query, callable);
        }
    }

    /**
     * Update Principal Name.
     * @param query AsyncAPI object to complete.
     * @param requestScope RequestScope object.
     */
    protected void updatePrincipalName(AsyncAPI query, RequestScope requestScope) {
        Principal principal = requestScope.getUser().getPrincipal();
        if (principal != null) {
            query.setPrincipalName(principal.getName());
        }
    }

    /**
     * Get Callable operation to submit.
     * @param query AsyncAPI object to complete.
     * @param requestScope RequestScope object.
     * @return Callable initialized.
     */
    public abstract Callable<AsyncAPIResult> getOperation(AsyncAPI query, RequestScope requestScope);
}
