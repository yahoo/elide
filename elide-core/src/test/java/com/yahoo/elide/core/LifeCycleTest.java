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
import com.yahoo.elide.annotation.LifeCycleHookBinding;
import com.yahoo.elide.audit.AuditLogger;
import com.yahoo.elide.core.datastore.inmemory.HashMapDataStore;
import com.yahoo.elide.core.datastore.inmemory.InMemoryDataStore;
import com.yahoo.elide.functions.LifeCycleHook;
import com.yahoo.elide.request.EntityProjection;
import com.yahoo.elide.security.ChangeSpec;
import com.yahoo.elide.security.TestUser;
import com.yahoo.elide.security.User;
import com.yahoo.elide.security.checks.Check;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import example.Author;
import example.Book;
import example.Editor;
import example.Publisher;

import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;
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

        static class ClassPostCommitHook implements LifeCycleHook<TestModel> {
            @Override
            public void execute(LifeCycleHookBinding.Operation operation,
                                TestModel elideEntity,
                                com.yahoo.elide.security.RequestScope requestScope,
                                Optional<ChangeSpec> changes) {
                elideEntity.classCallback(operation, POSTCOMMIT);
            }
        }

        static class FieldPreSecurityHook implements LifeCycleHook<TestModel> {
            @Override
            public void execute(LifeCycleHookBinding.Operation operation,
                                TestModel elideEntity,
                                com.yahoo.elide.security.RequestScope requestScope,
                                Optional<ChangeSpec> changes) {
                elideEntity.fieldCallback(operation, PRESECURITY);
            }
        }

        static class FieldPreCommitHook implements LifeCycleHook<TestModel> {
            @Override
            public void execute(LifeCycleHookBinding.Operation operation,
                                TestModel elideEntity,
                                com.yahoo.elide.security.RequestScope requestScope,
                                Optional<ChangeSpec> changes) {
                elideEntity.fieldCallback(operation, PRECOMMIT);
            }
        }

        static class FieldPostCommitHook implements LifeCycleHook<TestModel> {
            @Override
            public void execute(LifeCycleHookBinding.Operation operation,
                                TestModel elideEntity,
                                com.yahoo.elide.security.RequestScope requestScope,
                                Optional<ChangeSpec> changes) {
                elideEntity.fieldCallback(operation, POSTCOMMIT);
            }
        }

        @Getter
        @Setter
        @Id
        String id;

        @Getter
        @Setter
        @LifeCycleHookBinding(hook = TestModel.FieldPreSecurityHook.class, operation = CREATE, phase = PRESECURITY)
        @LifeCycleHookBinding(hook = TestModel.FieldPreCommitHook.class, operation = CREATE, phase = PRECOMMIT)
        @LifeCycleHookBinding(hook = TestModel.FieldPostCommitHook.class, operation = CREATE, phase = POSTCOMMIT)
        @LifeCycleHookBinding(hook = TestModel.FieldPreSecurityHook.class, operation = DELETE, phase = PRESECURITY)
        @LifeCycleHookBinding(hook = TestModel.FieldPreCommitHook.class, operation = DELETE, phase = PRECOMMIT)
        @LifeCycleHookBinding(hook = TestModel.FieldPostCommitHook.class, operation = DELETE, phase = POSTCOMMIT)
        @LifeCycleHookBinding(hook = TestModel.FieldPreSecurityHook.class, operation = UPDATE, phase = PRESECURITY)
        @LifeCycleHookBinding(hook = TestModel.FieldPreCommitHook.class, operation = UPDATE, phase = PRECOMMIT)
        @LifeCycleHookBinding(hook = TestModel.FieldPostCommitHook.class, operation = UPDATE, phase = POSTCOMMIT)
        @LifeCycleHookBinding(hook = TestModel.FieldPreSecurityHook.class, operation = READ, phase = PRESECURITY)
        @LifeCycleHookBinding(hook = TestModel.FieldPreCommitHook.class, operation = READ, phase = PRECOMMIT)
        @LifeCycleHookBinding(hook = TestModel.FieldPostCommitHook.class, operation = READ, phase = POSTCOMMIT)
        String field;

        public void classCallback(LifeCycleHookBinding.Operation operation,
                                  LifeCycleHookBinding.TransactionPhase phase) {
            //NOOP - this will be mocked to verify hook invocation.
        }

        public void fieldCallback(LifeCycleHookBinding.Operation operation,
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
        verify(mockModel, times(0)).classCallback(eq(UPDATE), eq(PRESECURITY));
        verify(mockModel, times(0)).classCallback(eq(UPDATE), eq(PRECOMMIT));
        verify(mockModel, times(0)).classCallback(eq(UPDATE), eq(POSTCOMMIT));
        verify(mockModel, times(0)).classCallback(eq(DELETE), eq(PRESECURITY));
        verify(mockModel, times(0)).classCallback(eq(DELETE), eq(PRECOMMIT));
        verify(mockModel, times(0)).classCallback(eq(DELETE), eq(POSTCOMMIT));

        verify(mockModel, times(1)).fieldCallback(eq(READ), eq(PRESECURITY));
        verify(mockModel, times(1)).fieldCallback(eq(READ), eq(PRECOMMIT));
        verify(mockModel, times(1)).fieldCallback(eq(READ), eq(POSTCOMMIT));
        verify(mockModel, times(1)).fieldCallback(eq(CREATE), eq(PRESECURITY));
        verify(mockModel, times(1)).fieldCallback(eq(CREATE), eq(PRECOMMIT));
        verify(mockModel, times(1)).fieldCallback(eq(CREATE), eq(POSTCOMMIT));
        verify(mockModel, times(0)).fieldCallback(eq(UPDATE), eq(PRESECURITY));
        verify(mockModel, times(0)).fieldCallback(eq(UPDATE), eq(PRECOMMIT));
        verify(mockModel, times(0)).fieldCallback(eq(UPDATE), eq(POSTCOMMIT));
        verify(mockModel, times(0)).fieldCallback(eq(DELETE), eq(PRESECURITY));
        verify(mockModel, times(0)).fieldCallback(eq(DELETE), eq(PRECOMMIT));
        verify(mockModel, times(0)).fieldCallback(eq(DELETE), eq(POSTCOMMIT));

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

        verify(mockModel, times(1)).classCallback(eq(READ), eq(PRESECURITY));
        verify(mockModel, times(0)).classCallback(eq(READ), eq(PRECOMMIT));
        verify(mockModel, times(0)).classCallback(eq(READ), eq(POSTCOMMIT));
        verify(mockModel, times(0)).classCallback(eq(CREATE), eq(PRESECURITY));
        verify(mockModel, times(0)).classCallback(eq(CREATE), eq(PRECOMMIT));
        verify(mockModel, times(0)).classCallback(eq(CREATE), eq(POSTCOMMIT));
        verify(mockModel, times(0)).classCallback(eq(UPDATE), eq(PRESECURITY));
        verify(mockModel, times(0)).classCallback(eq(UPDATE), eq(PRECOMMIT));
        verify(mockModel, times(0)).classCallback(eq(UPDATE), eq(POSTCOMMIT));
        verify(mockModel, times(0)).classCallback(eq(DELETE), eq(PRESECURITY));
        verify(mockModel, times(0)).classCallback(eq(DELETE), eq(PRECOMMIT));
        verify(mockModel, times(0)).classCallback(eq(DELETE), eq(POSTCOMMIT));

        verify(mockModel, times(1)).fieldCallback(eq(READ), eq(PRESECURITY));
        verify(mockModel, times(0)).fieldCallback(eq(READ), eq(PRECOMMIT));
        verify(mockModel, times(0)).fieldCallback(eq(READ), eq(POSTCOMMIT));
        verify(mockModel, times(0)).fieldCallback(eq(CREATE), eq(PRESECURITY));
        verify(mockModel, times(0)).fieldCallback(eq(CREATE), eq(PRECOMMIT));
        verify(mockModel, times(0)).fieldCallback(eq(CREATE), eq(POSTCOMMIT));
        verify(mockModel, times(0)).fieldCallback(eq(UPDATE), eq(PRESECURITY));
        verify(mockModel, times(0)).fieldCallback(eq(UPDATE), eq(PRECOMMIT));
        verify(mockModel, times(0)).fieldCallback(eq(UPDATE), eq(POSTCOMMIT));
        verify(mockModel, times(0)).fieldCallback(eq(DELETE), eq(PRESECURITY));
        verify(mockModel, times(0)).fieldCallback(eq(DELETE), eq(PRECOMMIT));
        verify(mockModel, times(0)).fieldCallback(eq(DELETE), eq(POSTCOMMIT));

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
        when(mockModel.getId()).thenReturn("1");

        Elide elide = getElide(store, dictionary, MOCK_AUDIT_LOGGER);

        when(store.beginReadTransaction()).thenCallRealMethod();
        when(store.beginTransaction()).thenReturn(tx);
        when(tx.loadObject(isA(EntityProjection.class), any(), isA(RequestScope.class))).thenReturn(mockModel);

        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        ElideResponse response = elide.get("/testModel/1", headers, null);
        assertEquals(HttpStatus.SC_OK, response.getResponseCode());

        verify(mockModel, times(1)).classCallback(eq(READ), eq(PRESECURITY));
        verify(mockModel, times(1)).classCallback(eq(READ), eq(PRECOMMIT));
        verify(mockModel, times(1)).classCallback(eq(READ), eq(POSTCOMMIT));
        verify(mockModel, times(0)).classCallback(eq(CREATE), eq(PRESECURITY));
        verify(mockModel, times(0)).classCallback(eq(CREATE), eq(PRECOMMIT));
        verify(mockModel, times(0)).classCallback(eq(CREATE), eq(POSTCOMMIT));
        verify(mockModel, times(0)).classCallback(eq(UPDATE), eq(PRESECURITY));
        verify(mockModel, times(0)).classCallback(eq(UPDATE), eq(PRECOMMIT));
        verify(mockModel, times(0)).classCallback(eq(UPDATE), eq(POSTCOMMIT));
        verify(mockModel, times(0)).classCallback(eq(DELETE), eq(PRESECURITY));
        verify(mockModel, times(0)).classCallback(eq(DELETE), eq(PRECOMMIT));
        verify(mockModel, times(0)).classCallback(eq(DELETE), eq(POSTCOMMIT));

        verify(mockModel, times(1)).fieldCallback(eq(READ), eq(PRESECURITY));
        verify(mockModel, times(1)).fieldCallback(eq(READ), eq(PRECOMMIT));
        verify(mockModel, times(1)).fieldCallback(eq(READ), eq(POSTCOMMIT));
        verify(mockModel, times(0)).fieldCallback(eq(CREATE), eq(PRESECURITY));
        verify(mockModel, times(0)).fieldCallback(eq(CREATE), eq(PRECOMMIT));
        verify(mockModel, times(0)).fieldCallback(eq(CREATE), eq(POSTCOMMIT));
        verify(mockModel, times(0)).fieldCallback(eq(UPDATE), eq(PRESECURITY));
        verify(mockModel, times(0)).fieldCallback(eq(UPDATE), eq(PRECOMMIT));
        verify(mockModel, times(0)).fieldCallback(eq(UPDATE), eq(POSTCOMMIT));
        verify(mockModel, times(0)).fieldCallback(eq(DELETE), eq(PRESECURITY));
        verify(mockModel, times(0)).fieldCallback(eq(DELETE), eq(PRECOMMIT));
        verify(mockModel, times(0)).fieldCallback(eq(DELETE), eq(POSTCOMMIT));

        verify(tx).preCommit();
        verify(tx).flush(any());
        verify(tx).commit(any());
        verify(tx).close();
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

        return new RequestScope(null, null, tx, user, null, getElideSettings(null, dict, MOCK_AUDIT_LOGGER));
    }
}
