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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.github.opendevl.JFlat;

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

    @Override
    public AsyncQueryResult call() throws Exception {
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
    protected AsyncQueryResult processQuery() throws Exception {
        UUID requestId = UUID.fromString(queryObj.getRequestId());

        ElideResponse response = null;
        Integer recCount = null;

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
        byte[] temp = tempResult.getBytes();

        if (queryObj.getResultType() == ResultType.DOWNLOAD) {
            url = resultStorageEngine.storeResults(queryObj.getId(), temp);
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
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(jsonStr);
            if (!jsonNode.isObject()) {
                return null;
            }
             jsonNode = jsonNode.get("data");
            if (jsonNode.isArray()) {
                rec = jsonNode.size();
            }

        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return rec;
    }

    /**
     * This method calculates the number of records from the response with GRAPHQL API.
     * @param jsonStr is the response.getBody() we get from the response
     * @return rec is the recordCount
     */
    protected Integer calculateRecordsGraphQL(String jsonStr) {
        Integer rec = null;
        try {
            JsonFactory factory = new JsonFactory();
            com.fasterxml.jackson.core.JsonParser parser  = factory.createParser(jsonStr);
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(jsonStr);
            if (!jsonNode.isObject()) {
                return null;
            }
            while (!parser.isClosed()) {
                com.fasterxml.jackson.core.JsonToken jsonToken = parser.nextToken();

                if (JsonToken.FIELD_NAME.equals(jsonToken)) {
                    String fieldName = parser.getCurrentName();
                    System.out.println(fieldName);

                    if (fieldName.equals("edges")) {
                        jsonNode = jsonNode.get(fieldName);
                        if (jsonNode.isArray()) {
                            rec = jsonNode.size();
                        }
                        break;
                    }
                    if (jsonNode.isObject()) {
                        jsonNode = jsonNode.get(fieldName);
                        System.out.println(jsonNode);
                    } else {
                        return null;
                    }
                }
            }

        } catch (JsonParseException e) {
            log.error("Exception: {}", e);
        } catch (JsonMappingException e) {
            log.error("Exception: {}", e);
        } catch (JsonProcessingException e) {
            log.error("Exception: {}", e);
        } catch (IOException e) {
            log.error("Exception: {}", e);
        }

        return rec;
    }

    /**
     * This method converts the JSON response to a CSV response type.
     * @param jsonStr is the response.getBody() of the query
     * @return retuns a string which nis in CSV format
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
