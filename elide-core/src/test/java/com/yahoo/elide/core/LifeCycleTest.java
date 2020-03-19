/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import static com.yahoo.elide.Elide.JSONAPI_CONTENT_TYPE;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.Operation.CREATE;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.Operation.DELETE;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.Operation.READ;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.Operation.UPDATE;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.TransactionPhase.POSTCOMMIT;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.TransactionPhase.PRECOMMIT;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.TransactionPhase.PRESECURITY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableSet;
import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.LifeCycleHookBinding;
import com.yahoo.elide.audit.AuditLogger;
import com.yahoo.elide.functions.LifeCycleHook;
import com.yahoo.elide.request.EntityProjection;
import com.yahoo.elide.security.ChangeSpec;
import com.yahoo.elide.security.TestUser;
import com.yahoo.elide.security.User;

import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Tests the invocation & sequencing of DataStoreTransaction method invocations and life cycle events.
 */
public class LifeCycleTest {

    /**
     * Model used to mock different lifecycle test scenarios.
     */
    @Include
    @LifeCycleHookBinding(hook = TestModel.ClassPreSecurityHook.class, operation = CREATE, phase = PRESECURITY)
    @LifeCycleHookBinding(hook = TestModel.ClassPreCommitHook.class, operation = CREATE, phase = PRECOMMIT)
    @LifeCycleHookBinding(hook = TestModel.ClassPostCommitHook.class, operation = CREATE, phase = POSTCOMMIT)
    @LifeCycleHookBinding(hook = TestModel.ClassPreSecurityHook.class, operation = DELETE, phase = PRESECURITY)
    @LifeCycleHookBinding(hook = TestModel.ClassPreCommitHookEverything.class, operation = CREATE,
            phase = PRECOMMIT, oncePerRequest = false)
    @LifeCycleHookBinding(hook = TestModel.ClassPreCommitHook.class, operation = DELETE, phase = PRECOMMIT)
    @LifeCycleHookBinding(hook = TestModel.ClassPostCommitHook.class, operation = DELETE, phase = POSTCOMMIT)
    @LifeCycleHookBinding(hook = TestModel.ClassPreSecurityHook.class, operation = UPDATE, phase = PRESECURITY)
    @LifeCycleHookBinding(hook = TestModel.ClassPreCommitHook.class, operation = UPDATE, phase = PRECOMMIT)
    @LifeCycleHookBinding(hook = TestModel.ClassPostCommitHook.class, operation = UPDATE, phase = POSTCOMMIT)
    @LifeCycleHookBinding(hook = TestModel.ClassPreSecurityHook.class, operation = READ, phase = PRESECURITY)
    @LifeCycleHookBinding(hook = TestModel.ClassPreCommitHook.class, operation = READ, phase = PRECOMMIT)
    @LifeCycleHookBinding(hook = TestModel.ClassPostCommitHook.class, operation = READ, phase = POSTCOMMIT)
    static class TestModel {

        static class ClassPreSecurityHook implements LifeCycleHook<TestModel> {
            @Override
            public void execute(LifeCycleHookBinding.Operation operation,
                                TestModel elideEntity,
                                com.yahoo.elide.security.RequestScope requestScope,
                                Optional<ChangeSpec> changes) {
                elideEntity.classCallback(operation, PRESECURITY);
            }
        }

        static class ClassPreCommitHook implements LifeCycleHook<TestModel> {
            @Override
            public void execute(LifeCycleHookBinding.Operation operation,
                                TestModel elideEntity,
                                com.yahoo.elide.security.RequestScope requestScope,
                                Optional<ChangeSpec> changes) {
                elideEntity.classCallback(operation, PRECOMMIT);
            }
        }

        static class ClassPreCommitHookEverything implements LifeCycleHook<TestModel> {
            @Override
            public void execute(LifeCycleHookBinding.Operation operation,
                                TestModel elideEntity,
                                com.yahoo.elide.security.RequestScope requestScope,
                                Optional<ChangeSpec> changes) {
                elideEntity.everythingCallback(operation, PRECOMMIT);
            }
        }

        static class ClassPostCommitHook implements LifeCycleHook<TestModel> {
            @Override
            public void execute(LifeCycleHookBinding.Operation operation,
                                TestModel elideEntity,
                                com.yahoo.elide.security.RequestScope requestScope,
                                Optional<ChangeSpec> changes) {
                elideEntity.classCallback(operation, POSTCOMMIT);
            }
        }

        static class AttributePreSecurityHook implements LifeCycleHook<TestModel> {
            @Override
            public void execute(LifeCycleHookBinding.Operation operation,
                                TestModel elideEntity,
                                com.yahoo.elide.security.RequestScope requestScope,
                                Optional<ChangeSpec> changes) {
                elideEntity.attributeCallback(operation, PRESECURITY, changes.orElse(null));
            }
        }

        static class AttributePreCommitHook implements LifeCycleHook<TestModel> {
            @Override
            public void execute(LifeCycleHookBinding.Operation operation,
                                TestModel elideEntity,
                                com.yahoo.elide.security.RequestScope requestScope,
                                Optional<ChangeSpec> changes) {
                elideEntity.attributeCallback(operation, PRECOMMIT, changes.orElse(null));
            }
        }

        static class AttributePostCommitHook implements LifeCycleHook<TestModel> {
            @Override
            public void execute(LifeCycleHookBinding.Operation operation,
                                TestModel elideEntity,
                                com.yahoo.elide.security.RequestScope requestScope,
                                Optional<ChangeSpec> changes) {
                elideEntity.attributeCallback(operation, POSTCOMMIT, changes.orElse(null));
            }
        }

        static class RelationPreSecurityHook implements LifeCycleHook<TestModel> {
            @Override
            public void execute(LifeCycleHookBinding.Operation operation,
                                TestModel elideEntity,
                                com.yahoo.elide.security.RequestScope requestScope,
                                Optional<ChangeSpec> changes) {
                elideEntity.relationCallback(operation, PRESECURITY, changes.orElse(null));
            }
        }

        static class RelationPreCommitHook implements LifeCycleHook<TestModel> {
            @Override
            public void execute(LifeCycleHookBinding.Operation operation,
                                TestModel elideEntity,
                                com.yahoo.elide.security.RequestScope requestScope,
                                Optional<ChangeSpec> changes) {
                elideEntity.relationCallback(operation, PRECOMMIT, changes.orElse(null));
            }
        }

        static class RelationPostCommitHook implements LifeCycleHook<TestModel> {
            @Override
            public void execute(LifeCycleHookBinding.Operation operation,
                                TestModel elideEntity,
                                com.yahoo.elide.security.RequestScope requestScope,
                                Optional<ChangeSpec> changes) {
                elideEntity.relationCallback(operation, POSTCOMMIT, changes.orElse(null));
            }
        }

        @Id
        String id;

        @Getter
        @Setter
        @LifeCycleHookBinding(hook = TestModel.AttributePreSecurityHook.class, operation = CREATE, phase = PRESECURITY)
        @LifeCycleHookBinding(hook = TestModel.AttributePreCommitHook.class, operation = CREATE, phase = PRECOMMIT)
        @LifeCycleHookBinding(hook = TestModel.AttributePostCommitHook.class, operation = CREATE, phase = POSTCOMMIT)
        @LifeCycleHookBinding(hook = TestModel.AttributePreSecurityHook.class, operation = DELETE, phase = PRESECURITY)
        @LifeCycleHookBinding(hook = TestModel.AttributePreCommitHook.class, operation = DELETE, phase = PRECOMMIT)
        @LifeCycleHookBinding(hook = TestModel.AttributePostCommitHook.class, operation = DELETE, phase = POSTCOMMIT)
        @LifeCycleHookBinding(hook = TestModel.AttributePreSecurityHook.class, operation = UPDATE, phase = PRESECURITY)
        @LifeCycleHookBinding(hook = TestModel.AttributePreCommitHook.class, operation = UPDATE, phase = PRECOMMIT)
        @LifeCycleHookBinding(hook = TestModel.AttributePostCommitHook.class, operation = UPDATE, phase = POSTCOMMIT)
        @LifeCycleHookBinding(hook = TestModel.AttributePreSecurityHook.class, operation = READ, phase = PRESECURITY)
        @LifeCycleHookBinding(hook = TestModel.AttributePreCommitHook.class, operation = READ, phase = PRECOMMIT)
        @LifeCycleHookBinding(hook = TestModel.AttributePostCommitHook.class, operation = READ, phase = POSTCOMMIT)
        String field;

        @Getter
        @Setter
        @OneToMany
        @LifeCycleHookBinding(hook = TestModel.RelationPreSecurityHook.class, operation = CREATE, phase = PRESECURITY)
        @LifeCycleHookBinding(hook = TestModel.RelationPreCommitHook.class, operation = CREATE, phase = PRECOMMIT)
        @LifeCycleHookBinding(hook = TestModel.RelationPostCommitHook.class, operation = CREATE, phase = POSTCOMMIT)
        @LifeCycleHookBinding(hook = TestModel.RelationPreSecurityHook.class, operation = DELETE, phase = PRESECURITY)
        @LifeCycleHookBinding(hook = TestModel.RelationPreCommitHook.class, operation = DELETE, phase = PRECOMMIT)
        @LifeCycleHookBinding(hook = TestModel.RelationPostCommitHook.class, operation = DELETE, phase = POSTCOMMIT)
        @LifeCycleHookBinding(hook = TestModel.RelationPreSecurityHook.class, operation = UPDATE, phase = PRESECURITY)
        @LifeCycleHookBinding(hook = TestModel.RelationPreCommitHook.class, operation = UPDATE, phase = PRECOMMIT)
        @LifeCycleHookBinding(hook = TestModel.RelationPostCommitHook.class, operation = UPDATE, phase = POSTCOMMIT)
        @LifeCycleHookBinding(hook = TestModel.RelationPreSecurityHook.class, operation = READ, phase = PRESECURITY)
        @LifeCycleHookBinding(hook = TestModel.RelationPreCommitHook.class, operation = READ, phase = PRECOMMIT)
        @LifeCycleHookBinding(hook = TestModel.RelationPostCommitHook.class, operation = READ, phase = POSTCOMMIT)
        Set<TestModel> models;

        public void classCallback(LifeCycleHookBinding.Operation operation,
                                  LifeCycleHookBinding.TransactionPhase phase) {
            //NOOP - this will be mocked to verify hook invocation.
        }

        public void attributeCallback(LifeCycleHookBinding.Operation operation,
                                      LifeCycleHookBinding.TransactionPhase phase,
                                      ChangeSpec changes) {
            //NOOP - this will be mocked to verify hook invocation.
        }

        public void relationCallback(LifeCycleHookBinding.Operation operation,
                                     LifeCycleHookBinding.TransactionPhase phase,
                                     ChangeSpec changes) {
            //NOOP - this will be mocked to verify hook invocation.
        }

        public void everythingCallback(LifeCycleHookBinding.Operation operation,
                                       LifeCycleHookBinding.TransactionPhase phase) {
            //NOOP - this will be mocked to verify hook invocation.
        }
    }

    private static final AuditLogger MOCK_AUDIT_LOGGER = mock(AuditLogger.class);
    private EntityDictionary dictionary;

    LifeCycleTest() throws Exception {
        dictionary = TestDictionary.getTestDictionary();
        dictionary.bindEntity(TestModel.class);
    }

    @Test
    public void testElideCreate() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        TestModel mockModel = mock(TestModel.class);

        Elide elide = getElide(store, dictionary, MOCK_AUDIT_LOGGER);

        String body = "{\"data\": {\"type\":\"testModel\",\"id\":\"1\",\"attributes\": {\"field\":\"Foo\"}}}";

        when(store.beginTransaction()).thenReturn(tx);
        when(tx.createNewObject(TestModel.class)).thenReturn(mockModel);

        ElideResponse response = elide.post("/testModel", body, null);
        assertEquals(HttpStatus.SC_CREATED, response.getResponseCode());

        verify(mockModel, times(1)).classCallback(eq(READ), eq(PRESECURITY));
        verify(mockModel, times(1)).classCallback(eq(READ), eq(PRECOMMIT));
        verify(mockModel, times(1)).classCallback(eq(READ), eq(POSTCOMMIT));
        verify(mockModel, times(1)).classCallback(eq(CREATE), eq(PRESECURITY));
        verify(mockModel, times(1)).classCallback(eq(CREATE), eq(PRECOMMIT));
        verify(mockModel, times(1)).classCallback(eq(CREATE), eq(POSTCOMMIT));
        verify(mockModel, never()).classCallback(eq(UPDATE), any());
        verify(mockModel, never()).classCallback(eq(DELETE), any());

        verify(mockModel, times(2)).everythingCallback(eq(CREATE), eq(PRECOMMIT));
        verify(mockModel, never()).everythingCallback(eq(UPDATE), any());
        verify(mockModel, never()).everythingCallback(eq(DELETE), any());
        verify(mockModel, never()).everythingCallback(eq(READ), any());

        verify(mockModel, times(1)).attributeCallback(eq(READ), eq(PRESECURITY), any());
        verify(mockModel, times(1)).attributeCallback(eq(READ), eq(PRECOMMIT), any());
        verify(mockModel, times(1)).attributeCallback(eq(READ), eq(POSTCOMMIT), any());
        verify(mockModel, times(1)).attributeCallback(eq(CREATE), eq(PRESECURITY), any());
        verify(mockModel, times(1)).attributeCallback(eq(CREATE), eq(PRECOMMIT), any());
        verify(mockModel, times(1)).attributeCallback(eq(CREATE), eq(POSTCOMMIT), any());
        verify(mockModel, never()).attributeCallback(eq(UPDATE), any(), any());
        verify(mockModel, never()).attributeCallback(eq(DELETE), any(), any());

        verify(mockModel, times(1)).relationCallback(eq(READ), eq(PRESECURITY), any());
        verify(mockModel, times(1)).relationCallback(eq(READ), eq(PRECOMMIT), any());
        verify(mockModel, times(1)).relationCallback(eq(READ), eq(POSTCOMMIT), any());
        verify(mockModel, times(1)).relationCallback(eq(CREATE), eq(PRESECURITY), any());
        verify(mockModel, times(1)).relationCallback(eq(CREATE), eq(PRECOMMIT), any());
        verify(mockModel, times(1)).relationCallback(eq(CREATE), eq(POSTCOMMIT), any());
        verify(mockModel, never()).relationCallback(eq(UPDATE), any(), any());
        verify(mockModel, never()).relationCallback(eq(DELETE), any(), any());

        verify(tx).preCommit();
        verify(tx, times(1)).createObject(eq(mockModel), isA(RequestScope.class));
        verify(tx).flush(isA(RequestScope.class));
        verify(tx).commit(isA(RequestScope.class));
        verify(tx).close();
    }

    @Test
    public void testElideCreateFailure() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        TestModel mockModel = mock(TestModel.class);
        doThrow(RuntimeException.class).when(mockModel).setField(anyString());

        Elide elide = getElide(store, dictionary, MOCK_AUDIT_LOGGER);

        String body = "{\"data\": {\"type\":\"testModel\",\"id\":\"1\",\"attributes\": {\"field\":\"Foo\"}}}";

        when(store.beginTransaction()).thenReturn(tx);
        when(tx.createNewObject(TestModel.class)).thenReturn(mockModel);

        ElideResponse response = elide.post("/testModel", body, null);
        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getResponseCode());
        assertEquals(
                "{\"errors\":[{\"detail\":\"Unexpected exception caught\"}]}",
                response.getBody());

        verify(mockModel, never()).classCallback(any(), any());
        verify(mockModel, never()).attributeCallback(any(), any(), any());
        verify(mockModel, never()).relationCallback(any(), any(), any());
        verify(mockModel, never()).everythingCallback(any(), any());

        verify(tx, never()).preCommit();
        verify(tx, never()).createObject(eq(mockModel), isA(RequestScope.class));
        verify(tx, never()).flush(isA(RequestScope.class));
        verify(tx, never()).commit(isA(RequestScope.class));
        verify(tx).close();
    }

    @Test
    public void testElideGet() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        TestModel mockModel = mock(TestModel.class);

        Elide elide = getElide(store, dictionary, MOCK_AUDIT_LOGGER);

        when(store.beginReadTransaction()).thenCallRealMethod();
        when(store.beginTransaction()).thenReturn(tx);
        when(tx.loadObject(isA(EntityProjection.class), any(), isA(RequestScope.class))).thenReturn(mockModel);

        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        ElideResponse response = elide.get("/testModel/1", headers, null);
        assertEquals(HttpStatus.SC_OK, response.getResponseCode());

        verify(mockModel, never()).everythingCallback(any(), any());

        verify(mockModel, times(1)).classCallback(eq(READ), eq(PRESECURITY));
        verify(mockModel, times(1)).classCallback(eq(READ), eq(PRECOMMIT));
        verify(mockModel, times(1)).classCallback(eq(READ), eq(POSTCOMMIT));
        verify(mockModel, never()).classCallback(eq(CREATE), any());
        verify(mockModel, never()).classCallback(eq(UPDATE), any());
        verify(mockModel, never()).classCallback(eq(DELETE), any());

        verify(mockModel, times(1)).attributeCallback(eq(READ), eq(PRESECURITY), any());
        verify(mockModel, times(1)).attributeCallback(eq(READ), eq(PRECOMMIT), any());
        verify(mockModel, times(1)).attributeCallback(eq(READ), eq(POSTCOMMIT), any());
        verify(mockModel, never()).attributeCallback(eq(CREATE), any(), any());
        verify(mockModel, never()).attributeCallback(eq(UPDATE), any(), any());
        verify(mockModel, never()).attributeCallback(eq(DELETE), any(), any());

        verify(mockModel, times(1)).relationCallback(eq(READ), eq(PRESECURITY), any());
        verify(mockModel, times(1)).relationCallback(eq(READ), eq(PRECOMMIT), any());
        verify(mockModel, times(1)).relationCallback(eq(READ), eq(POSTCOMMIT), any());
        verify(mockModel, never()).relationCallback(eq(CREATE), any(), any());
        verify(mockModel, never()).relationCallback(eq(UPDATE), any(), any());
        verify(mockModel, never()).relationCallback(eq(DELETE), any(), any());
        verify(tx).preCommit();
        verify(tx).flush(any());
        verify(tx).commit(any());
        verify(tx).close();
    }

    @Test
    public void testElideGetSparse() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        TestModel mockModel = mock(TestModel.class);

        Elide elide = getElide(store, dictionary, MOCK_AUDIT_LOGGER);

        when(store.beginReadTransaction()).thenCallRealMethod();
        when(store.beginTransaction()).thenReturn(tx);
        when(tx.loadObject(isA(EntityProjection.class), any(), isA(RequestScope.class))).thenReturn(mockModel);

        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.putSingle("fields[testModel]", "field");
        ElideResponse response = elide.get("/testModel/1", headers, null);
        assertEquals(HttpStatus.SC_OK, response.getResponseCode());

        verify(mockModel, never()).everythingCallback(any(), any());

        verify(mockModel, times(1)).classCallback(eq(READ), eq(PRESECURITY));
        verify(mockModel, times(1)).classCallback(eq(READ), eq(PRECOMMIT));
        verify(mockModel, times(1)).classCallback(eq(READ), eq(POSTCOMMIT));
        verify(mockModel, never()).classCallback(eq(CREATE), any());
        verify(mockModel, never()).classCallback(eq(UPDATE), any());
        verify(mockModel, never()).classCallback(eq(DELETE), any());

        verify(mockModel, times(1)).attributeCallback(eq(READ), eq(PRESECURITY), any());
        verify(mockModel, times(1)).attributeCallback(eq(READ), eq(PRECOMMIT), any());
        verify(mockModel, times(1)).attributeCallback(eq(READ), eq(POSTCOMMIT), any());
        verify(mockModel, never()).attributeCallback(eq(CREATE), any(), any());
        verify(mockModel, never()).attributeCallback(eq(UPDATE), any(), any());
        verify(mockModel, never()).attributeCallback(eq(DELETE), any(), any());

        verify(mockModel, times(0)).relationCallback(any(), any(), any());

        verify(tx).preCommit();
        verify(tx).flush(any());
        verify(tx).commit(any());
        verify(tx).close();
    }

    @Test
    public void testElideGetRelationship() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        TestModel mockModel = mock(TestModel.class);
        TestModel child = mock(TestModel.class);
        when(mockModel.getModels()).thenReturn(ImmutableSet.of(child));

        Elide elide = getElide(store, dictionary, MOCK_AUDIT_LOGGER);

        when(store.beginReadTransaction()).thenCallRealMethod();
        when(store.beginTransaction()).thenReturn(tx);
        when(tx.loadObject(isA(EntityProjection.class), any(), isA(RequestScope.class))).thenReturn(mockModel);

        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        ElideResponse response = elide.get("/testModel/1/relationships/models", headers, null);
        assertEquals(HttpStatus.SC_OK, response.getResponseCode());

        verify(mockModel, never()).everythingCallback(any(), any());

        verify(mockModel, times(1)).classCallback(eq(READ), eq(PRESECURITY));
        verify(mockModel, times(1)).classCallback(eq(READ), eq(PRECOMMIT));
        verify(mockModel, times(1)).classCallback(eq(READ), eq(POSTCOMMIT));
        verify(mockModel, never()).classCallback(eq(CREATE), any());
        verify(mockModel, never()).classCallback(eq(UPDATE), any());
        verify(mockModel, never()).classCallback(eq(DELETE), any());

        verify(mockModel, times(1)).attributeCallback(eq(READ), eq(PRESECURITY), any());
        verify(mockModel, times(1)).attributeCallback(eq(READ), eq(PRECOMMIT), any());
        verify(mockModel, times(1)).attributeCallback(eq(READ), eq(POSTCOMMIT), any());
        verify(mockModel, never()).attributeCallback(eq(CREATE), any(), any());
        verify(mockModel, never()).attributeCallback(eq(UPDATE), any(), any());
        verify(mockModel, never()).attributeCallback(eq(DELETE), any(), any());

        verify(mockModel, times(1)).relationCallback(eq(READ), eq(PRESECURITY), any());
        verify(mockModel, times(1)).relationCallback(eq(READ), eq(PRECOMMIT), any());
        verify(mockModel, times(1)).relationCallback(eq(READ), eq(POSTCOMMIT), any());
        verify(mockModel, never()).relationCallback(eq(CREATE), any(), any());
        verify(mockModel, never()).relationCallback(eq(UPDATE), any(), any());
        verify(mockModel, never()).relationCallback(eq(DELETE), any(), any());

        verify(tx).preCommit();
        verify(tx).flush(any());
        verify(tx).commit(any());
        verify(tx).close();
    }

    @Test
    public void testElidePatch() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        TestModel mockModel = mock(TestModel.class);

        Elide elide = getElide(store, dictionary, MOCK_AUDIT_LOGGER);

        String body = "{\"data\": {\"type\":\"testModel\",\"id\":\"1\",\"attributes\": {\"field\":\"Foo\"}}}";

        dictionary.setValue(mockModel, "id", "1");
        when(store.beginTransaction()).thenReturn(tx);
        when(tx.loadObject(isA(EntityProjection.class), any(), isA(RequestScope.class))).thenReturn(mockModel);

        String contentType = JSONAPI_CONTENT_TYPE;
        ElideResponse response = elide.patch(contentType, contentType, "/testModel/1", body, null);
        assertEquals(HttpStatus.SC_NO_CONTENT, response.getResponseCode());

        verify(mockModel, never()).everythingCallback(any(), any());

        verify(mockModel, never()).classCallback(eq(READ), any());
        verify(mockModel, never()).classCallback(eq(CREATE), any());
        verify(mockModel, never()).classCallback(eq(DELETE), any());
        verify(mockModel, times(1)).classCallback(eq(UPDATE), eq(PRESECURITY));
        verify(mockModel, times(1)).classCallback(eq(UPDATE), eq(PRECOMMIT));
        verify(mockModel, times(1)).classCallback(eq(UPDATE), eq(POSTCOMMIT));

        verify(mockModel, never()).attributeCallback(eq(READ), any(), any());
        verify(mockModel, never()).attributeCallback(eq(CREATE), any(), any());
        verify(mockModel, never()).attributeCallback(eq(DELETE), any(), any());
        verify(mockModel, times(1)).attributeCallback(eq(UPDATE), eq(PRESECURITY), any());
        verify(mockModel, times(1)).attributeCallback(eq(UPDATE), eq(PRECOMMIT), any());
        verify(mockModel, times(1)).attributeCallback(eq(UPDATE), eq(POSTCOMMIT), any());

        verify(mockModel, never()).relationCallback(eq(READ), any(), any());
        verify(mockModel, never()).relationCallback(eq(CREATE), any(), any());
        verify(mockModel, never()).relationCallback(eq(DELETE), any(), any());
        verify(mockModel, never()).relationCallback(eq(UPDATE), any(), any());

        verify(tx).preCommit();
        verify(tx).save(eq(mockModel), isA(RequestScope.class));
        verify(tx).flush(isA(RequestScope.class));
        verify(tx).commit(isA(RequestScope.class));
        verify(tx).close();
    }

    @Test
    public void testElideDelete() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        TestModel mockModel = mock(TestModel.class);

        Elide elide = getElide(store, dictionary, MOCK_AUDIT_LOGGER);

        String body = "{\"data\": {\"type\":\"testModel\",\"id\":\"1\",\"attributes\": {\"field\":\"Foo\"}}}";

        dictionary.setValue(mockModel, "id", "1");
        when(store.beginTransaction()).thenReturn(tx);
        when(tx.loadObject(isA(EntityProjection.class), any(), isA(RequestScope.class))).thenReturn(mockModel);

        ElideResponse response = elide.delete("/testModel/1", "", null);
        assertEquals(HttpStatus.SC_NO_CONTENT, response.getResponseCode());

        verify(mockModel, never()).everythingCallback(any(), any());

        verify(mockModel, never()).classCallback(eq(UPDATE), any());
        verify(mockModel, never()).classCallback(eq(CREATE), any());
        verify(mockModel, times(1)).classCallback(eq(READ), eq(PRESECURITY));
        verify(mockModel, times(1)).classCallback(eq(READ), eq(PRECOMMIT));
        verify(mockModel, times(1)).classCallback(eq(READ), eq(POSTCOMMIT));
        verify(mockModel, times(1)).classCallback(eq(DELETE), eq(PRESECURITY));
        verify(mockModel, times(1)).classCallback(eq(DELETE), eq(PRECOMMIT));
        verify(mockModel, times(1)).classCallback(eq(DELETE), eq(POSTCOMMIT));

        verify(mockModel, never()).attributeCallback(eq(UPDATE), any(), any());
        verify(mockModel, never()).attributeCallback(eq(CREATE), any(), any());
        verify(mockModel, never()).attributeCallback(eq(READ), any(), any());
        verify(mockModel, never()).attributeCallback(eq(DELETE), any(), any());

        //TODO - Read should not be called for a delete.
        verify(mockModel, never()).relationCallback(eq(UPDATE), any(), any());
        verify(mockModel, never()).relationCallback(eq(CREATE), any(), any());
        verify(mockModel, never()).relationCallback(eq(DELETE), any(), any());
        verify(mockModel, times(1)).relationCallback(eq(READ), eq(PRESECURITY), any());
        verify(mockModel, times(1)).relationCallback(eq(READ), eq(PRECOMMIT), any());
        verify(mockModel, times(1)).relationCallback(eq(READ), eq(POSTCOMMIT), any());

        verify(tx).preCommit();
        verify(tx).delete(eq(mockModel), isA(RequestScope.class));
        verify(tx).flush(isA(RequestScope.class));
        verify(tx).commit(isA(RequestScope.class));
        verify(tx).close();
    }

    @Test
    public void testElidePatchFailure() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        TestModel mockModel = mock(TestModel.class);

        Elide elide = getElide(store, dictionary, MOCK_AUDIT_LOGGER);

        String body = "{\"data\": {\"type\":\"testModel\",\"id\":\"1\",\"attributes\": {\"field\":\"Foo\"}}}";

        dictionary.setValue(mockModel, "id", "1");
        when(store.beginTransaction()).thenReturn(tx);
        when(tx.loadObject(isA(EntityProjection.class), any(), isA(RequestScope.class))).thenReturn(mockModel);
        doThrow(ConstraintViolationException.class).when(tx).flush(any());

        String contentType = JSONAPI_CONTENT_TYPE;
        ElideResponse response = elide.patch(contentType, contentType, "/testModel/1", body, null);
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getResponseCode());
        assertEquals(
                "{\"errors\":[{\"detail\":\"Constraint violation\"}]}",
                response.getBody());

        verify(mockModel, never()).everythingCallback(any(), any());

        verify(mockModel, never()).classCallback(eq(READ), any());
        verify(mockModel, never()).classCallback(eq(CREATE), any());
        verify(mockModel, never()).classCallback(eq(DELETE), any());

        verify(mockModel, times(1)).classCallback(eq(UPDATE), eq(PRESECURITY));
        verify(mockModel, times(0)).classCallback(eq(UPDATE), eq(PRECOMMIT));
        verify(mockModel, times(0)).classCallback(eq(UPDATE), eq(POSTCOMMIT));

        verify(mockModel, never()).attributeCallback(eq(READ), any(), any());
        verify(mockModel, never()).attributeCallback(eq(CREATE), any(), any());
        verify(mockModel, never()).attributeCallback(eq(DELETE), any(), any());
        verify(mockModel, times(1)).attributeCallback(eq(UPDATE), eq(PRESECURITY), any());
        verify(mockModel, times(0)).attributeCallback(eq(UPDATE), eq(PRECOMMIT), any());
        verify(mockModel, times(0)).attributeCallback(eq(UPDATE), eq(POSTCOMMIT), any());

        verify(mockModel, never()).relationCallback(eq(READ), any(), any());
        verify(mockModel, never()).relationCallback(eq(UPDATE), any(), any());
        verify(mockModel, never()).relationCallback(eq(CREATE), any(), any());
        verify(mockModel, never()).relationCallback(eq(DELETE), any(), any());

        verify(tx).preCommit();
        verify(tx).save(eq(mockModel), isA(RequestScope.class));
        verify(tx).flush(isA(RequestScope.class));
        verify(tx, never()).commit(isA(RequestScope.class));
        verify(tx).close();
    }

    @Test
    public void testCreate() {
        TestModel mockModel = mock(TestModel.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        when(tx.createNewObject(TestModel.class)).thenReturn(mockModel);
        RequestScope scope = buildRequestScope(dictionary, tx);
        PersistentResource resource = PersistentResource.createObject(TestModel.class, scope, Optional.of("1"));
        resource.setValueChecked("field", "should not affect calls since this is create!");
        assertNotNull(resource);

        verify(mockModel, never()).classCallback(any(), any());
        verify(mockModel, never()).attributeCallback(any(), any(), any());
        verify(mockModel, never()).relationCallback(any(), any(), any());
        verify(mockModel, never()).everythingCallback(any(), any());

        scope.runQueuedPreSecurityTriggers();

        verify(mockModel, times(1)).classCallback(any(), any());
        verify(mockModel, times(1)).classCallback(eq(CREATE), eq(PRESECURITY));
        verify(mockModel, times(1)).attributeCallback(any(), any(), any());
        verify(mockModel, times(1)).attributeCallback(eq(CREATE), eq(PRESECURITY), any());
        verify(mockModel, times(1)).relationCallback(any(), any(), any());
        verify(mockModel, times(1)).relationCallback(eq(CREATE), eq(PRESECURITY), any());
        verify(mockModel, never()).everythingCallback(any(), any());

        clearInvocations(mockModel);
        scope.runQueuedPreCommitTriggers();

        verify(mockModel, times(1)).classCallback(any(), any());
        verify(mockModel, times(1)).classCallback(eq(CREATE), eq(PRECOMMIT));
        verify(mockModel, times(1)).attributeCallback(any(), any(), any());
        verify(mockModel, times(1)).attributeCallback(eq(CREATE), eq(PRECOMMIT), any());
        verify(mockModel, times(1)).relationCallback(any(), any(), any());
        verify(mockModel, times(1)).relationCallback(eq(CREATE), eq(PRECOMMIT), any());
        verify(mockModel, times(2)).everythingCallback(any(), any());
        verify(mockModel, times(2)).everythingCallback(eq(CREATE), eq(PRECOMMIT));

        clearInvocations(mockModel);
        scope.getPermissionExecutor().executeCommitChecks();
        scope.runQueuedPostCommitTriggers();

        verify(mockModel, never()).everythingCallback(any(), any());
        verify(mockModel, times(1)).classCallback(any(), any());
        verify(mockModel, times(1)).classCallback(eq(CREATE), eq(POSTCOMMIT));
        verify(mockModel, times(1)).attributeCallback(any(), any(), any());
        verify(mockModel, times(1)).attributeCallback(eq(CREATE), eq(POSTCOMMIT), any());
        verify(mockModel, times(1)).relationCallback(any(), any(), any());
        verify(mockModel, times(1)).relationCallback(eq(CREATE), eq(POSTCOMMIT), any());
    }

    @Test
    public void testUpdate() {
        TestModel mockModel = mock(TestModel.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        when(tx.createNewObject(TestModel.class)).thenReturn(mockModel);
        RequestScope scope = buildRequestScope(dictionary, tx);

        PersistentResource resource = new PersistentResource(mockModel, null, scope.getUUIDFor(mockModel), scope);
        resource.setValueChecked("field", "new value");

        verify(mockModel, times(1)).classCallback(any(), any());
        verify(mockModel, times(1)).classCallback(eq(UPDATE), eq(PRESECURITY));
        verify(mockModel, times(1)).attributeCallback(any(), any(), any());
        verify(mockModel, times(1)).attributeCallback(eq(UPDATE), eq(PRESECURITY), notNull());

        verify(mockModel, never()).relationCallback(any(), any(), any());
        verify(mockModel, never()).everythingCallback(any(), any());

        clearInvocations(mockModel);
        scope.runQueuedPreSecurityTriggers();

        verify(mockModel, never()).everythingCallback(any(), any());
        verify(mockModel, never()).relationCallback(any(), any(), any());
        verify(mockModel, never()).attributeCallback(any(), any(), any());

        clearInvocations(mockModel);
        scope.runQueuedPreCommitTriggers();

        verify(mockModel, never()).relationCallback(any(), any(), any());
        verify(mockModel, never()).everythingCallback(any(), any());

        verify(mockModel, times(1)).classCallback(any(), any());
        verify(mockModel, times(1)).classCallback(eq(UPDATE), eq(PRECOMMIT));
        verify(mockModel, times(1)).attributeCallback(any(), any(), any());
        verify(mockModel, times(1)).attributeCallback(eq(UPDATE), eq(PRECOMMIT), notNull());

        clearInvocations(mockModel);
        scope.getPermissionExecutor().executeCommitChecks();
        scope.runQueuedPostCommitTriggers();

        verify(mockModel, never()).relationCallback(any(), any(), any());
        verify(mockModel, never()).everythingCallback(any(), any());

        verify(mockModel, times(1)).classCallback(any(), any());
        verify(mockModel, times(1)).classCallback(eq(UPDATE), eq(POSTCOMMIT));
        verify(mockModel, times(1)).attributeCallback(any(), any(), any());
        verify(mockModel, times(1)).attributeCallback(eq(UPDATE), eq(POSTCOMMIT), notNull());
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

    private RequestScope buildRequestScope(EntityDictionary dict, DataStoreTransaction tx) {
        User user = new TestUser("1");

        return new RequestScope(null, null, tx, user, null,
                getElideSettings(null, dict, MOCK_AUDIT_LOGGER));
    }
}
