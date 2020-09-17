/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.AsyncQueryResult;
import com.yahoo.elide.async.models.QueryType;
import com.yahoo.elide.async.models.ResultFormatType;
import com.yahoo.elide.async.models.ResultType;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.graphql.QueryRunner;
import com.yahoo.elide.security.User;

import com.jayway.jsonpath.InvalidJsonException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.reactivex.Observable;

import java.io.IOException;
import java.net.URISyntaxException;

public class AsyncQueryThreadTest {

    private User user;
    private Elide elide;
    private QueryRunner runner;
    private AsyncQueryResult queryResultObj;
    private AsyncQueryDAO asyncQueryDao;
    private ResultStorageEngine resultStorageEngine;
    private Boolean validationResult; //used for PersistentResource Test Status

    @BeforeEach
    public void setupMocks() {
        user = mock(User.class);
        elide = mock(Elide.class);
        runner = mock(QueryRunner.class);
        queryResultObj = mock(AsyncQueryResult.class);
        asyncQueryDao = mock(DefaultAsyncQueryDAO.class);
        resultStorageEngine = mock(ResultStorageEngine.class);
        validationResult = true;
    }

    //Test with record count = 3
    @Test
    public void testProcessQueryJsonApi() throws URISyntaxException, IOException {
        AsyncQuery queryObj = new AsyncQuery();
        String responseBody = "{\"data\":"
                + "[{\"type\":\"book\",\"id\":\"3\",\"attributes\":{\"title\":\"For Whom the Bell Tolls\"}}"
                + ",{\"type\":\"book\",\"id\":\"2\",\"attributes\":{\"title\":\"Song of Ice and Fire\"}},"
                + "{\"type\":\"book\",\"id\":\"1\",\"attributes\":{\"title\":\"Ender's Game\"}}]}";
        ElideResponse response = new ElideResponse(200, responseBody);
        String query = "/group?sort=commonName&fields%5Bgroup%5D=commonName,description";
        String id = "edc4a871-dff2-4054-804e-d80075cf827d";
        queryObj.setId(id);
        queryObj.setQuery(query);
        queryObj.setQueryType(QueryType.JSONAPI_V1_0);
        queryObj.setResultType(ResultType.EMBEDDED);

        when(elide.get(anyString(), anyString(), any(), any(), anyString(), any())).thenReturn(response);
        AsyncQueryThread queryThread = new AsyncQueryThread(queryObj, user, elide, runner, asyncQueryDao, "v1",
                resultStorageEngine);
        queryResultObj = queryThread.processQuery();
        assertEquals(queryResultObj.getRecordCount(), 3);
        assertEquals(queryResultObj.getResponseBody(), responseBody);
        assertEquals(queryResultObj.getHttpStatus(), 200);
    }

    //Test with record count = 0
    @Test
    public void testProcessQueryJsonApi2() throws Exception {
        AsyncQuery queryObj = new AsyncQuery();
        String responseBody = "{\"data\":[]}";
        ElideResponse response = new ElideResponse(200, responseBody);
        String query = "/group?sort=commonName&fields%5Bgroup%5D=commonName,description";
        String id = "edc4a871-dff2-4054-804e-d80075cf827d";
        queryObj.setId(id);
        queryObj.setQuery(query);
        queryObj.setQueryType(QueryType.JSONAPI_V1_0);
        queryObj.setResultType(ResultType.EMBEDDED);

        when(elide.get(any(), anyString(), any(), any(), anyString(), any())).thenReturn(response);
        AsyncQueryThread queryThread = new AsyncQueryThread(queryObj, user, elide, runner, asyncQueryDao, "v1",
                resultStorageEngine);
        queryResultObj = queryThread.processQuery();

        assertEquals(queryResultObj.getRecordCount(), 0);
        assertEquals(queryResultObj.getResponseBody(), responseBody);
        assertEquals(queryResultObj.getHttpStatus(), 200);
    }

    //Test with invalid input for responseBody. An exception will be thrown.
    @Test
    public void testProcessQueryJsonApi3() throws Exception {
        assertThrows(InvalidJsonException.class, () -> {
        AsyncQuery queryObj = new AsyncQuery();
        ElideResponse response = new ElideResponse(200, "ResponseBody");
        String query = "/group?sort=commonName&fields%5Bgroup%5D=commonName,description";
        String id = "edc4a871-dff2-4054-804e-d80075cf827d";
        queryObj.setId(id);
        queryObj.setQuery(query);
        queryObj.setQueryType(QueryType.JSONAPI_V1_0);
        queryObj.setResultType(ResultType.EMBEDDED);

        when(elide.get(any(), anyString(), any(), any(), anyString(), any())).thenReturn(response);
        AsyncQueryThread queryThread = new AsyncQueryThread(queryObj, user, elide, runner, asyncQueryDao, "v1",
                resultStorageEngine);
        queryResultObj = queryThread.processQuery();

        assertEquals(queryResultObj.getRecordCount(), null);
        assertEquals(queryResultObj.getResponseBody(), "ResponseBody");
        });
    }

    //Test with invalid input for responseBody. An exception will be thrown.
    @Test
    public void testProcessQueryGraphQl() throws Exception {
        assertThrows(InvalidJsonException.class, () -> {
            AsyncQuery queryObj = new AsyncQuery();
            ElideResponse response = new ElideResponse(200, "ResponseBody");
            String query = "{\"query\":\"{ group { edges { node { name commonName description } } } }\",\"variables\":null}";
            String id = "edc4a871-dff2-4054-804e-d80075cf827d";
            queryObj.setId(id);
            queryObj.setQuery(query);
            queryObj.setQueryType(QueryType.GRAPHQL_V1_0);
            queryObj.setResultType(ResultType.EMBEDDED);

            when(runner.run(any(), any(), eq(user), any())).thenReturn(response);
            AsyncQueryThread queryThread = new AsyncQueryThread(queryObj, user, elide, runner, asyncQueryDao, "v1",
                    resultStorageEngine);
            queryResultObj = queryThread.processQuery();
            assertEquals(queryResultObj.getResponseBody(), "ResponseBody");
            assertEquals(queryResultObj.getHttpStatus(), 200);
        });
    }

    //Test for when resultStorageEngine fails to store
    @Test
    public void testProcessQueryStoreException() throws Exception {
        assertThrows(IllegalStateException.class, () -> {
            AsyncQuery queryObj = new AsyncQuery();
            String id = "edc4a871-dff2-4054-804e-d80075cf827d";
            queryObj.setId(id);

            when(resultStorageEngine.storeResults(any(), any())).thenThrow(IllegalStateException.class);
            AsyncQueryThread queryThread = new AsyncQueryThread(queryObj, user, elide, runner, asyncQueryDao, "v1",
                    resultStorageEngine);
            queryThread.storeResults(queryObj, Observable.empty());
        });
    }

    //Test with record count = 3
    @Test
    public void testProcessQueryGraphQl2() throws Exception {

        AsyncQuery queryObj = new AsyncQuery();
        String responseBody = "{\"data\":{\"book\":{\"edges\":[{\"node\":{\"id\":\"1\",\"title\":\"Ender's Game\"}},"
                + "{\"node\":{\"id\":\"2\",\"title\":\"Song of Ice and Fire\"}},"
                + "{\"node\":{\"id\":\"3\",\"title\":\"For Whom the Bell Tolls\"}}]}}}";
        ElideResponse response = new ElideResponse(200, responseBody);
        String query = "{\"query\":\"{ group { edges { node { name commonName description } } } }\",\"variables\":null}";
        String id = "edc4a871-dff2-4054-804e-d80075cf827d";
        queryObj.setId(id);
        queryObj.setQuery(query);
        queryObj.setQueryType(QueryType.GRAPHQL_V1_0);
        queryObj.setResultType(ResultType.EMBEDDED);

        when(runner.run(any(), any(), eq(user), any())).thenReturn(response);
        AsyncQueryThread queryThread = new AsyncQueryThread(queryObj, user, elide, runner, asyncQueryDao, "v1",
                resultStorageEngine);
        queryResultObj = queryThread.processQuery();
        assertEquals(queryResultObj.getRecordCount(), 3);
        assertEquals(queryResultObj.getResponseBody(), responseBody);
        assertEquals(queryResultObj.getHttpStatus(), 200);

    }

    //Test with record count = 0
    @Test
    public void testProcessQueryGraphQl3() throws Exception {

        AsyncQuery queryObj = new AsyncQuery();
        String responseBody = "{\"data\":[]}";
        ElideResponse response = new ElideResponse(200, responseBody);
        String query = "{\"query\":\"{ group { edges { node { name commonName description } } } }\",\"variables\":null}";
        String id = "edc4a871-dff2-4054-804e-d80075cf827d";
        queryObj.setId(id);
        queryObj.setQuery(query);
        queryObj.setQueryType(QueryType.GRAPHQL_V1_0);
        queryObj.setResultType(ResultType.EMBEDDED);

        when(runner.run(any(), any(), eq(user), any())).thenReturn(response);
        AsyncQueryThread queryThread = new AsyncQueryThread(queryObj, user, elide, runner, asyncQueryDao, "v1",
                resultStorageEngine);
        queryResultObj = queryThread.processQuery();
        assertEquals(queryResultObj.getRecordCount(), 0);
        assertEquals(queryResultObj.getResponseBody(), responseBody);
        assertEquals(queryResultObj.getHttpStatus(), 200);

    }

    // Standard positive test case that converts json to csv format.
    @Test
    public void testConvertJsonToCSV() throws Exception {

        String csvStr = "key\n\"value\"";
        String jsonStr = "{\"key\":\"value\"}";

        AsyncQuery queryObj = new AsyncQuery();
        AsyncQueryThread queryThread = new AsyncQueryThread(queryObj, user, elide, runner, asyncQueryDao, "v1",
                resultStorageEngine);

        Observable<String> result = queryThread.convertJsonToCSV(jsonStr, true);
        String finalResult = result.collect(() -> new StringBuilder(),
              (resultBuilder, tempResult) -> {
                  if (resultBuilder.length() > 0) {
                      resultBuilder.append(System.getProperty("line.separator"));
                  }
                  resultBuilder.append(tempResult);
              }
        ).map(StringBuilder::toString).blockingGet();

        assertEquals(csvStr, finalResult);
    }

    // Null json to csv format.
    @Test
    public void testConvertJsonToCSVNull() throws Exception {

        String jsonStr = null;

        AsyncQuery queryObj = new AsyncQuery();
        AsyncQueryThread queryThread = new AsyncQueryThread(queryObj, user, elide, runner, asyncQueryDao, "v1",
                resultStorageEngine);

        assertEquals(Observable.empty(), queryThread.convertJsonToCSV(jsonStr, true));
    }

    //Invalid input for the json to csv conversion. This throws an exception.
    @Test
    public void testConvertJsonToCSV2() throws Exception {

        String csvStr = "ResponseBody";
        assertThrows(IllegalStateException.class, () -> {
            String jsonStr = "ResponseBody";

            AsyncQuery queryObj = new AsyncQuery();
            AsyncQueryThread queryThread = new AsyncQueryThread(queryObj, user, elide, runner, asyncQueryDao, "v1",
                    resultStorageEngine);

            assertEquals(queryThread.convertJsonToCSV(jsonStr, true), csvStr);
        });
    }

    @Test
    public void testCheckJsonStrErrorMessageValid() {
        AsyncQuery queryObj = new AsyncQuery();
        AsyncQueryThread queryThread = new AsyncQueryThread(queryObj, user, elide, runner, asyncQueryDao, "v1",
                resultStorageEngine);
        String message = "{\"data\": [{\"type \": \"book \",\"id \": \"3 \",\"attributes \": {\"title \": \"For Whom the Bell Tolls \"}}]}";
        assertEquals(false, queryThread.checkJsonStrErrorMessage(message));
    }

    @Test
    public void testCheckJsonStrErrorMessageInValid() {
        AsyncQuery queryObj = new AsyncQuery();
        AsyncQueryThread queryThread = new AsyncQueryThread(queryObj, user, elide, runner, asyncQueryDao, "v1",
                resultStorageEngine);
        String message = "{\"errors\": [{\"message \": \"error\"}]}";
        assertEquals(true, queryThread.checkJsonStrErrorMessage(message));
    }

    //Test with null resultStorageEngine
    @Test
    public void testProcessQueryNullResultEngine() throws Exception {

        AsyncQuery queryObj = new AsyncQuery();
        String responseBody = "{\"data\":{\"book\":{\"edges\":[{\"node\":{\"id\":\"1\",\"title\":\"Ender's Game\"}},"
                + "{\"node\":{\"id\":\"2\",\"title\":\"Song of Ice and Fire\"}},"
                + "{\"node\":{\"id\":\"3\",\"title\":\"For Whom the Bell Tolls\"}}]}}}";
        ElideResponse response = new ElideResponse(200, responseBody);
        String query = "{\"query\":\"{ group { edges { node { name commonName description } } } }\",\"variables\":null}";
        String id = "edc4a871-dff2-4054-804e-d80075cf827d";
        queryObj.setId(id);
        queryObj.setQuery(query);
        queryObj.setQueryType(QueryType.GRAPHQL_V1_0);
        queryObj.setResultType(ResultType.DOWNLOAD);
        queryObj.setResultFormatType(ResultFormatType.CSV);

        when(runner.run(any(), any(), eq(user), any())).thenReturn(response);
        AsyncQueryThread queryThread = new AsyncQueryThread(queryObj, user, elide, runner, asyncQueryDao, "v1",
                null);
        assertThrows(IllegalStateException.class, () -> {
            queryResultObj = queryThread.processQuery();
        });
    }

    @Test
    public void testDownloadPersistentResourceGraphQLJSON() throws Exception {
        Observable<String> jsonResults = populateDownloadString(ResultFormatType.JSON);
        String[] expected = new String[2];
        expected[0] = "edc4a871-dff2-4054-804e-d80075cf827e";
        expected[1] = "edc4a871-dff2-4054-804e-d80075cf827f";

        verifyObservableString(jsonResults, expected);
        assertTrue(validationResult);
    }

    @Test
    public void testDownloadPersistentResourceGraphQLCSV() throws Exception {
        Observable<String> csvResults = populateDownloadString(ResultFormatType.CSV);

        String[] expected = new String[3];
        expected[0] = "createdOn";
        expected[1] = "edc4a871-dff2-4054-804e-d80075cf827e";
        expected[2] = "edc4a871-dff2-4054-804e-d80075cf827f";

        verifyObservableString(csvResults, expected);
        assertTrue(validationResult);
    }

    private void verifyObservableString(Observable<String> results, String[] expected) {
        results
            .map(record -> record)
            .subscribe(
                record -> {
                    updateValidationResult(record, expected);
                },
                throwable -> {
                    throw new IllegalStateException(throwable);
                },
                () -> {
                    //do nothing
                }
            );
    }

    private void updateValidationResult(String result, String[] expected) {
        boolean status = false;
        for (int i = 0; i < expected.length; i++) {
            status = status || result.contains(expected[i]);
        }
        validationResult = status;
    }

    private Observable<String> populateDownloadString(ResultFormatType format) {
        AsyncQuery queryObj = new AsyncQuery();
        String id = "edc4a871-dff2-4054-804e-d80075cf827d";
        queryObj.setId(id);
        queryObj.setResultFormatType(format);

        AsyncQuery query1 = new AsyncQuery();
        AsyncQuery query2 = new AsyncQuery();
        query1.setId("edc4a871-dff2-4054-804e-d80075cf827e");
        query2.setId("edc4a871-dff2-4054-804e-d80075cf827f");

        RequestScope scope = mock(RequestScope.class);
        EntityDictionary dictionary = mock(EntityDictionary.class);
        when(scope.getDictionary()).thenReturn(dictionary);
        when(dictionary.getJsonAliasFor(any())).thenReturn("AsyncQuery");

        PersistentResource<AsyncQuery> asyncResource1 = new PersistentResource<>(query1, null, "1", scope);
        PersistentResource<AsyncQuery> asyncResource2 = new PersistentResource<>(query2, null, "2", scope);

        AsyncQueryThread queryThread = new AsyncQueryThread(queryObj, user, elide, runner, asyncQueryDao, "v1", null);
        queryThread.convertPersistentResourceToDownloadString(Observable.just(asyncResource1, asyncResource2));
        return queryThread.getDownloadString();
    }
}
