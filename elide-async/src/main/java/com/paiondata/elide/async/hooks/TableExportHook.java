/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.async.hooks;

import com.paiondata.elide.annotation.LifeCycleHookBinding.Operation;
import com.paiondata.elide.annotation.LifeCycleHookBinding.TransactionPhase;
import com.paiondata.elide.async.AsyncSettings;
import com.paiondata.elide.async.ResultTypeFileExtensionMapper;
import com.paiondata.elide.async.export.formatter.TableExportFormatter;
import com.paiondata.elide.async.models.AsyncApi;
import com.paiondata.elide.async.models.AsyncApiResult;
import com.paiondata.elide.async.models.QueryType;
import com.paiondata.elide.async.models.TableExport;
import com.paiondata.elide.async.operation.GraphQLTableExportOperation;
import com.paiondata.elide.async.operation.JsonApiTableExportOperation;
import com.paiondata.elide.async.service.AsyncExecutorService;
import com.paiondata.elide.async.service.storageengine.ResultStorageEngine;
import com.paiondata.elide.core.exceptions.InvalidOperationException;
import com.paiondata.elide.core.security.ChangeSpec;
import com.paiondata.elide.core.security.RequestScope;

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
        com.paiondata.elide.core.RequestScope scope = (com.paiondata.elide.core.RequestScope) requestScope;

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
