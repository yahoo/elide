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
import com.yahoo.elide.async.service.AsyncExecutorService;
import com.yahoo.elide.async.service.thread.TableExportThread;
import com.yahoo.elide.core.security.ChangeSpec;
import com.yahoo.elide.core.security.RequestScope;

import org.apache.http.client.utils.URIBuilder;

import lombok.extern.slf4j.Slf4j;

import java.net.URISyntaxException;
import java.util.Optional;

/**
 * LifeCycle Hook for execution of TableExpoer.
 */
@Slf4j
public class TableExportHook extends AsyncAPIHook<TableExport> {
    private boolean baseURLPresent = true;

    public TableExportHook (AsyncExecutorService asyncExecutorService, Integer maxAsyncAfterSeconds,
            boolean baseURLPresent) {
        super(asyncExecutorService, maxAsyncAfterSeconds);
        this.baseURLPresent = baseURLPresent;
    }

    @Override
    public void execute(Operation operation, TransactionPhase phase, TableExport query, RequestScope requestScope,
            Optional<ChangeSpec> changes) {
        AsyncExecutorService service = getAsyncExecutorService();

        // TODO: Make it a Bean
        TableExporter exporter = new TableExporter(service.getElide());

        String downloadURL = retrieveDownloadURL(requestScope.getBaseUrlEndPoint());
        TableExportThread queryWorker = new TableExportThread(query, exporter, downloadURL,
                requestScope.getApiVersion(), requestScope.getUser(), service.getResultStorageEngine(),
                service.isSkipCSVHeader());
        executeHook(operation, phase, query, requestScope, queryWorker);
    }

    @Override
    public void validateOptions(AsyncAPI query, RequestScope requestScope) {
        super.validateOptions(query, requestScope);
    }

    private String retrieveDownloadURL(String baseURL) {
        String downloadURL = null;
        if (baseURLPresent) {
            downloadURL = getAsyncExecutorService().getDownloadURL();
        } else {
            downloadURL = getBasePath(baseURL) + getAsyncExecutorService().getDownloadURL() + "/";
        }

        return downloadURL;
    }

    private String getBasePath(String URL) {
        URIBuilder uri;
        try {
            uri = new URIBuilder(URL);
        } catch (URISyntaxException e) {
            log.debug("extracting base path from requestURL failure. {}", e.getMessage());
            throw new IllegalStateException(e);
        }
        StringBuilder str = new StringBuilder(uri.getScheme() + "://" + uri.getHost());
        if (uri.getPort() != -1) {
            str.append(":" + uri.getPort());
        }
        return str.toString();
    }
}
