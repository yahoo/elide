/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.noop;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import com.yahoo.elide.beans.NoopBean;
import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;

import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashMap;

public class NoopDataStoreTest {

    @Test
    public void testPopulateEntityDictionary() throws Exception {
        DataStore store = new NoopDataStore(Arrays.asList(NoopBean.class));
        EntityDictionary dictionary = new EntityDictionary(new HashMap<>());
        store.populateEntityDictionary(dictionary);
        assertEquals(dictionary.getEntityClass("theNoopBean"), NoopBean.class);
    }

    @Test
    public void testBeginTransaction() throws Exception {
        DataStore store = new NoopDataStore(Arrays.asList(NoopBean.class));
        DataStoreTransaction tx = store.beginReadTransaction();
        assertTrue(tx instanceof NoopTransaction);
    }
}
