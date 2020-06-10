/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service;


import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.AsyncQueryResult;
import com.yahoo.elide.async.models.QueryStatus;
import com.yahoo.elide.async.models.QueryType;
import com.yahoo.elide.async.models.ResultType;
import com.yahoo.elide.graphql.QueryRunner;
import com.yahoo.elide.security.User;

import org.apache.http.NameValuePair;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.utils.URIBuilder;
import org.json.JSONException;
import org.json.JSONObject;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.net.URISyntaxException;
import java.util.Date;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

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
    private final QueryRunner runner;
    private AsyncQueryDAO asyncQueryDao;
    private String apiVersion;

    @Override
    public void run() {
        processQuery();
    }

   /**
    * This is the main method which processes the Async Query request, executes the query and updates
    * values for AsyncQuery and AsyncQueryResult models accordingly.
    */
    protected void processQuery() {
        Integer recCount = null;

        try {
            // Change async query to processing
            asyncQueryDao.updateStatus(queryObj, QueryStatus.PROCESSING);
            ElideResponse response = null;
            log.debug("AsyncQuery Object from request: {}", queryObj);
            if (queryObj.getQueryType().equals(QueryType.JSONAPI_V1_0)) {
                MultivaluedMap<String, String> queryParams = getQueryParams(queryObj.getQuery());
                log.debug("Extracted QueryParams from AsyncQuery Object: {}", queryParams);
                response = elide.get(getPath(queryObj.getQuery()), queryParams, user, apiVersion);
                log.debug("JSONAPI_V1_0 getResponseCode: {}, JSONAPI_V1_0 getBody: {}",
                        response.getResponseCode(), response.getBody());

                recCount = calculateRecordsJSON(response.getBody());

            }
            else if (queryObj.getQueryType().equals(QueryType.GRAPHQL_V1_0)) {
                response = runner.run(queryObj.getQuery(), user);
                log.debug("GRAPHQL_V1_0 getResponseCode: {}, GRAPHQL_V1_0 getBody: {}",
                        response.getResponseCode(), response.getBody());
                String tableName = getTableNameFromQuery(queryObj.getQuery());
                recCount = calculateRecordsGRAPHQL(response.getBody(), tableName);
            }
            if (response == null) {
                throw new NoHttpResponseException("Response for request returned as null");
            }

            // Create AsyncQueryResult entry for AsyncQuery

            AsyncQueryResult asyncQueryResult = new AsyncQueryResult();
            asyncQueryResult.setHttpStatus(response.getResponseCode());
            asyncQueryResult.setResponseBody(response.getBody());
            asyncQueryResult.setContentLength(response.getBody().length());
            asyncQueryResult.setRecordCount(recCount);
            asyncQueryResult.setResultType(ResultType.EMBEDDED);
            asyncQueryResult.setCompletedOn(new Date());

            // add queryResult object to query object
            asyncQueryDao.updateAsyncQueryResult(asyncQueryResult, queryObj);
            // If we receive a response update Query Status to complete
            asyncQueryDao.updateStatus(queryObj, QueryStatus.COMPLETE);

        } catch (Exception e) {
            log.error("Exception: {}", e);
            if (e.getClass().equals(InterruptedException.class)) {
                // An InterruptedException is encountered when we interrupt the query when it goes beyond max run time
                // We set the QueryStatus to TIMEDOUT
                // No AsyncQueryResult will be set for this case
                asyncQueryDao.updateStatus(queryObj, QueryStatus.TIMEDOUT);
            } else {
                // If an Exception is encountered we set the QueryStatus to FAILURE
                // No AsyncQueryResult will be set for this case
                asyncQueryDao.updateStatus(queryObj, QueryStatus.FAILURE);
            }
        }
    }

    /**
     * This method calculates the number of records from the response with JSON API.
     * @param jsonStr is the response.getBody() we get from the response
     * @return rec is the recordCount
     */
    protected Integer calculateRecordsJSON(String jsonStr) {
        Integer rec;
        try {
            JSONObject j = new JSONObject(jsonStr);
            rec = j.getJSONArray("data").length();

        } catch (JSONException e) {
            rec = null;
        }
        return rec;
    }

    /**
     * This method calculates the number of records from the response with GRAPHQL API.
     * @param response is the response.getBody() we get from the response
     * @param table_name is the table from which we extract the data
     * @return rec is the recordCount
     */
    protected Integer calculateRecordsGRAPHQL(String response, String table_name) {
        Integer rec;
        try {
            JSONObject j = new JSONObject(response);
            JSONObject j2 = j.getJSONObject("data");
            JSONObject j3 = j2.getJSONObject(table_name);
            rec = j3.getJSONArray("edges").length();

        } catch (JSONException e) {
            rec = null;
        }
        return rec;
    }

    /**
     * This method helps to extract the table name from the query.
     * @param jsonStr is the query with which we are extracting the data
     * @return the table name from the above query
     */
    protected String getTableNameFromQuery(String jsonStr) {
        JSONObject j = new JSONObject(jsonStr);
        String s = (String) j.get("query");
        String str = "";
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '{' || s.charAt(i) == '}') {
                continue;
            }
            else {
                str = str + s.charAt(i);
            }
        }
        String[] queryTerms = str.trim().split("\\s+");

        return queryTerms[0];
    }

    /**
     * This method parses the url and gets the query params.
     * And adds them into a MultivaluedMap to be used by underlying Elide.get method
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
     * This method parses the url and gets the query params.
     * And retrieves path to be used by underlying Elide.get method
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
