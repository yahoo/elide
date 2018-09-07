/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.OnCreatePostCommit;
import com.yahoo.elide.annotation.OnCreatePreCommit;
import com.yahoo.elide.annotation.OnCreatePreSecurity;
import com.yahoo.elide.annotation.OnDeletePostCommit;
import com.yahoo.elide.annotation.OnDeletePreCommit;
import com.yahoo.elide.annotation.OnDeletePreSecurity;
import com.yahoo.elide.annotation.OnReadPostCommit;
import com.yahoo.elide.annotation.OnReadPreCommit;
import com.yahoo.elide.annotation.OnReadPreSecurity;
import com.yahoo.elide.annotation.OnUpdatePostCommit;
import com.yahoo.elide.annotation.OnUpdatePreCommit;
import com.yahoo.elide.annotation.OnUpdatePreSecurity;
import com.yahoo.elide.audit.AuditLogger;
import com.yahoo.elide.functions.LifeCycleHook;
import com.yahoo.elide.security.ChangeSpec;
import com.yahoo.elide.security.User;
import com.yahoo.elide.security.checks.Check;
import example.Author;
import example.Book;
import example.TestCheckMappings;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.persistence.Entity;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

/**
 * Tests the invocation & sequencing of DataStoreTransaction method invocations and life cycle events.
 */
public class LifeCycleTest {

    private static final AuditLogger MOCK_AUDIT_LOGGER = mock(AuditLogger.class);
    private EntityDictionary dictionary;
    private MockCallback callback;
    private MockCallback onUpdateDeferredCallback;
    private MockCallback onUpdateImmediateCallback;


    public class MockCallback<T> implements LifeCycleHook<T> {
        @Override
        public void execute(T object, com.yahoo.elide.security.RequestScope scope, Optional<ChangeSpec> changes) {
            //NOOP
        }
    }

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

    LifeCycleTest() throws Exception {
        callback = mock(MockCallback.class);
        onUpdateDeferredCallback = mock(MockCallback.class);
        onUpdateImmediateCallback = mock(MockCallback.class);
        dictionary = new TestEntityDictionary(TestCheckMappings.MAPPINGS);
        dictionary.bindEntity(Book.class);
        dictionary.bindEntity(Author.class);
        dictionary.bindTrigger(Book.class, OnCreatePostCommit.class, callback);
        dictionary.bindTrigger(Book.class, OnCreatePreCommit.class, callback);
        dictionary.bindTrigger(Book.class, OnCreatePreSecurity.class, callback);
        dictionary.bindTrigger(Book.class, OnReadPostCommit.class, callback);
        dictionary.bindTrigger(Book.class, OnReadPreCommit.class, callback);
        dictionary.bindTrigger(Book.class, OnReadPreSecurity.class, callback);
        dictionary.bindTrigger(Book.class, OnDeletePostCommit.class, callback);
        dictionary.bindTrigger(Book.class, OnDeletePreCommit.class, callback);
        dictionary.bindTrigger(Book.class, OnDeletePreSecurity.class, callback);
        dictionary.bindTrigger(Book.class, OnUpdatePostCommit.class, "title", callback);
        dictionary.bindTrigger(Book.class, OnUpdatePreCommit.class, "title", callback);
        dictionary.bindTrigger(Book.class, OnUpdatePreSecurity.class, "title", callback);
        dictionary.bindTrigger(Book.class, OnUpdatePreCommit.class, onUpdateDeferredCallback, true);
        dictionary.bindTrigger(Book.class, OnUpdatePreSecurity.class, onUpdateImmediateCallback, true);
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

        ElideResponse response = elide.post("/book", bookBody, null);
        assertEquals(response.getResponseCode(), HttpStatus.SC_CREATED);

        /*
         * This gets called for :
         *  - read pre-security for the book
         *  - create pre-security for the book
         *  - read pre-commit for the book
         *  - create pre-commit for the book
         *  - read post-commit for the book
         *  - create post-commit for the book
         */
        verify(callback, times(6)).execute(eq(book), isA(RequestScope.class), any());
        verify(tx).accessUser(any());
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
        when(tx.loadObject(eq(Book.class), eq(1L), any(), any())).thenReturn(book);

        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        elide.get("/book/1", headers, null);

        /*
         * This gets called for :
         *  - read pre-security for the book
         *  - read pre-commit for the book
         *  - read post-commit for the book
         */
        verify(callback, times(3)).execute(eq(book), isA(RequestScope.class), any());
        verify(tx).accessUser(any());
        verify(tx).preCommit();
        verify(tx).flush(any());
        verify(tx).commit(any());
        verify(tx).close();
    }

    @Test
    public void testElidePatch() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        Book book = mock(Book.class);

        Elide elide = getElide(store, dictionary, MOCK_AUDIT_LOGGER);

        when(book.getId()).thenReturn(1L);
        when(store.beginTransaction()).thenReturn(tx);
        when(tx.loadObject(eq(Book.class), eq(1L), any(), any())).thenReturn(book);

        String bookBody = "{\"data\":{\"type\":\"book\",\"id\":1,\"attributes\": {\"title\":\"Grapes of Wrath\"}}}";

        String contentType = "application/vnd.api+json";
        elide.patch(contentType, contentType, "/book/1", bookBody, null);
        /*
         * This gets called for :
         *  - read pre-security for the book
         *  - update pre-security for the book.title
         *  - read pre-commit for the book
         *  - update pre-commit for the book.title
         *  - read post-commit for the book
         *  - update post-commit for the book.title
         */
        verify(callback, times(6)).execute(eq(book), isA(RequestScope.class), any());
        verify(onUpdateImmediateCallback, times(1)).execute(eq(book), isA(RequestScope.class), any());
        verify(onUpdateDeferredCallback, times(1)).execute(eq(book), isA(RequestScope.class), any());
        verify(tx).accessUser(any());
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

        when(book.getId()).thenReturn(1L);
        when(store.beginTransaction()).thenReturn(tx);
        when(tx.loadObject(eq(Book.class), eq(1L), any(), any())).thenReturn(book);

        elide.delete("/book/1", "", null);
        /*
         * This gets called for :
         *  - delete pre-security for the book
         *  - delete pre-commit for the book
         *  - delete post-commit for the book
         */
        verify(callback, times(3)).execute(eq(book), isA(RequestScope.class), any());
        verify(tx).accessUser(any());
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
        RequestScope scope = new RequestScope(null, null, tx, new User(1), null, getElideSettings(null, dictionary, MOCK_AUDIT_LOGGER),
                false);
        PersistentResource resource = PersistentResource.createObject(null, Book.class, scope, Optional.of("uuid"));
        resource.setValueChecked("title", "should not affect calls since this is create!");
        resource.setValueChecked("genre", "boring books");
        Assert.assertNotNull(resource);
        verify(book, never()).onCreateBook(scope);
        verify(book, never()).checkPermission(scope);

        scope.runQueuedPreSecurityTriggers();
        verify(book, times(1)).onCreateBook(scope);
        verify(book, never()).onDeleteBook(scope);
        verify(book, never()).onUpdateTitle(scope);
        verify(book, never()).onCreateBookPreCommit(eq(scope), any());
        verify(book, times(1)).preRead(scope);
        verify(book, never()).checkPermission(scope);

        scope.runQueuedPreCommitTriggers();
        verify(book, times(1)).preCreateBook(scope);
        verify(book, never()).preDeleteBook(scope);
        verify(book, never()).preUpdateTitle(scope);
        verify(book, times(2)).onCreateBookPreCommit(eq(scope), any());
        verify(book, times(1)).preCommitRead(scope);
        verify(book, never()).checkPermission(scope);

        scope.getPermissionExecutor().executeCommitChecks();
        verify(book, times(3)).checkPermission(scope);

        scope.runQueuedPostCommitTriggers();
        verify(book, times(1)).postCreateBook(scope);
        verify(book, never()).postDeleteBook(scope);
        verify(book, never()).postUpdateTitle(scope);
        verify(book, times(2)).onCreateBookPreCommit(eq(scope), any());
        verify(book, times(1)).postRead(scope);
        verify(book, times(3)).checkPermission(scope);
    }

    @Test
    public void testUpdate() {
        Book book = mock(Book.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);

        RequestScope scope = new RequestScope(null, null, tx, new User(1), null, getElideSettings(null, dictionary, MOCK_AUDIT_LOGGER),
                false);
        PersistentResource resource = new PersistentResource(book, null, scope.getUUIDFor(book), scope);
        resource.setValueChecked("title", "new title");
        verify(book, never()).onCreateBook(scope);
        verify(book, never()).onDeleteBook(scope);
        verify(book, times(1)).onUpdateTitle(scope);
        verify(book, times(1)).preRead(scope);
        verify(book, never()).alwaysOnUpdate();
        verify(onUpdateDeferredCallback, never()).execute(eq(book), isA(RequestScope.class), any());
        verify(onUpdateImmediateCallback, times(1)).execute(eq(book), isA(RequestScope.class), any());
        verify(book, times(1)).checkPermission(scope);

        scope.runQueuedPreSecurityTriggers();
        verify(book, never()).onCreateBook(scope);
        verify(book, never()).onDeleteBook(scope);
        verify(book, times(1)).onUpdateTitle(scope);
        verify(book, times(1)).preRead(scope);
        verify(book, never()).alwaysOnUpdate();
        verify(onUpdateDeferredCallback, never()).execute(eq(book), isA(RequestScope.class), any());
        verify(onUpdateImmediateCallback, times(1)).execute(eq(book), isA(RequestScope.class), any());
        verify(book, times(1)).checkPermission(scope);

        scope.runQueuedPreCommitTriggers();
        verify(book, never()).preCreateBook(scope);
        verify(book, never()).preDeleteBook(scope);
        verify(book, times(1)).preUpdateTitle(scope);
        verify(book, times(1)).preCommitRead(scope);
        verify(book, times(1)).alwaysOnUpdate();
        verify(onUpdateDeferredCallback, times(1)).execute(eq(book), isA(RequestScope.class), any());
        verify(onUpdateImmediateCallback, times(1)).execute(eq(book), isA(RequestScope.class), any());
        verify(book, times(1)).checkPermission(scope);

        scope.getPermissionExecutor().executeCommitChecks();
        verify(book, times(1)).checkPermission(scope);

        scope.runQueuedPostCommitTriggers();
        verify(book, never()).postCreateBook(scope);
        verify(book, never()).postDeleteBook(scope);
        verify(book, times(1)).postUpdateTitle(scope);
        verify(book, times(1)).postRead(scope);
        verify(book, times(1)).alwaysOnUpdate();
        verify(onUpdateDeferredCallback, times(1)).execute(eq(book), isA(RequestScope.class), any());
        verify(onUpdateImmediateCallback, times(1)).execute(eq(book), isA(RequestScope.class), any());
    }

    @Test
    public void testUpdateWithChangeSpec() {
        Book book = mock(Book.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);

        RequestScope scope = new RequestScope(null, null, tx , new User(1), null, getElideSettings(null, dictionary, MOCK_AUDIT_LOGGER),
                false);
        PersistentResource resource = new PersistentResource(book, null, scope.getUUIDFor(book), scope);

        //Verify changeSpec is passed to hooks
        resource.setValueChecked("genre", "new genre");

        verify(book, never()).onCreateBook(scope);
        verify(book, never()).onDeleteBook(scope);
        verify(book, times(1)).onUpdateGenre(any(RequestScope.class), any(ChangeSpec.class));
        verify(book, times(1)).preRead(scope);
        verify(book, never()).alwaysOnUpdate();
        verify(onUpdateDeferredCallback, never()).execute(eq(book), isA(RequestScope.class), any());
        verify(onUpdateImmediateCallback, times(1)).execute(eq(book), isA(RequestScope.class), any());
        verify(book, times(1)).checkPermission(scope);

        scope.runQueuedPreSecurityTriggers();
        verify(book, never()).onCreateBook(scope);
        verify(book, never()).onDeleteBook(scope);
        verify(book, times(1)).onUpdateGenre(any(RequestScope.class), any(ChangeSpec.class));
        verify(book, times(1)).preRead(scope);
        verify(book, never()).alwaysOnUpdate();
        verify(onUpdateDeferredCallback, never()).execute(eq(book), isA(RequestScope.class), any());
        verify(onUpdateImmediateCallback, times(1)).execute(eq(book), isA(RequestScope.class), any());
        verify(book, times(1)).checkPermission(scope);

        scope.runQueuedPreCommitTriggers();
        verify(book, never()).preCreateBook(scope);
        verify(book, never()).preDeleteBook(scope);
        verify(book, times(1)).preUpdateGenre(any(RequestScope.class), any(ChangeSpec.class));
        verify(book, times(1)).alwaysOnUpdate();
        verify(onUpdateDeferredCallback, times(1)).execute(eq(book), isA(RequestScope.class), any());
        verify(onUpdateImmediateCallback, times(1)).execute(eq(book), isA(RequestScope.class), any());
        verify(book, times(1)).checkPermission(scope);

        scope.getPermissionExecutor().executeCommitChecks();
        verify(book, times(1)).checkPermission(scope);

        scope.runQueuedPostCommitTriggers();
        verify(book, never()).postCreateBook(scope);
        verify(book, never()).postDeleteBook(scope);
        verify(book, times(1)).postUpdateGenre(any(RequestScope.class), any(ChangeSpec.class));
        verify(book, times(1)).postRead(scope);
        verify(book, times(1)).alwaysOnUpdate();
        verify(onUpdateDeferredCallback, times(1)).execute(eq(book), isA(RequestScope.class), any());
        verify(onUpdateImmediateCallback, times(1)).execute(eq(book), isA(RequestScope.class), any());
    }

    @Test
    public void testOnDelete() {
        Book book = mock(Book.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);

        RequestScope scope = new RequestScope(null, null, tx, new User(1), null, getElideSettings(null, dictionary, MOCK_AUDIT_LOGGER),
                false);
        PersistentResource resource = new PersistentResource(book, null, scope.getUUIDFor(book), scope);
        verify(book, never()).onCreateBook(scope);
        verify(book, never()).onDeleteBook(scope);
        verify(book, never()).onUpdateTitle(scope);
        verify(book, never()).preRead(scope);
        verify(book, never()).checkPermission(scope);

        resource.deleteResource();
        verify(book, never()).onCreateBook(scope);
        verify(book, times(1)).onDeleteBook(scope);
        verify(book, never()).onUpdateTitle(scope);
        verify(book, never()).preRead(scope);
        verify(book, times(1)).checkPermission(scope);

        scope.runQueuedPreSecurityTriggers();
        verify(book, never()).onCreateBook(scope);
        verify(book, times(1)).onDeleteBook(scope);
        verify(book, never()).onUpdateTitle(scope);
        verify(book, never()).preRead(scope);

        scope.runQueuedPreCommitTriggers();
        verify(book, never()).preCreateBook(scope);
        verify(book, times(1)).preDeleteBook(scope);
        verify(book, never()).preUpdateTitle(scope);
        verify(book, never()).preCommitRead(scope);

        scope.getPermissionExecutor().executeCommitChecks();
        verify(book, times(1)).checkPermission(scope);

        scope.runQueuedPostCommitTriggers();
        verify(book, never()).postCreateBook(scope);
        verify(book, times(1)).postDeleteBook(scope);
        verify(book, never()).postUpdateTitle(scope);
        verify(book, never()).postRead(scope);
    }

    @Test
    public void testOnRead() {
        Book book = mock(Book.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        RequestScope scope = new RequestScope(null, null, tx, new User(1), null, getElideSettings(null, dictionary, MOCK_AUDIT_LOGGER),
                false);
        PersistentResource resource = new PersistentResource(book, null, scope.getUUIDFor(book), scope);

        resource.getValueChecked("title");
        verify(book, never()).onCreateBook(scope);
        verify(book, never()).onDeleteBook(scope);
        verify(book, never()).onUpdateTitle(scope);
        verify(book, times(1)).preRead(scope);

        scope.runQueuedPreSecurityTriggers();
        verify(book, never()).onCreateBook(scope);
        verify(book, never()).onDeleteBook(scope);
        verify(book, never()).onUpdateTitle(scope);
        verify(book, times(1)).preRead(scope);

        scope.runQueuedPreCommitTriggers();
        verify(book, never()).preCreateBook(scope);
        verify(book, never()).preDeleteBook(scope);
        verify(book, never()).preUpdateTitle(scope);
        verify(book, times(1)).preCommitRead(scope);

        scope.getPermissionExecutor().executeCommitChecks();

        scope.runQueuedPostCommitTriggers();
        verify(book, never()).postCreateBook(scope);
        verify(book, never()).postDeleteBook(scope);
        verify(book, never()).postUpdateTitle(scope);
        verify(book, times(1)).postRead(scope);
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testPreSecurityLifecycleHookException() {
        @Entity
        @Include
        class Book {
            public String title;

            @OnUpdatePreSecurity(value = "title")
            public void blowUp(RequestScope scope) {
                throw new IllegalStateException();
            }
        }

        EntityDictionary dictionary = new EntityDictionary(new HashMap<>());
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        dictionary.bindEntity(Book.class);

        Book book = new Book();
        RequestScope scope = new RequestScope(null, null, tx, new User(1), null, getElideSettings(null, dictionary, MOCK_AUDIT_LOGGER),
                false);
        PersistentResource resource = new PersistentResource(book, null, "1", scope);

        resource.updateAttribute("title", "New value");
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testPreCommitLifeCycleHookException() {
        @Entity
        @Include
        class Book {
            public String title;

            @OnUpdatePreCommit(value = "title")
            public void blowUp(RequestScope scope) {
                throw new IllegalStateException();
            }
        }

        EntityDictionary dictionary = new EntityDictionary(new HashMap<>());
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        dictionary.bindEntity(Book.class);

        Book book = new Book();
        RequestScope scope = new RequestScope(null, null, tx, new User(1), null, getElideSettings(null, dictionary, MOCK_AUDIT_LOGGER),
                false);
        PersistentResource resource = new PersistentResource(book, null, "1", scope);

        resource.updateAttribute("title", "New value");

        scope.runQueuedPreCommitTriggers();
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
