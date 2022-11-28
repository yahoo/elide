/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.multiplex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.datastore.inmemory.HashMapDataStore;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.dictionary.Injector;
import com.yahoo.elide.core.exceptions.TransactionException;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.core.utils.ClassScanner;
import com.yahoo.elide.core.utils.DefaultClassScanner;
import com.yahoo.elide.example.beans.ComplexAttribute;
import com.yahoo.elide.example.beans.FirstBean;
import com.yahoo.elide.example.other.OtherBean;
import com.google.common.collect.Lists;
import org.apache.commons.collections4.IterableUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;

/**
 * MultiplexManager tests.
 */
@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MultiplexManagerTest {
    private MultiplexManager multiplexManager;
    private EntityDictionary entityDictionary;

    @BeforeAll
    public void setup() {
        ClassScanner scanner = DefaultClassScanner.getInstance();
        entityDictionary = EntityDictionary.builder().build();
        final HashMapDataStore inMemoryDataStore1 = new HashMapDataStore(scanner, FirstBean.class.getPackage());
        final HashMapDataStore inMemoryDataStore2 = new HashMapDataStore(scanner, OtherBean.class.getPackage());
        multiplexManager = new MultiplexManager(inMemoryDataStore1, inMemoryDataStore2);
        multiplexManager.populateEntityDictionary(entityDictionary);
    }

    @Test
    public void checkLoading() {
        EntityDictionary entityDictionary = multiplexManager.getDictionary();
        assertNotNull(entityDictionary.getJsonAliasFor(ClassType.of(FirstBean.class)));
        assertNotNull(entityDictionary.getJsonAliasFor(ClassType.of(OtherBean.class)));
        assertNotNull(entityDictionary.getJsonAliasFor(ClassType.of(ComplexAttribute.class)));
    }

    @Test
    public void testValidCommit() throws IOException {
        final FirstBean object = new FirstBean();
        object.setId(null);
        object.setName("Test");
        try (DataStoreTransaction t = multiplexManager.beginTransaction()) {
            assertFalse(t.loadObjects(EntityProjection.builder()
                    .type(FirstBean.class)
                    .build(), null)
                    .iterator().hasNext());
            t.createObject(object, null);
            assertFalse(t.loadObjects(EntityProjection.builder()
                    .type(FirstBean.class)
                    .build(), null)
                    .iterator().hasNext());
            t.commit(null);
        }
        try (DataStoreTransaction t = multiplexManager.beginTransaction()) {
            Iterable<Object> beans = t.loadObjects(EntityProjection.builder()
                            .type(FirstBean.class)
                            .build(), null);
            assertNotNull(beans);
            assertTrue(beans.iterator().hasNext());
            FirstBean bean = (FirstBean) IterableUtils.first(beans);
            assertTrue(bean.getId() != null && "Test".equals(bean.getName()));
        }
    }

    @Test
    public void partialCommitFailure() throws IOException {
        final EntityDictionary entityDictionary = EntityDictionary.builder().build();
        final HashMapDataStore ds1 = new HashMapDataStore(DefaultClassScanner.getInstance(),
                FirstBean.class.getPackage());
        final DataStore ds2 = new TestDataStore(OtherBean.class.getPackage());
        final MultiplexManager multiplexManager = new MultiplexManager(ds1, ds2);
        multiplexManager.populateEntityDictionary(entityDictionary);

        assertEquals(ds1, multiplexManager.getSubManager(ClassType.of(FirstBean.class)));
        assertEquals(ds2, multiplexManager.getSubManager(ClassType.of(OtherBean.class)));

        try (DataStoreTransaction t = ds1.beginTransaction()) {
            assertFalse(t.loadObjects(EntityProjection.builder()
                    .type(FirstBean.class)
                    .build(), null).iterator().hasNext());

            FirstBean firstBean = FirstBean.class.newInstance();
            firstBean.setName("name");
            t.createObject(firstBean, null);
            //t.save(firstBean);
            assertFalse(t.loadObjects(EntityProjection.builder()
                    .type(FirstBean.class)
                    .build(), null).iterator().hasNext());
            t.commit(null);
        } catch (InstantiationException | IllegalAccessException e) {
            log.error("", e);
        }
        try (DataStoreTransaction t = multiplexManager.beginTransaction()) {
            FirstBean firstBean = (FirstBean) t.loadObjects(EntityProjection.builder()
                    .type(FirstBean.class)
                    .build(), null).iterator().next();
            firstBean.setName("update");
            t.save(firstBean, null);
            OtherBean otherBean = OtherBean.class.newInstance();
            t.createObject(otherBean, null);
            //t.save(firstBean);

            assertThrows(TransactionException.class, () -> t.commit(null));
        } catch (InstantiationException | IllegalAccessException e) {
            log.error("", e);
        }
        // verify state
        try (DataStoreTransaction t = ds1.beginTransaction()) {
            Iterable<Object> beans = t.loadObjects(EntityProjection.builder()
                    .type(FirstBean.class)
                    .build(), null);
            assertNotNull(beans);
            ArrayList<Object> list = Lists.newArrayList(beans.iterator());
            assertEquals(list.size(), 1);
            assertEquals(((FirstBean) list.get(0)).getName(), "name");
        }
    }

    @Test
    public void subordinateEntityDictionaryInheritsInjector() {
        final Injector injector = entity -> {
                    //NOOP
            };
        final QueryDictionaryDataStore ds1 = new QueryDictionaryDataStore();
        final MultiplexManager multiplexManager = new MultiplexManager(ds1);
        multiplexManager.populateEntityDictionary(
                EntityDictionary.builder().injector(injector).build()
        );
        assertEquals(
            ds1.getDictionary().getInjector(),
            injector
        );
    }

    private static class QueryDictionaryDataStore implements DataStore {
        private EntityDictionary dictionary;

        @Override
        public void populateEntityDictionary(final EntityDictionary dictionary) {
            this.dictionary = dictionary;
        }

        @Override
        public DataStoreTransaction beginTransaction() {
            throw new UnsupportedOperationException();
        }

        public EntityDictionary getDictionary() {
            return this.dictionary;
        }

    }
}
