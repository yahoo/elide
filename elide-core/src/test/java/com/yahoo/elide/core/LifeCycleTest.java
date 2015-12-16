/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import com.yahoo.elide.audit.Logger;
import com.yahoo.elide.security.User;
import example.Author;
import example.Book;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Set;

import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test PersistentResource.
 */
public class LifeCycleTest {
    public class TestEntityDictionary extends EntityDictionary {

        @Override
        public Class<?> lookupEntityClass(Class<?> objClass) {
            // Special handling for mocked Book class which has Entity annotation
            if (objClass.getName().contains("$MockitoMock$")) {
                objClass = objClass.getSuperclass();
            }
            return super.lookupEntityClass(objClass);
        }
    }

    private static final Logger MOCK_LOGGER = mock(Logger.class);
    private EntityDictionary dictionary;

    LifeCycleTest() {
        dictionary = new TestEntityDictionary();
        dictionary.bindEntity(Book.class);
        dictionary.bindEntity(Author.class);
    }

    @Test
    public void testOnCreate() {
        Book book = mock(Book.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        when(tx.createObject(Book.class)).thenReturn(book);
        RequestScope scope = new RequestScope(null, tx, new User(1), dictionary, null, MOCK_LOGGER);
        PersistentResource resource = PersistentResource.createObject(Book.class, scope, "uuid");
        Assert.assertNotNull(resource);
        verify(book, times(1)).onCreateBook();
        verify(book, times(0)).onDeleteBook();
        verify(book, times(0)).onCommitBook();
        verify(book, times(0)).onUpdateTitle();
        verify(book, times(0)).onCommitTitle();
    }

    @Test
    public void createObjectOnCommit() {
        Book book = mock(Book.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        when(tx.createObject(Book.class)).thenReturn(book);
        RequestScope scope = new RequestScope(null, tx, new User(1), dictionary, null, MOCK_LOGGER);
        PersistentResource resource = PersistentResource.createObject(Book.class, scope, "uuid");
        scope.runCommitTriggers();
        Assert.assertNotNull(resource);
        verify(book, times(1)).onCreateBook();
        verify(book, times(0)).onDeleteBook();
        verify(book, times(1)).onCommitBook();
        verify(book, times(0)).onUpdateTitle();
        verify(book, times(0)).onCommitTitle();
    }

    @Test
    public void loadRecordOnCommit() {
        Book book = mock(Book.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        when(tx.loadObject(Book.class, 1L)).thenReturn(book);
        RequestScope scope = new RequestScope(null, tx, new User(1), dictionary, null, MOCK_LOGGER);
        PersistentResource resource = PersistentResource.loadRecord(Book.class, "1", scope);
        scope.runCommitTriggers();
        Assert.assertNotNull(resource);
        verify(book, times(0)).onCreateBook();
        verify(book, times(0)).onDeleteBook();
        verify(book, times(1)).onCommitBook();
        verify(book, times(0)).onUpdateTitle();
        verify(book, times(0)).onCommitTitle();
    }

    @Test
    public void loadRecordsOnCommit() {
        Book book = mock(Book.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        when(tx.loadObjects(eq(Book.class), isA(FilterScope.class))).thenReturn(Arrays.asList(book));
        RequestScope scope = new RequestScope(null, tx, new User(1), dictionary, null, MOCK_LOGGER);
        Set<PersistentResource<Book>> resources = PersistentResource.loadRecords(Book.class, scope);
        scope.runCommitTriggers();
        Assert.assertEquals(resources.size(), 1);
        verify(book, times(0)).onCreateBook();
        verify(book, times(0)).onDeleteBook();
        verify(book, times(1)).onCommitBook();
        verify(book, times(0)).onUpdateTitle();
        verify(book, times(0)).onCommitTitle();
    }

    @Test
    public void testOnUpdate() {
        Book book = mock(Book.class);
        RequestScope scope = new RequestScope(null, null, new User(1), dictionary, null, MOCK_LOGGER);
        PersistentResource resource = new PersistentResource(book, scope);
        resource.setValue("title", "new title");
        scope.runCommitTriggers();
        verify(book, times(0)).onCreateBook();
        verify(book, times(0)).onDeleteBook();
        verify(book, times(0)).onCommitBook();
        verify(book, times(1)).onUpdateTitle();
        verify(book, times(1)).onCommitTitle();
    }

    @Test
    public void testOnDelete() {
        Book book = mock(Book.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        RequestScope scope = new RequestScope(null, tx, new User(1), dictionary, null, MOCK_LOGGER);
        PersistentResource resource = new PersistentResource(book, scope);
        resource.deleteResource();
        verify(book, times(0)).onCreateBook();
        verify(book, times(1)).onDeleteBook();
        verify(book, times(0)).onCommitBook();
        verify(book, times(0)).onCommitTitle();
        verify(book, times(0)).onUpdateTitle();
    }
}
