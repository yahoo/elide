/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service;

import java.net.URISyntaxException;
import java.util.UUID;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.AsyncQueryResult;
import com.yahoo.elide.async.models.QueryStatus;
import com.yahoo.elide.async.models.QueryType;
import com.yahoo.elide.graphql.QueryRunner;
import com.yahoo.elide.security.User;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Runnable thread for executing the query provided in Async Query.
 * It will also update the query status and result object at different
 * stages of execution.
 */
@Slf4j
@Data
@AllArgsConstructor
public class AsyncQueryThread implements Runnable {

    private String query;
    private QueryType queryType;
    private User user;
    private Elide elide;
    private QueryRunner runner;
    private UUID id;

    @Override
    public void run() {
        processQuery();
    }

    /**
     * This is the main method which processes the Async Query request, executes the query and updates
     * values for AsyncQuery and AsyncQueryResult models accordingly.
     */
    protected void processQuery() {
        try {
            // Change async query to processing
            AsyncDbUtil asyncDbUtil = AsyncDbUtil.getInstance(elide);
            asyncDbUtil.updateAsyncQuery(id, (asyncQuery) -> {
                asyncQuery.setStatus(QueryStatus.PROCESSING);
                });
            //Thread.sleep(180000);
            ElideResponse response = null;
            log.debug("query: {}", query);
            log.debug("queryType: {}", queryType);
            AsyncQuery asyncQuery;
            AsyncQueryResult asyncQueryResult;
            if (queryType.equals(QueryType.JSONAPI_V1_0)) {
                MultivaluedMap<String, String> queryParams = getQueryParams(query);
                response = elide.get(getPath(query), queryParams, user);
                log.debug("JSONAPI_V1_0 getResponseCode: {}", response.getResponseCode());
                log.debug("JSONAPI_V1_0 getBody: {}", response.getBody());
            }
            else if (queryType.equals(QueryType.GRAPHQL_V1_0)) {
                response = runner.run(query, user);
                log.debug("GRAPHQL_V1_0 getResponseCode: {}", response.getResponseCode());
                log.debug("GRAPHQL_V1_0 getBody: {}", response.getBody());
            }
            // if 200 - response code then Change async query to complete else change to Failure
            if (response.getResponseCode() == 200) {
                asyncQuery = asyncDbUtil.updateAsyncQuery(id, (asyncQueryObj) -> {
                    asyncQueryObj.setStatus(QueryStatus.COMPLETE);
                    });
            } else {
                asyncQuery = asyncDbUtil.updateAsyncQuery(id, (asyncQueryObj) -> {
                    asyncQueryObj.setStatus(QueryStatus.FAILURE);
                    });
            }

            // Create AsyncQueryResult entry for AsyncQuery
            asyncQueryResult = asyncDbUtil.createAsyncQueryResult(response.getResponseCode(), response.getBody(), asyncQuery, id);

            // Add queryResult object to query object
            asyncDbUtil.updateAsyncQuery(id, (asyncQueryObj) -> {
                asyncQueryObj.setResult(asyncQueryResult);
                });

        } catch (URISyntaxException e) {
            log.error("URISyntaxException: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Exception: {}", e.getMessage());
        }
    }

    /**
     * This method parses the url and gets the query params and adds them into a MultivaluedMap
     * to be used by underlying Elide.get method
     * @param query query from the Async request
     * @throws URISyntaxException URISyntaxException from malformed or incorrect URI
     * @return MultivaluedMap with query parameters
     */
    protected MultivaluedMap<String, String> getQueryParams(String query) throws URISyntaxException {
        URIBuilder uri;
        uri = new URIBuilder(query);
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<String, String>();
        for (NameValuePair queryParam : uri.getQueryParams()) {
            queryParams.add(queryParam.getName(), queryParam.getValue());
        }
        log.debug("QueryParams: {}", queryParams);
        return queryParams;
    }

    /**
     * This method parses the url and gets the query params and retrieves path
     * to be used by underlying Elide.get method
     * @param query query from the Async request
     * @throws URISyntaxException URISyntaxException from malformed or incorrect URI
     * @return Path extracted from URI
     */
    protected String getPath(String query) throws URISyntaxException {
        URIBuilder uri;
        uri = new URIBuilder(query);
        log.debug("Retrieving path from query");
        return uri.getPath();
    }
}
