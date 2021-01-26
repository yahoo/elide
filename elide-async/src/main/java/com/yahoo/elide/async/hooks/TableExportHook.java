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
import com.yahoo.elide.async.operation.GraphQLTableExportCallableOperation;
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
        Callable operation = null;
        TableExportFormatter formatter = supportedFormatters.get(((TableExport) export).getResultType());

        if (formatter == null) {
            throw new InvalidOperationException("Formatter unavailable for " + ((TableExport) export).getResultType());
        }

        if (export.getQueryType().equals(QueryType.GRAPHQL_V1_0)) {
            operation = new GraphQLTableExportCallableOperation(formatter, getAsyncExecutorService(), export,
                    (com.yahoo.elide.core.RequestScope) requestScope);
        } else {
            throw new InvalidOperationException(export.getQueryType() + "is not supported");
        }
        return operation;
    }
}
