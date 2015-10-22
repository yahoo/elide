/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastore.multiplex;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.exceptions.TransactionException;
import com.yahoo.elide.datastore.InMemory.InMemoryDB;
import com.yahoo.elide.example.beans.FirstBean;
import com.yahoo.elide.example.other.OtherBean;

import com.google.common.collect.Lists;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;

/**
 * MultiplexManager tests.
 */
public class MultiplexManagerTest {

    @Test
    public void checkLoading() {
        MultiplexManager db = dbInstance();
        EntityDictionary ed = db.getDictionary();
        assertNotNull(ed.getBinding(FirstBean.class));
        assertNotNull(ed.getBinding(OtherBean.class));
    }

    @Test
    public void testValidCommit() throws IOException {
        MultiplexManager db = dbInstance();
        FirstBean object = new FirstBean();
        object.id = 0;
        object.name = "Test";
        try (DataStoreTransaction t = db.beginTransaction()) {
            assertFalse(t.loadObjects(FirstBean.class).iterator().hasNext());
            t.save(object);
            assertFalse(t.loadObjects(FirstBean.class).iterator().hasNext());
            t.commit();
        }
        try (DataStoreTransaction t = db.beginTransaction()) {
            Iterable<FirstBean> beans = t.loadObjects(FirstBean.class);
            assertNotNull(beans);
            assertTrue(beans.iterator().hasNext());
            FirstBean bean = beans.iterator().next();
            assertTrue(bean.id == 1 && bean.name.equals("Test"));
        }
    }

    @Test(priority = 3)
    public void partialCommitFailure() throws IOException {
        EntityDictionary ed = new EntityDictionary();
        InMemoryDB db1 = new InMemoryDB(FirstBean.class.getPackage());
        DataStore db2 = new TestDataStore(OtherBean.class.getPackage());
        MultiplexManager db = new MultiplexManager(db1, db2);
        db.populateEntityDictionary(ed);

        assertEquals(db.getSubManager(FirstBean.class), db1);
        assertEquals(db.getSubManager(OtherBean.class), db2);

        try (DataStoreTransaction t = db1.beginTransaction()) {
            assertFalse(t.loadObjects(FirstBean.class).iterator().hasNext());
            FirstBean firstBean = t.createObject(FirstBean.class);
            firstBean.name = "name";
            t.save(firstBean);
            assertFalse(t.loadObjects(FirstBean.class).iterator().hasNext());
            t.commit();
        }
        try (DataStoreTransaction t = db.beginTransaction()) {
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
        try (DataStoreTransaction t = db1.beginTransaction()) {
            Iterable<FirstBean> beans = t.loadObjects(FirstBean.class);
            assertNotNull(beans);
            ArrayList<FirstBean> list = Lists.newArrayList(beans.iterator());
            assertEquals(list.size(), 1);
            assertEquals(list.get(0).name, "name");
        }
    }

    private static MultiplexManager dbInstance() {
        EntityDictionary ed = new EntityDictionary();
        InMemoryDB db1 = new InMemoryDB(FirstBean.class.getPackage());
        InMemoryDB db2 = new InMemoryDB(OtherBean.class.getPackage());
        MultiplexManager db = new MultiplexManager(db1, db2);
        db.populateEntityDictionary(ed);
        return db;
    }
}
