/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.multiplex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import com.yahoo.elide.Injector;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.exceptions.TransactionException;
import com.yahoo.elide.datastores.inmemory.InMemoryDataStore;
import com.yahoo.elide.example.beans.FirstBean;
import com.yahoo.elide.example.other.OtherBean;
import com.yahoo.elide.request.EntityProjection;
import com.google.common.collect.Lists;
import org.apache.commons.collections4.IterableUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

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
        entityDictionary = new EntityDictionary(new HashMap<>());
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
            assertTrue(bean.id != null && "Test".equals(bean.name));
        }
    }

    @Test
    public void partialCommitFailure() throws IOException {
        final EntityDictionary entityDictionary = new EntityDictionary(new HashMap<>());
        final InMemoryDataStore ds1 = new InMemoryDataStore(FirstBean.class.getPackage());
        final DataStore ds2 = new TestDataStore(OtherBean.class.getPackage());
        final MultiplexManager multiplexManager = new MultiplexManager(ds1, ds2);
        multiplexManager.populateEntityDictionary(entityDictionary);

        assertEquals(ds1, multiplexManager.getSubManager(FirstBean.class));
        assertEquals(ds2, multiplexManager.getSubManager(OtherBean.class));

        try (DataStoreTransaction t = ds1.beginTransaction()) {
            assertFalse(t.loadObjects(EntityProjection.builder()
                    .type(FirstBean.class)
                    .build(), null).iterator().hasNext());

            FirstBean firstBean = FirstBean.class.newInstance();
            firstBean.name = "name";
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
            Iterable<Object> beans = t.loadObjects(EntityProjection.builder()
                    .type(FirstBean.class)
                    .build(), null);
            assertNotNull(beans);
            ArrayList<Object> list = Lists.newArrayList(beans.iterator());
            assertEquals(list.size(), 1);
            assertEquals(((FirstBean) list.get(0)).name, "name");
        }
    }

    @Test
    public void subordinateEntityDictionaryInheritsInjector() {
        final Injector injector =
             new Injector() {
                @Override
                public void inject(Object entity) {
                    throw new UnsupportedOperationException();
                }
            };
        final QueryDictionaryDataStore ds1 = new QueryDictionaryDataStore();
        final MultiplexManager multiplexManager = new MultiplexManager(ds1);
        multiplexManager.populateEntityDictionary(
            new EntityDictionary(
                new HashMap<>(),
                injector
            )
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
