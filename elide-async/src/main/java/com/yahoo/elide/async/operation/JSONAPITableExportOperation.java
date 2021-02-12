/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.operation;

import com.yahoo.elide.Elide;
import com.yahoo.elide.async.export.formatter.TableExportFormatter;
import com.yahoo.elide.async.export.validator.NoRelationshipsProjectionValidator;
import com.yahoo.elide.async.models.AsyncAPI;
import com.yahoo.elide.async.models.TableExport;
import com.yahoo.elide.async.service.AsyncExecutorService;
import com.yahoo.elide.async.service.storageengine.ResultStorageEngine;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.exceptions.BadRequestException;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.security.User;
import com.yahoo.elide.jsonapi.EntityProjectionMaker;
import org.apache.http.client.utils.URIBuilder;

import lombok.extern.slf4j.Slf4j;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

import javax.ws.rs.core.MultivaluedMap;

/**
 * JSONAPI TableExport Execute Operation.
 */
@Slf4j
public class JSONAPITableExportOperation extends TableExportOperation {

    public JSONAPITableExportOperation(TableExportFormatter formatter, AsyncExecutorService service,
            AsyncAPI export, RequestScope scope, ResultStorageEngine engine) {
        super(formatter, service, export, scope, engine,
                        Arrays.asList(new NoRelationshipsProjectionValidator()));
    }

    @Override
    public RequestScope getRequestScope(TableExport export, User user, String apiVersion, DataStoreTransaction tx) {
        UUID requestId = UUID.fromString(export.getRequestId());
        URIBuilder uri;
        try {
            uri = new URIBuilder(export.getQuery());
        } catch (URISyntaxException e) {
            throw new BadRequestException(e.getMessage());
        }
        MultivaluedMap<String, String> queryParams = JSONAPIAsyncQueryOperation.getQueryParams(uri);
        return new RequestScope("", JSONAPIAsyncQueryOperation.getPath(uri), apiVersion, null, tx, user, queryParams,
                Collections.emptyMap(), requestId, getService().getElide().getElideSettings());
    }

    @Override
    public Collection<EntityProjection> getProjections(TableExport export, RequestScope scope) {
        EntityProjection projection = null;
        try {
            URIBuilder uri = new URIBuilder(export.getQuery());
            Elide elide = getService().getElide();
            projection = new EntityProjectionMaker(elide.getElideSettings().getDictionary(),
                    scope).parsePath(JSONAPIAsyncQueryOperation.getPath(uri));

        } catch (URISyntaxException e) {
            throw new BadRequestException(e.getMessage());
        }
        return Collections.singletonList(projection);
    }
}
