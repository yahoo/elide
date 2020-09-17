/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.hooks;

import com.yahoo.elide.annotation.LifeCycleHookBinding;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.QueryStatus;
import com.yahoo.elide.async.models.QueryType;
import com.yahoo.elide.async.models.ResultFormatType;
import com.yahoo.elide.async.models.ResultType;
import com.yahoo.elide.async.service.AsyncExecutorService;
import com.yahoo.elide.async.service.ResultStorageEngine;
import com.yahoo.elide.core.exceptions.InvalidValueException;
import com.yahoo.elide.functions.LifeCycleHook;
import com.yahoo.elide.security.ChangeSpec;
import com.yahoo.elide.security.RequestScope;

import java.util.Optional;

/**
 * LifeCycle Hook for execution of AsyncQuery.
 */
public class ExecuteQueryHook implements LifeCycleHook<AsyncQuery> {

    private AsyncExecutorService asyncExecutorService;

    public ExecuteQueryHook(AsyncExecutorService asyncExecutorService) {
        this.asyncExecutorService = asyncExecutorService;
    }

    @Override
    public void execute(LifeCycleHookBinding.Operation operation, AsyncQuery query,
                        RequestScope requestScope, Optional<ChangeSpec> changes) {
        validateOptions(query);
        if (query.getStatus() == QueryStatus.QUEUED && query.getResult() == null) {
            asyncExecutorService.executeQuery(query, requestScope.getUser(), requestScope.getApiVersion());
        }
    }

    /**
     * Validate the options provided in AsyncQuery.
     * @param query AsyncQuery
     */
    public void validateOptions(AsyncQuery query) {
        ResultType resultType = query.getResultType();
        ResultFormatType resultFormat = query.getResultFormatType();
        ResultStorageEngine resultStorageEngine = asyncExecutorService.getResultStorageEngine();

        //If Downloading, ResultStorageEngine should be initialized.
        if (resultType == null || (resultType == ResultType.DOWNLOAD && resultStorageEngine == null)) {
            throw new InvalidValueException("resultType is invalid", (Throwable) null);
        }

        // ResultFormatType should support downloading.
        if (resultType == ResultType.DOWNLOAD && !resultFormat.supportsDownload()) {
            throw new InvalidValueException("resultFormatType is invalid", (Throwable) null);
        }

        if (query.getQueryType().equals(QueryType.GRAPHQL_V1_0)) {
            if (asyncExecutorService.getRunners().size() == 0) {
                throw new InvalidValueException("GraphQL is disabled", (Throwable) null);
            }
        }
    }
}
