/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.hooks;

import com.yahoo.elide.annotation.LifeCycleHookBinding;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.QueryStatus;
import com.yahoo.elide.async.service.AsyncExecutorService;
import com.yahoo.elide.functions.LifeCycleHook;
import com.yahoo.elide.security.ChangeSpec;
import com.yahoo.elide.security.RequestScope;

import java.util.Optional;

/**
 * LifeCycle Hook for execution of AsyncQuery.
 */
public class ExecuteQueryHook implements LifeCycleHook<AsyncQuery> {

    private AsyncExecutorService asyncExecutorService;

    public ExecuteQueryHook (AsyncExecutorService asyncExecutorService) {
        this.asyncExecutorService = asyncExecutorService;
    }

    @Override
    public void execute(LifeCycleHookBinding.Operation operation, AsyncQuery query,
                        RequestScope requestScope, Optional<ChangeSpec> changes) {
        if (query.getStatus() == QueryStatus.QUEUED && query.getResult() == null) {
            asyncExecutorService.executeQuery(query, requestScope.getUser(), requestScope.getApiVersion());
        }
    }
}
