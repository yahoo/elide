/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import com.yahoo.elide.Elide;
import com.yahoo.elide.audit.AuditLogger;
import com.yahoo.elide.security.User;
import com.yahoo.elide.security.checks.Check;
import example.Author;
import example.Book;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the invocation & sequencing of DataStoreTransaction method invocations and life cycle events.
 */
public class LifeCycleTest {
    public class TestEntityDictionary extends EntityDictionary {
        public TestEntityDictionary(Map<String, Class<? extends Check>> checks) {
            super(checks);
        }

        @Override
        public Class<?> lookupEntityClass(Class<?> objClass) {
            // Special handling for mocked Book class which has Entity annotation
            if (objClass.getName().contains("$MockitoMock$")) {
                objClass = objClass.getSuperclass();
            }
            return super.lookupEntityClass(objClass);
        }
    }

    private static final AuditLogger MOCK_AUDIT_LOGGER = mock(AuditLogger.class);
    private EntityDictionary dictionary;

    LifeCycleTest() {
        dictionary = new TestEntityDictionary(new HashMap<>());
        dictionary.bindEntity(Book.class);
        dictionary.bindEntity(Author.class);
    }

    @Test
    public void testElideCreate() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        Book book = mock(Book.class);

        Elide.Builder builder = new Elide.Builder(store);
        builder.withAuditLogger(MOCK_AUDIT_LOGGER);
        builder.withEntityDictionary(dictionary);
        Elide elide = builder.build();

        String bookBody = "{\"data\": {\"type\":\"book\",\"attributes\": {\"title\":\"Grapes of Wrath\"}}}";

        when(store.beginTransaction()).thenReturn(tx);
        when(tx.createObject(Book.class)).thenReturn(book);

        elide.post("/book", bookBody, null);
        verify(tx).accessUser(null);
        verify(tx).preCommit();

        verify(tx, times(1)).save(book);
        verify(tx).flush();
        verify(tx).commit();
        verify(tx).close();
    }

    @Test
    public void testElideGet() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        Book book = mock(Book.class);

        Elide.Builder builder = new Elide.Builder(store);
        builder.withAuditLogger(MOCK_AUDIT_LOGGER);
        builder.withEntityDictionary(dictionary);
        Elide elide = builder.build();

        when(store.beginReadTransaction()).thenReturn(tx);
        when(tx.loadObject(Book.class, new Long(1))).thenReturn(book);

        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        elide.get("/book/1", headers, null);
        verify(tx).accessUser(null);
        verify(tx).preCommit();
        verify(tx).flush();
        verify(tx).commit();
        verify(tx).close();
    }

    @Test
    public void testElidePatch() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        Book book = mock(Book.class);

        Elide.Builder builder = new Elide.Builder(store);
        builder.withAuditLogger(MOCK_AUDIT_LOGGER);
        builder.withEntityDictionary(dictionary);
        Elide elide = builder.build();

        when(book.getId()).thenReturn(new Long(1));
        when(store.beginTransaction()).thenReturn(tx);
        when(tx.loadObject(Book.class, new Long(1))).thenReturn(book);

        String bookBody = "{\"data\":{\"type\":\"book\",\"id\":1,\"attributes\": {\"title\":\"Grapes of Wrath\"}}}";

        String contentType = "application/vnd.api+json";
        elide.patch(contentType, contentType, "/book/1", bookBody, null);
        verify(tx).accessUser(null);
        verify(tx).preCommit();

        verify(tx).save(book);
        verify(tx).flush();
        verify(tx).commit();
        verify(tx).close();
    }

    @Test
    public void testElideDelete() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        Book book = mock(Book.class);

        Elide.Builder builder = new Elide.Builder(store);
        builder.withAuditLogger(MOCK_AUDIT_LOGGER);
        builder.withEntityDictionary(dictionary);
        Elide elide = builder.build();

        when(book.getId()).thenReturn(new Long(1));
        when(store.beginTransaction()).thenReturn(tx);
        when(tx.loadObject(Book.class, new Long(1))).thenReturn(book);

        elide.delete("/book/1", "", null);
        verify(tx).accessUser(null);
        verify(tx).preCommit();

        verify(tx).delete(book);
        verify(tx).flush();
        verify(tx).commit();
        verify(tx).close();
    }

    @Test
    public void testOnCreate() {
        Book book = mock(Book.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        when(tx.createObject(Book.class)).thenReturn(book);
        RequestScope scope = new RequestScope(null, tx, new User(1), dictionary, null, MOCK_AUDIT_LOGGER);
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
        RequestScope scope = new RequestScope(null, tx, new User(1), dictionary, null, MOCK_AUDIT_LOGGER);
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
        RequestScope scope = new RequestScope(null, tx, new User(1), dictionary, null, MOCK_AUDIT_LOGGER);
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
        RequestScope scope = new RequestScope(null, tx, new User(1), dictionary, null, MOCK_AUDIT_LOGGER);
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
        RequestScope scope = new RequestScope(null, null, new User(1), dictionary, null, MOCK_AUDIT_LOGGER);
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
        RequestScope scope = new RequestScope(null, tx, new User(1), dictionary, null, MOCK_AUDIT_LOGGER);
        PersistentResource resource = new PersistentResource(book, scope);
        resource.deleteResource();
        verify(book, times(0)).onCreateBook();
        verify(book, times(1)).onDeleteBook();
        verify(book, times(0)).onCommitBook();
        verify(book, times(0)).onCommitTitle();
        verify(book, times(0)).onUpdateTitle();
    }
}
