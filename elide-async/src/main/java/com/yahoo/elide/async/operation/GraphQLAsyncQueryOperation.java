/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.operation;

import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.async.models.AsyncAPI;
import com.yahoo.elide.async.models.AsyncAPIResult;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.AsyncQueryResult;
import com.yahoo.elide.async.service.AsyncExecutorService;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.security.User;
import com.yahoo.elide.graphql.QueryRunner;

import com.jayway.jsonpath.JsonPath;

import org.apache.http.NoHttpResponseException;

import lombok.extern.slf4j.Slf4j;

import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * GrapqhQL implementation of AsyncQueryOperation for executing the query provided in AsyncQuery.
 */
@Slf4j
public class GraphQLAsyncQueryOperation extends AsyncQueryOperation {

    public GraphQLAsyncQueryOperation(AsyncExecutorService service) {
        super(service);
    }

    @Override
    public AsyncAPIResult execute(AsyncAPI queryObj, RequestScope scope) {
        ElideResponse response = null;
        log.debug("AsyncQuery Object from request: {}", this);
        try {
            response = executeGraphqlRequest(queryObj, getService().getRunners(), scope.getUser(),
                    scope.getApiVersion());
            nullResponseCheck(response);
        } catch (URISyntaxException | NoHttpResponseException e) {
            throw new IllegalStateException(e);
        }

        AsyncQueryResult queryResult = new AsyncQueryResult();
        queryResult.setHttpStatus(response.getResponseCode());
        queryResult.setCompletedOn(new Date());
        queryResult.setResponseBody(response.getBody());
        queryResult.setContentLength(response.getBody().length());
        queryResult.setRecordCount(calculateRecordCount((AsyncQuery) queryObj, response));
        return queryResult;
    }

    private ElideResponse executeGraphqlRequest(AsyncAPI queryObj, Map<String, QueryRunner> runners, User user,
            String apiVersion) throws URISyntaxException {
        QueryRunner runner = runners.get(apiVersion);
        UUID requestUUID = UUID.fromString(queryObj.getRequestId());
        //TODO - we need to add the baseUrlEndpoint to the queryObject.
        ElideResponse response = runner.run("", queryObj.getQuery(), user, requestUUID);
        log.debug("GRAPHQL_V1_0 getResponseCode: {}, GRAPHQL_V1_0 getBody: {}",
                response.getResponseCode(), response.getBody());
        return response;
    }

    @Override
    public Integer calculateRecordCount(AsyncQuery queryObj, ElideResponse response) {
        Integer count = null;
        if (response.getResponseCode() == 200) {
            List<Integer> countList = JsonPath.read(response.getBody(), "$..edges.length()");
            count = countList.size() > 0 ? countList.get(0) : 0;
        }
        return count;
    }
}
