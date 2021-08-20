/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.operation;

import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.async.models.AsyncAPI;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.service.AsyncExecutorService;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.exceptions.InvalidOperationException;
import com.yahoo.elide.core.security.User;
import com.yahoo.elide.graphql.QueryRunner;

import lombok.extern.slf4j.Slf4j;

import java.net.URISyntaxException;
import java.util.UUID;

/**
 * GrapqhQL implementation of AsyncQueryOperation for executing the query provided in AsyncQuery.
 */
@Slf4j
public class GraphQLAsyncQueryOperation extends AsyncQueryOperation {

    public GraphQLAsyncQueryOperation(AsyncExecutorService service, AsyncAPI queryObj, RequestScope scope) {
        super(service, queryObj, scope);
    }

    @Override
    public ElideResponse execute(AsyncAPI queryObj, RequestScope scope) throws URISyntaxException {
        User user = scope.getUser();
        String apiVersion = scope.getApiVersion();
        QueryRunner runner = getService().getRunners().get(apiVersion);
        if (runner == null) {
            throw new InvalidOperationException("Invalid API Version");
        }
        UUID requestUUID = UUID.fromString(queryObj.getRequestId());
        //TODO - we need to add the baseUrlEndpoint to the queryObject.
        ElideResponse response = runner.run("", queryObj.getQuery(), user, requestUUID, scope.getRequestHeaders());
        log.debug("GRAPHQL_V1_0 getResponseCode: {}, GRAPHQL_V1_0 getBody: {}",
                response.getResponseCode(), response.getBody());
        return response;
    }

    @Override
    public Integer calculateRecordCount(AsyncQuery queryObj, ElideResponse response) {
        Integer count = 0;
        if (response.getResponseCode() == 200) {
            count = safeJsonPathLength(response.getBody(), "$..edges.length()");
        }
        return count;
    }
}
