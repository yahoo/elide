/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service;

import java.net.URISyntaxException;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.async.models.AsyncQuery;
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

    private AsyncQuery queryObj;
    private User user;
    private Elide elide;
    private QueryRunner runner;
    private AsyncQueryDAO asyncQueryDao;

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
            asyncQueryDao.updateAsyncQuery(queryObj.getId(), (asyncQuery) -> {
                asyncQuery.setStatus(QueryStatus.PROCESSING);
                });
            ElideResponse response = null;
            log.debug("AsyncQuery Object from request: {}", queryObj);
            if (queryObj.getQueryType().equals(QueryType.JSONAPI_V1_0)) {
                MultivaluedMap<String, String> queryParams = getQueryParams(queryObj.getQuery());
                log.debug("Extracted QueryParams from AsyncQuery Object: {}", queryParams);
                response = elide.get(getPath(queryObj.getQuery()), queryParams, user);
                log.debug("JSONAPI_V1_0 getResponseCode: {}, JSONAPI_V1_0 getBody: {}", response.getResponseCode(), response.getBody());
            }
            else if (queryObj.getQueryType().equals(QueryType.GRAPHQL_V1_0)) {
                response = runner.run(queryObj.getQuery(), user);
                log.debug("GRAPHQL_V1_0 getResponseCode: {}, GRAPHQL_V1_0 getBody: {}", response.getResponseCode(), response.getBody());
            }
            if (response != null){
                // If we receive a response update Query Status to complete
                queryObj.setStatus(QueryStatus.COMPLETE);

                // Create AsyncQueryResult entry for AsyncQuery and add queryResult object to query object
                asyncQueryDao.setAsyncQueryAndResult(response.getResponseCode(), response.getBody(), queryObj, queryObj.getId());

            } else {
                // If no response is returned on AsyncQuery request we set the QueryStatus to FAILURE
                // No AsyncQueryResult will be set for this case
                asyncQueryDao.updateAsyncQuery(queryObj.getId(), (asyncQueryObj) -> {
                    asyncQueryObj.setStatus(QueryStatus.FAILURE);
                 });
            }
        } catch (Exception e) {
            log.error("Exception: {}", e);
            // If an Exception is encountered we set the QueryStatus to FAILURE
            //No AsyncQueryResult will be set for this case
            asyncQueryDao.updateAsyncQuery(queryObj.getId(), (asyncQueryObj) -> {
                asyncQueryObj.setStatus(QueryStatus.FAILURE);
            });
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
        return uri.getPath();
    }
}
