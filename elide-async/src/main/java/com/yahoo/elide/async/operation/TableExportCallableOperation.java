/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.operation;

import com.yahoo.elide.Elide;
import com.yahoo.elide.async.export.formatter.TableExportFormatter;
import com.yahoo.elide.async.models.AsyncAPI;
import com.yahoo.elide.async.models.AsyncAPIResult;
import com.yahoo.elide.async.models.TableExport;
import com.yahoo.elide.async.models.TableExportResult;
import com.yahoo.elide.async.service.AsyncExecutorService;
import com.yahoo.elide.async.service.storageengine.ResultStorageEngine;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.exceptions.BadRequestException;
import com.yahoo.elide.core.exceptions.TransactionException;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.security.User;

import io.reactivex.Observable;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;

import java.util.concurrent.Callable;

/**
 * TableExport Execute Operation Interface.
 */
@Slf4j
public abstract class TableExportCallableOperation implements Callable<AsyncAPIResult> {
    private TableExportFormatter formatter;
    @Getter private AsyncExecutorService service;
    private Integer recordNumber = 0;
    private TableExport queryObj;
    private RequestScope scope;

    public TableExportCallableOperation (TableExportFormatter formatter, AsyncExecutorService service, AsyncAPI queryObj,
            RequestScope scope) {
        this.formatter = formatter;
        this.service = service;
        this.queryObj = (TableExport) queryObj;
        this.scope = scope;
    }

    @Override
    public AsyncAPIResult call() {
        String apiVersion = scope.getApiVersion();
        TableExport query = queryObj;

        EntityProjection projection = getProjection(query, apiVersion);

        Observable<PersistentResource> observableResults = export(query, scope, projection);

        Observable<String> results = Observable.empty();
        String preResult = formatter.preFormat(projection, query);
        results = observableResults.map(resource -> {
            this.recordNumber++;
            return formatter.format(resource, recordNumber);
        });
        String postResult = formatter.postFormat(projection, query);

        // Stitch together Pre-Formatted, Formatted, Post-Formatted results of Formatter in single observable.
        Observable<String> finalResults = Observable.empty();

        if (preResult != null) {
            finalResults = Observable.just(preResult).concatWith(results);
        } else {
            finalResults = results;
        }

        if (postResult != null) {
            finalResults.concatWith(Observable.just(postResult));
        }

        storeResults(query, service.getResultStorageEngine(), results);

        URL downloadURL;
        try {
            downloadURL = new URL(generateDownloadURL(query, (RequestScope) scope));
        } catch (MalformedURLException e) {
            log.debug("Generating download url failure. {}", e.getMessage());
            throw new IllegalStateException(e);
        }

        TableExportResult queryResult = new TableExportResult();
        queryResult.setHttpStatus(200);
        queryResult.setCompletedOn(new Date());
        queryResult.setUrl(downloadURL);
        queryResult.setRecordCount(recordNumber);
        return queryResult;
    }

    /**
     * Export Table Data.
     * @param query TableExport type object.
     * @param requestScope RequestScope object.
     * @param projection Entity projection.
     * @return Observable PersistentResource
     */
    public Observable<PersistentResource> export(TableExport query, RequestScope requestScope,
            EntityProjection projection) {
        Observable<PersistentResource> results = Observable.empty();
        Elide elide = service.getElide();

        UUID requestId = UUID.fromString(query.getRequestId());

        try (DataStoreTransaction tx = elide.getDataStore().beginTransaction()) {
            elide.getTransactionRegistry().addRunningTransaction(requestId, tx);

            //TODO - we need to add the baseUrlEndpoint to the queryObject.
            //TODO - Can we have projectionInfo as null?
            RequestScope exportRequestScope = getRequestScope(query, requestScope.getUser(),
                    requestScope.getApiVersion(), tx);

            if (projection != null) {
                results = PersistentResource.loadRecords(projection, Collections.emptyList(), exportRequestScope);
            }

            tx.preCommit(requestScope);
            requestScope.runQueuedPreSecurityTriggers();
            requestScope.getPermissionExecutor().executeCommitChecks();

            tx.flush(requestScope);

            requestScope.runQueuedPreCommitTriggers();

            elide.getAuditLogger().commit();
            tx.commit(requestScope);

            requestScope.runQueuedPostCommitTriggers();
        } catch (IOException e) {
            log.error("IOException during TableExport", e);
            throw new TransactionException(e);
        } finally {
            elide.getTransactionRegistry().removeRunningTransaction(requestId);
            elide.getAuditLogger().clear();
        }

        return results;
    }

    /**
     * Initialize and get RequestScope.
     * @param query TableExport type object.
     * @param user User Object.
     * @param apiVersion API Version.
     * @param tx DataStoreTransaction.
     * @return RequestScope Type Object
     */
    public abstract RequestScope getRequestScope(TableExport query, User user,
            String apiVersion, DataStoreTransaction tx);

    /**
     * Generate Download URL.
     * @param query TableExport type object.
     * @param scope RequestScope.
     * @return URL generated.
     */
    public String generateDownloadURL(TableExport query, RequestScope scope) {
        String downloadPath =  scope.getElideSettings().getDownloadApiPath();
        String baseURL = scope.getBaseUrlEndPoint();
        return baseURL + downloadPath + "/" + query.getId();
    }

    /**
     * Store Export Results using the ResultStorageEngine.
     * @param query TableExport type object.
     * @param resultStorageEngine ResultStorageEngine instance.
     * @param result Observable of String Results to store.
     * @return TableExport object.
     */
    protected TableExport storeResults(TableExport query, ResultStorageEngine resultStorageEngine,
            Observable<String> result) {
        return resultStorageEngine.storeResults(query, result);
    }

    /**
     * Generate Entity Projection from the query.
     * @param query TableExport type object.
     * @param apiVersion API Version.
     * @return EntityProjection object.
     * @throws BadRequestException BadRequestException.
     */
    public abstract EntityProjection getProjection(TableExport query, String apiVersion)
            throws BadRequestException;

}
