/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.multiplex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.paiondata.elide.core.datastore.DataStore;
import com.paiondata.elide.core.datastore.DataStoreTransaction;
import com.paiondata.elide.core.datastore.inmemory.HashMapDataStore;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.dictionary.Injector;
import com.paiondata.elide.core.exceptions.TransactionException;
import com.paiondata.elide.core.request.EntityProjection;
import com.paiondata.elide.core.type.ClassType;
import com.paiondata.elide.core.utils.ClassScanner;
import com.paiondata.elide.core.utils.DefaultClassScanner;
import com.paiondata.elide.example.beans.ComplexAttribute;
import com.paiondata.elide.example.beans.FirstBean;
import com.paiondata.elide.example.other.OtherBean;
import com.google.common.collect.Lists;
import org.apache.commons.collections4.IterableUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
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
        ClassScanner scanner = new DefaultClassScanner();
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

    /**
     * Tests the case where there is no commit to the hash map data store occurs and
     * a rollback occurs subsequently.
     *
     * It is expected that the update to the FirstBean from the hash map data store
     * to set the name to update does not update the underlying store if there is no
     * commit to the hash map data store.
     *
     * @throws IOException               the exception
     * @throws IllegalArgumentException  the exception
     * @throws InvocationTargetException the exception
     * @throws NoSuchMethodException     the exception
     * @throws SecurityException         the exception
     */
    @Test
    public void partialCommitFailureNoCommit() throws IOException, IllegalArgumentException, InvocationTargetException,
            NoSuchMethodException, SecurityException {
        final EntityDictionary entityDictionary = EntityDictionary.builder().build();
        final HashMapDataStore ds1 = new HashMapDataStore(new DefaultClassScanner(),
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

            FirstBean firstBean = FirstBean.class.getDeclaredConstructor().newInstance();
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
            OtherBean otherBean = OtherBean.class.getDeclaredConstructor().newInstance();
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
            assertEquals(1, list.size());
            assertEquals("name", ((FirstBean) list.get(0)).getName());
        }
    }

    /**
     * Tests the case where the commit to the hash map data store occurs and a
     * rollback occurs subsequently which the multiplex manager will attempt to
     * reverse.
     *
     * @throws IOException               the exception
     * @throws IllegalArgumentException  the exception
     * @throws InvocationTargetException the exception
     * @throws NoSuchMethodException     the exception
     * @throws SecurityException         the exception
     */
    @Test
    public void partialCommitFailureReverseTransactions() throws IOException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, SecurityException {
        final EntityDictionary entityDictionary = EntityDictionary.builder().build();
        final HashMapDataStore ds1 = new HashMapDataStore(new DefaultClassScanner(),
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

            FirstBean firstBean = FirstBean.class.getDeclaredConstructor().newInstance();
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
            OtherBean otherBean = OtherBean.class.getDeclaredConstructor().newInstance();
            t.createObject(otherBean, null);

            FirstBean firstBean = (FirstBean) t.loadObjects(EntityProjection.builder()
                    .type(FirstBean.class)
                    .build(), null).iterator().next();
            firstBean.setName("update");
            //t.save(firstBean, null);
            t.save(firstBean, null);

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
            assertEquals(1, list.size());
            assertEquals("name", ((FirstBean) list.get(0)).getName());
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
