/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.hooks;

import com.yahoo.elide.annotation.LifeCycleHookBinding.Operation;
import com.yahoo.elide.annotation.LifeCycleHookBinding.TransactionPhase;
import com.yahoo.elide.async.models.AsyncAPI;
import com.yahoo.elide.async.models.AsyncAPIResult;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.QueryType;
<<<<<<< HEAD
import com.yahoo.elide.async.operation.GraphQLAsyncQueryCallableOperation;
import com.yahoo.elide.async.operation.JSONAPIAsyncQueryCallableOperation;
import com.yahoo.elide.async.service.AsyncExecutorService;
=======
import com.yahoo.elide.async.operation.AsyncAPIOperation;
import com.yahoo.elide.async.operation.GraphQLAsyncQueryOperation;
import com.yahoo.elide.async.operation.JSONAPIAsyncQueryOperation;
import com.yahoo.elide.async.service.AsyncExecutorService;
<<<<<<< HEAD
import com.yahoo.elide.async.service.thread.AsyncAPICallable;
>>>>>>> Refactor part 2
=======
>>>>>>> Review Comments
import com.yahoo.elide.core.exceptions.InvalidOperationException;
import com.yahoo.elide.core.security.ChangeSpec;
import com.yahoo.elide.core.security.RequestScope;
import com.yahoo.elide.graphql.QueryRunner;

import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * LifeCycle Hook for execution of AsyncQuery.
 */
public class AsyncQueryHook extends AsyncAPIHook<AsyncQuery> {

    public AsyncQueryHook (AsyncExecutorService asyncExecutorService, Integer maxAsyncAfterSeconds) {
        super(asyncExecutorService, maxAsyncAfterSeconds);
    }

    @Override
    public void execute(Operation operation, TransactionPhase phase, AsyncQuery query, RequestScope requestScope,
            Optional<ChangeSpec> changes) {
        Callable<AsyncAPIResult> callable = getOperation(query, requestScope);
        executeHook(operation, phase, query, requestScope, callable);
    }

    @Override
    public void validateOptions(AsyncAPI query, RequestScope requestScope) {
        super.validateOptions(query, requestScope);

        if (query.getQueryType().equals(QueryType.GRAPHQL_V1_0)) {
            QueryRunner runner = getAsyncExecutorService().getRunners().get(requestScope.getApiVersion());
            if (runner == null) {
                throw new InvalidOperationException("Invalid API Version");
            }
        }
    }

    @Override
    public Callable<AsyncAPIResult> getOperation(AsyncAPI query, RequestScope requestScope) {
        Callable<AsyncAPIResult> operation = null;
        if (query.getQueryType().equals(QueryType.JSONAPI_V1_0)) {
            operation = new JSONAPIAsyncQueryCallableOperation(getAsyncExecutorService(), query,
                    (com.yahoo.elide.core.RequestScope) requestScope);
        } else {
            operation = new GraphQLAsyncQueryCallableOperation(getAsyncExecutorService(), query,
                    (com.yahoo.elide.core.RequestScope) requestScope);
        }
        return operation;
    }
}
