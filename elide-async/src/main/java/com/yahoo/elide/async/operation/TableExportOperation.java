/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.operation;

import com.yahoo.elide.Elide;
import com.yahoo.elide.async.export.formatter.TableExportFormatter;
import com.yahoo.elide.async.export.validator.SingleRootProjectionValidator;
import com.yahoo.elide.async.export.validator.Validator;
import com.yahoo.elide.async.models.AsyncAPI;
import com.yahoo.elide.async.models.AsyncAPIResult;
import com.yahoo.elide.async.models.FileExtensionType;
import com.yahoo.elide.async.models.TableExport;
import com.yahoo.elide.async.models.TableExportResult;
import com.yahoo.elide.async.service.AsyncExecutorService;
import com.yahoo.elide.async.service.storageengine.ResultStorageEngine;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.exceptions.BadRequestException;
import com.yahoo.elide.core.request.EntityProjection;

import io.reactivex.Observable;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private RequestScope scope;
    private ResultStorageEngine engine;
    private List<Validator> validators = new ArrayList<>(Arrays.asList(new SingleRootProjectionValidator()));

    public TableExportOperation(TableExportFormatter formatter, AsyncExecutorService service,
            AsyncAPI exportObj, RequestScope scope, ResultStorageEngine engine, List<Validator> validators) {
        this.formatter = formatter;
        this.service = service;
        this.exportObj = (TableExport) exportObj;
        this.scope = scope;
        this.engine = engine;
        this.validators.addAll(validators);
    }

    @Override
    public AsyncAPIResult call() {
        log.debug("TableExport Object from request: {}", exportObj);
        Elide elide = service.getElide();
        TableExportResult exportResult = new TableExportResult();
        UUID requestId = UUID.fromString(exportObj.getRequestId());
        try (DataStoreTransaction tx = elide.getDataStore().beginTransaction()) {
            // Do Not Cache Export Results
            Map<String, List<String>> requestHeaders = new HashMap<String, List<String>>();
            requestHeaders.put("bypasscache", new ArrayList<String>(Arrays.asList("true")));

            RequestScope requestScope = getRequestScope(exportObj, scope, tx, requestHeaders);
            Collection<EntityProjection> projections = getProjections(exportObj, requestScope);
            validateProjections(projections);
            EntityProjection projection = projections.iterator().next();

            Observable<PersistentResource> observableResults = Observable.empty();

            elide.getTransactionRegistry().addRunningTransaction(requestId, tx);

            //TODO - we need to add the baseUrlEndpoint to the queryObject.
            //TODO - Can we have projectionInfo as null?
            requestScope.setEntityProjection(projection);

            if (projection != null) {
                projection.setPagination(null);
                observableResults = PersistentResource.loadRecords(projection, Collections.emptyList(), requestScope);
            }

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

            TableExportResult result = storeResults(exportObj, engine, finalResults);

            if (result != null && result.getMessage() != null) {
                throw new IllegalStateException(result.getMessage());
            }

            exportResult.setUrl(new URL(generateDownloadURL(exportObj, scope)));
            exportResult.setRecordCount(recordNumber);

            tx.flush(requestScope);
            elide.getAuditLogger().commit();
            tx.commit(requestScope);
        } catch (BadRequestException e) {
            exportResult.setMessage(e.getMessage());
        } catch (MalformedURLException e) {
            exportResult.setMessage("Download url generation failure.");
        } catch (IOException e) {
            log.error("IOException during TableExport", e);
            exportResult.setMessage(e.getMessage());
        } catch (Exception e) {
            exportResult.setMessage(e.getMessage());
        } finally {
            // Follows same flow as GraphQL. The query may result in failure but request was successfully processed.
            exportResult.setHttpStatus(200);
            exportResult.setCompletedOn(new Date());
            elide.getTransactionRegistry().removeRunningTransaction(requestId);
            elide.getAuditLogger().clear();
        }
        return exportResult;
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
     * Initializes a new RequestScope for the export operation with the submitted query.
     * @param exportObj TableExport type object.
     * @param scope RequestScope from the original submission.
     * @param tx DataStoreTransaction.
     * @param additionalRequestHeaders Additional Request Headers.
     * @return RequestScope Type Object
     */
    public abstract RequestScope getRequestScope(TableExport exportObj, RequestScope scope, DataStoreTransaction tx,
            Map<String, List<String>> additionalRequestHeaders);

    /**
     * Generate Download URL.
     * @param exportObj TableExport type object.
     * @param scope RequestScope.
     * @return URL generated.
     */
    public String generateDownloadURL(TableExport exportObj, RequestScope scope) {
        String downloadPath =  scope.getElideSettings().getExportApiPath();
        String baseURL = scope.getBaseUrlEndPoint();
        String extension = this.engine.isExtensionEnabled()
                ? exportObj.getResultType().getFileExtensionType().getExtension()
                : FileExtensionType.NONE.getExtension();
        return baseURL + downloadPath + "/" + exportObj.getId() + extension;
    }

    /**
     * Store Export Results using the ResultStorageEngine.
     * @param exportObj TableExport type object.
     * @param resultStorageEngine ResultStorageEngine instance.
     * @param result Observable of String Results to store.
     * @return TableExportResult object.
     */
    protected TableExportResult storeResults(TableExport exportObj, ResultStorageEngine resultStorageEngine,
            Observable<String> result) {
        return resultStorageEngine.storeResults(exportObj, result);
    }

    private void validateProjections(Collection<EntityProjection> projections) {
        validators.forEach(validator -> validator.validateProjection(projections));
    }

    /**
     * Generate Entity Projection from the query.
     * @param exportObj TableExport type object.
     * @param requestScope requestScope object.
     * @return Collection of EntityProjection object.
     */
    public abstract Collection<EntityProjection> getProjections(TableExport exportObj, RequestScope requestScope);

}
