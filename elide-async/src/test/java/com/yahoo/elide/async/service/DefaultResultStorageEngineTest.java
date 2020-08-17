/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.AsyncQueryResult;
import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.security.checks.Check;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import javax.sql.rowset.serial.SerialBlob;
import javax.sql.rowset.serial.SerialException;

public class DefaultResultStorageEngineTest {
    private DataStoreTransaction tx;
    private DefaultResultStorageEngine defaultResultStorageEngine;
    private Elide elide;
    private DataStore dataStore;

    @BeforeEach
    public void setupMocks() {
        dataStore = mock(DataStore.class);
        tx = mock(DataStoreTransaction.class);
        Map<String, Class<? extends Check>> checkMappings = new HashMap<>();

        EntityDictionary dictionary = new EntityDictionary(checkMappings);
        dictionary.bindEntity(AsyncQuery.class);
        dictionary.bindEntity(AsyncQueryResult.class);

        ElideSettings elideSettings = new ElideSettingsBuilder(dataStore)
                .withEntityDictionary(dictionary)
                .withJoinFilterDialect(new RSQLFilterDialect(dictionary))
                .withSubqueryFilterDialect(new RSQLFilterDialect(dictionary))
                .withISO8601Dates("yyyy-MM-dd'T'HH:mm'Z'", TimeZone.getTimeZone("UTC"))
                .build();

        elide = new Elide(elideSettings);

        when(dataStore.beginTransaction()).thenReturn(tx);
        defaultResultStorageEngine = new DefaultResultStorageEngine("/api/v1/download", elide.getElideSettings(),
                dataStore);
    }

    @Test
    public void testStoreResults() throws SerialException, SQLException {
        String responseBody = "responseBody";
        byte[] testResponse = responseBody.getBytes();
        AsyncQueryResult result = new AsyncQueryResult();

        result = defaultResultStorageEngine.storeResults(result, responseBody, "ID");

        assertEquals(new SerialBlob(testResponse), result.getAttachment());
    }

    @Test
    public void testGenerateURLExceptionWithNullRequestURL() throws MalformedURLException {
        String asyncQueryID = "asyncQueryID";
        String requestURL = null;

        assertThrows(IllegalStateException.class, () -> {
            defaultResultStorageEngine.generateDownloadUrl(requestURL, asyncQueryID);
        });
    }

    @Test
    public void testGenerateURExceptionWithNullDownloadURI() throws MalformedURLException {
        String asyncQueryID = "asyncQueryID";
        String requestURL = "http://localhost:8080/";

        defaultResultStorageEngine = new DefaultResultStorageEngine(null, elide.getElideSettings(),
                dataStore);

        assertThrows(IllegalStateException.class, () -> {
            defaultResultStorageEngine.generateDownloadUrl(requestURL, asyncQueryID);
        });
    }

    @Test
    public void testGenerateURL() throws MalformedURLException {
        String asyncQueryID = "asyncQueryID";
        String requestURL = "http://localhost:8080/";
        URL testURL = new URL(requestURL + "api/v1/download/" + asyncQueryID);
        URL url = defaultResultStorageEngine.generateDownloadUrl(requestURL, asyncQueryID);

        assertEquals(url, testURL);
    }

    @Test
    public void testGetResultsByID() throws SerialException, SQLException {
        String id = "id";
        String test = "test";
        AsyncQuery query = new AsyncQuery();
        AsyncQueryResult result = new AsyncQueryResult();
        query.setId(id);
        result.setAttachment(new SerialBlob(test.getBytes()));
        query.setResult(result);

        when(tx.loadObject(any(), any(), any())).thenReturn(query);

        byte[] blob = defaultResultStorageEngine.getResultsByID(id);

        assertEquals(new String(blob), test);
        verify(tx, times(1)).loadObject(any(), any(), any());
    }
}
