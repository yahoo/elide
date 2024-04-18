/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.async.hooks;

import com.paiondata.elide.annotation.LifeCycleHookBinding.Operation;
import com.paiondata.elide.annotation.LifeCycleHookBinding.TransactionPhase;
import com.paiondata.elide.async.models.AsyncApi;
import com.paiondata.elide.async.models.AsyncApiResult;
import com.paiondata.elide.async.models.AsyncQuery;
import com.paiondata.elide.async.models.QueryType;
import com.paiondata.elide.async.operation.GraphQLAsyncQueryOperation;
import com.paiondata.elide.async.operation.JsonApiAsyncQueryOperation;
import com.paiondata.elide.async.service.AsyncExecutorService;
import com.paiondata.elide.core.exceptions.InvalidOperationException;
import com.paiondata.elide.core.security.ChangeSpec;
import com.paiondata.elide.core.security.RequestScope;
import com.paiondata.elide.graphql.QueryRunner;

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
                    (com.paiondata.elide.core.RequestScope) requestScope);
        } else {
            operation = new GraphQLAsyncQueryOperation(getAsyncExecutorService(), query,
                    (com.paiondata.elide.core.RequestScope) requestScope);
        }
        return operation;
    }
}
