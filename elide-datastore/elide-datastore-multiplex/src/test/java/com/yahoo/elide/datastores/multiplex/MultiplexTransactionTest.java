/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.multiplex;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.DataStoreTransaction;

import org.testng.annotations.Test;

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

        when(store1.beginReadTransaction()).thenReturn(tx1);
        when(store2.beginReadTransaction()).thenReturn(tx2);

        MultiplexManager store = new MultiplexManager(store1, store2);

        DataStoreTransaction multiplexTx = store.beginReadTransaction();

        multiplexTx.preCommit();

        verify(tx1).preCommit();
        verify(tx2).preCommit();
    }
}
