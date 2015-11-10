/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.dbmanager.InMemory;

import com.yahoo.elide.core.DatabaseTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.example.beans.ExcludedBean;
import com.yahoo.elide.example.beans.FirstBean;
import com.yahoo.elide.example.beans.NonEntity;
import com.yahoo.elide.example.beans.SecondBean;
import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * InMemoryDB tests.
 */
public class InMemoryDBTest {

    @Test(expectedExceptions = {IllegalArgumentException.class})
    public void checkLoading() {
        InMemoryDB db = dbInstance();
        EntityDictionary ed = db.getDictionary();
        assertNotNull(ed.getBinding(FirstBean.class));
        assertNotNull(ed.getBinding(SecondBean.class));
        assertNull(ed.getBinding(ExcludedBean.class));
        assertNull(ed.getBinding(NonEntity.class));
    }

    @Test
    public void testValidCommit() throws Exception {
        InMemoryDB db = dbInstance();
        FirstBean object = new FirstBean();
        object.id = 0;
        object.name = "Test";
        try (DatabaseTransaction t = db.beginTransaction()) {
            assertFalse(t.loadObjects(FirstBean.class).iterator().hasNext());
            t.save(object);
            assertFalse(t.loadObjects(FirstBean.class).iterator().hasNext());
            t.commit();
        }
        try (DatabaseTransaction t = db.beginTransaction()) {
            Iterable<FirstBean> beans = t.loadObjects(FirstBean.class);
            assertNotNull(beans);
            assertTrue(beans.iterator().hasNext());
            FirstBean bean = beans.iterator().next();
            assertTrue(bean.id == 1 && bean.name.equals("Test"));
        }
    }

    private static InMemoryDB dbInstance() {
        EntityDictionary ed = new EntityDictionary();
        InMemoryDB db = new InMemoryDB(FirstBean.class.getPackage());
        db.populateEntityDictionary(ed);
        return db;
    }
}
