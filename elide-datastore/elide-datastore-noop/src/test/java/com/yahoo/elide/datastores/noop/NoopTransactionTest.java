/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.noop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yahoo.elide.beans.NoopBean;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.ObjectEntityCache;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.jsonapi.JsonApiMapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.IterableUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.HashMap;
import java.util.Optional;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class NoopTransactionTest {
    DataStoreTransaction tx = new NoopTransaction();
    NoopBean bean = new NoopBean();
    RequestScope requestScope;

    @BeforeAll
    public void setup() {
        EntityDictionary dictionary = new EntityDictionary(new HashMap<>());
        dictionary.bindEntity(NoopBean.class);
        requestScope = mock(RequestScope.class);
        JsonApiMapper mapper = mock(JsonApiMapper.class);


        when(requestScope.getDictionary()).thenReturn(dictionary);
        when(requestScope.getObjectEntityCache()).thenReturn(new ObjectEntityCache());
        when(requestScope.getMapper()).thenReturn(mapper);
        when(mapper.getObjectMapper()).thenReturn(new ObjectMapper());
    }

    @Test
    public void testSave() throws Exception {
        // Should do nothing. No backing store, so should succeed
        tx.save(bean, null);
    }

    @Test
    public void testDelete() throws Exception {
        // Should do nothing. No backing store, so should succeed
        tx.delete(bean, null);
    }

    @Test
    public void testFlush() throws Exception {
        // Should do nothing. No backing store, so should succeed
        tx.flush(null);
    }

    @Test
    public void testCommit() throws Exception {
        // Should do nothing. No backing store, so should succeed
        tx.commit(null);
    }

    @Test
    public void testCreateObject() throws Exception {
        // Should do nothing. No backing store, so should succeed
        tx.createObject(bean, null);
    }

    @Test
    public void testLoadObject() throws Exception {

        // Should return bean with id set
        NoopBean bean = (NoopBean) tx.loadObject(NoopBean.class, 1, Optional.empty(), requestScope);
        assertEquals((Long) 1L, bean.getId());
    }

    @Test
    public void testLoadObjects() throws Exception {
        Iterable<NoopBean> iterable = (Iterable) tx.loadObjects(NoopBean.class, Optional.empty(), Optional.empty(), Optional.empty(), requestScope);
        NoopBean bean = IterableUtils.first(iterable);
        assertEquals((Long) 1L, bean.getId());
    }

    @Test
    public void testClose() throws Exception {
        tx.close();
    }
}
