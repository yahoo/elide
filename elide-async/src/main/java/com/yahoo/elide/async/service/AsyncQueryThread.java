/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service;

import static com.jayway.jsonpath.JsonPath.using;

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
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;

import org.apache.http.NameValuePair;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.utils.URIBuilder;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
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
    private String baseURLEndPoint;
    private String downloadBasePath;

    @Override
    public AsyncQueryResult call() throws Exception {
        return processQuery();
    }

    public AsyncQueryThread(AsyncQuery queryObj, User user, Elide elide, QueryRunner runner,
            AsyncQueryDAO asyncQueryDao, String apiVersion, ResultStorageEngine resultStorageEngine,
            String baseURLEndPoint, String downloadBasePath) {
        this.queryObj = queryObj;
        this.user = user;
        this.elide = elide;
        this.runner = runner;
        this.asyncQueryDao = asyncQueryDao;
        this.apiVersion = apiVersion;
        this.resultStorageEngine = resultStorageEngine;
        this.baseURLEndPoint = baseURLEndPoint;
        this.downloadBasePath = downloadBasePath;
    }

    /**
     * This is the main method which processes the Async Query request, executes the query and updates
     * values for AsyncQuery and AsyncQueryResult models accordingly.
     * @return AsyncQueryResult
     * @throws Exception Thrown by JFlat
     * @throws NoHttpResponseException Thrown by processQuery
     */
    protected AsyncQueryResult processQuery() throws Exception {
        UUID requestId = UUID.fromString(queryObj.getRequestId());

        ElideResponse response = null;
        Integer recCount = null;

        log.debug("AsyncQuery Object from request: {}", queryObj);
        if (queryObj.getQueryType().equals(QueryType.JSONAPI_V1_0)) {
            MultivaluedMap<String, String> queryParams = getQueryParams(queryObj.getQuery());
            log.debug("Extracted QueryParams from AsyncQuery Object: {}", queryParams);

            //TODO - we need to add the baseURLEndpoint to the queryObject.
            response = elide.get("", getPath(queryObj.getQuery()), queryParams, user, apiVersion, requestId);
            log.debug("JSONAPI_V1_0 getResponseCode: {}, JSONAPI_V1_0 getBody: {}",
                    response.getResponseCode(), response.getBody());

            if (response.getResponseCode() == 200) {
                recCount = calculateRecordsJSON(response.getBody());
            }
        }
        else if (queryObj.getQueryType().equals(QueryType.GRAPHQL_V1_0)) {
            //TODO - we need to add the baseURLEndpoint to the queryObject.

            response = runner.run("", queryObj.getQuery(), user, requestId);
            log.debug("GRAPHQL_V1_0 getResponseCode: {}, GRAPHQL_V1_0 getBody: {}",
                    response.getResponseCode(), response.getBody());

            if (response.getResponseCode() == 200) {
                recCount = calculateRecordsGraphQL(response.getBody());
            }
        }
        if (response == null) {
            throw new NoHttpResponseException("Response for request returned as null");
        }

        // Create AsyncQueryResult entry for AsyncQuery

        queryResultObj = new AsyncQueryResult();
        queryResultObj.setHttpStatus(response.getResponseCode());
        queryResultObj.setContentLength(response.getBody().length());
        queryResultObj.setRecordCount(recCount);
        queryResultObj.setCompletedOn(new Date());

        URL url = null;
        String tempResult = response.getBody();
        if (queryObj.getResultFormatType() == ResultFormatType.CSV) {
            tempResult = convertJsonToCSV(response.getBody());
        }

        if (queryObj.getResultType() == ResultType.DOWNLOAD) {
            String baseURL = baseURLEndPoint != null ? getBasePath(baseURLEndPoint) : null;
            String downloadURL = baseURL != null && downloadBasePath != null ? baseURL + downloadBasePath : null;

            byte[] temp = tempResult.getBytes();
            url = resultStorageEngine.storeResults(queryObj.getId(), temp, downloadURL);
            queryResultObj.setResponseBody(url.toString());
        } else if (queryObj.getResultType() == ResultType.EMBEDDED) {
            queryResultObj.setResponseBody(tempResult);
        }

        return queryResultObj;
    }

    /**
     * This method calculates the number of records from the response with JSON API.
     * @param jsonStr is the response.getBody() we get from the response
     * @return rec is the recordCount
     */
    protected Integer calculateRecordsJSON(String jsonStr) {
        Integer rec = null;
        rec = JsonPath.read(jsonStr, "$.data.length()");
        return rec;
    }

    /**
     * This method calculates the number of records from the response with GRAPHQL API.
     * @param jsonStr is the response.getBody() we get from the response
     * @return rec is the recordCount
     * @throws IOException Exception thrown by JsonPath
     */
    protected Integer calculateRecordsGraphQL(String jsonStr) throws IOException {
        Integer rec = null;

        Configuration conf = Configuration.builder()
                .options(Option.AS_PATH_LIST).build();

        List<String> pathList = using(conf).parse(jsonStr).read("$..edges");
        String modify = pathList.get(0).replace("[", ".").replace("'", "").replace("]", "") + ".length()";
        rec = JsonPath.read(jsonStr, modify);
        return rec;
    }

    /**
     * This method converts the JSON response to a CSV response type.
     * @param jsonStr is the response.getBody() of the query
     * @return retuns a string which nis in CSV format
     * @throws Exception Exception thrown by JFlat
     */
    protected String convertJsonToCSV(String jsonStr) throws Exception {
        if (jsonStr == null) {
            return null;
        }
        StringBuilder str = new StringBuilder();
        JFlat flatMe = new JFlat(jsonStr);
        List<Object[]> json2csv = flatMe.json2Sheet().headerSeparator("_").getJsonAsSheet();

        for (Object[] obj : json2csv) {
            str.append(Arrays.toString(obj));
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

    /**
     * This method parses the URL and gets the scheme, host, port.
     * @param URL URL from the Async request
     * @throws URISyntaxException URISyntaxException from malformed or incorrect URI
     * @return BasePath extracted from URI
     */
    protected String getBasePath(String URL) throws URISyntaxException {
        URIBuilder uri;
        uri = new URIBuilder(URL);
        if (uri.getPort() != -1) {
            return uri.getScheme() + "://" + uri.getHost() + ":" + uri.getPort();
        }
        return uri.getScheme() + "://" + uri.getHost();
    }
}
