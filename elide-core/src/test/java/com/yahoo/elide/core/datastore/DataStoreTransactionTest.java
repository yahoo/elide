/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.datastore;

import static com.yahoo.elide.core.type.ClassType.STRING_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.request.Attribute;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.request.Relationship;
import com.yahoo.elide.core.type.ClassType;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

public class DataStoreTransactionTest implements DataStoreTransaction {
    private static final String NAME = "name";
    private static final String NAME2 = "name2";
    private static final Attribute NAME_ATTRIBUTE = Attribute.builder().name(NAME).type(String.class).build();
    private static final String ENTITY = "entity";
    private RequestScope scope;

    @BeforeEach
    public void setupMocks() {
        // this will test with the default interface implementation
        scope = mock(RequestScope.class);
        EntityDictionary dictionary = mock(EntityDictionary.class);

        when(scope.getDictionary()).thenReturn(dictionary);
        when(dictionary.getIdType(STRING_TYPE)).thenReturn(new ClassType(Long.class));
        when(dictionary.getValue(ENTITY, NAME, scope)).thenReturn(3L);
        when(dictionary.getValue(ENTITY, NAME2, scope))
                .thenReturn(new DataStoreIterableBuilder(List.of(1L, 2L, 3L)).build());
    }

    @Test
    public void testPreCommit() {
        preCommit(scope);
        verify(scope, never()).getDictionary();
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
    public void testGetToOneRelation() {
        Long actual = getToOneRelation(this, ENTITY, Relationship.builder()
                .name(NAME)
                .projection(EntityProjection.builder()
                        .type(String.class)
                        .build())
                .build(), scope);
        assertEquals(3L, actual);
    }

    @Test
    public void testGetToManyRelation() {
        DataStoreIterable actual = getToOneRelation(this, ENTITY, Relationship.builder()
                .name(NAME2)
                .projection(EntityProjection.builder()
                        .type(String.class)
                        .build())
                .build(), scope);
        assertEquals(
                Lists.newArrayList(new DataStoreIterableBuilder(List.of(1L, 2L, 3L)).build()),
                Lists.newArrayList(actual));
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
    public DataStoreIterable<Object> loadObjects(EntityProjection entityProjection, RequestScope scope) {
        return new DataStoreIterableBuilder(Arrays.asList(ENTITY)).build();
    }

    @Override
    public void close() throws IOException {
        // nothing
    }

    @Override
    public void save(Object entity, RequestScope scope) {
        // nothing
    }

    @Override
    public void delete(Object entity, RequestScope scope) {
        // nothing
    }

    @Override
    public void flush(RequestScope scope) {
        // nothing
    }

    @Override
    public void commit(RequestScope scope) {
        // nothing
    }

    @Override
    public void createObject(Object entity, RequestScope scope) {
        // nothing
    }

    @Override
    public void cancel(RequestScope scope) {
       //nothing
    }
}
