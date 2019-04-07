/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.inmemory;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.example.beans.ExcludedBean;
import com.yahoo.elide.example.beans.FirstBean;
import com.yahoo.elide.example.beans.NonEntity;
import com.yahoo.elide.example.beans.SecondBean;

import com.google.common.collect.ImmutableSet;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * HashMapDataStore tests.
 */
public class HashMapDataStoreTest {
    private InMemoryDataStore inMemoryDataStore;

    @BeforeMethod
    public void setup() {
        final EntityDictionary entityDictionary = new EntityDictionary(new HashMap<>());
        inMemoryDataStore = new InMemoryDataStore(FirstBean.class.getPackage());
        inMemoryDataStore.populateEntityDictionary(entityDictionary);
    }

    @Test(expectedExceptions = {IllegalArgumentException.class})
    public void checkLoading() {
        final EntityDictionary entityDictionary = inMemoryDataStore.getDictionary();
        assertNotNull(entityDictionary.getJsonAliasFor(FirstBean.class));
        assertNotNull(entityDictionary.getJsonAliasFor(SecondBean.class));
        assertNull(entityDictionary.getJsonAliasFor(ExcludedBean.class));
        assertNull(entityDictionary.getJsonAliasFor(NonEntity.class));
    }

    @Test
    public void testValidCommit() throws Exception {
        FirstBean object = new FirstBean();
        object.id = "0";
        object.name = "Test";
        try (DataStoreTransaction t = inMemoryDataStore.beginTransaction()) {
            assertFalse(t.loadObjects(FirstBean.class, Optional.empty(), Optional.empty(), Optional.empty(), null).iterator().hasNext());
            t.createObject(object, null);
            assertFalse(t.loadObjects(FirstBean.class, Optional.empty(), Optional.empty(), Optional.empty(), null).iterator().hasNext());
            t.commit(null);
        }
        try (DataStoreTransaction t = inMemoryDataStore.beginTransaction()) {
            Iterable<Object> beans = t.loadObjects(FirstBean.class, Optional.empty(), Optional.empty(), Optional.empty(), null);
            assertNotNull(beans);
            assertTrue(beans.iterator().hasNext());
            FirstBean bean = (FirstBean) beans.iterator().next();
            assertTrue(!"0".equals(bean.id) && "Test".equals(bean.name));
        }
    }

    @Test
    public void testCanGenerateIdsAfterDataCommitted() throws Exception {
        // given an object with a non-generated ID has been created
        FirstBean object = new FirstBean();
        object.id = "1";
        object.name = "number one";

        try (DataStoreTransaction t = inMemoryDataStore.beginTransaction()) {
            t.createObject(object, null);
            t.save(object, null);
            t.commit(null);
        }

        // when an object without ID is created, that works
        FirstBean object2 = new FirstBean();
        object2.id = null;
        object2.name = "number two";

        try (DataStoreTransaction t = inMemoryDataStore.beginTransaction()) {
            t.createObject(object2, null);
            t.save(object2, null);
            t.commit(null);
        }

        // and a meaningful ID is assigned
        Set<String> names = new HashSet<>();
        try (DataStoreTransaction t = inMemoryDataStore.beginTransaction()) {
            for (Object objBean : t.loadObjects(FirstBean.class,
                                                Optional.empty(), Optional.empty(), Optional.empty(), null)) {
                FirstBean bean = (FirstBean) objBean;
                names.add(bean.name);
                assertFalse(bean.id == null);
            }
        }

        assertEquals(names, ImmutableSet.of("number one", "number two"));
    }
}
