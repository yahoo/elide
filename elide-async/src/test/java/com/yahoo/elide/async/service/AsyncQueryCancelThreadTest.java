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
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.async.models.AsyncAPI;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.QueryStatus;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.TransactionRegistry;
import com.yahoo.elide.core.datastore.inmemory.HashMapDataStore;
import com.yahoo.elide.core.filter.dialect.ParseException;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.security.checks.Check;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

public class AsyncQueryCancelThreadTest {

    private AsyncQueryCancelThread cancelThread;
    private Elide elide;
    private AsyncQueryDAO asyncQueryDao;
    private TransactionRegistry transactionRegistry;

    @BeforeEach
    public void setupMocks() {
        HashMapDataStore inMemoryStore = new HashMapDataStore(AsyncQuery.class.getPackage());
        Map<String, Class<? extends Check>> checkMappings = new HashMap<>();

        elide = new Elide(
                new ElideSettingsBuilder(inMemoryStore)
                        .withEntityDictionary(new EntityDictionary(checkMappings))
                        .withISO8601Dates("yyyy-MM-dd'T'HH:mm'Z'", TimeZone.getTimeZone("UTC"))
                        .build());

        asyncQueryDao = mock(DefaultAsyncQueryDAO.class);
        cancelThread = new AsyncQueryCancelThread<AsyncQuery>(7, elide, asyncQueryDao, AsyncQuery.class);
        transactionRegistry = elide.getTransactionRegistry();

    }

    @Test
    public void testAsyncQueryCancelThreadSet() {
        assertEquals(elide, cancelThread.getElide());
        assertEquals(asyncQueryDao, cancelThread.getAsyncQueryDao());
        assertEquals(7, cancelThread.getMaxRunTimeSeconds());
    }

    @Test
    public void testActiveTransactionCancellation() {
        DataStoreTransaction dtx = elide.getDataStore().beginTransaction();
        transactionRegistry.addRunningTransaction(UUID.fromString("edc4a871-dff2-4054-804e-d80075cf828d"), dtx);
        AsyncQuery asyncQuery1 = createAsyncQueryTestObject("edc4a871-dff2-4054-804e-d80075cf828d",
                1577883600000L, QueryStatus.QUEUED);
        AsyncQuery asyncQuery2 = createAsyncQueryTestObject("edc4a871-dff2-4054-804e-d80075cf827d",
                1577883600000L, QueryStatus.QUEUED);
        Collection<AsyncAPI> asyncCollection = new ArrayList<AsyncAPI>();
        asyncCollection.add(asyncQuery1);
        asyncCollection.add(asyncQuery2);
        when(cancelThread.getAsyncQueryDao().loadAsyncQueryCollection(any(), any())).thenReturn(asyncCollection);
        cancelThread.cancelAsyncQuery();
        ArgumentCaptor<FilterExpression> filterCaptor = ArgumentCaptor.forClass(FilterExpression.class);
        ArgumentCaptor<QueryStatus> statusCaptor = ArgumentCaptor.forClass(QueryStatus.class);
        verify(asyncQueryDao, times(1)).loadAsyncQueryCollection(any(), any());
        verify(asyncQueryDao, times(1)).updateStatusAsyncQueryCollection(filterCaptor.capture(), statusCaptor.capture(), any());
        assertEquals("asyncQuery.id IN [[edc4a871-dff2-4054-804e-d80075cf828d]]", filterCaptor.getValue().toString());
        assertEquals("CANCEL_COMPLETE", statusCaptor.getValue().toString());
    }

    @Test
    public void testStatusBasedFilter() throws ParseException {
        DataStoreTransaction dtx = elide.getDataStore().beginTransaction();
        transactionRegistry.addRunningTransaction(UUID.fromString("edc4a871-dff2-4054-804e-d80075cf828d"), dtx);
        transactionRegistry.addRunningTransaction(UUID.fromString("edc4a871-dff2-4054-804e-d80075cf827d"), dtx);
        transactionRegistry.addRunningTransaction(UUID.fromString("edc4a871-dff2-4054-804e-d80075cf826d"), dtx);
        AsyncQuery asyncQuery1 = createAsyncQueryTestObject("edc4a871-dff2-4054-804e-d80075cf828d",
                1577883600000L, QueryStatus.CANCEL_COMPLETE);
        AsyncQuery asyncQuery2 = createAsyncQueryTestObject("edc4a871-dff2-4054-804e-d80075cf827d",
                1577883600000L, QueryStatus.CANCELLED);
        AsyncQuery asyncQuery3 = createAsyncQueryTestObject("edc4a871-dff2-4054-804e-d80075cf826d",
                1577883600000L, QueryStatus.PROCESSING);
        Collection<AsyncAPI> asyncCollection = new ArrayList<AsyncAPI>();
        asyncCollection.add(asyncQuery1);
        asyncCollection.add(asyncQuery2);
        asyncCollection.add(asyncQuery3);
        when(cancelThread.getAsyncQueryDao().loadAsyncQueryCollection(any(), any())).thenReturn(asyncCollection);
        cancelThread.cancelAsyncQuery();
        ArgumentCaptor<FilterExpression> fltStatusCaptor = ArgumentCaptor.forClass(FilterExpression.class);
        verify(asyncQueryDao, times(1)).loadAsyncQueryCollection(fltStatusCaptor.capture(), any());
        assertEquals("asyncQuery.status IN [[CANCELLED, PROCESSING, QUEUED]]", fltStatusCaptor.getValue().toString());
        verify(asyncQueryDao, times(1)).updateStatusAsyncQueryCollection(any(), any(), any());

    }

    @Test
    public void testTimeBasedCancellation() {
        DataStoreTransaction dtx = elide.getDataStore().beginTransaction();
        transactionRegistry.addRunningTransaction(UUID.fromString("edc4a871-dff2-4054-804e-d80075cf828d"), dtx);
        transactionRegistry.addRunningTransaction(UUID.fromString("edc4a871-dff2-4054-804e-d80075cf827d"), dtx);
        AsyncQuery asyncQuery1 = createAsyncQueryTestObject("edc4a871-dff2-4054-804e-d80075cf828d",
                System.currentTimeMillis(), QueryStatus.QUEUED);
        AsyncQuery asyncQuery2 = createAsyncQueryTestObject("edc4a871-dff2-4054-804e-d80075cf827d",
                1577883600000L, QueryStatus.QUEUED);
        AsyncQuery asyncQuery3 = createAsyncQueryTestObject("edc4a871-dff2-4054-804e-d80075cf826d",
                1577883600000L, QueryStatus.QUEUED);
        Collection<AsyncAPI> asyncCollection = new ArrayList<AsyncAPI>();
        asyncCollection.add(asyncQuery1);
        asyncCollection.add(asyncQuery2);
        asyncCollection.add(asyncQuery3);
        when(cancelThread.getAsyncQueryDao().loadAsyncQueryCollection(any(), any())).thenReturn(asyncCollection);
        cancelThread.cancelAsyncQuery();
        ArgumentCaptor<FilterExpression> filterCaptor = ArgumentCaptor.forClass(FilterExpression.class);
        ArgumentCaptor<QueryStatus> statusCaptor = ArgumentCaptor.forClass(QueryStatus.class);
        verify(asyncQueryDao, times(1)).updateStatusAsyncQueryCollection(filterCaptor.capture(), statusCaptor.capture(), any());
        assertEquals("asyncQuery.id IN [[edc4a871-dff2-4054-804e-d80075cf827d]]", filterCaptor.getValue().toString());
        assertEquals("CANCEL_COMPLETE", statusCaptor.getValue().toString());

    }

    public AsyncQuery createAsyncQueryTestObject(String id, Long createdOn, QueryStatus status) {
        AsyncQuery asyncQuery = new AsyncQuery();
        asyncQuery.setId(id);
        asyncQuery.setRequestId(id);
        asyncQuery.setCreatedOn(new Date(createdOn));
        asyncQuery.setStatus(status);
        return asyncQuery;
    }
}
