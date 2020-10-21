/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.hooks;

import com.yahoo.elide.async.models.AsyncAPI;
import com.yahoo.elide.async.models.AsyncAPIResult;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.service.AsyncExecutorService;
import com.yahoo.elide.security.RequestScope;

import lombok.Data;

import java.security.Principal;
import java.util.concurrent.Callable;

/**
 * Async Hook methods.
 */
@Data
public abstract class AsyncHook {
    private AsyncExecutorService asyncExecutorService;

    public AsyncHook() { };

    public AsyncHook(AsyncExecutorService asyncExecutorService) {
        this.asyncExecutorService = asyncExecutorService;
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
        asyncExecutorService.executeQuery(query, callable);
    }

    /**
     * Update Principal Name.
     * @param query AsyncAPI object to complete.
     * @param requestScope RequestScope object.
     */
    protected void updatePrincipalName(AsyncQuery query, RequestScope requestScope) {
        Principal principal = requestScope.getUser().getPrincipal();
        if (principal != null) {
            query.setPrincipalName(principal.getName());
        }
    }
}
