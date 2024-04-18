/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.noop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.paiondata.elide.beans.NoopBean;
import com.paiondata.elide.core.ObjectEntityCache;
import com.paiondata.elide.core.datastore.DataStoreTransaction;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.request.EntityProjection;
import com.paiondata.elide.jsonapi.JsonApiMapper;
import com.paiondata.elide.jsonapi.JsonApiRequestScope;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.IterableUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class NoopTransactionTest {
    private DataStoreTransaction tx = new NoopTransaction();
    private NoopBean bean = new NoopBean();
    private JsonApiRequestScope requestScope;
    private EntityDictionary dictionary;

    @BeforeAll
    public void setup() {
        dictionary = EntityDictionary.builder().build();
        dictionary.bindEntity(NoopBean.class);
        requestScope = mock(JsonApiRequestScope.class);
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
                .build(), 1, requestScope);
        assertEquals(bean.getId(), (Long) 1L);
    }

    @Test
    public void testLoadObjects() throws Exception {
        Iterable<NoopBean> iterable = (Iterable) tx.loadObjects(EntityProjection.builder()
                .type(NoopBean.class)
                .build(), requestScope);
        NoopBean bean = IterableUtils.first(iterable);
        assertEquals((Long) 1L, bean.getId());
    }

    @Test
    public void testClose() throws Exception {
        tx.close();
    }
}
