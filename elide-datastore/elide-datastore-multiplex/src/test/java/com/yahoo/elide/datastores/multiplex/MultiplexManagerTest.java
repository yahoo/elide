/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.multiplex;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.exceptions.TransactionException;
import com.yahoo.elide.datastores.inmemory.InMemoryDataStore;
import com.yahoo.elide.example.beans.FirstBean;
import com.yahoo.elide.example.other.OtherBean;

import com.google.common.collect.Lists;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

/**
 * MultiplexManager tests.
 */
@Slf4j
public class MultiplexManagerTest {
    private MultiplexManager multiplexManager;

    @BeforeTest
    public void setup() {
        final EntityDictionary entityDictionary = new EntityDictionary(new HashMap<>());
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
        object.id = null;
        object.name = "Test";
        try (DataStoreTransaction t = multiplexManager.beginTransaction()) {
            assertFalse(t.loadObjects(FirstBean.class, Optional.empty(), Optional.empty(), Optional.empty(), null)
                    .iterator().hasNext());
            t.createObject(object, null);
            assertFalse(t.loadObjects(FirstBean.class, Optional.empty(), Optional.empty(), Optional.empty(), null)
                    .iterator().hasNext());
            t.commit(null);
        }
        try (DataStoreTransaction t = multiplexManager.beginTransaction()) {
            Iterable<Object> beans = t.loadObjects(FirstBean.class, Optional.empty(), Optional.empty(), Optional.empty(), null);
            assertNotNull(beans);
            assertTrue(beans.iterator().hasNext());
            FirstBean bean = (FirstBean) beans.iterator().next();
            assertTrue(bean.id != null && "Test".equals(bean.name));
        }
    }

    @Test(priority = 3)
    public void partialCommitFailure() throws IOException {
        final EntityDictionary entityDictionary = new EntityDictionary(new HashMap<>());
        final InMemoryDataStore ds1 = new InMemoryDataStore(FirstBean.class.getPackage());
        final DataStore ds2 = new TestDataStore(OtherBean.class.getPackage());
        final MultiplexManager multiplexManager = new MultiplexManager(ds1, ds2);
        multiplexManager.populateEntityDictionary(entityDictionary);

        assertEquals(multiplexManager.getSubManager(FirstBean.class), ds1);
        assertEquals(multiplexManager.getSubManager(OtherBean.class), ds2);

        try (DataStoreTransaction t = ds1.beginTransaction()) {
            assertFalse(t.loadObjects(FirstBean.class, Optional.empty(), Optional.empty(), Optional.empty(), null).iterator().hasNext());

            FirstBean firstBean = FirstBean.class.newInstance();
            firstBean.name = "name";
            t.createObject(firstBean, null);
            //t.save(firstBean);
            assertFalse(t.loadObjects(FirstBean.class, Optional.empty(), Optional.empty(), Optional.empty(), null).iterator().hasNext());
            t.commit(null);
        } catch (InstantiationException | IllegalAccessException e) {
            log.error("", e);
        }
        try (DataStoreTransaction t = multiplexManager.beginTransaction()) {
            FirstBean firstBean = (FirstBean) t.loadObjects(FirstBean.class, Optional.empty(), Optional.empty(), Optional.empty(), null).iterator().next();
            firstBean.name = "update";
            t.save(firstBean, null);
            OtherBean otherBean = OtherBean.class.newInstance();
            t.createObject(otherBean, null);
            //t.save(firstBean);
            try {
                t.commit(null);
                fail("TransactionException expected");
            } catch (TransactionException expected) {
                // expected
            }
        } catch (InstantiationException | IllegalAccessException e) {
            log.error("", e);
        }
        // verify state
        try (DataStoreTransaction t = ds1.beginTransaction()) {
            Iterable<Object> beans = t.loadObjects(FirstBean.class, Optional.empty(), Optional.empty(), Optional.empty(), null);
            assertNotNull(beans);
            ArrayList<Object> list = Lists.newArrayList(beans.iterator());
            assertEquals(list.size(), 1);
            assertEquals(((FirstBean) list.get(0)).name, "name");
        }
    }
}
