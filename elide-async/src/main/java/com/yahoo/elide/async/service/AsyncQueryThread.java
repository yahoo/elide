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
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

import org.apache.http.NameValuePair;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.utils.URIBuilder;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
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
    private String requestURL;

    @Override
    public AsyncQueryResult call() throws URISyntaxException, IOException {
        return processQuery();
    }

    /**
     * Constructor for AsyncQueryThread.
     * @param queryObj AsyncQuery object
     * @param user Elide User Object
     * @param elide Elide Object
     * @param runner QueryRunner Object for GraphQL executions
     * @param asyncQueryDao AsyncQueryDAO implementation Object
     * @param apiVersion Api Version
     * @param resultStorageEngine ResultStorageEngine implementation Object
     * @param requestURL URL of the AsyncRequest pulled from Request Scope
     */
    public AsyncQueryThread(AsyncQuery queryObj, User user, Elide elide, QueryRunner runner,
            AsyncQueryDAO asyncQueryDao, String apiVersion, ResultStorageEngine resultStorageEngine,
            String requestURL) {
        this.queryObj = queryObj;
        this.user = user;
        this.elide = elide;
        this.runner = runner;
        this.asyncQueryDao = asyncQueryDao;
        this.apiVersion = apiVersion;
        this.resultStorageEngine = resultStorageEngine;
        this.requestURL = requestURL;
    }

    /**
     * This is the main method which processes the Async Query request, executes the query and updates
     * values for AsyncQuery and AsyncQueryResult models accordingly.
     * @return AsyncQueryResult
     * @throws URISyntaxException URISyntaxException
     * @throws IOException IOException
     */
    protected AsyncQueryResult processQuery() throws URISyntaxException, IOException {
        UUID requestId = UUID.fromString(queryObj.getRequestId());

        ElideResponse response = null;
        Integer recCount = null;
        String responseBody = null;
        boolean isError = false;
        log.debug("AsyncQuery Object from request: {}", queryObj);
        if (queryObj.getQueryType().equals(QueryType.JSONAPI_V1_0)) {
            MultivaluedMap<String, String> queryParams = getQueryParams(queryObj.getQuery());
            log.debug("Extracted QueryParams from AsyncQuery Object: {}", queryParams);

            //TODO - we need to add the baseUrlEndpoint to the queryObject.
            response = elide.get("", getPath(queryObj.getQuery()), queryParams, user, apiVersion, requestId);
            responseBody = response.getBody();
            isError = checkJsonStrErrorMessage(responseBody);
            log.debug("JSONAPI_V1_0 getResponseCode: {}, JSONAPI_V1_0 getBody: {}",
                    response.getResponseCode(), responseBody);

            if (response.getResponseCode() == 200 && !isError) {
                recCount = calculateRecordsJSON(responseBody);
            }
        }
        else if (queryObj.getQueryType().equals(QueryType.GRAPHQL_V1_0)) {
            //TODO - we need to add the baseURLEndpoint to the queryObject.
            response = runner.run("", queryObj.getQuery(), user, requestId);
            responseBody = response.getBody();
            isError = checkJsonStrErrorMessage(responseBody);
            log.debug("GRAPHQL_V1_0 getResponseCode: {}, GRAPHQL_V1_0 getBody: {}",
                    response.getResponseCode(), responseBody);

            if (response.getResponseCode() == 200 && !isError) {
                recCount = calculateRecordsGraphQL(responseBody);
            }
        }
        if (response == null) {
            throw new NoHttpResponseException("Response for request returned as null");
        }

        // Create AsyncQueryResult entry for AsyncQuery

        queryResultObj = new AsyncQueryResult();
        queryResultObj.setHttpStatus(response.getResponseCode());
        queryResultObj.setContentLength(responseBody.length());
        queryResultObj.setRecordCount(recCount);
        queryResultObj.setCompletedOn(new Date());

        String tempResult = queryObj.getResultFormatType() == ResultFormatType.CSV && !isError
                ? convertJsonToCSV(responseBody) : responseBody;

        if (queryObj.getResultType() == ResultType.EMBEDDED || isError) {
            queryResultObj.setResponseBody(tempResult);
        } else if (queryObj.getResultType() == ResultType.DOWNLOAD) {
            if (resultStorageEngine == null) {
                throw new IllegalStateException("Unable to store async results for download");
            }
            queryResultObj = resultStorageEngine.storeResults(queryResultObj, tempResult, queryObj.getId());
            queryResultObj.setResponseBody(resultStorageEngine.generateDownloadUrl(requestURL,
                    queryObj.getId()).toString());
        }

        return queryResultObj;
    }

    /**
     * This method calculates the number of records from the response with JSON API.
     * @param jsonStr is the response.getBody() we get from the response
     * @return rec is the recordCount
     */
    protected Integer calculateRecordsJSON(String jsonStr) {
        return JsonPath.read(jsonStr, "$.data.length()");
    }

    /**
     * This method calculates the number of records from the response with GRAPHQL API.
     * @param jsonStr is the response.getBody() we get from the response
     * @return rec is the recordCount
     * @throws IOException Exception thrown by JsonPath
     */
    protected Integer calculateRecordsGraphQL(String jsonStr) throws IOException {
        List<Integer> countList = JsonPath.read(jsonStr, "$..edges.length()");
        Integer count = countList.size() > 0 ? countList.get(0) : 0;
        return count;
    }

    /**
     * This method checks if the json string is error message.
     * @param jsonStr is the response.getBody() we get from the response
     * @return is the message an error message
     */
    protected boolean checkJsonStrErrorMessage(String jsonStr) {
        boolean isError = false;
        try {
            isError = (Integer) JsonPath.read(jsonStr, "$.errors.length()") >= 1;
        } catch (PathNotFoundException e) {
            //ignore when not an error message
            log.debug(e.getMessage());
        }
        return isError;
    }

    /**
     * This method converts the JSON response to a CSV response type.
     * @param jsonStr is the response.getBody() of the query
     * @return retuns a string which nis in CSV format
     * @throws IllegalStateException Exception thrown
     */
    protected String convertJsonToCSV(String jsonStr) {
        if (jsonStr == null) {
            return null;
        }
        StringBuilder str = new StringBuilder();
        JFlat flatMe = new JFlat(jsonStr);
        List<Object[]> json2csv;
        try {
            json2csv = flatMe.json2Sheet().headerSeparator("_").getJsonAsSheet();
            for (Object[] obj : json2csv) {
                String objString = Arrays.toString(obj);
                if (objString != null) {
                    objString = objString.substring(1, objString.length() - 1);
                }
                str.append(objString);
                str.append(System.getProperty("line.separator"));
            }
        } catch (Exception e) {
            log.debug("Exception while converting to CSV: {}", e.getMessage());
            throw new IllegalStateException(e);
        }

        return str.toString();

    }

    /**
     * This method parses the URL and gets the query params.
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
     * This method parses the URL and gets the query params.
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
