/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import static com.yahoo.elide.core.EntityDictionary.NO_VERSION;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.request.EntityProjection;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

public class QueryLoggerTest {

    private EntityDictionary dictionary;

    public QueryLoggerTest() throws Exception {
        this.dictionary = TestDictionary.getTestDictionary();
        this.dictionary.bindEntity(FieldTestModel.class);
    }

    private ElideSettings getElideSettings(DataStore dataStore, EntityDictionary dictionary, QueryLogger ql) {
        return new ElideSettingsBuilder(dataStore)
                .withEntityDictionary(dictionary)
                .withQueryLogger(ql)
                .withVerboseErrors()
                .build();
    }

    private Elide getElide(DataStore dataStore, EntityDictionary dictionary, QueryLogger ql) {
        return new Elide(getElideSettings(dataStore, dictionary, ql));
    }

    @Test
    public void testElidePostQueryLogger() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        QueryLogger ql = mock(QueryLogger.class);
        FieldTestModel mockModel = mock(FieldTestModel.class);

        Elide elide = getElide(store, dictionary, ql);

        String body = "{\"data\": {\"type\":\"testModel\",\"id\":\"1\",\"attributes\": {\"field\":\"Foo\"}}}";

        when(store.beginTransaction()).thenReturn(tx);
        when(tx.createNewObject(FieldTestModel.class)).thenReturn(mockModel);

        ElideResponse response = elide.post("/testModel", body, null, NO_VERSION);
        assertEquals(HttpStatus.SC_CREATED, response.getResponseCode());

        verify(ql, times(1)).acceptQuery(isA(UUID.class), isNull(), any(), any(), any(), eq("/testModel"));
        verify(ql, times(1)).processQuery(isA(UUID.class), isA(EntityProjection.class), isA(RequestScope.class), isA(DataStoreTransaction.class));
        verify(ql, times(1)).completeQuery(isA(UUID.class), eq(response));
    }

    @Test
    public void testElideGetQueryLogger() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        QueryLogger ql = mock(QueryLogger.class);
        FieldTestModel mockModel = mock(FieldTestModel.class);

        Elide elide = getElide(store, dictionary, ql);

        when(store.beginReadTransaction()).thenCallRealMethod();
        when(store.beginTransaction()).thenReturn(tx);
        when(tx.loadObject(isA(EntityProjection.class), any(), isA(RequestScope.class))).thenReturn(mockModel);

        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        ElideResponse response = elide.get("/testModel/1", headers, null, NO_VERSION);
        assertEquals(HttpStatus.SC_OK, response.getResponseCode());

        verify(ql, times(1)).acceptQuery(isA(UUID.class), isNull(), any(), any(), any(), eq("/testModel/1"));
        verify(ql, times(1)).processQuery(isA(UUID.class), isA(EntityProjection.class), isA(RequestScope.class), isA(DataStoreTransaction.class));
        verify(ql, times(1)).completeQuery(isA(UUID.class), eq(response));
    }

    @Test
    public void testElideDeleteQueryLogger() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        QueryLogger ql = mock(QueryLogger.class);
        FieldTestModel mockModel = mock(FieldTestModel.class);

        Elide elide = getElide(store, dictionary, ql);

        dictionary.setValue(mockModel, "id", "1");
        when(store.beginTransaction()).thenReturn(tx);
        when(tx.loadObject(isA(EntityProjection.class), any(), isA(RequestScope.class))).thenReturn(mockModel);

        ElideResponse response = elide.delete("/testModel/1", "", null, NO_VERSION);
        assertEquals(HttpStatus.SC_NO_CONTENT, response.getResponseCode());

        verify(ql, times(1)).acceptQuery(isA(UUID.class), isNull(), any(), any(), any(), eq("/testModel/1"));
        verify(ql, times(1)).processQuery(isA(UUID.class), isA(EntityProjection.class), isA(RequestScope.class), isA(DataStoreTransaction.class));
        verify(ql, times(1)).completeQuery(isA(UUID.class), eq(response));
    }

    @Test
    public void testElidePatchQueryLogger() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        QueryLogger ql = mock(QueryLogger.class);
        FieldTestModel mockModel = mock(FieldTestModel.class);

        Elide elide = getElide(store, dictionary, ql);

        String body = "{\"data\": {\"type\":\"testModel\",\"id\":\"1\",\"attributes\": {\"field\":\"Foo\"}}}";

        dictionary.setValue(mockModel, "id", "1");
        when(store.beginTransaction()).thenReturn(tx);
        when(tx.loadObject(isA(EntityProjection.class), any(), isA(RequestScope.class))).thenReturn(mockModel);

        ElideResponse response = elide.patch(Elide.JSONAPI_CONTENT_TYPE, Elide.JSONAPI_CONTENT_TYPE, "/testModel/1", body, null, NO_VERSION);
        assertEquals(HttpStatus.SC_NO_CONTENT, response.getResponseCode());

        verify(ql, times(1)).acceptQuery(isA(UUID.class), isNull(), any(), any(), any(), eq("/testModel/1"));
        verify(ql, times(1)).processQuery(isA(UUID.class), isA(EntityProjection.class), isA(RequestScope.class), isA(DataStoreTransaction.class));
        verify(ql, times(1)).completeQuery(isA(UUID.class), eq(response));
    }
}
