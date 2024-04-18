/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.async.service.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.paiondata.elide.Elide;
import com.paiondata.elide.ElideSettings;
import com.paiondata.elide.async.models.AsyncQuery;
import com.paiondata.elide.async.models.AsyncQueryResult;
import com.paiondata.elide.async.models.QueryStatus;
import com.paiondata.elide.core.RequestScope;
import com.paiondata.elide.core.datastore.DataStore;
import com.paiondata.elide.core.datastore.DataStoreIterableBuilder;
import com.paiondata.elide.core.datastore.DataStoreTransaction;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.filter.dialect.RSQLFilterDialect;
import com.paiondata.elide.core.filter.expression.FilterExpression;
import com.paiondata.elide.core.security.checks.Check;
import com.paiondata.elide.jsonapi.JsonApiSettings;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

public class DefaultAsyncApiDaoTest {

    private DefaultAsyncApiDao asyncApiDao;
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

        JsonApiSettings.JsonApiSettingsBuilder jsonApiSettings = JsonApiSettings.builder()
                .joinFilterDialect(RSQLFilterDialect.builder().dictionary(dictionary).build())
                .subqueryFilterDialect(RSQLFilterDialect.builder().dictionary(dictionary).build());

        ElideSettings elideSettings = ElideSettings.builder().dataStore(dataStore)
                .entityDictionary(dictionary)
                .settings(jsonApiSettings)
                .serdes(serdes -> serdes.withISO8601Dates("yyyy-MM-dd'T'HH:mm'Z'", TimeZone.getTimeZone("UTC")))
                .build();

        elide = new Elide(elideSettings);
        when(dataStore.beginTransaction()).thenReturn(tx);
        asyncApiDao = new DefaultAsyncApiDao(elide.getElideSettings(), dataStore);
    }

    @Test
    public void testAsyncQueryCleanerThreadSet() {
        assertEquals(elide.getElideSettings(), asyncApiDao.getElideSettings());
        assertEquals(dataStore, asyncApiDao.getDataStore());
    }

    @Test
    public void testUpdateStatus() {
        when(tx.loadObject(any(), any(), any())).thenReturn(asyncQuery);
        asyncApiDao.updateStatus("1234", QueryStatus.PROCESSING, asyncQuery.getClass());
        verify(dataStore, times(1)).beginTransaction();
        verify(tx, times(1)).save(any(AsyncQuery.class), any(RequestScope.class));
        verify(asyncQuery, times(1)).setStatus(QueryStatus.PROCESSING);
    }

   @Test
   public void testUpdateStatusAsyncQueryCollection() {
       Iterable<Object> loaded = Arrays.asList(asyncQuery, asyncQuery);
       when(tx.loadObjects(any(), any())).thenReturn(new DataStoreIterableBuilder(loaded).build());
       asyncApiDao.updateStatusAsyncApiByFilter(filter, QueryStatus.TIMEDOUT, asyncQuery.getClass());
       verify(tx, times(2)).save(any(AsyncQuery.class), any(RequestScope.class));
       verify(asyncQuery, times(2)).setStatus(QueryStatus.TIMEDOUT);
   }

    @Test
    public void testDeleteAsyncQueryAndResultCollection() {
        Iterable<Object> loaded = Arrays.asList(asyncQuery, asyncQuery, asyncQuery);
        when(tx.loadObjects(any(), any())).thenReturn(new DataStoreIterableBuilder(loaded).build());
        asyncApiDao.deleteAsyncApiAndResultByFilter(filter, asyncQuery.getClass());
        verify(dataStore, times(1)).beginTransaction();
        verify(tx, times(1)).loadObjects(any(), any());
        verify(tx, times(3)).delete(any(AsyncQuery.class), any(RequestScope.class));
    }

    @Test
    public void testUpdateAsyncQueryResult() {
        when(tx.loadObject(any(), any(), any())).thenReturn(asyncQuery);
        when(asyncQuery.getStatus()).thenReturn(QueryStatus.PROCESSING);
        asyncApiDao.updateAsyncApiResult(asyncQueryResult, asyncQuery.getId(), asyncQuery.getClass());
        verify(dataStore, times(1)).beginTransaction();
        verify(tx, times(1)).save(any(AsyncQuery.class), any(RequestScope.class));
        verify(asyncQuery, times(1)).setResult(asyncQueryResult);
        verify(asyncQuery, times(1)).setStatus(QueryStatus.COMPLETE);

    }

    @Test
    public void testLoadAsyncQueryCollection() {
        Iterable<Object> loaded = Arrays.asList(asyncQuery, asyncQuery, asyncQuery);
        when(tx.loadObjects(any(), any())).thenReturn(new DataStoreIterableBuilder(loaded).build());
        asyncApiDao.loadAsyncApiByFilter(filter, asyncQuery.getClass());
        verify(dataStore, times(1)).beginTransaction();
        verify(tx, times(1)).loadObjects(any(), any());
    }
}
