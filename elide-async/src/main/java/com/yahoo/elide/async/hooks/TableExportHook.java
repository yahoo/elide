/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.hooks;

import com.yahoo.elide.annotation.LifeCycleHookBinding.Operation;
import com.yahoo.elide.annotation.LifeCycleHookBinding.TransactionPhase;
import com.yahoo.elide.async.models.AsyncAPI;
import com.yahoo.elide.async.models.TableExport;
import com.yahoo.elide.async.service.AsyncExecutorService;
import com.yahoo.elide.async.service.thread.TableExportThread;
import com.yahoo.elide.security.ChangeSpec;
import com.yahoo.elide.security.RequestScope;

import java.util.Optional;

/**
 * LifeCycle Hook for execution of TableExport.
 */
public class TableExportHook extends AsyncAPIHook<TableExport> {

    public TableExportHook (AsyncExecutorService asyncExecutorService) {
        super(asyncExecutorService);
    }

    @Override
    public void execute(Operation operation, TransactionPhase phase, TableExport query, RequestScope requestScope,
            Optional<ChangeSpec> changes) {
    	TableExportThread queryWorker = new TableExportThread(query, requestScope.getUser(), getAsyncExecutorService(),
                requestScope.getApiVersion());
        executeHook(operation, phase, query, requestScope, queryWorker);
    }

    @Override
    public void validateOptions(AsyncAPI query, RequestScope requestScope) {
        //Do Nothing
    }
}
