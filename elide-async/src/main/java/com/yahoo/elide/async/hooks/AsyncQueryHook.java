/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.hooks;

import com.yahoo.elide.annotation.LifeCycleHookBinding.Operation;
import com.yahoo.elide.annotation.LifeCycleHookBinding.TransactionPhase;
import com.yahoo.elide.async.models.AsyncAPI;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.QueryType;
import com.yahoo.elide.async.service.AsyncExecutorService;
import com.yahoo.elide.async.service.thread.AsyncQueryThread;
import com.yahoo.elide.core.exceptions.InvalidOperationException;
import com.yahoo.elide.core.security.ChangeSpec;
import com.yahoo.elide.core.security.RequestScope;
import com.yahoo.elide.graphql.QueryRunner;

import java.util.Optional;

/**
 * LifeCycle Hook for execution of AsyncQuery.
 */
public class AsyncQueryHook extends AsyncAPIHook<AsyncQuery> {

    private boolean enableGraphQL;
    public AsyncQueryHook (AsyncExecutorService asyncExecutorService, Integer maxAsyncAfterSeconds,
            boolean enableGraphQL) {
        super(asyncExecutorService, maxAsyncAfterSeconds);
        this.enableGraphQL = enableGraphQL;
    }

    @Override
    public void execute(Operation operation, TransactionPhase phase, AsyncQuery query, RequestScope requestScope,
            Optional<ChangeSpec> changes) {
        AsyncQueryThread queryWorker = new AsyncQueryThread(query, requestScope.getUser(), getAsyncExecutorService(),
                requestScope.getApiVersion());
        executeHook(operation, phase, query, requestScope, queryWorker);
    }

    @Override
    public void validateOptions(AsyncAPI query, RequestScope requestScope) {
        super.validateOptions(query, requestScope);
        if (query.getQueryType().equals(QueryType.GRAPHQL_V1_0)) {
            if (!this.enableGraphQL) {
                throw new InvalidOperationException("GraphQL is disabled. Please enable GraphQL in settings.");
            }
            QueryRunner runner = getAsyncExecutorService().getRunners().get(requestScope.getApiVersion());
            if (runner == null) {
                throw new InvalidOperationException("Invalid API Version");
            }
        }
    }
}
