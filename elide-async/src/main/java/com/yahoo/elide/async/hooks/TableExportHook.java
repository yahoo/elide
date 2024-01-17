/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.hooks;

import com.yahoo.elide.annotation.LifeCycleHookBinding.Operation;
import com.yahoo.elide.annotation.LifeCycleHookBinding.TransactionPhase;
import com.yahoo.elide.async.AsyncSettings;
import com.yahoo.elide.async.ResultTypeFileExtensionMapper;
import com.yahoo.elide.async.export.formatter.TableExportFormatter;
import com.yahoo.elide.async.models.AsyncApi;
import com.yahoo.elide.async.models.AsyncApiResult;
import com.yahoo.elide.async.models.QueryType;
import com.yahoo.elide.async.models.TableExport;
import com.yahoo.elide.async.operation.GraphQLTableExportOperation;
import com.yahoo.elide.async.operation.JsonApiTableExportOperation;
import com.yahoo.elide.async.service.AsyncExecutorService;
import com.yahoo.elide.async.service.storageengine.ResultStorageEngine;
import com.yahoo.elide.core.exceptions.InvalidOperationException;
import com.yahoo.elide.core.security.ChangeSpec;
import com.yahoo.elide.core.security.RequestScope;

import lombok.EqualsAndHashCode;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * LifeCycle Hook for execution of TableExport.
 */
@EqualsAndHashCode(callSuper = true)
public class TableExportHook extends AsyncApiHook<TableExport> {
    private final Map<String, TableExportFormatter> supportedFormatters;
    private final ResultStorageEngine engine;
    private final ResultTypeFileExtensionMapper resultTypeFileExtensionMapper;

    public TableExportHook (AsyncExecutorService asyncExecutorService, Duration maxAsyncAfter,
            Map<String, TableExportFormatter> supportedFormatters, ResultStorageEngine engine,
            ResultTypeFileExtensionMapper resultTypeFileExtensionMapper) {
        super(asyncExecutorService, maxAsyncAfter);
        this.supportedFormatters = supportedFormatters;
        this.engine = engine;
        this.resultTypeFileExtensionMapper = resultTypeFileExtensionMapper;
    }

    @Override
    public void execute(Operation operation, TransactionPhase phase, TableExport export, RequestScope requestScope,
            Optional<ChangeSpec> changes) {
        Callable<AsyncApiResult> callable = getOperation(export, requestScope);
        executeHook(operation, phase, export, requestScope, callable);
    }

    @Override
    public void validateOptions(AsyncApi export, RequestScope requestScope) {
        AsyncSettings asyncSettings = requestScope.getElideSettings().getSettings(AsyncSettings.class);
        if (!asyncSettings.getExport().isEnabled()) {
            throw new InvalidOperationException("TableExport is not supported.");
        }
        super.validateOptions(export, requestScope);
    }

    @Override
    public Callable<AsyncApiResult> getOperation(AsyncApi export, RequestScope requestScope) {
        Callable<AsyncApiResult> operation = null;
        TableExport exportObj = (TableExport) export;
        String resultType = exportObj.getResultType();
        QueryType queryType = exportObj.getQueryType();
        com.yahoo.elide.core.RequestScope scope = (com.yahoo.elide.core.RequestScope) requestScope;

        TableExportFormatter formatter = supportedFormatters.get(resultType);

        if (formatter == null) {
            throw new InvalidOperationException("Formatter unavailable for " + resultType);
        }

        if (queryType.equals(QueryType.GRAPHQL_V1_0)) {
            operation = new GraphQLTableExportOperation(formatter, getAsyncExecutorService(), export, scope, engine,
                    resultTypeFileExtensionMapper);
        } else if (queryType.equals(QueryType.JSONAPI_V1_0)) {
            operation = new JsonApiTableExportOperation(formatter, getAsyncExecutorService(), export, scope, engine,
                    resultTypeFileExtensionMapper);
        } else {
            throw new InvalidOperationException(queryType + "is not supported");
        }
        return operation;
    }
}
