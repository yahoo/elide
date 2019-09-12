/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.noop;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import com.yahoo.elide.beans.NoopBean;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.ObjectEntityCache;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.jsonapi.JsonApiMapper;
import com.yahoo.elide.request.EntityProjection;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;

public class NoopTransactionTest {
    private DataStoreTransaction tx = new NoopTransaction();
    private NoopBean bean = new NoopBean();
    private RequestScope requestScope;
    private EntityDictionary dictionary;

    @BeforeClass
    public void setup() {
        dictionary = new EntityDictionary(new HashMap<>());
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
        NoopBean bean = (NoopBean) tx.loadObject(EntityProjection.builder()
                .type(NoopBean.class)
                .dictionary(dictionary)
                .build(), 1, requestScope);
        assertEquals(bean.getId(), (Long) 1L);
    }

    @Test
    public void testLoadObjects() throws Exception {
        Iterable<NoopBean> iterable = (Iterable) tx.loadObjects(EntityProjection.builder()
                .type(NoopBean.class)
                .dictionary(dictionary)
                .build(), requestScope);
        NoopBean bean = iterable.iterator().next();
        assertEquals(bean.getId(), (Long) 1L);
    }

    @Test
    public void testClose() throws Exception {
        tx.close();
    }
}
