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

import com.yahoo.elide.request.Attribute;
import com.yahoo.elide.request.EntityProjection;
import com.yahoo.elide.request.Relationship;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;

public class DataStoreTransactionTest implements DataStoreTransaction {
    private static final String NAME = "name";
    private static final Attribute NAME_ATTRIBUTE = Attribute.builder().name(NAME).type(String.class).build();
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
        boolean actual = supportsPagination(null, null);
        assertTrue(actual);
    }

    @Test
    public void testSupportsFiltering() {
        DataStoreTransaction.FeatureSupport actual = supportsFiltering(null, null);
        assertEquals(DataStoreTransaction.FeatureSupport.FULL, actual);
    }

    @Test
    public void testGetAttribute() {
        Object actual = getAttribute(ENTITY, NAME_ATTRIBUTE, scope);
        assertEquals(3L, actual);
        verify(scope, times(1)).getDictionary();
    }

    @Test
    public void testSetAttribute() {
        setAttribute(ENTITY, NAME_ATTRIBUTE, scope);
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
        Object actual = getRelation(this, ENTITY, Relationship.builder()
                .name(NAME)
                .projection(EntityProjection.builder()
                        .type(String.class)
                        .build())
                .build(), scope);
        assertEquals(3L, actual);
    }

    @Test
    public void testLoadObject() {
        String string = (String) loadObject(EntityProjection.builder().type(String.class).build(), 2L, scope);
        assertEquals(ENTITY, string);
    }

    /** Implemented to support the interface only. No need to test these. **/
    @Override
    public Object loadObject(EntityProjection entityProjection, Serializable id, RequestScope scope) {
        return ENTITY;
    }

    @Override
    public Iterable<Object> loadObjects(EntityProjection entityProjection, RequestScope scope) {
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

    @Override
    public void cancel(RequestScope scope) {
       //nothing
    }
}
