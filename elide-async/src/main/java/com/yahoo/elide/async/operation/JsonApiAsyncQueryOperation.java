/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.operation;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.async.models.AsyncApi;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.service.AsyncExecutorService;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.request.route.Route;
import com.yahoo.elide.core.security.User;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;

import lombok.extern.slf4j.Slf4j;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * JSON API implementation of AsyncQueryOperation for executing the query provided in AsyncQuery.
 */
@Slf4j
public class JsonApiAsyncQueryOperation extends AsyncQueryOperation {

    public JsonApiAsyncQueryOperation(AsyncExecutorService service, AsyncApi queryObj, RequestScope scope) {
        super(service, queryObj, scope);
    }

    @Override
    public ElideResponse execute(AsyncApi queryObj, RequestScope scope)
            throws URISyntaxException {
        Elide elide = getService().getElide();
        User user = scope.getUser();
        String apiVersion = scope.getApiVersion();
        UUID requestUUID = UUID.fromString(queryObj.getRequestId());
        URIBuilder uri = new URIBuilder(queryObj.getQuery());
        Map<String, List<String>> queryParams = getQueryParams(uri);
        log.debug("Extracted QueryParams from AsyncQuery Object: {}", queryParams);

        //TODO - we need to add the baseUrlEndpoint to the queryObject.
        Route route = Route.builder().baseUrl(scope.getBaseUrlEndPoint()).path(getPath(uri)).parameters(queryParams)
                .headers(scope.getRequestHeaders()).apiVersion(apiVersion).build();
        ElideResponse response = elide.get(route, user, requestUUID);
        log.debug("JSONAPI_V1_0 getResponseCode: {}, JSONAPI_V1_0 getBody: {}",
                response.getResponseCode(), response.getBody());
        return response;
    }

    /**
     * This method parses the url and gets the query params.
     * And adds them into a Map to be used by underlying Elide.get method
     * @param uri URIBuilder instance
     * @return Map with query parameters
     */
    public static Map<String, List<String>> getQueryParams(URIBuilder uri) {
        Map<String, List<String>> queryParams = new LinkedHashMap<>();
        for (NameValuePair queryParam : uri.getQueryParams()) {
            queryParams.computeIfAbsent(queryParam.getName(), key -> new ArrayList<>())
                    .add(queryParam.getValue());
        }
        return queryParams;
    }

    /**
     * This method parses the url and gets the query params.
     * And retrieves path to be used by underlying Elide.get method
     * @param uri URIBuilder instance
     * @return Path extracted from URI
     */
    public static String getPath(URIBuilder uri) {
        return uri.getPath();
    }

    @Override
    public Integer calculateRecordCount(AsyncQuery queryObj, ElideResponse response) {
        Integer count = null;
        if (response.getResponseCode() == 200) {
            count = safeJsonPathLength(response.getBody(), "$.data.length()");
        }
        return count;
    }
}
