/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.noop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.paiondata.elide.beans.NoopBean;
import com.paiondata.elide.core.datastore.DataStore;
import com.paiondata.elide.core.datastore.DataStoreTransaction;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.type.ClassType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class NoopDataStoreTest {

    @Test
    public void testPopulateEntityDictionary() throws Exception {
        DataStore store = new NoopDataStore(Arrays.asList(NoopBean.class));
        EntityDictionary dictionary = EntityDictionary.builder().build();
        store.populateEntityDictionary(dictionary);
        assertEquals(ClassType.of(NoopBean.class), dictionary.getEntityClass("theNoopBean", EntityDictionary.NO_VERSION));
    }

    @Test
    public void testBeginTransaction() throws Exception {
        DataStore store = new NoopDataStore(Arrays.asList(NoopBean.class));
        DataStoreTransaction tx = store.beginReadTransaction();
        assertTrue(tx instanceof NoopTransaction);
    }
}
