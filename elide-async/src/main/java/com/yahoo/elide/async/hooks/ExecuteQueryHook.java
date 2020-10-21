/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.hooks;

import com.yahoo.elide.annotation.LifeCycleHookBinding;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.QueryType;
import com.yahoo.elide.async.service.AsyncExecutorService;
import com.yahoo.elide.async.service.AsyncQueryThread;
import com.yahoo.elide.core.exceptions.InvalidOperationException;
import com.yahoo.elide.functions.LifeCycleHook;
import com.yahoo.elide.graphql.QueryRunner;
import com.yahoo.elide.security.ChangeSpec;
import com.yahoo.elide.security.RequestScope;

import java.util.Optional;

/**
 * LifeCycle Hook for execution of AsyncQuery.
 */
public class ExecuteQueryHook extends AsyncHook implements LifeCycleHook<AsyncQuery> {

    public ExecuteQueryHook (AsyncExecutorService asyncExecutorService) {
        super(asyncExecutorService);
    }

    @Override
    public void execute(LifeCycleHookBinding.Operation operation, LifeCycleHookBinding.TransactionPhase phase,
                        AsyncQuery query, RequestScope requestScope, Optional<ChangeSpec> changes) {
        validateOptions(query, requestScope);
        AsyncQueryThread queryWorker = new AsyncQueryThread(query, requestScope.getUser(), getAsyncExecutorService(),
                requestScope.getApiVersion());
        executeAsync(query, queryWorker);
    }

    protected void validateOptions(AsyncQuery query, RequestScope requestScope) {
        if (query.getQueryType().equals(QueryType.GRAPHQL_V1_0)) {
            QueryRunner runner = getAsyncExecutorService().getRunners().get(requestScope.getApiVersion());
            if (runner == null) {
                throw new InvalidOperationException("Invalid API Version");
            }
        }
    }
}
