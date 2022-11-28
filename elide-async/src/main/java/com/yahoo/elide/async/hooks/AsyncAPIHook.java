/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.hooks;

import static com.yahoo.elide.annotation.LifeCycleHookBinding.Operation.CREATE;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.TransactionPhase.POSTCOMMIT;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.TransactionPhase.PREFLUSH;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.TransactionPhase.PRESECURITY;

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
    private final Integer maxAsyncAfterSeconds;

    public AsyncAPIHook(AsyncExecutorService asyncExecutorService, Integer maxAsyncAfterSeconds) {
        this.asyncExecutorService = asyncExecutorService;
        this.maxAsyncAfterSeconds = maxAsyncAfterSeconds;
    }

    /**
     * Validate the Query Options before executing.
     * @param query AsyncAPI type object.
     * @param requestScope RequestScope object.
     * @throws InvalidValueException InvalidValueException
     */
    protected void validateOptions(AsyncAPI query, RequestScope requestScope) {
        if (query.getAsyncAfterSeconds() > maxAsyncAfterSeconds) {
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
        if (operation.equals(CREATE)) {
            if (phase.equals(PREFLUSH)) {
                validateOptions(query, requestScope);
                executeAsync(query, queryWorker);
                return;
            }
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
