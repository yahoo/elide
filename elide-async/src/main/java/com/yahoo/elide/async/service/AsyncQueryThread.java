/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.async.models.AsyncAPI;
import com.yahoo.elide.async.models.AsyncAPIResult;
import com.yahoo.elide.async.models.AsyncQueryResult;
import com.yahoo.elide.async.models.QueryType;
import com.yahoo.elide.core.security.User;
import com.yahoo.elide.graphql.QueryRunner;
import org.apache.http.NameValuePair;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.utils.URIBuilder;
import lombok.extern.slf4j.Slf4j;

import java.net.URISyntaxException;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

/**
 * AsyncQuery implementation of Callable for executing the query provided in AsyncQuery.
 * It will also update the query status and result object at different stages of execution.
 */
@Slf4j
public class AsyncQueryThread implements Callable<AsyncAPIResult> {
    private AsyncAPI queryObj;
    private User user;
    private AsyncExecutorService service;
    private String apiVersion;

    public AsyncQueryThread(AsyncAPI queryObj, User user, AsyncExecutorService service, String apiVersion) {
        this.queryObj = queryObj;
        this.user = user;
        this.service = service;
        this.apiVersion = apiVersion;
    }

    @Override
    public AsyncAPIResult call() throws URISyntaxException, NoHttpResponseException {
        ElideResponse response = null;
        log.debug("AsyncQuery Object from request: {}", this);
        UUID requestUUID = UUID.fromString(queryObj.getRequestId());
        if (queryObj.getQueryType().equals(QueryType.JSONAPI_V1_0)) {
            response = executeJsonApiRequest(service.getElide(), user, apiVersion, requestUUID);
        } else if (queryObj.getQueryType().equals(QueryType.GRAPHQL_V1_0)) {
            response = executeGraphqlRequest(service.getRunners(), user, apiVersion, requestUUID);
        }
        nullResponseCheck(response);

        AsyncQueryResult queryResult = new AsyncQueryResult();
        queryResult.setHttpStatus(response.getResponseCode());
        queryResult.setCompletedOn(new Date());
        queryResult.setResponseBody(response.getBody());
        queryResult.setContentLength(response.getBody().length());
        //TODO Add recordcount to queryResultObj
        return queryResult;
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

    private ElideResponse executeJsonApiRequest(Elide elide, User user, String apiVersion, UUID requestUUID)
            throws URISyntaxException {
        URIBuilder uri = new URIBuilder(queryObj.getQuery());
        MultivaluedMap<String, String> queryParams = getQueryParams(uri);
        log.debug("Extracted QueryParams from AsyncQuery Object: {}", queryParams);

        //TODO - we need to add the baseUrlEndpoint to the queryObject.
        ElideResponse response = elide.get("", getPath(uri), queryParams, user, apiVersion, requestUUID);
        log.debug("JSONAPI_V1_0 getResponseCode: {}, JSONAPI_V1_0 getBody: {}",
                response.getResponseCode(), response.getBody());
        return response;
    }

    private ElideResponse executeGraphqlRequest(Map<String, QueryRunner> runners, User user, String apiVersion,
            UUID requestUUID) throws URISyntaxException {
        QueryRunner runner = runners.get(apiVersion);
        //TODO - we need to add the baseUrlEndpoint to the queryObject.
        ElideResponse response = runner.run("", queryObj.getQuery(), user, requestUUID);
        log.debug("GRAPHQL_V1_0 getResponseCode: {}, GRAPHQL_V1_0 getBody: {}",
                response.getResponseCode(), response.getBody());
        return response;
    }

    private void nullResponseCheck(ElideResponse response) throws NoHttpResponseException {
        if (response == null) {
            throw new NoHttpResponseException("Response for request returned as null");
        }
    }
}
