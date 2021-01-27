/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.operation;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.async.models.AsyncAPI;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.service.AsyncExecutorService;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.security.User;

import com.jayway.jsonpath.JsonPath;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;

import lombok.extern.slf4j.Slf4j;

import java.net.URISyntaxException;
import java.util.UUID;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

/**
 * JSON API implementation of AsyncQueryOperation for executing the query provided in AsyncQuery.
 */
@Slf4j
public class JSONAPIAsyncQueryOperation extends AsyncQueryOperation {

    public JSONAPIAsyncQueryOperation(AsyncExecutorService service, AsyncAPI queryObj, RequestScope scope) {
        super(service, queryObj, scope);
    }

    @Override
    public ElideResponse execute(AsyncAPI queryObj, User user, String apiVersion)
            throws URISyntaxException {
        Elide elide = getService().getElide();
        UUID requestUUID = UUID.fromString(queryObj.getRequestId());
        URIBuilder uri = new URIBuilder(queryObj.getQuery());
        MultivaluedMap<String, String> queryParams = getQueryParams(uri);
        log.debug("Extracted QueryParams from AsyncQuery Object: {}", queryParams);

        //TODO - we need to add the baseUrlEndpoint to the queryObject.
        ElideResponse response = elide.get("", getPath(uri), queryParams, user, apiVersion, requestUUID);
        log.debug("JSONAPI_V1_0 getResponseCode: {}, JSONAPI_V1_0 getBody: {}",
                response.getResponseCode(), response.getBody());
        return response;
    }

    /**
     * This method parses the url and gets the query params.
     * And adds them into a MultivaluedMap to be used by underlying Elide.get method
     * @param uri URIBuilder instance
     * @return MultivaluedMap with query parameters
     */
    private MultivaluedMap<String, String> getQueryParams(URIBuilder uri) {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<String, String>();
        for (NameValuePair queryParam : uri.getQueryParams()) {
            queryParams.add(queryParam.getName(), queryParam.getValue());
        }
        return queryParams;
    }

    /**
     * This method parses the url and gets the query params.
     * And retrieves path to be used by underlying Elide.get method
     * @param uri URIBuilder instance
     * @throws URISyntaxException URISyntaxException from malformed or incorrect URI
     * @return Path extracted from URI
     */
    private String getPath(URIBuilder uri) {
        return uri.getPath();
    }

    @Override
    public Integer calculateRecordCount(AsyncQuery queryObj, ElideResponse response) {
        Integer count = null;
        if (response.getResponseCode() == 200) {
            count = JsonPath.read(response.getBody(), "$.data.length()");
        }
        return count;
    }
}
