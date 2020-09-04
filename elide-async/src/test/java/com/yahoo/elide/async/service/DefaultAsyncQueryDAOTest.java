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
import com.yahoo.elide.async.models.QueryStatus;
import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.security.checks.Check;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

public class DefaultAsyncQueryDAOTest {

    private DefaultAsyncQueryDAO asyncQueryDAO;
    private Elide elide;
    private DataStore dataStore;
    private AsyncQuery asyncQuery;
    private AsyncQueryResult asyncQueryResult;
    private DataStoreTransaction tx;
    private EntityDictionary dictionary;
    private FilterExpression filter;

    @BeforeEach
    public void setupMocks() {
        dataStore = mock(DataStore.class);
        asyncQuery = mock(AsyncQuery.class);
        asyncQueryResult = mock(AsyncQueryResult.class);
        filter = mock(FilterExpression.class);
        tx = mock(DataStoreTransaction.class);

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
    }

    @Test
    public void testAsyncQueryCleanerThreadSet() {
        assertEquals(elide, asyncQueryDAO.getElide());
        assertEquals(dataStore, asyncQueryDAO.getDataStore());
    }

    @Test
    public void testUpdateStatus() {
        when(tx.loadObject(any(), any(), any())).thenReturn(asyncQuery);
        asyncQueryDAO.updateStatus("1234", QueryStatus.PROCESSING);
        verify(dataStore, times(1)).beginTransaction();
        verify(tx, times(1)).save(any(AsyncQuery.class), any(RequestScope.class));
        verify(asyncQuery, times(1)).setStatus(QueryStatus.PROCESSING);
    }

   @Test
   public void testUpdateStatusAsyncQueryCollection() {
       Iterable<Object> loaded = Arrays.asList(asyncQuery, asyncQuery);
       when(tx.loadObjects(any(), any())).thenReturn(loaded);
       asyncQueryDAO.updateStatusAsyncQueryCollection(filter, QueryStatus.TIMEDOUT);
       verify(tx, times(2)).save(any(AsyncQuery.class), any(RequestScope.class));
       verify(asyncQuery, times(2)).setStatus(QueryStatus.TIMEDOUT);
   }

    @Test
    public void testDeleteAsyncQueryAndResultCollection() {
        Iterable<Object> loaded = Arrays.asList(asyncQuery, asyncQuery, asyncQuery);
        when(tx.loadObjects(any(), any())).thenReturn(loaded);
        asyncQueryDAO.deleteAsyncQueryAndResultCollection(filter);
        verify(dataStore, times(1)).beginTransaction();
        verify(tx, times(1)).loadObjects(any(), any());
        verify(tx, times(3)).delete(any(AsyncQuery.class), any(RequestScope.class));
    }

    @Test
    public void testUpdateAsyncQueryResult() {
        when(tx.loadObject(any(), any(), any())).thenReturn(asyncQuery);
        when(asyncQuery.getStatus()).thenReturn(QueryStatus.PROCESSING);
        asyncQueryDAO.updateAsyncQueryResult(asyncQueryResult, asyncQuery.getId());
        verify(dataStore, times(1)).beginTransaction();
        verify(tx, times(1)).save(any(AsyncQuery.class), any(RequestScope.class));
        verify(asyncQuery, times(1)).setResult(asyncQueryResult);
        verify(asyncQuery, times(1)).setStatus(QueryStatus.COMPLETE);

    }

    @Test
    public void testLoadAsyncQueryCollection() {
        Iterable<Object> loaded = Arrays.asList(asyncQuery, asyncQuery, asyncQuery);
        when(tx.loadObjects(any(), any())).thenReturn(loaded);
        asyncQueryDAO.loadAsyncQueryCollection(filter);
        verify(dataStore, times(1)).beginTransaction();
        verify(tx, times(1)).loadObjects(any(), any());
    }
}
