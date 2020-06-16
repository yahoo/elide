/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.pagination.Pagination;
import com.yahoo.elide.core.sort.Sorting;
import com.yahoo.elide.security.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

public class DataStoreTransactionTest implements DataStoreTransaction {
    private static final String NAME = "name";
    private static final String ENTITY = "entity";
    private RequestScope scope;

    @BeforeEach
    public void setupMocks() {
        // this will test with the default interface implementation
        scope = mock(RequestScope.class);
        EntityDictionary dictionary = mock(EntityDictionary.class);

        when(scope.getDictionary()).thenReturn(dictionary);
        when(dictionary.getIdType(String.class)).thenReturn((Class) Long.class);
        when(dictionary.getValue(ENTITY, NAME, scope)).thenReturn(3L);
    }

    @Test
    public void testAccessUser() {
        User actualUser = accessUser(2L);
        assertEquals(2L, actualUser.getOpaqueUser());
    }

    @Test
    public void testPreCommit() {
        preCommit();
        verify(scope, never()).getDictionary();
    }

    @Test
    public void testCreateNewObject() {
        Object actual = createNewObject(String.class);
        assertEquals("", actual);
    }

    @Test
    public void testSupportsSorting() {
        boolean actual = supportsSorting(null, null);
        assertTrue(actual);
    }

    @Test
    public void testSupportsPagination() {
        boolean actual = supportsPagination(null);
        assertTrue(actual);
    }

    @Test
    public void testSupportsFiltering() {
        DataStoreTransaction.FeatureSupport actual = supportsFiltering(null, null);
        assertEquals(DataStoreTransaction.FeatureSupport.FULL, actual);
    }

    @Test
    public void testGetAttribute() {
        Object actual = getAttribute(ENTITY, NAME, scope);
        assertEquals(3L, actual);
        verify(scope, times(1)).getDictionary();
    }

    @Test
    public void testSetAttribute() {
        setAttribute(ENTITY, NAME, null, scope);
        verify(scope, never()).getDictionary();
    }

    @Test
    public void testUpdateToOneRelation() {
        updateToOneRelation(this, ENTITY, NAME, null, scope);
        verify(scope, never()).getDictionary();
    }

    @Test
    public void testUpdateToManyRelation() {
        updateToManyRelation(this, ENTITY, NAME, null, null, scope);
        verify(scope, never()).getDictionary();
    }

    @Test
    public void testGetRelation() {
        Object actual = getRelation(this, ENTITY, NAME, Optional.empty(), Optional.empty(), Optional.empty(), scope);
        assertEquals(3L, actual);
        verify(scope, times(1)).getDictionary();
    }

    @Test
    public void testLoadObject() {
        String string = (String) loadObject(String.class, 2L, Optional.empty(), scope);
        assertEquals(ENTITY, string);
        verify(scope, times(1)).getDictionary();
    }

    /** Implemented to support the interface only. No need to test these. **/

    @Override
    @Deprecated
    public Iterable<Object> loadObjects(Class<?> entityClass, Optional<FilterExpression> filterExpression,
            Optional<Sorting> sorting, Optional<Pagination> pagination, RequestScope scope) {
        return Arrays.asList(ENTITY);
    }

    @Override
    @Deprecated
    public void close() throws IOException {
        // nothing
    }

    @Override
    @Deprecated
    public void save(Object entity, RequestScope scope) {
        // nothing
    }

    @Override
    @Deprecated
    public void delete(Object entity, RequestScope scope) {
        // nothing
    }

    @Override
    @Deprecated
    public void flush(RequestScope scope) {
        // nothing
    }

    @Override
    @Deprecated
    public void commit(RequestScope scope) {
        // nothing
    }

    @Override
    @Deprecated
    public void createObject(Object entity, RequestScope scope) {
        // nothing
    }
}
