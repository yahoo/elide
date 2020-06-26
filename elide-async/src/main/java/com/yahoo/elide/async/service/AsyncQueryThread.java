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
import com.yahoo.elide.async.models.QueryType;
import com.yahoo.elide.async.models.ResultFormatType;
import com.yahoo.elide.async.models.ResultType;
import com.yahoo.elide.graphql.QueryRunner;
import com.yahoo.elide.security.User;

import com.github.opendevl.JFlat;
import com.jayway.jsonpath.PathNotFoundException;

import org.apache.http.NameValuePair;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.utils.URIBuilder;
import org.json.JSONException;
import org.json.JSONObject;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;
import java.util.List;
import java.util.concurrent.Callable;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Callable thread for executing the query provided in Async Query.
 * It will also update the query status and result object at different
 * stages of execution.
 */
@Slf4j
@Data
public class AsyncQueryThread implements Callable<AsyncQueryResult> {

    private AsyncQuery queryObj;
    private AsyncQueryResult queryResultObj;
    private User user;
    private Elide elide;
    private final QueryRunner runner;
    private AsyncQueryDAO asyncQueryDao;
    private String apiVersion;
    private ResultStorageEngine resultStorageEngine;

    @Override
    public AsyncQueryResult call() throws NoHttpResponseException, URISyntaxException {
        return processQuery();
    }

    public AsyncQueryThread(AsyncQuery queryObj, User user, Elide elide, QueryRunner runner,
        AsyncQueryDAO asyncQueryDao, String apiVersion, ResultStorageEngine resultStorageEngine) {
        this.queryObj = queryObj;
        this.user = user;
        this.elide = elide;
        this.runner = runner;
        this.asyncQueryDao = asyncQueryDao;
        this.apiVersion = apiVersion;
        this.resultStorageEngine = resultStorageEngine;
    }

    /**
     * This is the main method which processes the Async Query request, executes the query and updates
     * values for AsyncQuery and AsyncQueryResult models accordingly.
     * @return AsyncQueryResult
     * @throws URISyntaxException
     * @throws NoHttpResponseException
     */
    protected AsyncQueryResult processQuery() throws URISyntaxException, NoHttpResponseException {
        UUID requestId = UUID.fromString(queryObj.getRequestId());

        ElideResponse response = null;
        Integer recCount = null;

        Date startTime = new Date();

        log.debug("AsyncQuery Object from request: {}", queryObj);
        if (queryObj.getQueryType().equals(QueryType.JSONAPI_V1_0)) {
            MultivaluedMap<String, String> queryParams = getQueryParams(queryObj.getQuery());
            log.debug("Extracted QueryParams from AsyncQuery Object: {}", queryParams);

            //TODO - we need to add the baseUrlEndpoint to the queryObject.
            response = elide.get("", getPath(queryObj.getQuery()), queryParams, user, apiVersion, requestId);
            log.debug("JSONAPI_V1_0 getResponseCode: {}, JSONAPI_V1_0 getBody: {}",
                    response.getResponseCode(), response.getBody());

            if (response.getResponseCode() == 200) {
                recCount = calculateRecordsJSON(response.getBody());
            }
        }
        else if (queryObj.getQueryType().equals(QueryType.GRAPHQL_V1_0)) {
            //TODO - we need to add the baseUrlEndpoint to the queryObject.

            response = runner.run("", queryObj.getQuery(), user, requestId);
            log.debug("GRAPHQL_V1_0 getResponseCode: {}, GRAPHQL_V1_0 getBody: {}",
                    response.getResponseCode(), response.getBody());

            if (response.getResponseCode() == 200) {
                String tableName = getTableNameFromQuery(queryObj.getQuery());
                recCount = calculateRecordsGRAPHQL(response.getBody(), tableName);
            }
        }
        if (response == null) {
            throw new NoHttpResponseException("Response for request returned as null");
        }

        long timeElapsed = ((new Date()).getTime() - startTime.getTime());
        long asyncAfterMilliSeconds = queryObj.getAsyncAfterSeconds() * 1000;

        // Create AsyncQueryResult entry for AsyncQuery

        queryResultObj = new AsyncQueryResult();
        queryResultObj.setHttpStatus(response.getResponseCode());
        queryResultObj.setContentLength(response.getBody().length());
        queryResultObj.setRecordCount(recCount);
        queryResultObj.setCompletedOn(new Date());

        if (timeElapsed - asyncAfterMilliSeconds > 0) {

            URL url = null;
            if (queryObj.getResultFormatType() == ResultFormatType.CSV) {
                String jsonToCSV = convertJsonToCSV(response.getBody());
                byte[] temp = jsonToCSV.getBytes();
                url = resultStorageEngine.storeResults(queryObj.getId(), temp);
            }
            else {
                byte[] temp = response.getBody().getBytes();
                url = resultStorageEngine.storeResults(queryObj.getId(), temp);
            }

            queryResultObj.setResultType(ResultType.DOWNLOAD);
            queryResultObj.setResponseBody(url.toString());
        } else {
            queryResultObj.setResultType(ResultType.EMBEDDED);
            queryResultObj.setResponseBody(response.getBody());
        }

        return queryResultObj;
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
        StringBuilder str = new StringBuilder();
        try {
            JSONObject j = new JSONObject(jsonStr);
            String s = (String) j.get("query");

            Integer countBrackets = 0;
            for (int i = 0; i < s.length(); i++) {
                if (s.charAt(i) == '{' || s.charAt(i) == '}') {
                    countBrackets++;
                    if (countBrackets == 2) {
                        break;
                    }
                }
                else {
                    str.append(s.charAt(i));
                }
            }
        } catch (JSONException e) {
            log.error("Exception: {}", e);

        }
        return str.toString().trim().split("\\s+")[0];
    }

    /**
     * This method converts the JSON response to a CSV response type.
     * @param jsonStr is the response.getBody() of the query
     * @return retuns a string which nis in CSV format
     */
    protected String convertJsonToCSV(String jsonStr) {
        if (jsonStr == null) {
            return null;
        }
        StringBuilder str = new StringBuilder();

        try {
            new JSONObject(jsonStr);
            JFlat flatMe = new JFlat(jsonStr);
            List<Object[]> json2csv = flatMe.json2Sheet().headerSeparator("_").getJsonAsSheet();

            for (Object[] obj : json2csv) {
                str.append(Arrays.toString(obj));
            }

        } catch (JSONException e) {
            log.error("Exception: {}", e);
        } catch (PathNotFoundException e) {
            log.error("Exception: {}", e);
        } catch (Exception e) {
            log.error("Exception: {}", e);
        }

        return str.toString();

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
