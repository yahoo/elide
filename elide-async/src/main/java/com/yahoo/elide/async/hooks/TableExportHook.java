/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.hooks;

import com.yahoo.elide.annotation.LifeCycleHookBinding.Operation;
import com.yahoo.elide.annotation.LifeCycleHookBinding.TransactionPhase;
import com.yahoo.elide.async.export.TableExporter;
import com.yahoo.elide.async.models.AsyncAPI;
import com.yahoo.elide.async.models.TableExport;
import com.yahoo.elide.async.operation.AsyncAPIOperation;
import com.yahoo.elide.async.service.AsyncExecutorService;
import com.yahoo.elide.async.service.thread.TableExportCallable;
import com.yahoo.elide.core.security.ChangeSpec;
import com.yahoo.elide.core.security.RequestScope;

import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * LifeCycle Hook for execution of TableExpoer.
 */
public class TableExportHook extends AsyncAPIHook<TableExport> {

    public TableExportHook (AsyncExecutorService asyncExecutorService, Integer maxAsyncAfterSeconds) {
        super(asyncExecutorService, maxAsyncAfterSeconds);
    }

    @Override
    public void execute(Operation operation, TransactionPhase phase, TableExport query, RequestScope requestScope,
            Optional<ChangeSpec> changes) {
        AsyncExecutorService service = getAsyncExecutorService();
        TableExporter exporter = new TableExporter(service.getElide(), requestScope.getApiVersion(),
                requestScope.getUser());
        TableExportCallable queryWorker = new TableExportCallable(query, service.getResultStorageEngine(), exporter);
        executeHook(operation, phase, query, requestScope, queryWorker);
    }

    @Override
    public void validateOptions(AsyncAPI query, RequestScope requestScope) {
        super.validateOptions(query, requestScope);
    }

    @Override
    public Callable getOperation(AsyncAPI query, RequestScope requestScope) {
        // TODO Auto-generated method stub
        return null;
    }
}
