/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service.thread;


import com.yahoo.elide.Elide;
import com.yahoo.elide.async.export.formatter.CSVExportFormatter;
import com.yahoo.elide.async.export.formatter.JSONExportFormatter;
import com.yahoo.elide.async.export.formatter.TableExportFormatter;
import com.yahoo.elide.async.export.validator.SingleRootProjectionValidator;
import com.yahoo.elide.async.export.validator.Validator;
import com.yahoo.elide.async.models.AsyncAPI;
import com.yahoo.elide.async.models.AsyncAPIJob;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.QueryStatus;
import com.yahoo.elide.async.models.ResultType;
import com.yahoo.elide.async.models.TableExport;
import com.yahoo.elide.async.models.TableExportResult;
import com.yahoo.elide.async.service.dao.AsyncAPIDAO;
import com.yahoo.elide.async.service.storageengine.FileResultStorageEngine;
import com.yahoo.elide.async.service.storageengine.ResultStorageEngine;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.exceptions.BadRequestException;
import com.yahoo.elide.core.exceptions.TransactionException;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.security.User;
import com.yahoo.elide.graphql.GraphQLRequestScope;
import com.yahoo.elide.graphql.QueryRunner;
import com.yahoo.elide.graphql.parser.GraphQLEntityProjectionMaker;
import com.yahoo.elide.graphql.parser.GraphQLProjectionInfo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.reactivex.Observable;
import lombok.Data;
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

/**
 * Runnable for running the async queries in background.
 */
@Slf4j
@Data
public class AsyncAPIBackgroundRunnable implements Runnable {

    private Elide elide;
    private AsyncAPIDAO asyncAPIDao;
    private AsyncAPIJob job;
    private RequestScope scope;
    private Integer recordNumber = 0;

    public AsyncAPIBackgroundRunnable(Elide elide, AsyncAPIDAO asyncAPIDao,
            AsyncAPIJob job, RequestScope scope) {
        this.elide = elide;
        this.asyncAPIDao = asyncAPIDao;
        this.job = job;
        this.scope = scope;
    }

    @Override
    public void run() {
        executeAsyncAPI(AsyncQuery.class);
    }

    /**
     * This method deletes the historical queries based on threshold.
     * @param type AsyncAPI Type Implementation.
     */
    protected <T extends AsyncAPI> void executeAsyncAPI(Class<T> type) {
        TableExport exportObj = (TableExport) job.getAsyncApi();
        Map<ResultType, TableExportFormatter> supportedFormatters = new HashMap<>();
        supportedFormatters.put(ResultType.CSV, new CSVExportFormatter(elide, false));
        supportedFormatters.put(ResultType.JSON, new JSONExportFormatter(elide));
        TableExportFormatter formatter = supportedFormatters.get(exportObj.getResultType());

        log.debug("TableExport Object from request: {}", exportObj);
        TableExportResult exportResult = new TableExportResult();
        ResultStorageEngine engine = new FileResultStorageEngine("/tmp");
        try (DataStoreTransaction tx = elide.getDataStore().beginTransaction()) {
        	// Change Status to Processing
        	asyncAPIDao.updateStatus(exportObj.getId(), QueryStatus.PROCESSING, TableExport.class);
            // Do Not Cache Export Results
            Map<String, List<String>> requestHeaders = new HashMap<String, List<String>>();
            requestHeaders.put("bypasscache", new ArrayList<String>(Arrays.asList("true")));

            RequestScope requestScope = getRequestScope(exportObj, scope, tx, requestHeaders);
            Collection<EntityProjection> projections = getProjections(exportObj, requestScope);
            validateProjections(projections);
            EntityProjection projection = projections.iterator().next();

            Observable<PersistentResource> observableResults = export(exportObj, requestScope, projection);

            Observable<String> results = Observable.empty();
            String preResult = formatter.preFormat(projection, exportObj);
            results = observableResults.map(resource -> {
                recordNumber++;
                return formatter.format(resource, recordNumber);
            });
            String postResult = formatter.postFormat(projection, exportObj);

            // Stitch together Pre-Formatted, Formatted, Post-Formatted results of Formatter in single observable.
            Observable<String> interimResults = concatStringWithObservable(preResult, results, true);
            Observable<String> finalResults = concatStringWithObservable(postResult, interimResults, false);

            storeResults(exportObj, engine, finalResults);

            exportResult.setUrl(new URL(generateDownloadURL(exportObj, scope)));
            exportResult.setRecordCount(recordNumber);
            asyncAPIDao.updateStatus(exportObj.getId(), QueryStatus.COMPLETE, TableExport.class);
            exportObj.setStatus(QueryStatus.COMPLETE);
        } catch (BadRequestException e) {
            exportResult.setMessage(e.getMessage());
            asyncAPIDao.updateStatus(exportObj.getId(), QueryStatus.FAILURE, TableExport.class);
            exportObj.setStatus(QueryStatus.FAILURE);
        } catch (MalformedURLException e) {
            exportResult.setMessage("Download url generation failure.");
            asyncAPIDao.updateStatus(exportObj.getId(), QueryStatus.FAILURE, TableExport.class);
            exportObj.setStatus(QueryStatus.FAILURE);
        }  catch (Exception e) {
            exportResult.setMessage(e.getMessage());
            asyncAPIDao.updateStatus(exportObj.getId(), QueryStatus.FAILURE, TableExport.class);
            exportObj.setStatus(QueryStatus.FAILURE);
        } finally {
            // Follows same flow as GraphQL. The query may result in failure but request was successfully processed.
            exportResult.setHttpStatus(200);
            exportResult.setCompletedOn(new Date());
            exportObj.setResult(exportResult);
            asyncAPIDao.updateAsyncAPIResult(exportResult, exportObj.getId(), TableExport.class);
            job.setAsyncApi(exportObj);

            //Notify listeners
            job.getDone().countDown();
        }
    }

    public String generateDownloadURL(TableExport exportObj, RequestScope scope) {
        String downloadPath =  scope.getElideSettings().getExportApiPath();
        String baseURL = scope.getBaseUrlEndPoint();
        return baseURL + downloadPath + "/" + exportObj.getId();
    }

    protected TableExport storeResults(TableExport exportObj, ResultStorageEngine resultStorageEngine,
            Observable<String> result) {
        return resultStorageEngine.storeResults(exportObj, result);
    }

    private Observable<String> concatStringWithObservable(String toConcat, Observable<String> observable,
            boolean stringFirst) {
        if (toConcat == null) {
            return observable;
        }

        return stringFirst ? Observable.just(toConcat).concatWith(observable)
                : observable.concatWith(Observable.just(toConcat));
    }

    private void validateProjections(Collection<EntityProjection> projections) {
        List<Validator> validators = new ArrayList<>(Arrays.asList(new SingleRootProjectionValidator()));
        validators.forEach(validator -> validator.validateProjection(projections));
    }

    public RequestScope getRequestScope(TableExport export, RequestScope scope, DataStoreTransaction tx,
            Map<String, List<String>> additionalRequestHeaders) {
        UUID requestId = UUID.fromString(export.getRequestId());
        User user = scope.getUser();
        String apiVersion = scope.getApiVersion();
        return new GraphQLRequestScope("", tx, user, apiVersion, elide.getElideSettings(),
                null, requestId, additionalRequestHeaders);
    }

    public Collection<EntityProjection> getProjections(TableExport export, RequestScope scope) {
        GraphQLProjectionInfo projectionInfo;
        try {
            String graphQLDocument = export.getQuery();
            ObjectMapper mapper = elide.getMapper().getObjectMapper();

            JsonNode node = QueryRunner.getTopLevelNode(mapper, graphQLDocument);
            Map<String, Object> variables = QueryRunner.extractVariables(mapper, node);
            String queryString = QueryRunner.extractQuery(node);

            projectionInfo = new GraphQLEntityProjectionMaker(elide.getElideSettings(), variables,
                            scope.getApiVersion()).make(queryString);

        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        return projectionInfo.getProjections().values();
    }

    private Observable<PersistentResource> export(TableExport exportObj, RequestScope scope,
            EntityProjection projection) {
        Observable<PersistentResource> results = Observable.empty();

        UUID requestId = UUID.fromString(exportObj.getRequestId());

        try {
            DataStoreTransaction tx = scope.getTransaction();
            elide.getTransactionRegistry().addRunningTransaction(requestId, tx);

            //TODO - we need to add the baseUrlEndpoint to the queryObject.
            //TODO - Can we have projectionInfo as null?
            scope.setEntityProjection(projection);

            if (projection != null) {
                projection.setPagination(null);
                results = PersistentResource.loadRecords(projection, Collections.emptyList(), scope);
            }

            tx.preCommit(scope);
            scope.runQueuedPreSecurityTriggers();
            scope.getPermissionExecutor().executeCommitChecks();

            tx.flush(scope);

            scope.runQueuedPreCommitTriggers();

            elide.getAuditLogger().commit();
            tx.commit(scope);

            scope.runQueuedPostCommitTriggers();
        } catch (IOException e) {
            log.error("IOException during TableExport", e);
            throw new TransactionException(e);
        } finally {
            elide.getTransactionRegistry().removeRunningTransaction(requestId);
            elide.getAuditLogger().clear();
        }

        return results;
    }
}
