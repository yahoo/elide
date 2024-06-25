/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.operation;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideModules;
import com.yahoo.elide.async.AsyncSettings;
import com.yahoo.elide.async.ResultTypeFileExtensionMapper;
import com.yahoo.elide.async.export.formatter.ResourceWriter;
import com.yahoo.elide.async.export.formatter.TableExportFormatter;
import com.yahoo.elide.async.export.validator.SingleRootProjectionValidator;
import com.yahoo.elide.async.export.validator.Validator;
import com.yahoo.elide.async.models.AsyncApi;
import com.yahoo.elide.async.models.AsyncApiResult;
import com.yahoo.elide.async.models.TableExport;
import com.yahoo.elide.async.models.TableExportResult;
import com.yahoo.elide.async.service.AsyncExecutorService;
import com.yahoo.elide.async.service.storageengine.ResultStorageEngine;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.exceptions.BadRequestException;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.graphql.GraphQLSettings;
import com.yahoo.elide.jsonapi.JsonApiSettings;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

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

    /**
     * Holder to prevent class not found.
     */
    public static class GraphQL {
        public static String getBaseUrl(RequestScope scope, String baseUrl) {
            GraphQLSettings graphqlSettings = scope.getElideSettings().getSettings(GraphQLSettings.class);
            if (graphqlSettings != null) {
                String graphqlApiPath = graphqlSettings.getPath();
                if (graphqlApiPath != null && baseUrl.endsWith(graphqlApiPath)) {
                    baseUrl = baseUrl.substring(0, baseUrl.length() - graphqlApiPath.length());
                }
            }
            return baseUrl;
        }
    }

    /**
     * Holder to prevent class no found.
     */
    public static class JsonApi {
        public static String getBaseUrl(RequestScope scope, String baseUrl) {
            JsonApiSettings jsonApiSettings = scope.getElideSettings().getSettings(JsonApiSettings.class);
            if (jsonApiSettings != null) {
                String jsonApiPath = jsonApiSettings.getPath();
                if (jsonApiPath != null && baseUrl.endsWith(jsonApiPath)) {
                    baseUrl = baseUrl.substring(0, baseUrl.length() - jsonApiPath.length());
                }
            }
            return baseUrl;
        }
    }

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

            Flux<PersistentResource> observableResults = Flux.empty();

            elide.getTransactionRegistry().addRunningTransaction(requestId, tx);

            //TODO - we need to add the baseUrlEndpoint to the queryObject.
            //TODO - Can we have projectionInfo as null?
            requestScope.setEntityProjection(projection);

            if (projection != null) {
                projection.setPagination(null);
                observableResults = PersistentResource.loadRecords(projection, Collections.emptyList(), requestScope);
            }

            Flux<PersistentResource> results = observableResults;
            Consumer<OutputStream> data = outputStream -> {
                try (ResourceWriter writer = formatter.newResourceWriter(outputStream, projection, exportObj)) {
                    results.subscribe(resource -> {
                        this.recordNumber++;
                        try {
                            writer.write(resource);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
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

    private Flux<String> concatStringWithFlux(String toConcat, Flux<String> observable,
            boolean stringFirst) {
        if (toConcat == null) {
            return observable;
        }

        return stringFirst ? Flux.just(toConcat).concatWith(observable)
                : observable.concatWith(Flux.just(toConcat));
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

        String downloadPath = asyncSettings.getExport().getPath();
        String baseURL = scope.getRoute().getBaseUrl();
        if (ElideModules.isJsonApiPresent()) {
            baseURL = JsonApi.getBaseUrl(scope, baseURL);
        }
        if (ElideModules.isGraphQLPresent()) {
            baseURL = GraphQL.getBaseUrl(scope, baseURL);
        }
        String tableExportID = getTableExportID(exportObj);
        return baseURL + downloadPath + "/" + tableExportID;
    }

    /**
     * Store Export Results using the ResultStorageEngine.
     * @param exportObj TableExport type object.
     * @param resultStorageEngine ResultStorageEngine instance.
     * @param result Flux of String Results to store.
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
