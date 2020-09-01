/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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

import io.reactivex.Observable;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import javax.sql.rowset.serial.SerialException;

public class DefaultResultStorageEngineTest {
    private DataStoreTransaction tx;
    private DefaultResultStorageEngine defaultResultStorageEngine;
    private Elide elide;
    private DataStore dataStore;
    private AsyncQueryDAO asyncQueryDAO;

    @BeforeEach
    public void setupMocks() {
        dataStore = mock(DataStore.class);
        asyncQueryDAO = mock(DefaultAsyncQueryDAO.class);
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
        defaultResultStorageEngine = new DefaultResultStorageEngine(elide.getElideSettings(),
                dataStore, asyncQueryDAO);
    }

    @Test
    public void testStoreResults() throws SerialException, SQLException {
        String responseBody = "responseBody";
        AsyncQueryResult result = new AsyncQueryResult();

        result = defaultResultStorageEngine.storeResults(result, responseBody, "ID");

        assertEquals(responseBody, result.getAttachment());
    }

    @Test
    public void testGetResultsByID() throws SerialException, SQLException {
        String id = "id";
        String test = "test";
        AsyncQuery query = new AsyncQuery();
        AsyncQueryResult result = new AsyncQueryResult();
        query.setId(id);
        result.setAttachment(test);
        query.setResult(result);

        Collection<AsyncQuery> asyncCollection = new ArrayList<AsyncQuery>();
        asyncCollection.add(query);
        when(asyncQueryDAO.loadAsyncQueryCollection(any())).thenReturn(asyncCollection);

        Observable<String> observableLob = defaultResultStorageEngine.getResultsByID(id);

        assertEquals(observableLob.toList().blockingGet().get(0), test);
    }
}
