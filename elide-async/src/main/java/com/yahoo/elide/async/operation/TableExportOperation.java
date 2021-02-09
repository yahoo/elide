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
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;

import java.util.concurrent.Callable;

/**
 * TableExport Execute Operation Interface.
 */
@Slf4j
public abstract class TableExportOperation implements Callable<AsyncAPIResult> {
    private TableExportFormatter formatter;
    @Getter private AsyncExecutorService service;
    private Integer recordNumber = 0;
    private TableExport exportObj;
    @Getter private RequestScope scope;

    public TableExportOperation(TableExportFormatter formatter, AsyncExecutorService service,
            AsyncAPI exportObj, RequestScope scope) {
        this.formatter = formatter;
        this.service = service;
        this.exportObj = (TableExport) exportObj;
        this.scope = scope;
    }

    @Override
    public AsyncAPIResult call() {
        String apiVersion = scope.getApiVersion();
        log.debug("TableExport Object from request: {}", exportObj);

        TableExportResult exportResult = new TableExportResult();
        try {
            EntityProjection projection = getProjection(exportObj, apiVersion);

            Observable<PersistentResource> observableResults = export(exportObj, scope, projection);

            Observable<String> results = Observable.empty();
            String preResult = formatter.preFormat(projection, exportObj);
            results = observableResults.map(resource -> {
                this.recordNumber++;
                return formatter.format(resource, recordNumber);
            });
            String postResult = formatter.postFormat(projection, exportObj);

            // Stitch together Pre-Formatted, Formatted, Post-Formatted results of Formatter in single observable.
            Observable<String> interimResults = concatStringWithObservable(preResult, results, true);
            Observable<String> finalResults = concatStringWithObservable(postResult, interimResults, false);

            storeResults(exportObj, service.getResultStorageEngine(), finalResults);

            exportResult.setUrl(new URL(generateDownloadURL(exportObj, (RequestScope) scope)));
            exportResult.setRecordCount(recordNumber);
        } catch (BadRequestException e) {
            exportResult.setMessage("Download url generation failure.");
        } catch (MalformedURLException e) {
            exportResult.setMessage("EntityProjection generation failure.");
        }  catch (Exception e) {
            exportResult.setMessage(e.getMessage());
        } finally {
            // Follows same flow as GraphQL. The query may result in failure but request was successfully processed.
            exportResult.setHttpStatus(200);
            exportResult.setCompletedOn(new Date());
            return exportResult;
        }
    }

    private Observable<String> concatStringWithObservable(String toConcat, Observable<String> observable,
            boolean stringFirst) {
        if (toConcat == null) {
            return observable;
        }

        return stringFirst ? Observable.just(toConcat).concatWith(observable)
                : observable.concatWith(Observable.just(toConcat));
    }

    /**
     * Export Table Data.
     * @param exportObj TableExport type object.
     * @param prevScope RequestScope object.
     * @param projection Entity projection.
     * @return Observable PersistentResource
     * @throws URISyntaxException
     */
    public Observable<PersistentResource> export(TableExport exportObj, RequestScope prevScope,
            EntityProjection projection) throws URISyntaxException {
        Observable<PersistentResource> results = Observable.empty();
        Elide elide = service.getElide();

        UUID requestId = UUID.fromString(exportObj.getRequestId());

        try (DataStoreTransaction tx = elide.getDataStore().beginTransaction()) {
            elide.getTransactionRegistry().addRunningTransaction(requestId, tx);

            //TODO - we need to add the baseUrlEndpoint to the queryObject.
            //TODO - Can we have projectionInfo as null?
            RequestScope exportRequestScope = getRequestScope(exportObj, prevScope.getUser(),
                    prevScope.getApiVersion(), tx);

            if (projection != null) {
                results = PersistentResource.loadRecords(projection, Collections.emptyList(), exportRequestScope);
            }

            tx.preCommit(exportRequestScope);
            exportRequestScope.runQueuedPreSecurityTriggers();
            exportRequestScope.getPermissionExecutor().executeCommitChecks();

            tx.flush(exportRequestScope);

            exportRequestScope.runQueuedPreCommitTriggers();

            elide.getAuditLogger().commit();
            tx.commit(exportRequestScope);

            exportRequestScope.runQueuedPostCommitTriggers();
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
     * @param exportObj TableExport type object.
     * @param user User Object.
     * @param apiVersion API Version.
     * @param tx DataStoreTransaction.
     * @return RequestScope Type Object
     * @throws URISyntaxException
     */
    public abstract RequestScope getRequestScope(TableExport exportObj, User user,
            String apiVersion, DataStoreTransaction tx) throws URISyntaxException;

    /**
     * Generate Download URL.
     * @param exportObj TableExport type object.
     * @param scope RequestScope.
     * @return URL generated.
     */
    public String generateDownloadURL(TableExport exportObj, RequestScope scope) {
        String downloadPath =  scope.getElideSettings().getDownloadApiPath();
        String baseURL = scope.getBaseUrlEndPoint();
        return baseURL + downloadPath + "/" + exportObj.getId();
    }

    /**
     * Store Export Results using the ResultStorageEngine.
     * @param exportObj TableExport type object.
     * @param resultStorageEngine ResultStorageEngine instance.
     * @param result Observable of String Results to store.
     * @return TableExport object.
     */
    protected TableExport storeResults(TableExport exportObj, ResultStorageEngine resultStorageEngine,
            Observable<String> result) {
        return resultStorageEngine.storeResults(exportObj, result);
    }

    /**
     * Generate Entity Projection from the query.
     * @param exportObj TableExport type object.
     * @param apiVersion API Version.
     * @return EntityProjection object.
     * @throws BadRequestException BadRequestException.
     */
    public abstract EntityProjection getProjection(TableExport exportObj, String apiVersion)
            throws BadRequestException;

}
