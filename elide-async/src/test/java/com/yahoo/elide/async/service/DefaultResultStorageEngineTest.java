/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.yahoo.elide.async.models.AsyncQueryResultStorage;
import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.security.checks.Check;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

public class DefaultResultStorageEngineTest {

    private com.yahoo.elide.async.service.DefaultAsyncQueryDAO asyncQueryDAO;
    private Elide elide;
    private DataStore dataStore;
    private AsyncQuery asyncQuery;
    private DataStoreTransaction tx;
    private EntityDictionary dictionary;
    private com.yahoo.elide.async.service.DefaultResultStorageEngine defaultResultStorageEngine;
    private AsyncQueryResultStorage asyncQueryResultStorage;

    @BeforeEach
    public void setupMocks() {
        dataStore = mock(DataStore.class);
        tx = mock(DataStoreTransaction.class);
        asyncQueryResultStorage = mock(AsyncQueryResultStorage.class);
        asyncQuery = mock(AsyncQuery.class);
        Map<String, Class<? extends Check>> checkMappings = new HashMap<>();

        dictionary = new EntityDictionary(checkMappings);
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
        asyncQueryDAO = new DefaultAsyncQueryDAO(elide.getElideSettings(), dataStore);
        defaultResultStorageEngine = new DefaultResultStorageEngine(elide.getElideSettings(),
                dataStore, "http://localhost:8080/AsyncQueryResultStorage/");
    }

    @Test
    public void testStoreResults() throws MalformedURLException {
        String responseBody = "responseBody";
        byte[] testResponse = responseBody.getBytes();
        when(asyncQueryResultStorage.getId()).thenReturn("asyncQueryID");
        URL testURL = new URL("http://localhost:8080/AsyncQueryResultStorage/asyncQueryID");

        URL url = defaultResultStorageEngine.storeResults(
                asyncQueryResultStorage.getId(), testResponse);

        assertEquals(url, testURL);
    }

    @Test
    public void testGetResultsByID() {
        String id = "id";
        defaultResultStorageEngine.getResultsByID(id);

        verify(tx, times(1)).loadObject(any(), any(), any());

    }

    @Test
    public void testDeleteResultsCollection() {
        Iterable<Object> loaded = Arrays.asList(asyncQuery, asyncQuery, asyncQuery);
        when(tx.loadObjects(any(), any())).thenReturn(loaded);

        Collection<AsyncQuery> asyncQueryCollection = asyncQueryDAO.deleteAsyncQueryAndResultCollection("createdOn=le='2020-03-23T02:02Z'");

        defaultResultStorageEngine.deleteResultsCollection(asyncQueryCollection);

        verify(tx, times(3)).loadObject(any(), any(), any());

    }
}
