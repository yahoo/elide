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

import io.reactivex.Observable;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
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

    @Override
    public AsyncQueryResult call() throws URISyntaxException, IOException {
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
    * @return AsyncQueryResult AsyncQueryResult
    * @throws URISyntaxException URISyntaxException
    * @throws IOException IOException
    */
    protected AsyncQueryResult processQuery() throws URISyntaxException, IOException {
        UUID requestId = UUID.fromString(queryObj.getRequestId());

        ElideResponse response = null;
        String responseBody = null;
        boolean isError = false;
        Integer recCount = null;
        Observable<String> observableResult = Observable.empty();
        QueryType queryType = queryObj.getQueryType();
        ResultFormatType resultFormatType = queryObj.getResultFormatType();
        boolean isDownload = queryObj.getResultType() == ResultType.DOWNLOAD;

        if (isDownload && resultStorageEngine == null) {
            throw new IllegalStateException("ResultStorageEngine unavailable.");
        }

        log.debug("AsyncQuery Object from request: {}", queryObj);

        // TODO: Use PersistentResource.loadObjects for Download
        if (queryType.equals(QueryType.JSONAPI_V1_0)) {
            response = executeJsonApiRequest(queryObj, requestId);
        }
        else if (queryType.equals(QueryType.GRAPHQL_V1_0)) {
            response = executeGraphqlRequest(queryObj, requestId);
        }

        nullResponseCheck(response);

        responseBody = response.getBody();
        isError = checkJsonStrErrorMessage(responseBody);

        // Create AsyncQueryResult entry for AsyncQuery
        queryResultObj = new AsyncQueryResult();
        queryResultObj.setHttpStatus(response.getResponseCode());
        queryResultObj.setResponseBody(responseBody);
        queryResultObj.setContentLength(responseBody.length());
        queryResultObj.setCompletedOn(new Date());

        if (isError) {
            queryResultObj.setResponseBody(responseBody);
            return queryResultObj;
        }

        recCount = calculateRecords(response, queryType);
        queryResultObj.setRecordCount(recCount);

        if (resultFormatType == ResultFormatType.CSV) {
            observableResult = convertJsonToCSV(responseBody);
        }

        if (isDownload) {
            queryObj.setResult(queryResultObj);
            resultStorageEngine.storeResults(queryObj, observableResult);
            // TODO: Generate URL and set in responsebody
            queryResultObj.setResponseBody("URL to be generated");
        } else if (resultFormatType == ResultFormatType.CSV) {
            queryResultObj.setResponseBody(mergeObservable(observableResult));
        } else {
            queryResultObj.setResponseBody(responseBody);
        }

        return queryResultObj;
    }

    /**
     * This method calculates the number of records from the response.
     * @param response is the ElideResponse
     * @return The recordCount
     * @throws IOException Exception thrown by JsonPath
     */
    protected Integer calculateRecords(ElideResponse response, QueryType queryType)
            throws IOException {
        Integer count = null;
        if (response.getResponseCode() == 200) {
            if (queryType.equals(QueryType.GRAPHQL_V1_0)) {
                List<Integer> countList = JsonPath.read(response.getBody(), "$..edges.length()");
                count = countList.size() > 0 ? countList.get(0) : 0;
            } else if (queryType.equals(QueryType.JSONAPI_V1_0)) {
                count = JsonPath.read(response.getBody(), "$.data.length()");
            }
        }
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
            log.trace(e.getMessage());
        }
        return isError;
    }

    /**
     * This method converts the JSON response to a CSV response type.
     * @param jsonStr is the response.getBody() of the query
     * @return returns an Observable of string which is in CSV format
     * @throws IllegalStateException Exception thrown
     */
    protected Observable<String> convertJsonToCSV(String jsonStr) {
        if (jsonStr == null) {
            return null;
        }
        JFlat flatMe = new JFlat(jsonStr);
        List<Object[]> json2csv;

        try {
            json2csv = flatMe.json2Sheet().headerSeparator("_").getJsonAsSheet();
        } catch (Exception e) {
            log.debug("Exception while converting to CSV: {}", e.getMessage());
            throw new IllegalStateException(e);
        }

        return Observable.using(
                () -> json2csv,
                records -> {
                    return Observable.fromIterable(() -> {
                        return new Iterator<String>() {
                            private String record = null;
                            private Iterator<Object[]> recordIterator = records.iterator();

                            @Override
                            public boolean hasNext() {
                                record = null;
                                if (recordIterator.hasNext()) {
                                    record = Arrays.toString(recordIterator.next());
                                }
                                return record != null;
                            }

                            @Override
                            public String next() {
                                if (record != null) {
                                    return record.substring(1, record.length() - 1);
                                }
                                throw new IllegalStateException("null record found.");
                            }
                        };
                    });
                },
                List::clear
        );
    }

    /**
     * Merge Observable of String to a single String.
     * @param result observable to merge
     * @return String object
     */
    protected String mergeObservable(Observable<String> result) {
        return result.collect(() -> new StringBuilder(),
                (resultBuilder, tempResult) -> {
                    if (resultBuilder.length() > 0) {
                        resultBuilder.append(System.getProperty("line.separator"));
                    }
                    resultBuilder.append(tempResult);
                }
            ).map(StringBuilder::toString).blockingGet();
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

    private ElideResponse executeJsonApiRequest(AsyncQuery queryObj, UUID requestId) throws URISyntaxException {
        MultivaluedMap<String, String> queryParams = getQueryParams(queryObj.getQuery());
        log.debug("Extracted QueryParams from AsyncQuery Object: {}", queryParams);

        //TODO - we need to add the baseUrlEndpoint to the queryObject.
        ElideResponse response = elide.get("", getPath(queryObj.getQuery()), queryParams, user, apiVersion, requestId);
        log.debug("JSONAPI_V1_0 getResponseCode: {}, JSONAPI_V1_0 getBody: {}",
                response.getResponseCode(), response.getBody());
        return response;
    }

    private ElideResponse executeGraphqlRequest(AsyncQuery queryObj, UUID requestId) throws URISyntaxException {
        //TODO - we need to add the baseUrlEndpoint to the queryObject.
        ElideResponse response = runner.run("", queryObj.getQuery(), user, requestId);
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
