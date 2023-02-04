/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service.dao;

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
import com.yahoo.elide.async.models.QueryStatus;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.datastore.DataStoreIterableBuilder;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.security.checks.Check;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

public class DefaultAsyncAPIDAOTest {

    private DefaultAsyncAPIDAO asyncAPIDAO;
    private Elide elide;
    private DataStore dataStore;
    private AsyncQuery asyncQuery;
    private AsyncQueryResult asyncQueryResult;
    private DataStoreTransaction tx;
    private FilterExpression filter;

    @BeforeEach
    public void setupMocks() {
        dataStore = mock(DataStore.class);
        asyncQuery = mock(AsyncQuery.class);
        asyncQueryResult = mock(AsyncQueryResult.class);
        filter = mock(FilterExpression.class);
        tx = mock(DataStoreTransaction.class);

        Map<String, Class<? extends Check>> checkMappings = new HashMap<>();

        EntityDictionary dictionary = EntityDictionary.builder().checks(checkMappings).build();
        dictionary.bindEntity(AsyncQuery.class);
        dictionary.bindEntity(AsyncQueryResult.class);

        ElideSettings elideSettings = new ElideSettingsBuilder(dataStore)
                .withEntityDictionary(dictionary)
                .withJoinFilterDialect(RSQLFilterDialect.builder().dictionary(dictionary).build())
                .withSubqueryFilterDialect(RSQLFilterDialect.builder().dictionary(dictionary).build())
                .withISO8601Dates("yyyy-MM-dd'T'HH:mm'Z'", TimeZone.getTimeZone("UTC"))
                .build();

        elide = new Elide(elideSettings);
        when(dataStore.beginTransaction()).thenReturn(tx);
        asyncAPIDAO = new DefaultAsyncAPIDAO(elide.getElideSettings(), dataStore);
    }

    @Test
    public void testAsyncQueryCleanerThreadSet() {
        assertEquals(elide.getElideSettings(), asyncAPIDAO.getElideSettings());
        assertEquals(dataStore, asyncAPIDAO.getDataStore());
    }

    @Test
    public void testUpdateStatus() {
        when(tx.loadObject(any(), any(), any())).thenReturn(asyncQuery);
        asyncAPIDAO.updateStatus("1234", QueryStatus.PROCESSING, asyncQuery.getClass());
        verify(dataStore, times(1)).beginTransaction();
        verify(tx, times(1)).save(any(AsyncQuery.class), any(RequestScope.class));
        verify(asyncQuery, times(1)).setStatus(QueryStatus.PROCESSING);
    }

   @Test
   public void testUpdateStatusAsyncQueryCollection() {
       Iterable<Object> loaded = Arrays.asList(asyncQuery, asyncQuery);
       when(tx.loadObjects(any(), any())).thenReturn(new DataStoreIterableBuilder(loaded).build());
       asyncAPIDAO.updateStatusAsyncAPIByFilter(filter, QueryStatus.TIMEDOUT, asyncQuery.getClass());
       verify(tx, times(2)).save(any(AsyncQuery.class), any(RequestScope.class));
       verify(asyncQuery, times(2)).setStatus(QueryStatus.TIMEDOUT);
   }

    @Test
    public void testDeleteAsyncQueryAndResultCollection() {
        Iterable<Object> loaded = Arrays.asList(asyncQuery, asyncQuery, asyncQuery);
        when(tx.loadObjects(any(), any())).thenReturn(new DataStoreIterableBuilder(loaded).build());
        asyncAPIDAO.deleteAsyncAPIAndResultByFilter(filter, asyncQuery.getClass());
        verify(dataStore, times(1)).beginTransaction();
        verify(tx, times(1)).loadObjects(any(), any());
        verify(tx, times(3)).delete(any(AsyncQuery.class), any(RequestScope.class));
    }

    @Test
    public void testUpdateAsyncQueryResult() {
        when(tx.loadObject(any(), any(), any())).thenReturn(asyncQuery);
        when(asyncQuery.getStatus()).thenReturn(QueryStatus.PROCESSING);
        asyncAPIDAO.updateAsyncAPIResult(asyncQueryResult, asyncQuery.getId(), asyncQuery.getClass());
        verify(dataStore, times(1)).beginTransaction();
        verify(tx, times(1)).save(any(AsyncQuery.class), any(RequestScope.class));
        verify(asyncQuery, times(1)).setResult(asyncQueryResult);
        verify(asyncQuery, times(1)).setStatus(QueryStatus.COMPLETE);

    }

    @Test
    public void testLoadAsyncQueryCollection() {
        Iterable<Object> loaded = Arrays.asList(asyncQuery, asyncQuery, asyncQuery);
        when(tx.loadObjects(any(), any())).thenReturn(new DataStoreIterableBuilder(loaded).build());
        asyncAPIDAO.loadAsyncAPIByFilter(filter, asyncQuery.getClass());
        verify(dataStore, times(1)).beginTransaction();
        verify(tx, times(1)).loadObjects(any(), any());
    }
}
