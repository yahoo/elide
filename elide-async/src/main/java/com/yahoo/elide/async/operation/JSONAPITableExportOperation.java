/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.operation;

import com.yahoo.elide.Elide;
import com.yahoo.elide.async.export.formatter.TableExportFormatter;
import com.yahoo.elide.async.models.AsyncAPI;
import com.yahoo.elide.async.models.TableExport;
import com.yahoo.elide.async.service.AsyncExecutorService;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.exceptions.BadRequestException;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.security.User;
import com.yahoo.elide.jsonapi.EntityProjectionMaker;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;

import lombok.extern.slf4j.Slf4j;

import java.net.URISyntaxException;
import java.util.Collections;
import java.util.UUID;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

@Slf4j
public class JSONAPITableExportOperation extends TableExportOperation {

    private static final String PATH = "/tableExport";

    public JSONAPITableExportOperation(TableExportFormatter formatter, AsyncExecutorService service,
            AsyncAPI export, RequestScope scope) {
        super(formatter, service, export, scope);
    }

    @Override
    public RequestScope getRequestScope(TableExport export, User user, String apiVersion, DataStoreTransaction tx)
            throws URISyntaxException {
        UUID requestId = UUID.fromString(export.getRequestId());
        MultivaluedMap<String, String> queryParams = getQueryParams(export.getQuery());
        return new RequestScope("", "", apiVersion, null, tx, user, queryParams,
                Collections.emptyMap(), requestId, getService().getElide().getElideSettings());
    }

    @Override
    public EntityProjection getProjection(TableExport export, String apiVersion) throws BadRequestException {
        EntityProjection projection = null;
        try {
            Elide elide = getService().getElide();
            projection = new EntityProjectionMaker(elide.getElideSettings().getDictionary(),
                    getScope()).parsePath(PATH);

        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return projection;
    }

    /**
     * This method parses the url and gets the query params.
     * And adds them into a MultivaluedMap
     * @param uri URIBuilder instance
     * @return MultivaluedMap with query parameters
     * @throws URISyntaxException
     */
    private MultivaluedMap<String, String> getQueryParams(String query) throws URISyntaxException {
        URIBuilder uri = new URIBuilder(query);
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<String, String>();
        for (NameValuePair queryParam : uri.getQueryParams()) {
            queryParams.add(queryParam.getName(), queryParam.getValue());
        }
        return queryParams;
    }
}
