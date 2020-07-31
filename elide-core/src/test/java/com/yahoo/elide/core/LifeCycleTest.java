/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import static com.yahoo.elide.Elide.JSONAPI_CONTENT_TYPE;
import static com.yahoo.elide.Elide.JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.annotation.Exclude;
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
import com.yahoo.elide.core.datastore.inmemory.HashMapDataStore;
import com.yahoo.elide.core.datastore.inmemory.InMemoryDataStore;
import com.yahoo.elide.functions.LifeCycleHook;
import com.yahoo.elide.security.ChangeSpec;
import com.yahoo.elide.security.User;
import com.yahoo.elide.security.checks.Check;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import example.Author;
import example.Book;
import example.Editor;
import example.Publisher;
import example.TestCheckMappings;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Tests the invocation &amp; sequencing of DataStoreTransaction method invocations and life cycle events.
 */
public class LifeCycleTest {

    private static final AuditLogger MOCK_AUDIT_LOGGER = mock(AuditLogger.class);
    private final String baseUrl = "http://localhost:8080/api/v1";
    private EntityDictionary dictionary;
    private MockCallback callback;
    private MockCallback onUpdateDeferredCallback;
    private MockCallback onUpdateImmediateCallback;
    private MockCallback onUpdatePostCommitCallback;
    private MockCallback onUpdatePostCommitAuthor;


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
        public Class<?> lookupBoundClass(Class<?> objClass) {
            // Special handling for mocked Book class which has Entity annotation
            if (objClass.getName().contains("$MockitoMock$")) {
                objClass = objClass.getSuperclass();
            }
            return super.lookupBoundClass(objClass);
        }

    }

    LifeCycleTest() throws Exception {
        callback = mock(MockCallback.class);
        onUpdateDeferredCallback = mock(MockCallback.class);
        onUpdateImmediateCallback = mock(MockCallback.class);
        onUpdatePostCommitCallback = mock(MockCallback.class);
        onUpdatePostCommitAuthor = mock(MockCallback.class);
        dictionary = new TestEntityDictionary(TestCheckMappings.MAPPINGS);
        dictionary.bindEntity(Book.class);
        dictionary.bindEntity(Author.class);
        dictionary.bindEntity(Publisher.class);
        dictionary.bindEntity(Editor.class);
        ImmutableList.of(
                OnCreatePostCommit.class, OnCreatePreCommit.class, OnCreatePreSecurity.class,
                OnReadPostCommit.class, OnReadPreCommit.class, OnReadPreSecurity.class,
                OnDeletePostCommit.class, OnDeletePreCommit.class, OnDeletePreSecurity.class)
                .stream().forEach(cls -> dictionary.bindTrigger(Book.class, cls, callback));
        ImmutableList.of(
                OnUpdatePostCommit.class, OnUpdatePreCommit.class, OnUpdatePreSecurity.class)
                .stream().forEach(cls -> dictionary.bindTrigger(Book.class, cls, "title", callback));
        dictionary.bindTrigger(Book.class, OnUpdatePreCommit.class, onUpdateDeferredCallback, true);
        dictionary.bindTrigger(Book.class, OnUpdatePreSecurity.class, onUpdateImmediateCallback, true);
        dictionary.bindTrigger(Book.class, OnUpdatePostCommit.class, onUpdatePostCommitCallback, true);
        dictionary.bindTrigger(Author.class, OnUpdatePostCommit.class, onUpdatePostCommitAuthor, true);
    }

    @BeforeEach
    public void clearMocks() {
        clearInvocations(onUpdatePostCommitAuthor, onUpdateImmediateCallback, onUpdatePostCommitCallback, onUpdatePostCommitAuthor);
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

        ElideResponse response = elide.post(baseUrl, "/book", bookBody, null);
        assertEquals(HttpStatus.SC_CREATED, response.getResponseCode());

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
    public void testElideCreateFailure() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        Book book = mock(Book.class);
        doThrow(RuntimeException.class).when(book).setTitle(anyString());

        Elide elide = getElide(store, dictionary, MOCK_AUDIT_LOGGER);

        String bookBody = "{\"data\": {\"type\":\"book\",\"attributes\": {\"title\":\"Grapes of Wrath\"}}}";

        when(store.beginTransaction()).thenReturn(tx);
        when(tx.createNewObject(Book.class)).thenReturn(book);

        ElideResponse response = elide.post(baseUrl, "/book", bookBody, null);
        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getResponseCode());
        assertEquals(
                "{\"errors\":[{\"detail\":\"InternalServerErrorException: Unexpected exception caught\"}]}",
                response.getBody());

        /*
         * This gets called for :
         *  - read pre-security for the book
         *  - create pre-security for the book
         *  - read pre-commit for the book
         *  - create pre-commit for the book
         *  - read post-commit for the book
         *  - create post-commit for the book
         */
        verify(callback, times(1)).execute(eq(book), isA(RequestScope.class), any());
        verify(tx).accessUser(any());
        verify(tx, never()).preCommit();
        verify(tx, never()).createObject(eq(book), isA(RequestScope.class));
        verify(tx, never()).flush(isA(RequestScope.class));
        verify(tx, never()).commit(isA(RequestScope.class));
        verify(tx).close();
    }


    @Test
    public void testElideGet() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        Book book = mock(Book.class);
        when(book.getId()).thenReturn(1L);

        Elide elide = getElide(store, dictionary, MOCK_AUDIT_LOGGER);

        when(store.beginReadTransaction()).thenCallRealMethod();
        when(store.beginTransaction()).thenReturn(tx);
        when(tx.loadObject(eq(Book.class), any(), any(), isA(RequestScope.class))).thenReturn(book);

        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        ElideResponse response = elide.get(baseUrl, "/book/1", headers, null);
        assertEquals(HttpStatus.SC_OK, response.getResponseCode());

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
    public void testElideGetRelationship() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        Book book = mock(Book.class);
        Author author = mock(Author.class);
        when(book.getId()).thenReturn(1L);
        when(author.getId()).thenReturn(2L);
        when(book.getAuthors()).thenReturn(ImmutableSet.of(author));

        Elide elide = getElide(store, dictionary, MOCK_AUDIT_LOGGER);

        when(store.beginReadTransaction()).thenCallRealMethod();
        when(store.beginTransaction()).thenReturn(tx);
        when(tx.loadObject(eq(Book.class), any(), any(), isA(RequestScope.class))).thenReturn(book);

        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        ElideResponse response = elide.get(baseUrl, "/book/1/relationships/authors", headers, null);
        assertEquals(HttpStatus.SC_OK, response.getResponseCode());

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
        when(tx.loadObject(eq(Book.class), any(), any(), isA(RequestScope.class))).thenReturn(book);

        String bookBody = "{\"data\":{\"type\":\"book\",\"id\":1,\"attributes\": {\"title\":\"Grapes of Wrath\"}}}";

        String contentType = JSONAPI_CONTENT_TYPE;
        ElideResponse response = elide.patch(baseUrl, contentType, contentType, "/book/1", bookBody, null);
        assertEquals(HttpStatus.SC_NO_CONTENT, response.getResponseCode());

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
        verify(onUpdateImmediateCallback, never()).execute(eq(book), isA(RequestScope.class), eq(Optional.empty()));
        verify(onUpdateDeferredCallback, never()).execute(eq(book), isA(RequestScope.class), eq(Optional.empty()));
        verify(tx).accessUser(any());
        verify(tx).preCommit();

        verify(tx).save(eq(book), isA(RequestScope.class));
        verify(tx).flush(isA(RequestScope.class));
        verify(tx).commit(isA(RequestScope.class));
        verify(tx).close();
    }

    @Test
    public void testElidePatchFailure() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        Book book = mock(Book.class);

        Elide elide = getElide(store, dictionary, MOCK_AUDIT_LOGGER);

        when(book.getId()).thenReturn(1L);
        when(store.beginTransaction()).thenReturn(tx);
        when(tx.loadObject(eq(Book.class), any(), any(), isA(RequestScope.class))).thenReturn(book);
        doThrow(ConstraintViolationException.class).when(tx).flush(any());

        String bookBody = "{\"data\":{\"type\":\"book\",\"id\":1,\"attributes\": {\"title\":\"Grapes of Wrath\"}}}";

        String contentType = JSONAPI_CONTENT_TYPE;
        ElideResponse response = elide.patch(baseUrl, contentType, contentType, "/book/1", bookBody, null);
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getResponseCode());
        assertEquals(
                "{\"errors\":[{\"detail\":\"Constraint violation\"}]}",
                response.getBody());

        /*
         * This gets called for :
         *  - read pre-security for the book
         *  - update pre-security for the book.title
         *  - read pre-commit for the book
         *  - update pre-commit for the book.title
         *  - read post-commit for the book
         *  - update post-commit for the book.title
         */
        verify(callback, times(2)).execute(eq(book), isA(RequestScope.class), any());
        verify(onUpdateImmediateCallback, times(1)).execute(eq(book), isA(RequestScope.class), any());
        verify(onUpdateDeferredCallback, never()).execute(eq(book), isA(RequestScope.class), any());
        verify(onUpdateImmediateCallback, never()).execute(eq(book), isA(RequestScope.class), eq(Optional.empty()));
        verify(onUpdateDeferredCallback, never()).execute(eq(book), isA(RequestScope.class), eq(Optional.empty()));
        verify(tx).accessUser(any());
        verify(tx, times(1)).preCommit();

        verify(tx, times(1)).save(eq(book), isA(RequestScope.class));
        verify(tx, times(1)).flush(isA(RequestScope.class));
        verify(tx, never()).commit(isA(RequestScope.class));
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
        when(tx.loadObject(eq(Book.class), any(), any(), isA(RequestScope.class))).thenReturn(book);

        ElideResponse response = elide.delete(baseUrl, "/book/1", "", null);
        assertEquals(HttpStatus.SC_NO_CONTENT, response.getResponseCode());

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
    public void testElidePatchExtensionCreate() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        Book book = mock(Book.class);

        Elide elide = getElide(store, dictionary, MOCK_AUDIT_LOGGER);

        String bookBody = "[{\"op\": \"add\",\"path\": \"/book\",\"value\":{"
                + "\"type\":\"book\",\"id\": \"A\",\"attributes\": {\"title\":\"Grapes of Wrath\"}}}]";

        when(store.beginTransaction()).thenReturn(tx);
        when(tx.createNewObject(Book.class)).thenReturn(book);

        String contentType = JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION;
        ElideResponse response = elide.patch(baseUrl, contentType, contentType, "/", bookBody, null);
        assertEquals(HttpStatus.SC_OK, response.getResponseCode());

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
    public void failElidePatchExtensionCreate() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        Book book = mock(Book.class);

        Elide elide = getElide(store, dictionary, MOCK_AUDIT_LOGGER);

        String bookBody = "[{\"op\": \"add\",\"path\": \"/book\",\"value\":{"
                + "\"type\":\"book\",\"attributes\": {\"title\":\"Grapes of Wrath\"}}}]";

        when(store.beginTransaction()).thenReturn(tx);
        when(tx.createNewObject(Book.class)).thenReturn(book);

        String contentType = JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION;
        ElideResponse response = elide.patch(baseUrl, contentType, contentType, "/", bookBody, null);
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getResponseCode());
        assertEquals(
                "{\"errors\":[{\"detail\":\"JsonPatchExtensionException\"}]}",
                response.getBody());

        verify(callback, never()).execute(eq(book), isA(RequestScope.class), any());
        verify(tx).accessUser(any());
        verify(tx, never()).preCommit();
        verify(tx, never()).flush(isA(RequestScope.class));
        verify(tx, never()).commit(isA(RequestScope.class));
        verify(tx).close();
    }

    @Test
    public void testElidePatchExtensionUpdate() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        Book book = mock(Book.class);

        Elide elide = getElide(store, dictionary, MOCK_AUDIT_LOGGER);

        when(book.getId()).thenReturn(1L);
        when(store.beginTransaction()).thenReturn(tx);
        when(tx.loadObject(eq(Book.class), any(), any(), isA(RequestScope.class))).thenReturn(book);

        String bookBody = "[{\"op\": \"replace\",\"path\": \"/book/1\",\"value\":{"
                + "\"type\":\"book\",\"id\":1,\"attributes\": {\"title\":\"Grapes of Wrath\"}}}]";

        String contentType = JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION;
        ElideResponse response = elide.patch(baseUrl, contentType, contentType, "/", bookBody, null);
        assertEquals(HttpStatus.SC_OK, response.getResponseCode());
        assertEquals("[{\"data\":null}]", response.getBody());

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
        verify(onUpdateImmediateCallback, never()).execute(eq(book), isA(RequestScope.class), eq(Optional.empty()));
        verify(onUpdateDeferredCallback, never()).execute(eq(book), isA(RequestScope.class), eq(Optional.empty()));
        verify(tx).accessUser(any());
        verify(tx).preCommit();

        verify(tx).save(eq(book), isA(RequestScope.class));
        verify(tx).flush(isA(RequestScope.class));
        verify(tx).commit(isA(RequestScope.class));
        verify(tx).close();
    }

    @Test
    public void testElidePatchExtensionDelete() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        Book book = mock(Book.class);

        Elide elide = getElide(store, dictionary, MOCK_AUDIT_LOGGER);

        when(book.getId()).thenReturn(1L);
        when(store.beginTransaction()).thenReturn(tx);
        when(tx.loadObject(eq(Book.class), any(), any(), isA(RequestScope.class))).thenReturn(book);

        String bookBody = "[{\"op\": \"remove\",\"path\": \"/book\",\"value\":{"
                + "\"type\":\"book\",\"id\": \"1\"}}]";

        String contentType = JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION;
        ElideResponse response = elide.patch(baseUrl, contentType, contentType, "/", bookBody, null);
        assertEquals(HttpStatus.SC_OK, response.getResponseCode());

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
        RequestScope scope = buildRequestScope(dictionary, tx);
        PersistentResource resource = PersistentResource.createObject(null, Book.class, scope, Optional.of("uuid"));
        resource.setValueChecked("title", "should not affect calls since this is create!");
        resource.setValueChecked("genre", "boring books");
        assertNotNull(resource);
        verify(book, never()).onCreatePreSecurity(scope);
        verify(book, never()).checkPermission(scope);

        scope.runQueuedPreSecurityTriggers();
        verify(book, times(1)).onCreatePreSecurity(scope);
        verify(book, never()).onDeletePreSecurity(scope);
        verify(book, never()).onUpdatePreSecurityTitle(scope);
        verify(book, never()).onCreatePreCommitStar(eq(scope), any());
        verify(book, times(1)).onReadPreSecurity(scope);
        verify(book, never()).checkPermission(scope);

        scope.runQueuedPreCommitTriggers();
        verify(book, times(1)).onCreatePreCommit(scope);
        verify(book, never()).onDeletePreCommit(scope);
        verify(book, never()).onUpdatePreCommitTitle(scope);
        verify(book, times(2)).onCreatePreCommitStar(eq(scope), any());
        verify(book, times(1)).onReadPreCommitTitle(scope);
        verify(book, never()).checkPermission(scope);

        scope.getPermissionExecutor().executeCommitChecks();
        verify(book, times(3)).checkPermission(scope);

        scope.runQueuedPostCommitTriggers();
        verify(book, times(1)).onCreatePostCommit(scope);
        verify(book, never()).onDeletePostCommit(scope);
        verify(book, never()).onUpdatePostCommitTitle(scope);
        verify(book, times(2)).onCreatePreCommitStar(eq(scope), any());
        verify(book, times(1)).onReadPostCommit(scope);
        verify(book, times(3)).checkPermission(scope);
    }

    @Test
    public void testUpdate() {
        Book book = mock(Book.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);

        RequestScope scope = buildRequestScope(dictionary, tx);
        PersistentResource resource = new PersistentResource(book, null, scope.getUUIDFor(book), scope);
        resource.setValueChecked("title", "new title");
        verify(book, never()).onCreatePreSecurity(scope);
        verify(book, never()).onDeletePreSecurity(scope);
        verify(book, times(1)).onUpdatePreSecurityTitle(scope);
        verify(book, times(1)).onReadPreSecurity(scope);
        verify(book, never()).onUpdatePreCommit();
        verify(onUpdateDeferredCallback, never()).execute(eq(book), isA(RequestScope.class), any());
        verify(onUpdateImmediateCallback, times(1)).execute(eq(book), isA(RequestScope.class), any());
        verify(book, times(1)).checkPermission(scope);

        scope.runQueuedPreSecurityTriggers();
        verify(book, never()).onCreatePreSecurity(scope);
        verify(book, never()).onDeletePreSecurity(scope);
        verify(book, times(1)).onUpdatePreSecurityTitle(scope);
        verify(book, times(1)).onReadPreSecurity(scope);
        verify(book, never()).onUpdatePreCommit();
        verify(onUpdateDeferredCallback, never()).execute(eq(book), isA(RequestScope.class), any());
        verify(onUpdateImmediateCallback, times(1)).execute(eq(book), isA(RequestScope.class), any());
        verify(book, times(1)).checkPermission(scope);

        scope.runQueuedPreCommitTriggers();
        verify(book, never()).onCreatePreCommit(scope);
        verify(book, never()).onDeletePreCommit(scope);
        verify(book, times(1)).onUpdatePreCommitTitle(scope);
        verify(book, times(1)).onReadPreCommitTitle(scope);
        verify(book, times(1)).onUpdatePreCommit();
        verify(onUpdateDeferredCallback, times(1)).execute(eq(book), isA(RequestScope.class), any());
        verify(onUpdateImmediateCallback, times(1)).execute(eq(book), isA(RequestScope.class), any());
        verify(book, times(1)).checkPermission(scope);

        scope.getPermissionExecutor().executeCommitChecks();
        verify(book, times(1)).checkPermission(scope);

        scope.runQueuedPostCommitTriggers();
        verify(book, never()).onCreatePostCommit(scope);
        verify(book, never()).onDeletePostCommit(scope);
        verify(book, times(1)).onUpdatePostCommitTitle(scope);
        verify(book, times(1)).onReadPostCommit(scope);
        verify(book, times(1)).onUpdatePreCommit();
        verify(onUpdateDeferredCallback, times(1)).execute(eq(book), isA(RequestScope.class), any());
        verify(onUpdateImmediateCallback, times(1)).execute(eq(book), isA(RequestScope.class), any());
        verify(onUpdatePostCommitAuthor, never()).execute(any(), isA(RequestScope.class), any());

        // verify no empty callbacks
        verifyNoEmptyCallbacks();
    }

    @Test
    public void testUpdateWithChangeSpec() {
        Book book = mock(Book.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);

        RequestScope scope = buildRequestScope(dictionary, tx);
        PersistentResource resource = new PersistentResource(book, null, scope.getUUIDFor(book), scope);

        verify(book, never()).onCreatePreSecurity(scope);
        verify(book, never()).onDeletePreSecurity(scope);
        verify(book, never()).onUpdatePreSecurityGenre(any(RequestScope.class), any(ChangeSpec.class));
        verify(book, never()).onUpdatePreSecurityGenre(any(RequestScope.class), isNull());
        verify(book, never()).onReadPreSecurity(scope);
        verify(book, never()).onUpdatePreCommit();
        verify(onUpdateDeferredCallback, never()).execute(eq(book), isA(RequestScope.class), any());
        verify(onUpdateImmediateCallback, never()).execute(eq(book), isA(RequestScope.class), any());
        verify(onUpdatePostCommitCallback, never()).execute(eq(book), isA(RequestScope.class), any());
        verify(book, never()).checkPermission(scope);

        //Verify changeSpec is passed to hooks
        resource.setValueChecked("genre", "new genre");

        verify(book, never()).onCreatePreSecurity(scope);
        verify(book, never()).onDeletePreSecurity(scope);
        verify(book, times(1)).onUpdatePreSecurityGenre(any(RequestScope.class), any(ChangeSpec.class));
        verify(book, times(1)).onReadPreSecurity(scope);
        verify(book, never()).onUpdatePreCommit();
        verify(onUpdateDeferredCallback, never()).execute(eq(book), isA(RequestScope.class), any());
        verify(onUpdateImmediateCallback, times(1)).execute(eq(book), isA(RequestScope.class), any());
        verify(book, times(1)).checkPermission(scope);

        scope.runQueuedPreSecurityTriggers();
        verify(book, never()).onCreatePreSecurity(scope);
        verify(book, never()).onDeletePreSecurity(scope);
        verify(book, times(1)).onUpdatePreSecurityGenre(any(RequestScope.class), any(ChangeSpec.class));
        verify(book, times(1)).onReadPreSecurity(scope);
        verify(book, never()).onUpdatePreCommit();
        verify(onUpdateDeferredCallback, never()).execute(eq(book), isA(RequestScope.class), any());
        verify(onUpdateImmediateCallback, times(1)).execute(eq(book), isA(RequestScope.class), any());
        verify(book, times(1)).checkPermission(scope);

        scope.runQueuedPreCommitTriggers();
        verify(book, never()).onCreatePreCommit(scope);
        verify(book, never()).onDeletePreCommit(scope);
        verify(book, times(1)).onUpdatePreCommitGenre(any(RequestScope.class), any(ChangeSpec.class));
        verify(book, times(1)).onUpdatePreCommit();
        verify(onUpdateDeferredCallback, times(1)).execute(eq(book), isA(RequestScope.class), any());
        verify(onUpdateImmediateCallback, times(1)).execute(eq(book), isA(RequestScope.class), any());
        verify(book, times(1)).checkPermission(scope);

        scope.getPermissionExecutor().executeCommitChecks();
        verify(book, times(1)).checkPermission(scope);

        scope.runQueuedPostCommitTriggers();
        verify(book, never()).onCreatePostCommit(scope);
        verify(book, never()).onDeletePostCommit(scope);
        verify(book, times(1)).onUpdatePostCommitGenre(any(RequestScope.class), any(ChangeSpec.class));
        verify(book, times(1)).onReadPostCommit(scope);
        verify(book, times(1)).onUpdatePreCommit();
        verify(onUpdateDeferredCallback, times(1)).execute(eq(book), isA(RequestScope.class), any());
        verify(onUpdateImmediateCallback, times(1)).execute(eq(book), isA(RequestScope.class), any());
        verify(onUpdatePostCommitCallback, times(1)).execute(eq(book), isA(RequestScope.class), any());
        verify(onUpdatePostCommitAuthor, never()).execute(isA(Author.class), isA(RequestScope.class), any());

        // verify no empty callbacks
        verifyNoEmptyCallbacks();
    }

    @Test
    public void testMultipleUpdateWithChangeSpec() {
        Book book = mock(Book.class);

        DataStoreTransaction tx = mock(DataStoreTransaction.class);

        RequestScope scope = buildRequestScope(dictionary, tx);
        PersistentResource resource = new PersistentResource(book, null, scope.getUUIDFor(book), scope);

        verify(book, never()).onCreatePreSecurity(scope);
        verify(book, never()).onDeletePreSecurity(scope);
        verify(book, never()).onUpdatePreSecurityGenre(any(RequestScope.class), any(ChangeSpec.class));
        verify(book, never()).onUpdatePreSecurityGenre(any(RequestScope.class), isNull());
        verify(book, never()).onReadPreSecurity(scope);
        verify(book, never()).onUpdatePreCommit();
        verify(onUpdateDeferredCallback, never()).execute(eq(book), isA(RequestScope.class), any());
        verify(onUpdateImmediateCallback, never()).execute(eq(book), isA(RequestScope.class), any());
        verify(onUpdatePostCommitCallback, never()).execute(eq(book), isA(RequestScope.class), any());
        verify(book, never()).checkPermission(scope);

        // Verify changeSpec is passed to hooks
        resource.setValueChecked("genre", "new genre");
        resource.setValueChecked("title", "new title");

        verify(book, never()).onCreatePreSecurity(scope);
        verify(book, never()).onDeletePreSecurity(scope);
        verify(book, times(1)).onUpdatePreSecurityGenre(any(RequestScope.class), any(ChangeSpec.class));
        verify(book, never()).onUpdatePreSecurityGenre(any(RequestScope.class), isNull());
        verify(book, times(1)).onReadPreSecurity(scope);
        verify(book, never()).onUpdatePreCommit();
        verify(onUpdateDeferredCallback, never()).execute(eq(book), isA(RequestScope.class), any());
        verify(onUpdateImmediateCallback, times(2)).execute(eq(book), isA(RequestScope.class), any());
        verify(onUpdatePostCommitCallback, never()).execute(eq(book), isA(RequestScope.class), any());
        verify(book, times(2)).checkPermission(scope);

        scope.runQueuedPreSecurityTriggers();
        verify(book, never()).onCreatePreSecurity(scope);
        verify(book, never()).onDeletePreSecurity(scope);
        verify(book, times(1)).onUpdatePreSecurityGenre(any(RequestScope.class), any(ChangeSpec.class));
        verify(book, times(1)).onReadPreSecurity(scope);
        verify(book, never()).onUpdatePreCommit();
        verify(onUpdateDeferredCallback, never()).execute(eq(book), isA(RequestScope.class), any());
        verify(onUpdateImmediateCallback, times(2)).execute(eq(book), isA(RequestScope.class), any());
        verify(onUpdatePostCommitCallback, never()).execute(eq(book), isA(RequestScope.class), any());
        verify(book, times(2)).checkPermission(scope);

        scope.runQueuedPreCommitTriggers();
        verify(book, never()).onCreatePreCommit(scope);
        verify(book, never()).onDeletePreCommit(scope);
        verify(book, times(1)).onUpdatePreCommitGenre(any(RequestScope.class), any(ChangeSpec.class));
        verify(book, times(1)).onUpdatePreCommit();
        verify(onUpdateDeferredCallback, times(2)).execute(eq(book), isA(RequestScope.class), any());
        verify(onUpdateImmediateCallback, times(2)).execute(eq(book), isA(RequestScope.class), any());
        verify(onUpdatePostCommitCallback, never()).execute(eq(book), isA(RequestScope.class), any());
        verify(book, times(2)).checkPermission(scope);

        scope.getPermissionExecutor().executeCommitChecks();
        verify(book, times(2)).checkPermission(scope);

        scope.runQueuedPostCommitTriggers();
        verify(book, never()).onCreatePostCommit(scope);
        verify(book, never()).onDeletePostCommit(scope);
        verify(book, times(1)).onUpdatePostCommitGenre(any(RequestScope.class), any(ChangeSpec.class));
        verify(book, times(1)).onReadPostCommit(scope);
        verify(book, times(1)).onUpdatePreCommit();
        verify(onUpdateDeferredCallback, times(2)).execute(eq(book), isA(RequestScope.class), any());
        verify(onUpdateImmediateCallback, times(2)).execute(eq(book), isA(RequestScope.class), any());
        verify(onUpdatePostCommitCallback, times(2)).execute(eq(book), isA(RequestScope.class), any());
        verify(onUpdatePostCommitAuthor, never()).execute(any(), isA(RequestScope.class), any());

        verifyNoEmptyCallbacks();
    }

    @Test
    public void testUpdateRelationshipWithChangeSpec() {
        Book book = new Book();
        Author author = new Author();
        book.setAuthors(Sets.newHashSet(author));
        author.setBooks(Sets.newHashSet(book));
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        when(tx.getRelation(any(), eq(author), eq("books"), any(), any(), any(), any())).then((i) -> author.getBooks());
        when(tx.getRelation(any(), eq(book), eq("authors"), any(), any(), any(), any())).then((i) -> book.getAuthors());

        RequestScope scope = buildRequestScope(dictionary, tx);
        PersistentResource<Author> resourceBook = new PersistentResource(book, null, scope.getUUIDFor(book), scope);
        PersistentResource<Author> resourceAuthor = new PersistentResource(author, null, scope.getUUIDFor(book), scope);

        verify(onUpdateDeferredCallback, never()).execute(eq(book), isA(RequestScope.class), any());
        verify(onUpdateImmediateCallback, never()).execute(eq(book), isA(RequestScope.class), any());
        verify(onUpdatePostCommitCallback, never()).execute(eq(book), isA(RequestScope.class), any());
        verify(onUpdatePostCommitAuthor, never()).execute(eq(book), isA(RequestScope.class), any());

        //Verify changeSpec is passed to hooks
        resourceAuthor.removeRelation("books", resourceBook);

        scope.runQueuedPreSecurityTriggers();
        verify(onUpdateDeferredCallback, never()).execute(eq(book), isA(RequestScope.class), any());
        verify(onUpdateImmediateCallback, times(1)).execute(eq(book), isA(RequestScope.class), any());

        scope.runQueuedPreCommitTriggers();
        verify(onUpdateDeferredCallback, times(1)).execute(eq(book), isA(RequestScope.class), any());

        scope.getPermissionExecutor().executeCommitChecks();
        verify(onUpdatePostCommitCallback, never()).execute(eq(book), isA(RequestScope.class), any());
        verify(onUpdatePostCommitAuthor, never()).execute(eq(book), isA(RequestScope.class), any());

        scope.runQueuedPostCommitTriggers();
        verify(onUpdateDeferredCallback, times(1)).execute(eq(book), isA(RequestScope.class), any());
        verify(onUpdateImmediateCallback, times(1)).execute(eq(book), isA(RequestScope.class), any());
        verify(onUpdatePostCommitCallback, times(1)).execute(eq(book), isA(RequestScope.class), any());
        verify(onUpdatePostCommitAuthor, times(1)).execute(eq(author), isA(RequestScope.class), any());

        verifyNoEmptyCallbacks();
    }

    @Test
    public void testOnDelete() {
        Book book = mock(Book.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);

        RequestScope scope = buildRequestScope(dictionary, tx);
        PersistentResource resource = new PersistentResource(book, null, scope.getUUIDFor(book), scope);
        verify(book, never()).onCreatePreSecurity(scope);
        verify(book, never()).onDeletePreSecurity(scope);
        verify(book, never()).onUpdatePreSecurityTitle(scope);
        verify(book, never()).onReadPreSecurity(scope);
        verify(book, never()).checkPermission(scope);

        resource.deleteResource();
        verify(book, never()).onCreatePreSecurity(scope);
        verify(book, times(1)).onDeletePreSecurity(scope);
        verify(book, never()).onUpdatePreSecurityTitle(scope);
        verify(book, never()).onReadPreSecurity(scope);
        verify(book, times(1)).checkPermission(scope);

        scope.runQueuedPreSecurityTriggers();
        verify(book, never()).onCreatePreSecurity(scope);
        verify(book, times(1)).onDeletePreSecurity(scope);
        verify(book, never()).onUpdatePreSecurityTitle(scope);
        verify(book, never()).onReadPreSecurity(scope);

        scope.runQueuedPreCommitTriggers();
        verify(book, never()).onCreatePreCommit(scope);
        verify(book, times(1)).onDeletePreCommit(scope);
        verify(book, never()).onUpdatePreCommitTitle(scope);
        verify(book, never()).onReadPreCommitTitle(scope);

        scope.getPermissionExecutor().executeCommitChecks();
        verify(book, times(1)).checkPermission(scope);

        scope.runQueuedPostCommitTriggers();
        verify(book, never()).onCreatePostCommit(scope);
        verify(book, times(1)).onDeletePostCommit(scope);
        verify(book, never()).onUpdatePostCommitTitle(scope);
        verify(book, never()).onReadPostCommit(scope);
    }

    @Test
    public void testOnRead() {
        Book book = mock(Book.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        RequestScope scope = buildRequestScope(dictionary, tx);
        PersistentResource resource = new PersistentResource(book, null, scope.getUUIDFor(book), scope);

        resource.getValueChecked("title");
        verify(book, never()).onCreatePreSecurity(scope);
        verify(book, never()).onDeletePreSecurity(scope);
        verify(book, never()).onUpdatePreSecurityTitle(scope);
        verify(book, times(1)).onReadPreSecurity(scope);

        scope.runQueuedPreSecurityTriggers();
        verify(book, never()).onCreatePreSecurity(scope);
        verify(book, never()).onDeletePreSecurity(scope);
        verify(book, never()).onUpdatePreSecurityTitle(scope);
        verify(book, times(1)).onReadPreSecurity(scope);

        scope.runQueuedPreCommitTriggers();
        verify(book, never()).onCreatePreCommit(scope);
        verify(book, never()).onDeletePreCommit(scope);
        verify(book, never()).onUpdatePreCommitTitle(scope);
        verify(book, times(1)).onReadPreCommitTitle(scope);

        scope.getPermissionExecutor().executeCommitChecks();

        scope.runQueuedPostCommitTriggers();
        verify(book, never()).onCreatePostCommit(scope);
        verify(book, never()).onDeletePostCommit(scope);
        verify(book, never()).onUpdatePostCommitTitle(scope);
        verify(book, times(1)).onReadPostCommit(scope);
    }

    @Test
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
        RequestScope scope = buildRequestScope(dictionary, tx);
        PersistentResource resource = new PersistentResource(book, null, "1", scope);

        assertThrows(IllegalStateException.class, () -> resource.updateAttribute("title", "New value"));
    }

    @Test
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
        RequestScope scope = buildRequestScope(dictionary, tx);
        PersistentResource resource = new PersistentResource(book, null, "1", scope);

        resource.updateAttribute("title", "New value");

        assertThrows(IllegalStateException.class, () -> scope.runQueuedPreCommitTriggers());
    }

    /**
     * Tests that Entities that use field level access (as opposed to properties)
     * can register read hooks on the entity class.
     */
    @Test
    public void testReadHookOnEntityFields() {
        @Entity
        @Include
        class Book {
            @Id
            private String id;
            private String title;

            @Exclude
            @Transient
            private int readPreSecurityInvoked = 0;

            @Exclude
            @Transient
            private int readPreCommitInvoked = 0;

            @Exclude
            @Transient
            private int readPostCommitInvoked = 0;

            @OnReadPreSecurity("title")
            public void readPreSecurity(RequestScope scope) {
                readPreSecurityInvoked++;
            }

            @OnReadPreCommit("title")
            public void readPreCommit(RequestScope scope) {
                readPreCommitInvoked++;
            }

            @OnReadPostCommit("title")
            public void readPostCommit(RequestScope scope) {
                readPostCommitInvoked++;
            }
        }

        EntityDictionary dictionary = new EntityDictionary(new HashMap<>());
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        dictionary.bindEntity(Book.class);

        Book book = new Book();
        RequestScope scope = buildRequestScope(dictionary, tx);
        PersistentResource resource = new PersistentResource(book, null, "1", scope);

        resource.getAttribute("title");

        assertEquals(1, book.readPreSecurityInvoked);
        assertEquals(0, book.readPreCommitInvoked);
        scope.runQueuedPreCommitTriggers();
        assertEquals(1, book.readPreCommitInvoked);
        assertEquals(0, book.readPostCommitInvoked);
        scope.runQueuedPostCommitTriggers();
        assertEquals(1, book.readPreSecurityInvoked);
        assertEquals(1, book.readPreCommitInvoked);
        assertEquals(1, book.readPostCommitInvoked);
    }

    /**
     * Tests that Entities that use field level access (as opposed to properties)
     * can register update hooks on the entity class.
     */
    @Test
    public void testUpdateHookOnEntityFields() {
        @Entity
        @Include
        class Book {
            @Id
            private String id;
            private String title;

            @Exclude
            @Transient
            private int updatePreSecurityInvoked = 0;

            @Exclude
            @Transient
            private int updatePreCommitInvoked = 0;

            @Exclude
            @Transient
            private int updatePostCommitInvoked = 0;

            @OnUpdatePreSecurity("title")
            public void updatePreSecurity(RequestScope scope) {
                updatePreSecurityInvoked++;
            }

            @OnUpdatePreCommit("title")
            public void updatePreCommit(RequestScope scope) {
                updatePreCommitInvoked++;
            }

            @OnUpdatePostCommit("title")
            public void updatePostCommit(RequestScope scope) {
                updatePostCommitInvoked++;
            }
        }

        EntityDictionary dictionary = new EntityDictionary(new HashMap<>());
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        dictionary.bindEntity(Book.class);

        Book book = new Book();
        RequestScope scope = buildRequestScope(dictionary, tx);
        PersistentResource resource = new PersistentResource(book, null, "1", scope);

        resource.updateAttribute("title", "foo");

        assertEquals(1, book.updatePreSecurityInvoked);
        assertEquals(0, book.updatePreCommitInvoked);
        scope.runQueuedPreCommitTriggers();
        assertEquals(1, book.updatePreCommitInvoked);
        assertEquals(0, book.updatePostCommitInvoked);
        scope.runQueuedPostCommitTriggers();
        assertEquals(1, book.updatePreSecurityInvoked);
        assertEquals(1, book.updatePreCommitInvoked);
        assertEquals(1, book.updatePostCommitInvoked);
    }

    /**
     * Tests that Entities that use field level access (as opposed to properties)
     * can register create hooks on the entity class.
     */
    @Test
    public void testCreateHookOnEntityFields() {
        @Entity
        @Include
        class Book {
            @Id
            private String id;
            private String title;

            @Exclude
            @Transient
            private int createPreCommitInvoked = 0;

            @Exclude
            @Transient
            private int createPostCommitInvoked = 0;

            @Exclude
            @Transient
            private int createPreSecurityInvoked = 0;

            @OnCreatePreSecurity
            public void createPreSecurity(RequestScope scope) {
                createPreSecurityInvoked++;
            }

            @OnCreatePreCommit("title")
            public void createPreCommit(RequestScope scope) {
                createPreCommitInvoked++;
            }

            @OnCreatePostCommit("title")
            public void createPostCommit(RequestScope scope) {
                createPostCommitInvoked++;
            }
        }

        EntityDictionary dictionary = new EntityDictionary(new HashMap<>());
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        dictionary.bindEntity(Book.class);

        Book book = new Book();
        when(tx.createNewObject(Book.class)).thenReturn(book);
        RequestScope scope = buildRequestScope(dictionary, tx);
        PersistentResource bookResource = PersistentResource.createObject(null, Book.class, scope, Optional.of("123"));
        bookResource.updateAttribute("title", "Foo");

        assertEquals(0, book.createPreSecurityInvoked);
        scope.runQueuedPreSecurityTriggers();
        assertEquals(1, book.createPreSecurityInvoked);
        assertEquals(0, book.createPreCommitInvoked);
        scope.runQueuedPreCommitTriggers();
        assertEquals(1, book.createPreCommitInvoked);
        assertEquals(0, book.createPostCommitInvoked);
        scope.runQueuedPostCommitTriggers();
        assertEquals(1, book.createPreSecurityInvoked);
        assertEquals(1, book.createPreCommitInvoked);
        assertEquals(1, book.createPostCommitInvoked);
    }

    /**
     * Tests that Entities that use field level access (as opposed to properties)
     * can register delete hooks on the entity class.
     */
    @Test
    public void testDeleteHookOnEntityFields() {
        @Entity
        @Include
        class Book {
            @Id
            private String id;
            private String title;

            @Exclude
            @Transient
            private int deletePreSecurityInvoked = 0;

            @Exclude
            @Transient
            private int deletePreCommitInvoked = 0;

            @Exclude
            @Transient
            private int deletePostCommitInvoked = 0;

            @OnDeletePreSecurity
            public void deletePreSecurity(RequestScope scope) {
                deletePreSecurityInvoked++;
            }

            @OnDeletePreCommit
            public void deletePreCommit(RequestScope scope) {
                deletePreCommitInvoked++;
            }

            @OnDeletePostCommit
            public void deletePostCommit(RequestScope scope) {
                deletePostCommitInvoked++;
            }
        }

        EntityDictionary dictionary = new EntityDictionary(new HashMap<>());
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        dictionary.bindEntity(Book.class);

        Book book = new Book();

        RequestScope scope = buildRequestScope(dictionary, tx);
        PersistentResource resource = new PersistentResource(book, null, "1", scope);

        resource.deleteResource();

        assertEquals(1, book.deletePreSecurityInvoked);
        assertEquals(0, book.deletePreCommitInvoked);
        scope.runQueuedPreCommitTriggers();
        assertEquals(1, book.deletePreCommitInvoked);
        assertEquals(0, book.deletePostCommitInvoked);
        scope.runQueuedPostCommitTriggers();
        assertEquals(1, book.deletePreSecurityInvoked);
        assertEquals(1, book.deletePreCommitInvoked);
        assertEquals(1, book.deletePostCommitInvoked);
    }

    /**
     * Tests that Update lifecycle hooks are triggered when a relationship collection has elements added.
     */
    @Test
    public void testAddToCollectionTrigger() {
        HashMapDataStore wrapped = new HashMapDataStore(Book.class.getPackage());
        InMemoryDataStore store = new InMemoryDataStore(wrapped);
        HashMap<String, Class<? extends Check>> checkMappings = new HashMap<>();
        checkMappings.put("Book operation check", Book.BookOperationCheck.class);
        checkMappings.put("Field path editor check", Editor.FieldPathFilterExpression.class);
        store.populateEntityDictionary(new EntityDictionary(checkMappings));
        DataStoreTransaction tx = store.beginTransaction();

        RequestScope scope = buildRequestScope(wrapped.getDictionary(), tx);
        PersistentResource publisherResource = PersistentResource.createObject(null, Publisher.class, scope, Optional.of("1"));
        PersistentResource book1Resource = PersistentResource.createObject(publisherResource, Book.class, scope, Optional.of("1"));
        publisherResource.updateRelation("books", new HashSet<>(Arrays.asList(book1Resource)));

        scope.runQueuedPreCommitTriggers();
        tx.save(publisherResource.getObject(), scope);
        tx.save(book1Resource.getObject(), scope);
        tx.commit(scope);

        Publisher publisher = (Publisher) publisherResource.getObject();

        /* Only the creat hooks should be triggered */
        assertFalse(publisher.isUpdateHookInvoked());

        scope = buildRequestScope(wrapped.getDictionary(), tx);

        PersistentResource book2Resource = PersistentResource.createObject(publisherResource, Book.class, scope, Optional.of("2"));
        publisherResource = PersistentResource.loadRecord(Publisher.class, "1", scope);
        publisherResource.addRelation("books", book2Resource);

        scope.runQueuedPreCommitTriggers();

        publisher = (Publisher) publisherResource.getObject();
        assertTrue(publisher.isUpdateHookInvoked());
    }

    /**
     * Tests that Update lifecycle hooks are triggered when a relationship collection has elements removed.
     */
    @Test
    public void testRemoveFromCollectionTrigger() {
        HashMapDataStore wrapped = new HashMapDataStore(Book.class.getPackage());
        InMemoryDataStore store = new InMemoryDataStore(wrapped);
        HashMap<String, Class<? extends Check>> checkMappings = new HashMap<>();
        checkMappings.put("Book operation check", Book.BookOperationCheck.class);
        checkMappings.put("Field path editor check", Editor.FieldPathFilterExpression.class);
        store.populateEntityDictionary(new EntityDictionary(checkMappings));
        DataStoreTransaction tx = store.beginTransaction();

        RequestScope scope = buildRequestScope(wrapped.getDictionary(), tx);

        PersistentResource publisherResource = PersistentResource.createObject(null, Publisher.class, scope, Optional.of("1"));
        PersistentResource book1Resource = PersistentResource.createObject(publisherResource, Book.class, scope, Optional.of("1"));
        PersistentResource book2Resource = PersistentResource.createObject(publisherResource, Book.class, scope, Optional.of("2"));
        publisherResource.updateRelation("books", new HashSet<>(Arrays.asList(book1Resource, book2Resource)));

        scope.runQueuedPreCommitTriggers();
        tx.save(publisherResource.getObject(), scope);
        tx.save(book1Resource.getObject(), scope);
        tx.commit(scope);

        Publisher publisher = (Publisher) publisherResource.getObject();

        /* Only the creat hooks should be triggered */
        assertFalse(publisher.isUpdateHookInvoked());

        scope = buildRequestScope(wrapped.getDictionary(), tx);

        book2Resource = PersistentResource.createObject(publisherResource, Book.class, scope, Optional.of("2"));
        publisherResource = PersistentResource.loadRecord(Publisher.class, "1", scope);
        publisherResource.updateRelation("books", new HashSet<>(Arrays.asList(book2Resource)));

        scope.runQueuedPreCommitTriggers();

        publisher = (Publisher) publisherResource.getObject();
        assertTrue(publisher.isUpdateHookInvoked());
    }

    private Elide getElide(DataStore dataStore, EntityDictionary dictionary, AuditLogger auditLogger) {
        return new Elide(getElideSettings(dataStore, dictionary, auditLogger));
    }

    private ElideSettings getElideSettings(DataStore dataStore, EntityDictionary dictionary, AuditLogger auditLogger) {
        return new ElideSettingsBuilder(dataStore)
                .withEntityDictionary(dictionary)
                .withAuditLogger(auditLogger)
                .withReturnErrorObjects(true)
                .build();
    }

    private void verifyNoEmptyCallbacks() {
        verify(onUpdateDeferredCallback, never()).execute(any(), isA(RequestScope.class), eq(Optional.empty()));
        verify(onUpdateImmediateCallback, never()).execute(any(), isA(RequestScope.class), eq(Optional.empty()));
        verify(onUpdatePostCommitCallback, never()).execute(any(), isA(RequestScope.class), eq(Optional.empty()));
        verify(onUpdatePostCommitAuthor, never()).execute(any(), isA(RequestScope.class), eq(Optional.empty()));
    }

    private RequestScope buildRequestScope(EntityDictionary dict, DataStoreTransaction tx) {
        return new RequestScope(null, null, null, tx, new User(1),
                null, getElideSettings(null, dict, MOCK_AUDIT_LOGGER));
    }
}
