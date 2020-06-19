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


import java.sql.Blob;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import javax.sql.rowset.serial.SerialBlob;

public class DefaultResultStorageEngineTest {

    private DefaultAsyncQueryDAO asyncQueryDAO;
    private Elide elide;
    private DataStore dataStore;
    private AsyncQuery asyncQuery;
    private DataStoreTransaction tx;
    private EntityDictionary dictionary;
    private DefaultResultStorageEngine defaultResultStorageEngine;
    private AsyncQueryResultStorage asyncQueryResultStorage;

    @BeforeEach
    public void setupMocks() {
        dataStore = mock(DataStore.class);
        asyncQuery = mock(AsyncQuery.class);
        tx = mock(DataStoreTransaction.class);
        asyncQueryResultStorage = mock(AsyncQueryResultStorage.class);

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

        asyncQueryDAO = new DefaultAsyncQueryDAO(elide, dataStore);
        defaultResultStorageEngine = new DefaultResultStorageEngine(elide, dataStore);
    }

    @Test
    public void testStoreResults() {
        String responseBody = "responseBody";
        byte[] testResponse = responseBody.getBytes();
        Blob result = null;

        try {
            result = new SerialBlob(testResponse);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        AsyncQueryResultStorage storeResultsOutput = defaultResultStorageEngine.storeResults(
                asyncQueryResultStorage.getId(), responseBody);

        assertEquals(asyncQueryResultStorage.getId(), storeResultsOutput.getId());
        assertEquals(result, storeResultsOutput.getResult());


    }

    @Test
    public void testGetResultsByID() {
        UUID uuid = UUID.randomUUID();
        AsyncQueryResultStorage n = new AsyncQueryResultStorage();
        n.setId(uuid);
        n.setResult(null);

        Object loaded = n;
        when(tx.loadObject(any(), any(), any())).thenReturn(loaded);

        defaultResultStorageEngine.getResultsByID(n.getId().toString());

        verify(tx, times(1)).loadObject(any(), any(), any());
        assertEquals(uuid,
                defaultResultStorageEngine.getResultsByID(n.getId().toString()).getId());
    }

    @Test
    public void testDeleteResultsByID() {
        UUID uuid = UUID.randomUUID();
        AsyncQueryResultStorage n = new AsyncQueryResultStorage();
        n.setId(uuid);
        n.setResult(null);

        Object loaded = n;
        when(tx.loadObject(any(), any(), any())).thenReturn(loaded);

        defaultResultStorageEngine.deleteResultsByID(n.getId().toString());

        verify(tx, times(1)).loadObject(any(), any(), any());
        verify(tx, times(1)).delete(any(), any());
        assertEquals(uuid,
                defaultResultStorageEngine.deleteResultsByID(n.getId().toString()).getId());
    }

    @Test
    public void testDeleteAllResults() {
        Iterable<Object> loaded = Arrays.asList(asyncQueryResultStorage, asyncQueryResultStorage);
        when(tx.loadObjects(any(), any())).thenReturn(loaded);
        defaultResultStorageEngine.deleteAllResults();

        verify(tx, times(1)).loadObjects(any(), any());
        verify(tx, times(2)).delete(any(), any());

    }
}
