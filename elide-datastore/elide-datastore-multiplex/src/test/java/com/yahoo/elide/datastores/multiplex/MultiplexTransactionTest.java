/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.multiplex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.datastore.inmemory.HashMapDataStore;
import com.yahoo.elide.core.datastore.inmemory.HashMapStoreTransaction;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Tests MultiplexTransaction.
 */
public class MultiplexTransactionTest {
    @Test
    public void testPrecommit() throws Exception {
        DataStore store1 =  mock(DataStore.class);
        DataStore store2 =  mock(DataStore.class);
        DataStoreTransaction tx1 = mock(DataStoreTransaction.class);
        DataStoreTransaction tx2 = mock(DataStoreTransaction.class);
        RequestScope scope = mock(RequestScope.class);

        when(store1.beginReadTransaction()).thenReturn(tx1);
        when(store2.beginReadTransaction()).thenReturn(tx2);

        MultiplexManager store = new MultiplexManager(store1, store2);

        DataStoreTransaction multiplexTx = store.beginReadTransaction();

        multiplexTx.preCommit(scope);

        // Since transactions are lazy initialized,
        // preCommit will not be called on individual transactions.
        verify(tx1, Mockito.times(0)).preCommit(any());
        verify(tx2, Mockito.times(0)).preCommit(any());

        MultiplexReadTransaction multiplexReadTx = (MultiplexReadTransaction) multiplexTx;

        long countInitializedTransaction = multiplexReadTx.transactions.values().stream()
                .filter(dataStoreTransaction -> dataStoreTransaction != null)
                .count();

        assertEquals(0, countInitializedTransaction);

    }

    @Test
    public void testGetProperty() throws Exception {
        DataStore store1 =  mock(HashMapDataStore.class);
        DataStore store2 =  mock(DataStore.class);
        DataStoreTransaction tx1 = mock(HashMapStoreTransaction.class);
        DataStoreTransaction tx2 = mock(DataStoreTransaction.class);

        when(store1.beginReadTransaction()).thenReturn(tx1);
        when(store2.beginReadTransaction()).thenReturn(tx2);
        when(tx1.getProperty(any())).thenReturn(null);
        when(tx2.getProperty(any())).thenReturn("Foo");

        MultiplexManager store = new MultiplexManager(store1, store2);
        DataStoreTransaction multiplexTx = store.beginReadTransaction();
        String propertyName = "com.yahoo.elide.core.datastore.Bar";

        String result = multiplexTx.getProperty(propertyName);

        verify(tx1, Mockito.never()).getProperty(eq(propertyName));
        verify(tx2, Mockito.times(1)).getProperty(eq(propertyName));

        assertEquals("Foo", result);
    }
}
