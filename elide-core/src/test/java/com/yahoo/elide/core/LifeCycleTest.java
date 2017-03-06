/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.audit.AuditLogger;
import com.yahoo.elide.security.User;
import com.yahoo.elide.security.checks.Check;

import org.testng.Assert;
import org.testng.annotations.Test;

import example.Author;
import example.Book;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

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

        Elide elide = getElide(store, dictionary, MOCK_AUDIT_LOGGER);

        String bookBody = "{\"data\": {\"type\":\"book\",\"attributes\": {\"title\":\"Grapes of Wrath\"}}}";

        when(store.beginTransaction()).thenReturn(tx);
        when(tx.createNewObject(Book.class)).thenReturn(book);

        elide.post("/book", bookBody, null);
        verify(tx).accessUser(anyObject());
        verify(tx).preCommit();
        verify(tx, times(1)).createObject(eq(book), isA(RequestScope.class));
        verify(tx).flush(isA(RequestScope.class));
        verify(tx).commit(isA(RequestScope.class));
        verify(tx).close();
    }

    @Test
    public void testElideGet() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        Book book = mock(Book.class);

        Elide elide = getElide(store, dictionary, MOCK_AUDIT_LOGGER);

        when(store.beginReadTransaction()).thenReturn(tx);
        when(tx.loadObject(eq(Book.class), eq(1L), anyObject(), anyObject())).thenReturn(book);

        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        elide.get("/book/1", headers, null);
        verify(tx).accessUser(anyObject());
        verify(tx).preCommit();
        verify(tx).flush(anyObject());
        verify(tx).commit(anyObject());
        verify(tx).close();
    }

    @Test
    public void testElidePatch() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        Book book = mock(Book.class);

        Elide elide = getElide(store, dictionary, MOCK_AUDIT_LOGGER);

        when(book.getId()).thenReturn(new Long(1));
        when(store.beginTransaction()).thenReturn(tx);
        when(tx.loadObject(eq(Book.class), eq(1L), anyObject(), anyObject())).thenReturn(book);

        String bookBody = "{\"data\":{\"type\":\"book\",\"id\":1,\"attributes\": {\"title\":\"Grapes of Wrath\"}}}";

        String contentType = "application/vnd.api+json";
        elide.patch(contentType, contentType, "/book/1", bookBody, null);
        verify(tx).accessUser(anyObject());
        verify(tx).preCommit();

        verify(tx).save(eq(book), isA(RequestScope.class));
        verify(tx).flush(isA(RequestScope.class));
        verify(tx).commit(isA(RequestScope.class));
        verify(tx).close();
    }

    @Test
    public void testElideDelete() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        Book book = mock(Book.class);

        Elide elide = getElide(store, dictionary, MOCK_AUDIT_LOGGER);

        when(book.getId()).thenReturn(new Long(1));
        when(store.beginTransaction()).thenReturn(tx);
        when(tx.loadObject(eq(Book.class), eq(1L), anyObject(), anyObject())).thenReturn(book);

        elide.delete("/book/1", "", null);
        verify(tx).accessUser(anyObject());
        verify(tx).preCommit();

        verify(tx).delete(eq(book), isA(RequestScope.class));
        verify(tx).flush(isA(RequestScope.class));
        verify(tx).commit(isA(RequestScope.class));
        verify(tx).close();
    }

    @Test
    public void testCreate() {
        Book book = mock(Book.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        when(tx.createNewObject(Book.class)).thenReturn(book);
        RequestScope scope = new RequestScope(null, null, tx, new User(1), null, getElideSettings(null, dictionary, MOCK_AUDIT_LOGGER));
        PersistentResource resource = PersistentResource.createObject(Book.class, scope, "uuid");
        resource.setValue("title", "should not affect calls since this is create!");
        Assert.assertNotNull(resource);
        scope.runQueuedPreSecurityTriggers();
        verify(book, times(1)).onCreateBook(scope);
        verify(book, times(0)).onDeleteBook(scope);
        verify(book, times(0)).onUpdateTitle(scope);
        verify(book, times(1)).preRead(scope);

        scope.runQueuedPreCommitTriggers();
        verify(book, times(1)).preCreateBook(scope);
        verify(book, times(0)).preDeleteBook(scope);
        verify(book, times(0)).preUpdateTitle(scope);
        verify(book, times(1)).preCommitRead(scope);

        scope.runQueuedPostCommitTriggers();
        verify(book, times(1)).postCreateBook(scope);
        verify(book, times(0)).postDeleteBook(scope);
        verify(book, times(0)).postUpdateTitle(scope);
        verify(book, times(1)).postRead(scope);
    }

    @Test
    public void testUpdate() {
        Book book = mock(Book.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);

        RequestScope scope = new RequestScope(null, null, tx , new User(1), null, getElideSettings(null, dictionary, MOCK_AUDIT_LOGGER));
        PersistentResource resource = new PersistentResource(book, scope);
        resource.setValue("title", "new title");
        scope.runQueuedPreSecurityTriggers();
        verify(book, times(0)).onCreateBook(scope);
        verify(book, times(0)).onDeleteBook(scope);
        verify(book, times(1)).onUpdateTitle(scope);
        verify(book, times(1)).preRead(scope);

        scope.runQueuedPreCommitTriggers();
        verify(book, times(0)).preCreateBook(scope);
        verify(book, times(0)).preDeleteBook(scope);
        verify(book, times(1)).preUpdateTitle(scope);
        verify(book, times(1)).preCommitRead(scope);

        scope.runQueuedPostCommitTriggers();
        verify(book, times(0)).postCreateBook(scope);
        verify(book, times(0)).postDeleteBook(scope);
        verify(book, times(1)).postUpdateTitle(scope);
        verify(book, times(1)).postRead(scope);
    }

    @Test
    public void testOnDelete() {
        Book book = mock(Book.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);

        RequestScope scope = new RequestScope(null, null, tx, new User(1), null, getElideSettings(null, dictionary, MOCK_AUDIT_LOGGER));
        PersistentResource resource = new PersistentResource(book, scope);
        resource.deleteResource();
        scope.runQueuedPreSecurityTriggers();
        verify(book, times(0)).onCreateBook(scope);
        verify(book, times(1)).onDeleteBook(scope);
        verify(book, times(0)).onUpdateTitle(scope);
        verify(book, times(0)).preRead(scope);

        scope.runQueuedPreCommitTriggers();
        verify(book, times(0)).preCreateBook(scope);
        verify(book, times(1)).preDeleteBook(scope);
        verify(book, times(0)).preUpdateTitle(scope);
        verify(book, times(0)).preCommitRead(scope);

        scope.runQueuedPostCommitTriggers();
        verify(book, times(0)).postCreateBook(scope);
        verify(book, times(1)).postDeleteBook(scope);
        verify(book, times(0)).postUpdateTitle(scope);
        verify(book, times(0)).postRead(scope);
    }

    @Test
    public void testOnRead() {
        Book book = mock(Book.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        RequestScope scope = new RequestScope(null, null, tx, new User(1), null, getElideSettings(null, dictionary, MOCK_AUDIT_LOGGER));
        PersistentResource resource = new PersistentResource(book, scope);

        resource.getValueChecked("title");
        scope.runQueuedPreSecurityTriggers();
        verify(book, times(0)).onCreateBook(scope);
        verify(book, times(0)).onDeleteBook(scope);
        verify(book, times(0)).onUpdateTitle(scope);
        verify(book, times(1)).preRead(scope);

        scope.runQueuedPreCommitTriggers();
        verify(book, times(0)).preCreateBook(scope);
        verify(book, times(0)).preDeleteBook(scope);
        verify(book, times(0)).preUpdateTitle(scope);
        verify(book, times(1)).preCommitRead(scope);

        scope.runQueuedPostCommitTriggers();
        verify(book, times(0)).postCreateBook(scope);
        verify(book, times(0)).postDeleteBook(scope);
        verify(book, times(0)).postUpdateTitle(scope);
        verify(book, times(1)).postRead(scope);
    }

    private Elide getElide(DataStore dataStore, EntityDictionary dictionary, AuditLogger auditLogger) {
        return new Elide(getElideSettings(dataStore, dictionary, auditLogger));
    }

    private ElideSettings getElideSettings(DataStore dataStore, EntityDictionary dictionary, AuditLogger auditLogger) {
        return new ElideSettingsBuilder(dataStore)
                .withEntityDictionary(dictionary)
                .withAuditLogger(auditLogger)
                .build();
    }
}
