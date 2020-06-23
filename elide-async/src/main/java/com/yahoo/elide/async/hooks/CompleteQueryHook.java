/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.hooks;

import com.yahoo.elide.annotation.LifeCycleHookBinding;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.service.AsyncExecutorService;
import com.yahoo.elide.functions.LifeCycleHook;
import com.yahoo.elide.security.ChangeSpec;
import com.yahoo.elide.security.RequestScope;

import java.util.Optional;

/**
 * LifeCycle Hook for completion of AsyncQuery.
 */
public class CompleteQueryHook implements LifeCycleHook<AsyncQuery> {

    private AsyncExecutorService asyncExecutorService;

    public CompleteQueryHook (AsyncExecutorService asyncExecutorService) {
        this.asyncExecutorService = asyncExecutorService;
    }

    @Override
    public void execute(LifeCycleHookBinding.Operation operation, AsyncQuery query,
                        RequestScope requestScope, Optional<ChangeSpec> changes) {
        asyncExecutorService.completeQuery(query, requestScope.getUser(), requestScope.getApiVersion());
    }
}
