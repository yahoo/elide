/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service.thread;

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
import com.yahoo.elide.async.service.dao.AsyncAPIDAO;
import com.yahoo.elide.async.service.dao.DefaultAsyncAPIDAO;
import com.yahoo.elide.core.TransactionRegistry;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.datastore.inmemory.HashMapDataStore;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.security.checks.Check;
import com.yahoo.elide.core.utils.DefaultClassScanner;
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

public class AsyncAPICancelRunnableTest {

    private AsyncAPICancelRunnable cancelThread;
    private Elide elide;
    private AsyncAPIDAO asyncAPIDao;
    private TransactionRegistry transactionRegistry;

    @BeforeEach
    public void setupMocks() {
        HashMapDataStore inMemoryStore = new HashMapDataStore(DefaultClassScanner.getInstance(),
                AsyncQuery.class.getPackage());
        Map<String, Class<? extends Check>> checkMappings = new HashMap<>();

        elide = new Elide(
                new ElideSettingsBuilder(inMemoryStore)
                        .withEntityDictionary(EntityDictionary.builder().checks(checkMappings).build())
                        .withISO8601Dates("yyyy-MM-dd'T'HH:mm'Z'", TimeZone.getTimeZone("UTC"))
                        .build());

        asyncAPIDao = mock(DefaultAsyncAPIDAO.class);
        cancelThread = new AsyncAPICancelRunnable(7, elide, asyncAPIDao);
        transactionRegistry = elide.getTransactionRegistry();

    }

    @Test
    public void testAsyncQueryCancelThreadSet() {
        assertEquals(elide, cancelThread.getElide());
        assertEquals(asyncAPIDao, cancelThread.getAsyncAPIDao());
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
        Collection<AsyncAPI> asyncCollection = new ArrayList<>();
        asyncCollection.add(asyncQuery1);
        asyncCollection.add(asyncQuery2);
        when(cancelThread.getAsyncAPIDao().loadAsyncAPIByFilter(any(), any())).thenReturn(asyncCollection);
        cancelThread.cancelAsyncAPI(AsyncQuery.class);
        ArgumentCaptor<FilterExpression> filterCaptor = ArgumentCaptor.forClass(FilterExpression.class);
        ArgumentCaptor<QueryStatus> statusCaptor = ArgumentCaptor.forClass(QueryStatus.class);
        verify(asyncAPIDao, times(1)).loadAsyncAPIByFilter(any(), any());
        verify(asyncAPIDao, times(1)).updateStatusAsyncAPIByFilter(filterCaptor.capture(), statusCaptor.capture(), any());
        assertEquals("asyncQuery.id IN [[edc4a871-dff2-4054-804e-d80075cf828d]]", filterCaptor.getValue().toString());
        assertEquals("CANCEL_COMPLETE", statusCaptor.getValue().toString());
    }

    @Test
    public void testStatusBasedFilter() {
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
        Collection<AsyncAPI> asyncCollection = new ArrayList<>();
        asyncCollection.add(asyncQuery1);
        asyncCollection.add(asyncQuery2);
        asyncCollection.add(asyncQuery3);
        when(cancelThread.getAsyncAPIDao().loadAsyncAPIByFilter(any(), any())).thenReturn(asyncCollection);
        cancelThread.cancelAsyncAPI(AsyncQuery.class);
        ArgumentCaptor<FilterExpression> fltStatusCaptor = ArgumentCaptor.forClass(FilterExpression.class);
        verify(asyncAPIDao, times(1)).loadAsyncAPIByFilter(fltStatusCaptor.capture(), any());
        assertEquals("asyncQuery.status IN [CANCELLED, PROCESSING, QUEUED]", fltStatusCaptor.getValue().toString());
        verify(asyncAPIDao, times(1)).updateStatusAsyncAPIByFilter(any(), any(), any());

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
        Collection<AsyncAPI> asyncCollection = new ArrayList<>();
        asyncCollection.add(asyncQuery1);
        asyncCollection.add(asyncQuery2);
        asyncCollection.add(asyncQuery3);
        when(cancelThread.getAsyncAPIDao().loadAsyncAPIByFilter(any(), any())).thenReturn(asyncCollection);
        cancelThread.cancelAsyncAPI(AsyncQuery.class);
        ArgumentCaptor<FilterExpression> filterCaptor = ArgumentCaptor.forClass(FilterExpression.class);
        ArgumentCaptor<QueryStatus> statusCaptor = ArgumentCaptor.forClass(QueryStatus.class);
        verify(asyncAPIDao, times(1)).updateStatusAsyncAPIByFilter(filterCaptor.capture(), statusCaptor.capture(), any());
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
