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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.AsyncQueryResult;
import com.yahoo.elide.async.models.QueryStatus;
import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;

public class DefaultAsyncQueryDAOTest {

    private DefaultAsyncQueryDAO asyncQueryDAO;
    private Elide elide;
    private DataStore dataStore;
    private AsyncQuery asyncQuery;
    private DataStoreTransaction tx;
    private ElideSettings elideSettings;
    private EntityDictionary dictionary;
    private RSQLFilterDialect filterParser;

    @BeforeEach
    public void setupMocks() {
        elide = mock(Elide.class);
        dataStore = mock(DataStore.class);
        asyncQuery = mock(AsyncQuery.class);
        tx = mock(DataStoreTransaction.class);
        elideSettings = mock(ElideSettings.class);
        dictionary = mock(EntityDictionary.class);
        filterParser = mock(RSQLFilterDialect.class);

        when(dataStore.beginTransaction()).thenReturn(tx);
        when(elide.getElideSettings()).thenReturn(elideSettings);
        when(elideSettings.getDictionary()).thenReturn(dictionary);

        asyncQueryDAO = new DefaultAsyncQueryDAO(elide, dataStore);

        asyncQueryDAO.setDictionary(dictionary);
        asyncQueryDAO.setFilterParser(filterParser);
    }

    @Test
    public void testAsyncQueryCleanerThreadSet() {
        assertEquals(elide, asyncQueryDAO.getElide());
        assertEquals(dataStore, asyncQueryDAO.getDataStore());
        assertEquals(dictionary, asyncQueryDAO.getDictionary());
        assertEquals(filterParser, asyncQueryDAO.getFilterParser());
    }

    @Test
    public void testUpdateStatus() {
        AsyncQuery result = asyncQueryDAO.updateStatus(asyncQuery, QueryStatus.PROCESSING);

        assertEquals(result, asyncQuery);
        verify(tx, times(1)).save(any(AsyncQuery.class), any(RequestScope.class));
        verify(asyncQuery, times(1)).setStatus(QueryStatus.PROCESSING);
    }

    @Test
    public void testUpdateStatusAsyncQueryCollection() {
        Collection<AsyncQuery> asyncQueryList = new ArrayList<AsyncQuery>();
        asyncQueryList.add(asyncQuery);
        asyncQueryList.add(asyncQuery);

        Collection<AsyncQuery> result = asyncQueryDAO.updateStatusAsyncQueryCollection(asyncQueryList, QueryStatus.TIMEDOUT);

        assertEquals(result, asyncQueryList);
        verify(tx, times(2)).save(any(AsyncQuery.class), any(RequestScope.class));
        verify(asyncQuery, times(2)).setStatus(QueryStatus.TIMEDOUT);
    }

    @Test
    public void testDeleteAsyncQueryAndResultCollection() {
        Iterable<Object> loaded = Arrays.asList(asyncQuery, asyncQuery, asyncQuery);
        when(tx.loadObjects(any(), any())).thenReturn(loaded);

        asyncQueryDAO.deleteAsyncQueryAndResultCollection("createdOn=le='2020-03-23T02:02Z'");

        verify(tx, times(3)).delete(any(AsyncQuery.class), any(RequestScope.class));
    }

    @Test
    public void testLoadQueries() {
        asyncQueryDAO.loadQueries("createdOn=le='2020-03-23T02:02Z'");

        verify(tx, times(1)).loadObjects(any(), any(RequestScope.class));
    }

    @Test
    public void testCreateAsyncQueryResult() {
        Integer status = 200;
        String responseBody = "responseBody";
        UUID uuid = UUID.fromString("ba31ca4e-ed8f-4be0-a0f3-12088fa9263e");
        AsyncQueryResult result = asyncQueryDAO.createAsyncQueryResult(status, "responseBody", asyncQuery, uuid);

        assertEquals(status,result.getStatus());
        assertEquals(responseBody,result.getResponseBody());
        assertEquals(asyncQuery,result.getQuery());
        assertEquals(uuid,result.getId());
        verify(tx, times(1)).createObject(any(), any(RequestScope.class));
        verify(tx, times(1)).save(any(), any(RequestScope.class));

    }

}
