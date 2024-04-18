/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.async.operation;

import com.paiondata.elide.Elide;
import com.paiondata.elide.async.AsyncSettings;
import com.paiondata.elide.async.ResultTypeFileExtensionMapper;
import com.paiondata.elide.async.export.formatter.ResourceWriter;
import com.paiondata.elide.async.export.formatter.TableExportFormatter;
import com.paiondata.elide.async.export.validator.SingleRootProjectionValidator;
import com.paiondata.elide.async.export.validator.Validator;
import com.paiondata.elide.async.models.AsyncApi;
import com.paiondata.elide.async.models.AsyncApiResult;
import com.paiondata.elide.async.models.TableExport;
import com.paiondata.elide.async.models.TableExportResult;
import com.paiondata.elide.async.service.AsyncExecutorService;
import com.paiondata.elide.async.service.storageengine.ResultStorageEngine;
import com.paiondata.elide.core.PersistentResource;
import com.paiondata.elide.core.RequestScope;
import com.paiondata.elide.core.datastore.DataStoreTransaction;
import com.paiondata.elide.core.exceptions.BadRequestException;
import com.paiondata.elide.core.request.EntityProjection;
import com.paiondata.elide.graphql.GraphQLSettings;
import com.paiondata.elide.jsonapi.JsonApiSettings;

import io.reactivex.Observable;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
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
import java.util.function.Consumer;

/**
 * TableExport Execute Operation Interface.
 */
@Slf4j
public abstract class TableExportOperation implements Callable<AsyncApiResult> {
    private TableExportFormatter formatter;
    @Getter private AsyncExecutorService service;
    private Integer recordNumber = 0;
    private TableExport exportObj;
    private RequestScope scope;
    private ResultStorageEngine engine;
    private List<Validator> validators = new ArrayList<>(Arrays.asList(new SingleRootProjectionValidator()));
    private ResultTypeFileExtensionMapper resultTypeFileExtensionMapper;

    public TableExportOperation(TableExportFormatter formatter, AsyncExecutorService service,
            AsyncApi exportObj, RequestScope scope, ResultStorageEngine engine, List<Validator> validators,
            ResultTypeFileExtensionMapper resultTypeFileExtensionMapper) {
        this.formatter = formatter;
        this.service = service;
        this.exportObj = (TableExport) exportObj;
        this.scope = scope;
        this.engine = engine;
        this.validators.addAll(validators);
        this.resultTypeFileExtensionMapper = resultTypeFileExtensionMapper;
    }

    @Override
    public AsyncApiResult call() {
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

            Observable<PersistentResource> results = observableResults;
            Consumer<OutputStream> data = outputStream -> {
                try (ResourceWriter writer = formatter.newResourceWriter(outputStream, projection, exportObj)) {
                    results.subscribe(resource -> {
                        this.recordNumber++;
                        writer.write(resource);
                    });
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            };

            TableExportResult result = storeResults(exportObj, engine, data);

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
        AsyncSettings asyncSettings = scope.getElideSettings().getSettings(AsyncSettings.class);
        JsonApiSettings jsonApiSettings = scope.getElideSettings().getSettings(JsonApiSettings.class);
        GraphQLSettings graphqlSettings = scope.getElideSettings().getSettings(GraphQLSettings.class);

        String downloadPath = asyncSettings.getExport().getPath();
        String baseURL = scope.getRoute().getBaseUrl();
        if (jsonApiSettings != null) {
            String jsonApiPath = jsonApiSettings.getPath();
            if (jsonApiPath != null && baseURL.endsWith(jsonApiPath)) {
                baseURL = baseURL.substring(0, baseURL.length() - jsonApiPath.length());
            }
        }
        if (graphqlSettings != null) {
            String graphqlApiPath = graphqlSettings.getPath();
            if (graphqlApiPath != null && baseURL.endsWith(graphqlApiPath)) {
                baseURL = baseURL.substring(0, baseURL.length() - graphqlApiPath.length());
            }
        }
        String tableExportID = getTableExportID(exportObj);
        return baseURL + downloadPath + "/" + tableExportID;
    }

    /**
     * Store Export Results using the ResultStorageEngine.
     * @param exportObj TableExport type object.
     * @param resultStorageEngine ResultStorageEngine instance.
     * @param result Observable of String Results to store.
     * @return TableExportResult object.
     */
    protected TableExportResult storeResults(TableExport exportObj, ResultStorageEngine resultStorageEngine,
            Consumer<OutputStream> result) {
        return resultStorageEngine.storeResults(getTableExportID(exportObj), result);
    }

    protected String getTableExportID(TableExport exportObj) {
        String extension = resultTypeFileExtensionMapper != null
                ? resultTypeFileExtensionMapper.getFileExtension(exportObj.getResultType())
                : "";
        return exportObj.getId() + extension;
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
