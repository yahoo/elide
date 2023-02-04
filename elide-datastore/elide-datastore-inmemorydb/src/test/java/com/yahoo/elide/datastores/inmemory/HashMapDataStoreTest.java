/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.inmemory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.core.utils.DefaultClassScanner;
import com.yahoo.elide.example.beans.ExcludedBean;
import com.yahoo.elide.example.beans.FirstBean;
import com.yahoo.elide.example.beans.FirstChildBean;
import com.yahoo.elide.example.beans.NonEntity;
import com.yahoo.elide.example.beans.SecondBean;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.collections4.IterableUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * HashMapDataStore tests.
 */
public class HashMapDataStoreTest {
    private InMemoryDataStore inMemoryDataStore;
    private EntityDictionary entityDictionary;

    @BeforeEach
    public void setup() {
        entityDictionary = EntityDictionary.builder().build();
        inMemoryDataStore = new InMemoryDataStore(DefaultClassScanner.getInstance(), FirstBean.class.getPackage());
        inMemoryDataStore.populateEntityDictionary(entityDictionary);
    }

    private <T extends Object> T createNewInheritanceObject(Class<T> type)
            throws IOException, InstantiationException, IllegalAccessException {
        T obj = type.newInstance();
        try (DataStoreTransaction t = inMemoryDataStore.beginTransaction()) {
            t.createObject(obj, null);
            t.commit(null);
        }
        return obj;
    }

    @Test
    public void dataStoreTestInheritance() throws IOException, InstantiationException, IllegalAccessException {
        Map<String, Object> entry = inMemoryDataStore.get(ClassType.of(FirstBean.class));
        assertEquals(0, entry.size());

        FirstChildBean child = createNewInheritanceObject(FirstChildBean.class);

        // Adding Child object, adds a parent entry.
        try (DataStoreTransaction t = inMemoryDataStore.beginTransaction()) {
            Iterable<Object> beans = t.loadObjects(EntityProjection.builder()
                    .type(FirstBean.class)
                    .build(), null);
            assertNotNull(beans);
            assertTrue(beans.iterator().hasNext());
            FirstBean bean = (FirstBean) IterableUtils.first(beans);
            assertEquals("1", bean.getId());
        }

        assertEquals("1", child.getId());
        assertNotNull(entry);
        assertEquals(1, entry.size());

        // New Parent avoids id collision.
        FirstBean parent = createNewInheritanceObject(FirstBean.class);
        assertEquals("2", parent.getId());

        // New Child avoids id collision
        FirstChildBean child1 = createNewInheritanceObject(FirstChildBean.class);
        assertEquals("3", child1.getId());
    }

    @Test
    public void dataStoreTestInheritanceDelete() throws IOException, InstantiationException, IllegalAccessException {
        Map<String, Object> entry = inMemoryDataStore.get(ClassType.of(FirstBean.class));
        assertEquals(0, entry.size());

        FirstChildBean child = createNewInheritanceObject(FirstChildBean.class);
        createNewInheritanceObject(FirstBean.class);

        // Delete Child
        try (DataStoreTransaction t = inMemoryDataStore.beginTransaction()) {
            t.delete(child, null);
            t.commit(null);
        }

        // Only 1 parent entry should remain.
        try (DataStoreTransaction t = inMemoryDataStore.beginTransaction()) {
            Iterable<Object> beans = t.loadObjects(EntityProjection.builder()
                    .type(FirstBean.class)
                    .build(), null);
            assertNotNull(beans);
            assertTrue(beans.iterator().hasNext());
            FirstBean bean = (FirstBean) IterableUtils.first(beans);
            assertEquals("2", bean.getId());
        }
    }

    @Test
    public void dataStoreTestInheritanceUpdate() throws IOException, InstantiationException, IllegalAccessException {
        Map<String, Object> entry = inMemoryDataStore.get(ClassType.of(FirstBean.class));
        assertEquals(0, entry.size());

        FirstChildBean child = createNewInheritanceObject(FirstChildBean.class);
        createNewInheritanceObject(FirstBean.class);

        // update Child
        try (DataStoreTransaction t = inMemoryDataStore.beginTransaction()) {
            child.setNickname("hello");
            t.save(child, null);
            t.commit(null);
        }

        // Only 1 parent entry should remain.
        try (DataStoreTransaction t = inMemoryDataStore.beginTransaction()) {
            Iterable<Object> beans = t.loadObjects(EntityProjection.builder()
                    .type(FirstBean.class)
                    .build(), null);
            assertNotNull(beans);
            assertTrue(beans.iterator().hasNext());
            FirstChildBean bean = (FirstChildBean) IterableUtils.first(beans);
            assertEquals("1", bean.getId());
            assertEquals("hello", bean.getNickname());
        }
    }

    @Test
    public void checkLoading() {
        final EntityDictionary entityDictionary = inMemoryDataStore.getDictionary();
        assertNotNull(entityDictionary.getJsonAliasFor(ClassType.of(FirstBean.class)));
        assertNotNull(entityDictionary.getJsonAliasFor(ClassType.of(SecondBean.class)));
        assertThrows(IllegalArgumentException.class, () -> entityDictionary.getJsonAliasFor(ClassType.of(NonEntity.class)));
        assertThrows(IllegalArgumentException.class, () -> entityDictionary.getJsonAliasFor(ClassType.of(ExcludedBean.class)));
    }

    @Test
    public void testValidCommit() throws Exception {
        FirstBean object = new FirstBean();
        object.id = "0";
        object.name = "Test";
        try (DataStoreTransaction t = inMemoryDataStore.beginTransaction()) {
            assertFalse(t.loadObjects(EntityProjection.builder()
                    .type(FirstBean.class)
                    .build(), null).iterator().hasNext());
            t.createObject(object, null);
            assertFalse(t.loadObjects(EntityProjection.builder()
                    .type(FirstBean.class)
                    .build(), null).iterator().hasNext());
            t.commit(null);
        }
        try (DataStoreTransaction t = inMemoryDataStore.beginTransaction()) {
            Iterable<Object> beans = t.loadObjects(EntityProjection.builder()
                    .type(FirstBean.class)
                    .build(), null);
            assertNotNull(beans);
            assertTrue(beans.iterator().hasNext());
            FirstBean bean = (FirstBean) IterableUtils.first(beans);
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
            for (Object objBean : t.loadObjects(EntityProjection.builder()
                    .type(FirstBean.class)
                    .build(), null)) {
                FirstBean bean = (FirstBean) objBean;
                names.add(bean.name);
                assertFalse(bean.id == null);
            }
        }

        assertEquals(ImmutableSet.of("number one", "number two"), names);
    }
}
