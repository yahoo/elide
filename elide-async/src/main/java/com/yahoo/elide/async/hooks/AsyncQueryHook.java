/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.hooks;

import com.yahoo.elide.annotation.LifeCycleHookBinding.Operation;
import com.yahoo.elide.annotation.LifeCycleHookBinding.TransactionPhase;
import com.yahoo.elide.async.models.AsyncApi;
import com.yahoo.elide.async.models.AsyncApiResult;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.QueryType;
import com.yahoo.elide.async.operation.GraphQLAsyncQueryOperation;
import com.yahoo.elide.async.operation.JsonApiAsyncQueryOperation;
import com.yahoo.elide.async.service.AsyncExecutorService;
import com.yahoo.elide.core.exceptions.InvalidOperationException;
import com.yahoo.elide.core.security.ChangeSpec;
import com.yahoo.elide.core.security.RequestScope;
import com.yahoo.elide.graphql.QueryRunner;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * LifeCycle Hook for execution of AsyncQuery.
 */
public class AsyncQueryHook extends AsyncApiHook<AsyncQuery> {

    public AsyncQueryHook (AsyncExecutorService asyncExecutorService, Duration maxAsyncAfter) {
        super(asyncExecutorService, maxAsyncAfter);
    }

    @Override
    public void execute(Operation operation, TransactionPhase phase, AsyncQuery query, RequestScope requestScope,
            Optional<ChangeSpec> changes) {
        Callable<AsyncApiResult> callable = getOperation(query, requestScope);
        executeHook(operation, phase, query, requestScope, callable);
    }

    @Override
    public void validateOptions(AsyncApi query, RequestScope requestScope) {
        super.validateOptions(query, requestScope);

        if (query.getQueryType().equals(QueryType.GRAPHQL_V1_0)) {
            QueryRunner runner = getAsyncExecutorService().getRunners().get(requestScope.getRoute().getApiVersion());
            if (runner == null) {
                throw new InvalidOperationException("Invalid API Version");
            }
        }
    }

    @Override
    public Callable<AsyncApiResult> getOperation(AsyncApi query, RequestScope requestScope) {
        Callable<AsyncApiResult> operation = null;
        if (query.getQueryType().equals(QueryType.JSONAPI_V1_0)) {
            operation = new JsonApiAsyncQueryOperation(getAsyncExecutorService(), query,
                    (com.yahoo.elide.core.RequestScope) requestScope);
        } else {
            operation = new GraphQLAsyncQueryOperation(getAsyncExecutorService(), query,
                    (com.yahoo.elide.core.RequestScope) requestScope);
        }
        return operation;
    }
}
