/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.hooks;

import com.yahoo.elide.annotation.LifeCycleHookBinding.Operation;
import com.yahoo.elide.annotation.LifeCycleHookBinding.TransactionPhase;
import com.yahoo.elide.async.export.formatter.TableExportFormatter;
import com.yahoo.elide.async.models.AsyncAPI;
import com.yahoo.elide.async.models.AsyncAPIResult;
import com.yahoo.elide.async.models.QueryType;
import com.yahoo.elide.async.models.ResultType;
import com.yahoo.elide.async.models.TableExport;
import com.yahoo.elide.async.operation.GraphQLTableExportOperation;
import com.yahoo.elide.async.operation.JSONAPITableExportOperation;
import com.yahoo.elide.async.service.AsyncExecutorService;
import com.yahoo.elide.core.exceptions.InvalidOperationException;
import com.yahoo.elide.core.security.ChangeSpec;
import com.yahoo.elide.core.security.RequestScope;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * LifeCycle Hook for execution of TableExpoer.
 */
public class TableExportHook extends AsyncAPIHook<TableExport> {
    Map<ResultType, TableExportFormatter> supportedFormatters;

    public TableExportHook (AsyncExecutorService asyncExecutorService, Integer maxAsyncAfterSeconds,
            Map<ResultType, TableExportFormatter> supportedFormatters) {
        super(asyncExecutorService, maxAsyncAfterSeconds);
        this.supportedFormatters = supportedFormatters;
    }

    @Override
    public void execute(Operation operation, TransactionPhase phase, TableExport export, RequestScope requestScope,
            Optional<ChangeSpec> changes) {
        Callable<AsyncAPIResult> callable = getOperation(export, requestScope);
        executeHook(operation, phase, export, requestScope, callable);
    }

    @Override
    public void validateOptions(AsyncAPI export, RequestScope requestScope) {
        super.validateOptions(export, requestScope);
    }

    @Override
    public Callable<AsyncAPIResult> getOperation(AsyncAPI export, RequestScope requestScope) {
        Callable<AsyncAPIResult> operation = null;
        TableExport exportObj = (TableExport) export;
        ResultType resultType = exportObj.getResultType();
        QueryType queryType = exportObj.getQueryType();
        com.yahoo.elide.core.RequestScope scope = (com.yahoo.elide.core.RequestScope) requestScope;

        TableExportFormatter formatter = supportedFormatters.get(resultType);

        if (formatter == null) {
            throw new InvalidOperationException("Formatter unavailable for " + resultType);
        }

        if (queryType.equals(QueryType.GRAPHQL_V1_0)) {
            operation = new GraphQLTableExportOperation(formatter, getAsyncExecutorService(), export, scope);
        } else if (queryType.equals(QueryType.JSONAPI_V1_0)) {
            operation = new JSONAPITableExportOperation(formatter, getAsyncExecutorService(), export, scope);
        } else {
            // TODO - Support JSONAPI
            throw new InvalidOperationException(queryType + "is not supported");
        }
        return operation;
    }
}
