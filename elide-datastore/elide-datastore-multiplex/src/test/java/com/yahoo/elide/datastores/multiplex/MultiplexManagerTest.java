/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.multiplex;

import com.google.common.collect.Lists;
import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.exceptions.TransactionException;
import com.yahoo.elide.datastores.inmemory.InMemoryDataStore;
import com.yahoo.elide.example.beans.FirstBean;
import com.yahoo.elide.example.other.OtherBean;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * MultiplexManager tests.
 */
public class MultiplexManagerTest {
    private MultiplexManager multiplexManager;

    @BeforeTest
    public void setup() {
        final EntityDictionary entityDictionary = new EntityDictionary();
        final InMemoryDataStore inMemoryDataStore1 = new InMemoryDataStore(FirstBean.class.getPackage());
        final InMemoryDataStore inMemoryDataStore2 = new InMemoryDataStore(OtherBean.class.getPackage());
        multiplexManager = new MultiplexManager(inMemoryDataStore1, inMemoryDataStore2);
        multiplexManager.populateEntityDictionary(entityDictionary);
    }

    @Test
    public void checkLoading() {
        EntityDictionary entityDictionary = multiplexManager.getDictionary();
        assertNotNull(entityDictionary.getJsonAliasFor(FirstBean.class));
        assertNotNull(entityDictionary.getJsonAliasFor(OtherBean.class));
    }

    @Test
    public void testValidCommit() throws IOException {
        final FirstBean object = new FirstBean();
        object.id = 0;
        object.name = "Test";
        try (DataStoreTransaction t = multiplexManager.beginTransaction()) {
            assertFalse(t.loadObjects(FirstBean.class).iterator().hasNext());
            t.save(object);
            assertFalse(t.loadObjects(FirstBean.class).iterator().hasNext());
            t.commit();
        }
        try (DataStoreTransaction t = multiplexManager.beginTransaction()) {
            Iterable<FirstBean> beans = t.loadObjects(FirstBean.class);
            assertNotNull(beans);
            assertTrue(beans.iterator().hasNext());
            FirstBean bean = beans.iterator().next();
            assertTrue(bean.id == 1 && bean.name.equals("Test"));
        }
    }

    @Test(priority = 3)
    public void partialCommitFailure() throws IOException {
        final EntityDictionary entityDictionary = new EntityDictionary();
        final InMemoryDataStore ds1 = new InMemoryDataStore(FirstBean.class.getPackage());
        final DataStore ds2 = new TestDataStore(OtherBean.class.getPackage());
        final MultiplexManager multiplexManager = new MultiplexManager(ds1, ds2);
        multiplexManager.populateEntityDictionary(entityDictionary);

        assertEquals(multiplexManager.getSubManager(FirstBean.class), ds1);
        assertEquals(multiplexManager.getSubManager(OtherBean.class), ds2);

        try (DataStoreTransaction t = ds1.beginTransaction()) {
            assertFalse(t.loadObjects(FirstBean.class).iterator().hasNext());
            FirstBean firstBean = t.createObject(FirstBean.class);
            firstBean.name = "name";
            t.save(firstBean);
            assertFalse(t.loadObjects(FirstBean.class).iterator().hasNext());
            t.commit();
        }
        try (DataStoreTransaction t = multiplexManager.beginTransaction()) {
            FirstBean firstBean = t.loadObjects(FirstBean.class).iterator().next();
            firstBean.name = "update";
            t.save(firstBean);
            OtherBean otherBean = t.createObject(OtherBean.class);
            t.save(otherBean);
            try {
                t.commit();
                fail("TransactionException expected");
            } catch (TransactionException expected) {
                // expected
            }
        }
        // verify state
        try (DataStoreTransaction t = ds1.beginTransaction()) {
            Iterable<FirstBean> beans = t.loadObjects(FirstBean.class);
            assertNotNull(beans);
            ArrayList<FirstBean> list = Lists.newArrayList(beans.iterator());
            assertEquals(list.size(), 1);
            assertEquals(list.get(0).name, "name");
        }
    }
}
