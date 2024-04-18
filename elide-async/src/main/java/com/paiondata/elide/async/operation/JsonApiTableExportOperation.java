/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.async.operation;

import com.paiondata.elide.Elide;
import com.paiondata.elide.async.ResultTypeFileExtensionMapper;
import com.paiondata.elide.async.export.formatter.TableExportFormatter;
import com.paiondata.elide.async.export.validator.NoRelationshipsProjectionValidator;
import com.paiondata.elide.async.models.AsyncApi;
import com.paiondata.elide.async.models.TableExport;
import com.paiondata.elide.async.service.AsyncExecutorService;
import com.paiondata.elide.async.service.storageengine.ResultStorageEngine;
import com.paiondata.elide.core.RequestScope;
import com.paiondata.elide.core.datastore.DataStoreTransaction;
import com.paiondata.elide.core.exceptions.BadRequestException;
import com.paiondata.elide.core.request.EntityProjection;
import com.paiondata.elide.core.request.route.Route;
import com.paiondata.elide.core.security.User;
import com.paiondata.elide.jsonapi.EntityProjectionMaker;
import com.paiondata.elide.jsonapi.JsonApiRequestScope;

import org.apache.hc.core5.net.URIBuilder;

import lombok.extern.slf4j.Slf4j;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * JSON-API TableExport Execute Operation.
 */
@Slf4j
public class JsonApiTableExportOperation extends TableExportOperation {

    public JsonApiTableExportOperation(TableExportFormatter formatter, AsyncExecutorService service,
            AsyncApi export, RequestScope scope, ResultStorageEngine engine,
            ResultTypeFileExtensionMapper resultTypeFileExtensionMapper) {
        super(formatter, service, export, scope, engine, Arrays.asList(new NoRelationshipsProjectionValidator()),
                resultTypeFileExtensionMapper);
    }

    @Override
    public RequestScope getRequestScope(TableExport export, RequestScope scope, DataStoreTransaction tx,
            Map<String, List<String>> additionalRequestHeaders) {
        UUID requestId = UUID.fromString(export.getRequestId());
        User user = scope.getUser();
        String apiVersion = scope.getRoute().getApiVersion();
        URIBuilder uri;
        try {
            uri = new URIBuilder(export.getQuery());
        } catch (URISyntaxException e) {
            throw new BadRequestException(e.getMessage());
        }

        Map<String, List<String>> queryParams = JsonApiAsyncQueryOperation.getQueryParams(uri);

        // Call with additionalHeader alone
        if (scope.getRoute().getHeaders().isEmpty()) {
            Route route = Route.builder().baseUrl("").path(JsonApiAsyncQueryOperation.getPath(uri))
                    .apiVersion(apiVersion).headers(additionalRequestHeaders).parameters(queryParams).build();

            return JsonApiRequestScope.builder().route(route).dataStoreTransaction(tx).user(user).requestId(requestId)
                    .elideSettings(getService().getElide().getElideSettings()).build();
        }

        // Combine additionalRequestHeaders and existing scope's request headers
        Map<String, List<String>> finalRequestHeaders = new HashMap<>();
        scope.getRoute().getHeaders().forEach(finalRequestHeaders::put);

        //additionalRequestHeaders will override any headers in scope.getRequestHeaders()
        additionalRequestHeaders.forEach(finalRequestHeaders::put);

        Route route = Route.builder().baseUrl("").path(JsonApiAsyncQueryOperation.getPath(uri))
                .apiVersion(apiVersion).headers(scope.getRoute().getHeaders()).parameters(queryParams).build();
        return JsonApiRequestScope.builder().route(route).dataStoreTransaction(tx).user(user).requestId(requestId)
                .elideSettings(getService().getElide().getElideSettings()).build();
    }

    @Override
    public Collection<EntityProjection> getProjections(TableExport export, RequestScope scope) {
        EntityProjection projection = null;
        try {
            URIBuilder uri = new URIBuilder(export.getQuery());
            Elide elide = getService().getElide();
            projection = new EntityProjectionMaker(elide.getElideSettings().getEntityDictionary(),
                    scope).parsePath(JsonApiAsyncQueryOperation.getPath(uri));

        } catch (URISyntaxException e) {
            throw new BadRequestException(e.getMessage());
        }
        return Collections.singletonList(projection);
    }
}
