/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.datastore.wrapped;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.security.User;
import org.junit.jupiter.api.Test;

import java.util.Optional;

public class TransactionWrapperTest {

    private class TestTransactionWrapper extends TransactionWrapper {

        public TestTransactionWrapper(DataStoreTransaction wrapped) {
            super(wrapped);
        }
    }

    @Test
    public void testAccessUser() {
        DataStoreTransaction wrapped = mock(DataStoreTransaction.class);
        DataStoreTransaction wrapper = new TestTransactionWrapper(wrapped);

        Object wrappedUser = new Object();
        User expectedUser = new User(wrappedUser);
        when(wrapped.accessUser(eq(wrappedUser))).thenReturn(expectedUser);

        User actualUser = wrapper.accessUser(wrappedUser);

        verify(wrapped, times(1)).accessUser(eq(wrappedUser));
        assertEquals(expectedUser, actualUser);
    }

    @Test
    public void testPreCommit() {
        DataStoreTransaction wrapped = mock(DataStoreTransaction.class);
        DataStoreTransaction wrapper = new TestTransactionWrapper(wrapped);

        wrapper.preCommit();
        verify(wrapped, times(1)).preCommit();
    }

    @Test
    public void testCreateNewObject() {
        DataStoreTransaction wrapped = mock(DataStoreTransaction.class);
        DataStoreTransaction wrapper = new TestTransactionWrapper(wrapped);

        Object expected = new Object();
        when(wrapped.createNewObject(eq(Object.class))).thenReturn(expected);

        Object actual = wrapper.createNewObject(Object.class);

        verify(wrapped, times(1)).createNewObject(eq(Object.class));
        assertEquals(expected, actual);
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

        Iterable<Object> expected = mock(Iterable.class);
        when(wrapped.loadObjects(any(), any(), any(), any(), any())).thenReturn(expected);

        Iterable<Object> actual = wrapper.loadObjects(null, Optional.empty(),
                Optional.empty(), Optional.empty(), null);

        verify(wrapped, times(1)).loadObjects(any(), any(), any(), any(), any());
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
    public void testSupportsSorting() {
        DataStoreTransaction wrapped = mock(DataStoreTransaction.class);
        DataStoreTransaction wrapper = new TestTransactionWrapper(wrapped);

        when(wrapped.supportsSorting(any(), any())).thenReturn(true);
        boolean actual = wrapper.supportsSorting(null, null);

        verify(wrapped, times(1)).supportsSorting(any(), any());
        assertTrue(actual);
    }

    @Test
    public void testSupportsPagination() {
        DataStoreTransaction wrapped = mock(DataStoreTransaction.class);
        DataStoreTransaction wrapper = new TestTransactionWrapper(wrapped);

        when(wrapped.supportsPagination(any())).thenReturn(true);
        boolean actual = wrapper.supportsPagination(null);

        verify(wrapped, times(1)).supportsPagination(any());
        assertTrue(actual);
    }

    @Test
    public void testSupportsFiltering() {
        DataStoreTransaction wrapped = mock(DataStoreTransaction.class);
        DataStoreTransaction wrapper = new TestTransactionWrapper(wrapped);

        when(wrapped.supportsFiltering(any(), any())).thenReturn(DataStoreTransaction.FeatureSupport.FULL);
        DataStoreTransaction.FeatureSupport actual = wrapper.supportsFiltering(null, null);

        verify(wrapped, times(1)).supportsFiltering(any(), any());
        assertEquals(DataStoreTransaction.FeatureSupport.FULL, actual);
    }

    @Test
    public void testGetAttribute() {
        DataStoreTransaction wrapped = mock(DataStoreTransaction.class);
        DataStoreTransaction wrapper = new TestTransactionWrapper(wrapped);

        when(wrapped.getAttribute(any(), any(), any())).thenReturn(1L);

        Object actual = wrapper.getAttribute(null, null, null);

        verify(wrapped, times(1)).getAttribute(any(), any(), any());
        assertEquals(1L, actual);
    }

    @Test
    public void testSetAttribute() {
        DataStoreTransaction wrapped = mock(DataStoreTransaction.class);
        DataStoreTransaction wrapper = new TestTransactionWrapper(wrapped);

        wrapper.setAttribute(null, null, null, null);

        verify(wrapped, times(1)).setAttribute(any(), any(), any(), any());
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
    public void testGetRelation() {
        DataStoreTransaction wrapped = mock(DataStoreTransaction.class);
        DataStoreTransaction wrapper = new TestTransactionWrapper(wrapped);

        when(wrapped.getRelation(any(), any(), any(), any(), any(), any(), any())).thenReturn(1L);

        Object actual = wrapper.getRelation(null, null, null, null,
                null, null, null);

        verify(wrapped, times(1)).getRelation(any(), any(), any(), any(), any(), any(), any());
        assertEquals(1L, actual);
    }

    @Test
    public void testLoadObject() {
        DataStoreTransaction wrapped = mock(DataStoreTransaction.class);
        DataStoreTransaction wrapper = new TestTransactionWrapper(wrapped);

        when(wrapped.loadObject(any(), any(), any(), any())).thenReturn(1L);

        Object actual = wrapper.loadObject(null, null, null, null);

        verify(wrapped, times(1)).loadObject(any(), any(), any(), any());
        assertEquals(1L, actual);
    }
}
