/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.async.service.thread;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.paiondata.elide.Elide;
import com.paiondata.elide.ElideSettings;
import com.paiondata.elide.async.models.AsyncApi;
import com.paiondata.elide.async.models.AsyncQuery;
import com.paiondata.elide.async.models.QueryStatus;
import com.paiondata.elide.async.service.dao.AsyncApiDao;
import com.paiondata.elide.async.service.dao.DefaultAsyncApiDao;
import com.paiondata.elide.core.TransactionRegistry;
import com.paiondata.elide.core.datastore.DataStoreTransaction;
import com.paiondata.elide.core.datastore.inmemory.HashMapDataStore;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.filter.expression.FilterExpression;
import com.paiondata.elide.core.security.checks.Check;
import com.paiondata.elide.core.utils.DefaultClassScanner;
import com.paiondata.elide.jsonapi.JsonApiSettings;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

public class AsyncApiCancelRunnableTest {

    private AsyncApiCancelRunnable cancelThread;
    private Elide elide;
    private AsyncApiDao asyncApiDao;
    private TransactionRegistry transactionRegistry;

    @BeforeEach
    public void setupMocks() {
        HashMapDataStore inMemoryStore = new HashMapDataStore(new DefaultClassScanner(),
                AsyncQuery.class.getPackage());
        Map<String, Class<? extends Check>> checkMappings = new HashMap<>();

        elide = new Elide(
                ElideSettings.builder().dataStore(inMemoryStore)
                        .entityDictionary(EntityDictionary.builder().checks(checkMappings).build())
                        .serdes(serdes -> serdes.withISO8601Dates("yyyy-MM-dd'T'HH:mm'Z'", TimeZone.getTimeZone("UTC")))
                        .settings(JsonApiSettings.builder())
                        .build());

        asyncApiDao = mock(DefaultAsyncApiDao.class);
        cancelThread = new AsyncApiCancelRunnable(Duration.ofSeconds(7), elide, asyncApiDao);
        transactionRegistry = elide.getTransactionRegistry();

    }

    @Test
    public void testAsyncQueryCancelThreadSet() {
        assertEquals(elide, cancelThread.getElide());
        assertEquals(asyncApiDao, cancelThread.getAsyncApiDao());
        assertEquals(7, cancelThread.getQueryMaxRunTimeSeconds());
    }

    @Test
    public void testActiveTransactionCancellation() {
        DataStoreTransaction dtx = elide.getDataStore().beginTransaction();
        transactionRegistry.addRunningTransaction(UUID.fromString("edc4a871-dff2-4054-804e-d80075cf828d"), dtx);
        AsyncQuery asyncQuery1 = createAsyncQueryTestObject("edc4a871-dff2-4054-804e-d80075cf828d",
                1577883600000L, QueryStatus.QUEUED);
        AsyncQuery asyncQuery2 = createAsyncQueryTestObject("edc4a871-dff2-4054-804e-d80075cf827d",
                1577883600000L, QueryStatus.QUEUED);
        Collection<AsyncApi> asyncCollection = new ArrayList<>();
        asyncCollection.add(asyncQuery1);
        asyncCollection.add(asyncQuery2);
        when(cancelThread.getAsyncApiDao().loadAsyncApiByFilter(any(), any())).thenReturn(asyncCollection);
        cancelThread.cancelAsyncApi(AsyncQuery.class);
        ArgumentCaptor<FilterExpression> filterCaptor = ArgumentCaptor.forClass(FilterExpression.class);
        ArgumentCaptor<QueryStatus> statusCaptor = ArgumentCaptor.forClass(QueryStatus.class);
        verify(asyncApiDao, times(1)).loadAsyncApiByFilter(any(), any());
        verify(asyncApiDao, times(1)).updateStatusAsyncApiByFilter(filterCaptor.capture(), statusCaptor.capture(), any());
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
        Collection<AsyncApi> asyncCollection = new ArrayList<>();
        asyncCollection.add(asyncQuery1);
        asyncCollection.add(asyncQuery2);
        asyncCollection.add(asyncQuery3);
        when(cancelThread.getAsyncApiDao().loadAsyncApiByFilter(any(), any())).thenReturn(asyncCollection);
        cancelThread.cancelAsyncApi(AsyncQuery.class);
        ArgumentCaptor<FilterExpression> fltStatusCaptor = ArgumentCaptor.forClass(FilterExpression.class);
        verify(asyncApiDao, times(1)).loadAsyncApiByFilter(fltStatusCaptor.capture(), any());
        assertEquals("asyncQuery.status IN [CANCELLED, PROCESSING, QUEUED]", fltStatusCaptor.getValue().toString());
        verify(asyncApiDao, times(1)).updateStatusAsyncApiByFilter(any(), any(), any());

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
        Collection<AsyncApi> asyncCollection = new ArrayList<>();
        asyncCollection.add(asyncQuery1);
        asyncCollection.add(asyncQuery2);
        asyncCollection.add(asyncQuery3);
        when(cancelThread.getAsyncApiDao().loadAsyncApiByFilter(any(), any())).thenReturn(asyncCollection);
        cancelThread.cancelAsyncApi(AsyncQuery.class);
        ArgumentCaptor<FilterExpression> filterCaptor = ArgumentCaptor.forClass(FilterExpression.class);
        ArgumentCaptor<QueryStatus> statusCaptor = ArgumentCaptor.forClass(QueryStatus.class);
        verify(asyncApiDao, times(1)).updateStatusAsyncApiByFilter(filterCaptor.capture(), statusCaptor.capture(), any());
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
