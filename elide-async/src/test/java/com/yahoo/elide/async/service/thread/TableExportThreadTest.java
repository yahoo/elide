/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service.thread;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yahoo.elide.async.export.TableExporter;
import com.yahoo.elide.async.models.QueryType;
import com.yahoo.elide.async.models.ResultType;
import com.yahoo.elide.async.models.TableExport;
import com.yahoo.elide.async.models.TableExportResult;
import com.yahoo.elide.async.service.storageengine.ResultStorageEngine;
import com.yahoo.elide.core.PersistentResource;

import org.apache.http.NoHttpResponseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.reactivex.Observable;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;

public class TableExportThreadTest {

    private TableExporter exporter;
    private ResultStorageEngine engine;

    @BeforeEach
    public void setupMocks() {
        exporter = mock(TableExporter.class);
        engine = mock(ResultStorageEngine.class);
    }

    @Test
    public void testProcessQueryGraphqlApiEmptyResults() throws NoHttpResponseException, URISyntaxException,
            MalformedURLException {
        TableExport queryObj = new TableExport();
        String query = "/tableExport";
        String id = "edc4a871-dff2-4054-804e-d80075cf827d";
        queryObj.setId(id);
        queryObj.setQuery(query);
        queryObj.setQueryType(QueryType.GRAPHQL_V1_0);
        queryObj.setResultType(ResultType.CSV);

        when(exporter.export(any())).thenReturn(Observable.empty());
        when(engine.storeResults(any(), any())).thenReturn(queryObj);

        TableExportThread queryThread = new TableExportThread(queryObj, engine, exporter);
        TableExportResult queryResultObj = (TableExportResult) queryThread.call();
        assertEquals(queryResultObj.getHttpStatus(), 200);
        assertEquals(queryResultObj.getRecordCount(), 0);
    }

    @Test
    public void testJsonToCSV() {
        String row1 = "id, query, queryType, requestId, principalName, status, createdOn, updatedOn, asyncAfterSeconds, resultType, result";
        String row2 = "\"edc4a871-dff2-4054-804e-d80075cf827d\", \"/tableExport\", \"GRAPHQL_V1_0\"";
        String row2End = "10.0, \"CSV\", null";
        TableExport queryObj = new TableExport();
        String query = "/tableExport";
        String id = "edc4a871-dff2-4054-804e-d80075cf827d";
        queryObj.setId(id);
        queryObj.setQuery(query);
        queryObj.setQueryType(QueryType.GRAPHQL_V1_0);
        queryObj.setResultType(ResultType.CSV);

        PersistentResource resource = mock(PersistentResource.class);
        when(resource.getObject()).thenReturn(queryObj);
        TableExportThread queryThread = new TableExportThread(queryObj, engine, exporter);
        queryThread.incrementRecordCount();
        String output = queryThread.convertToCSV(resource);
        assertEquals(true, output.startsWith(row1));
        assertEquals(true, output.contains(row2));
        assertEquals(true, output.endsWith(row2End));
    }

    @Test
    public void testResourceToJSON() throws IOException {
        String start = "{\"id\":\"edc4a871-dff2-4054-804e-d80075cf827d\",\"query\":\"/tableExport\","
                + "\"queryType\":\"GRAPHQL_V1_0\",";
        String end = "\"asyncAfterSeconds\":10,\"resultType\":\"CSV\",\"result\":null}";
        TableExport queryObj = new TableExport();
        String query = "/tableExport";
        String id = "edc4a871-dff2-4054-804e-d80075cf827d";
        queryObj.setId(id);
        queryObj.setQuery(query);
        queryObj.setQueryType(QueryType.GRAPHQL_V1_0);
        queryObj.setResultType(ResultType.CSV);

        PersistentResource resource = mock(PersistentResource.class);
        when(resource.getObject()).thenReturn(queryObj);
        TableExportThread queryThread = new TableExportThread(queryObj, engine, exporter);
        String output = queryThread.resourceToJsonStr(resource);
        assertEquals(true, output.startsWith(start));
        assertEquals(true, output.endsWith(end));
    }
    //TODO - Additional Test Cases
}
