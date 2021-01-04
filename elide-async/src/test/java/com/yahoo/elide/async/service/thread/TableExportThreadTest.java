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

import java.net.MalformedURLException;
import java.net.URISyntaxException;

import org.apache.http.NoHttpResponseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.yahoo.elide.async.export.TableExporter;
import com.yahoo.elide.async.models.QueryType;
import com.yahoo.elide.async.models.ResultType;
import com.yahoo.elide.async.models.TableExport;
import com.yahoo.elide.async.models.TableExportResult;
import com.yahoo.elide.async.service.storageengine.ResultStorageEngine;

import io.reactivex.Observable;

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
        String query = "/group?sort=commonName&fields%5Bgroup%5D=commonName,description";
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

    //TODO - Additional Test Cases
}
