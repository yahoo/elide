/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.datastore.wrapped;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yahoo.elide.core.datastore.DataStoreIterable;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.request.Attribute;
import org.junit.jupiter.api.Test;

public class TransactionWrapperTest {

    private static class TestTransactionWrapper extends TransactionWrapper {
        public TestTransactionWrapper(DataStoreTransaction wrapped) {
            super(wrapped);
        }
    }

    @Test
    public void testPreCommit() {
        DataStoreTransaction wrapped = mock(DataStoreTransaction.class);
        DataStoreTransaction wrapper = new TestTransactionWrapper(wrapped);

        wrapper.preCommit(null);
        verify(wrapped, times(1)).preCommit(any());
    }

    @Test
    public void testClose() throws Exception {
        DataStoreTransaction wrapped = mock(DataStoreTransaction.class);
        DataStoreTransaction wrapper = new TestTransactionWrapper(wrapped);

        wrapper.close();

        verify(wrapped, times(1)).close();
    }

    @Test
    public void testLoadObjects() throws Exception {
        DataStoreTransaction wrapped = mock(DataStoreTransaction.class);
        DataStoreTransaction wrapper = new TestTransactionWrapper(wrapped);

        DataStoreIterable<Object> expected = mock(DataStoreIterable.class);
        when(wrapped.loadObjects(any(), any())).thenReturn(expected);

        Iterable<Object> actual = wrapper.loadObjects(null, null);

        verify(wrapped, times(1)).loadObjects(any(), any());
        assertEquals(expected, actual);
    }

    @Test
    public void testCreateObject() {
        DataStoreTransaction wrapped = mock(DataStoreTransaction.class);
        DataStoreTransaction wrapper = new TestTransactionWrapper(wrapped);

        wrapper.createObject(null, null);

        verify(wrapped, times(1)).createObject(any(), any());
    }

    @Test
    public void testCommit() {
        DataStoreTransaction wrapped = mock(DataStoreTransaction.class);
        DataStoreTransaction wrapper = new TestTransactionWrapper(wrapped);

        wrapper.commit(null);

        verify(wrapped, times(1)).commit(any());
    }

    @Test
    public void testFlush() {
        DataStoreTransaction wrapped = mock(DataStoreTransaction.class);
        DataStoreTransaction wrapper = new TestTransactionWrapper(wrapped);

        wrapper.flush(null);

        verify(wrapped, times(1)).flush(any());
    }

    @Test
    public void testDelete() {
        DataStoreTransaction wrapped = mock(DataStoreTransaction.class);
        DataStoreTransaction wrapper = new TestTransactionWrapper(wrapped);

        wrapper.delete(null, null);

        verify(wrapped, times(1)).delete(any(), any());
    }

    @Test
    public void testSave() {
        DataStoreTransaction wrapped = mock(DataStoreTransaction.class);
        DataStoreTransaction wrapper = new TestTransactionWrapper(wrapped);

        wrapper.save(null, null);

        verify(wrapped, times(1)).save(any(), any());
    }

    @Test
    public void testGetAttribute() {
        DataStoreTransaction wrapped = mock(DataStoreTransaction.class);
        DataStoreTransaction wrapper = new TestTransactionWrapper(wrapped);

        when(wrapped.getAttribute(any(), isA(Attribute.class), any())).thenReturn(1L);

        Object actual = wrapper.getAttribute(null, Attribute.builder().name("foo").type(String.class).build(), null);

        verify(wrapped, times(1)).getAttribute(any(), isA(Attribute.class), any());
        assertEquals(1L, actual);
    }

    @Test
    public void testSetAttribute() {
        DataStoreTransaction wrapped = mock(DataStoreTransaction.class);
        DataStoreTransaction wrapper = new TestTransactionWrapper(wrapped);

        wrapper.setAttribute(null, null, null);

        verify(wrapped, times(1)).setAttribute(any(), any(), any());
    }

    @Test
    public void testUpdateToOneRelation() {
        DataStoreTransaction wrapped = mock(DataStoreTransaction.class);
        DataStoreTransaction wrapper = new TestTransactionWrapper(wrapped);

        wrapper.updateToOneRelation(null, null, null, null, null);

        verify(wrapped, times(1)).updateToOneRelation(any(), any(), any(), any(), any());
    }

    @Test
    public void testUpdateToManyRelation() {
        DataStoreTransaction wrapped = mock(DataStoreTransaction.class);
        DataStoreTransaction wrapper = new TestTransactionWrapper(wrapped);

        wrapper.updateToManyRelation(null, null, null, null, null, null);

        verify(wrapped, times(1)).updateToManyRelation(any(), any(), any(), any(), any(), any());
    }

    @Test
    public void testGetToManyRelation() {
        DataStoreTransaction wrapped = mock(DataStoreTransaction.class);
        DataStoreTransaction wrapper = new TestTransactionWrapper(wrapped);

        DataStoreIterable expected = mock(DataStoreIterable.class);
        when(wrapped.getToManyRelation(any(), any(), any(), any())).thenReturn(expected);

        DataStoreIterable actual = wrapper.getToManyRelation(null, null, null, null);

        verify(wrapped, times(1)).getToManyRelation(any(), any(), any(), any());
        assertEquals(expected, actual);
    }

    @Test
    public void testGetToOneRelation() {
        DataStoreTransaction wrapped = mock(DataStoreTransaction.class);
        DataStoreTransaction wrapper = new TestTransactionWrapper(wrapped);

        when(wrapped.getToOneRelation(any(), any(), any(), any())).thenReturn(1L);

        Long actual = wrapper.getToOneRelation(null, null, null, null);

        verify(wrapped, times(1)).getToOneRelation(any(), any(), any(), any());
        assertEquals(1L, actual);
    }

    @Test
    public void testLoadObject() {
        DataStoreTransaction wrapped = mock(DataStoreTransaction.class);
        DataStoreTransaction wrapper = new TestTransactionWrapper(wrapped);

        when(wrapped.loadObject(any(), any(), any())).thenReturn(1L);

        Object actual = wrapper.loadObject(null, null, null);

        verify(wrapped, times(1)).loadObject(any(), any(), any());
        assertEquals(1L, actual);
    }

    @Test
    public void testGetProperty() {
        DataStoreTransaction wrapped = mock(DataStoreTransaction.class);
        DataStoreTransaction wrapper = new TestTransactionWrapper(wrapped);

        when(wrapped.getProperty(any())).thenReturn(1L);

        Object actual = wrapper.getProperty("foo");

        verify(wrapped, times(1)).getProperty(any());
        assertEquals(1L, actual);
    }
}
