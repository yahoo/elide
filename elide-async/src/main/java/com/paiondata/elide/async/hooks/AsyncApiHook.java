/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.async.hooks;

import static com.paiondata.elide.annotation.LifeCycleHookBinding.Operation.CREATE;
import static com.paiondata.elide.annotation.LifeCycleHookBinding.TransactionPhase.POSTCOMMIT;
import static com.paiondata.elide.annotation.LifeCycleHookBinding.TransactionPhase.PREFLUSH;
import static com.paiondata.elide.annotation.LifeCycleHookBinding.TransactionPhase.PRESECURITY;

import com.paiondata.elide.annotation.LifeCycleHookBinding;
import com.paiondata.elide.async.models.AsyncApi;
import com.paiondata.elide.async.models.AsyncApiResult;
import com.paiondata.elide.async.models.QueryStatus;
import com.paiondata.elide.async.service.AsyncExecutorService;
import com.paiondata.elide.core.exceptions.InvalidOperationException;
import com.paiondata.elide.core.exceptions.InvalidValueException;
import com.paiondata.elide.core.lifecycle.LifeCycleHook;
import com.paiondata.elide.core.security.RequestScope;

import lombok.Data;

import java.security.Principal;
import java.time.Duration;
import java.util.concurrent.Callable;

/**
 * AsyncApi Base Hook methods.
 * @param <T> Type of AsyncApi.
 */
@Data
public abstract class AsyncApiHook<T extends AsyncApi> implements LifeCycleHook<T> {
    private final AsyncExecutorService asyncExecutorService;
    private final long maxAsyncAfterSeconds;

    protected AsyncApiHook(AsyncExecutorService asyncExecutorService, Duration maxAsyncAfter) {
        this.asyncExecutorService = asyncExecutorService;
        this.maxAsyncAfterSeconds = maxAsyncAfter.toSeconds();
    }

    /**
     * Validate the Query Options before executing.
     * @param query AsyncApi type object.
     * @param requestScope RequestScope object.
     * @throws InvalidValueException InvalidValueException
     */
    protected void validateOptions(AsyncApi query, RequestScope requestScope) {
        if (query.getAsyncAfterSeconds() > maxAsyncAfterSeconds) {
            throw new InvalidValueException("Invalid Async After Seconds");
        }
    }

    /**
     * Execute the Hook.
     * @param query AsyncApi type object.
     * @param requestScope RequestScope object.
     * @param queryWorker Thread to execute.
     * @throws InvalidOperationException InvalidOperationException
     */
    protected void executeHook(LifeCycleHookBinding.Operation operation, LifeCycleHookBinding.TransactionPhase phase,
            AsyncApi query, RequestScope requestScope, Callable<AsyncApiResult> queryWorker) {
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
     * @param query AsyncApi object to complete.
     * @param requestScope RequestScope object.
     */
    protected void completeAsync(AsyncApi query, RequestScope requestScope) {
        asyncExecutorService.completeQuery(query, requestScope.getUser(), requestScope.getRoute().getApiVersion());
    }

    /**
     * Call the executeQuery process on AsyncExecutorService.
     * @param query AsyncApi object to complete.
     * @param callable CallableThread instance.
     */
    protected void executeAsync(AsyncApi query, Callable<AsyncApiResult> callable) {
        if (query.getStatus() == QueryStatus.QUEUED && query.getResult() == null) {
            asyncExecutorService.executeQuery(query, callable);
        }
    }

    /**
     * Update Principal Name.
     * @param query AsyncApi object to complete.
     * @param requestScope RequestScope object.
     */
    protected void updatePrincipalName(AsyncApi query, RequestScope requestScope) {
        Principal principal = requestScope.getUser().getPrincipal();
        if (principal != null) {
            query.setPrincipalName(principal.getName());
        }
    }

    /**
     * Get Callable operation to submit.
     * @param query AsyncApi object to complete.
     * @param requestScope RequestScope object.
     * @return Callable initialized.
     */
    public abstract Callable<AsyncApiResult> getOperation(AsyncApi query, RequestScope requestScope);
}
