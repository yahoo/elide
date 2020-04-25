/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import static com.yahoo.elide.Elide.JSONAPI_CONTENT_TYPE;
import static com.yahoo.elide.Elide.JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.Operation.CREATE;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.Operation.DELETE;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.Operation.READ;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.Operation.UPDATE;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.TransactionPhase.POSTCOMMIT;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.TransactionPhase.PRECOMMIT;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.TransactionPhase.PRESECURITY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.LifeCycleHookBinding;
import com.yahoo.elide.audit.AuditLogger;
import com.yahoo.elide.functions.LifeCycleHook;
import com.yahoo.elide.request.Attribute;
import com.yahoo.elide.request.EntityProjection;
import com.yahoo.elide.request.Relationship;
import com.yahoo.elide.security.ChangeSpec;
import com.yahoo.elide.security.TestUser;
import com.yahoo.elide.security.User;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;

import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Model used to mock different lifecycle test scenarios.  This model uses fields instead of properties.
 */
@Include(type = "testModel")
@LifeCycleHookBinding(hook = FieldTestModel.ClassPreSecurityHook.class, operation = CREATE, phase = PRESECURITY)
@LifeCycleHookBinding(hook = FieldTestModel.ClassPreCommitHook.class, operation = CREATE, phase = PRECOMMIT)
@LifeCycleHookBinding(hook = FieldTestModel.ClassPostCommitHook.class, operation = CREATE, phase = POSTCOMMIT)
@LifeCycleHookBinding(hook = FieldTestModel.ClassPreSecurityHook.class, operation = DELETE, phase = PRESECURITY)
@LifeCycleHookBinding(hook = FieldTestModel.ClassPreCommitHookEverything.class, operation = CREATE,
        phase = PRECOMMIT, oncePerRequest = false)
@LifeCycleHookBinding(hook = FieldTestModel.ClassPreCommitHook.class, operation = DELETE, phase = PRECOMMIT)
@LifeCycleHookBinding(hook = FieldTestModel.ClassPostCommitHook.class, operation = DELETE, phase = POSTCOMMIT)
@LifeCycleHookBinding(hook = FieldTestModel.ClassPreSecurityHook.class, operation = UPDATE, phase = PRESECURITY)
@LifeCycleHookBinding(hook = FieldTestModel.ClassPreCommitHook.class, operation = UPDATE, phase = PRECOMMIT)
@LifeCycleHookBinding(hook = FieldTestModel.ClassPostCommitHook.class, operation = UPDATE, phase = POSTCOMMIT)
@LifeCycleHookBinding(hook = FieldTestModel.ClassPreSecurityHook.class, operation = READ, phase = PRESECURITY)
@LifeCycleHookBinding(hook = FieldTestModel.ClassPreCommitHook.class, operation = READ, phase = PRECOMMIT)
@LifeCycleHookBinding(hook = FieldTestModel.ClassPostCommitHook.class, operation = READ, phase = POSTCOMMIT)
class FieldTestModel {

    @Id
    private String id;

    @Getter
    @Setter
    @LifeCycleHookBinding(hook = FieldTestModel.AttributePreSecurityHook.class, operation = CREATE, phase = PRESECURITY)
    @LifeCycleHookBinding(hook = FieldTestModel.AttributePreCommitHook.class, operation = CREATE, phase = PRECOMMIT)
    @LifeCycleHookBinding(hook = FieldTestModel.AttributePostCommitHook.class, operation = CREATE, phase = POSTCOMMIT)
    @LifeCycleHookBinding(hook = FieldTestModel.AttributePreSecurityHook.class, operation = DELETE, phase = PRESECURITY)
    @LifeCycleHookBinding(hook = FieldTestModel.AttributePreCommitHook.class, operation = DELETE, phase = PRECOMMIT)
    @LifeCycleHookBinding(hook = FieldTestModel.AttributePostCommitHook.class, operation = DELETE, phase = POSTCOMMIT)
    @LifeCycleHookBinding(hook = FieldTestModel.AttributePreSecurityHook.class, operation = UPDATE, phase = PRESECURITY)
    @LifeCycleHookBinding(hook = FieldTestModel.AttributePreCommitHook.class, operation = UPDATE, phase = PRECOMMIT)
    @LifeCycleHookBinding(hook = FieldTestModel.AttributePostCommitHook.class, operation = UPDATE, phase = POSTCOMMIT)
    @LifeCycleHookBinding(hook = FieldTestModel.AttributePreSecurityHook.class, operation = READ, phase = PRESECURITY)
    @LifeCycleHookBinding(hook = FieldTestModel.AttributePreCommitHook.class, operation = READ, phase = PRECOMMIT)
    @LifeCycleHookBinding(hook = FieldTestModel.AttributePostCommitHook.class, operation = READ, phase = POSTCOMMIT)
    private String field;

    @Getter
    @Setter
    @OneToMany
    @LifeCycleHookBinding(hook = FieldTestModel.RelationPreSecurityHook.class, operation = CREATE, phase = PRESECURITY)
    @LifeCycleHookBinding(hook = FieldTestModel.RelationPreCommitHook.class, operation = CREATE, phase = PRECOMMIT)
    @LifeCycleHookBinding(hook = FieldTestModel.RelationPostCommitHook.class, operation = CREATE, phase = POSTCOMMIT)
    @LifeCycleHookBinding(hook = FieldTestModel.RelationPreSecurityHook.class, operation = DELETE, phase = PRESECURITY)
    @LifeCycleHookBinding(hook = FieldTestModel.RelationPreCommitHook.class, operation = DELETE, phase = PRECOMMIT)
    @LifeCycleHookBinding(hook = FieldTestModel.RelationPostCommitHook.class, operation = DELETE, phase = POSTCOMMIT)
    @LifeCycleHookBinding(hook = FieldTestModel.RelationPreSecurityHook.class, operation = UPDATE, phase = PRESECURITY)
    @LifeCycleHookBinding(hook = FieldTestModel.RelationPreCommitHook.class, operation = UPDATE, phase = PRECOMMIT)
    @LifeCycleHookBinding(hook = FieldTestModel.RelationPostCommitHook.class, operation = UPDATE, phase = POSTCOMMIT)
    @LifeCycleHookBinding(hook = FieldTestModel.RelationPreSecurityHook.class, operation = READ, phase = PRESECURITY)
    @LifeCycleHookBinding(hook = FieldTestModel.RelationPreCommitHook.class, operation = READ, phase = PRECOMMIT)
    @LifeCycleHookBinding(hook = FieldTestModel.RelationPostCommitHook.class, operation = READ, phase = POSTCOMMIT)
    private Set<FieldTestModel> models = new HashSet<>();

    static class ClassPreSecurityHook implements LifeCycleHook<FieldTestModel> {
        @Override
        public void execute(LifeCycleHookBinding.Operation operation,
                            FieldTestModel elideEntity,
                            com.yahoo.elide.security.RequestScope requestScope,
                            Optional<ChangeSpec> changes) {
            elideEntity.classCallback(operation, PRESECURITY);
        }
    }

    static class ClassPreCommitHook implements LifeCycleHook<FieldTestModel> {
        @Override
        public void execute(LifeCycleHookBinding.Operation operation,
                            FieldTestModel elideEntity,
                            com.yahoo.elide.security.RequestScope requestScope,
                            Optional<ChangeSpec> changes) {
            elideEntity.classCallback(operation, PRECOMMIT);
        }
    }

    static class ClassPreCommitHookEverything implements LifeCycleHook<FieldTestModel> {
        @Override
        public void execute(LifeCycleHookBinding.Operation operation,
                            FieldTestModel elideEntity,
                            com.yahoo.elide.security.RequestScope requestScope,
                            Optional<ChangeSpec> changes) {
            elideEntity.classAllFieldsCallback(operation, PRECOMMIT);
        }
    }

    static class ClassPostCommitHook implements LifeCycleHook<FieldTestModel> {
        @Override
        public void execute(LifeCycleHookBinding.Operation operation,
                            FieldTestModel elideEntity,
                            com.yahoo.elide.security.RequestScope requestScope,
                            Optional<ChangeSpec> changes) {
            elideEntity.classCallback(operation, POSTCOMMIT);
        }
    }

    static class AttributePreSecurityHook implements LifeCycleHook<FieldTestModel> {
        @Override
        public void execute(LifeCycleHookBinding.Operation operation,
                            FieldTestModel elideEntity,
                            com.yahoo.elide.security.RequestScope requestScope,
                            Optional<ChangeSpec> changes) {
            elideEntity.attributeCallback(operation, PRESECURITY, changes.orElse(null));
        }
    }

    static class AttributePreCommitHook implements LifeCycleHook<FieldTestModel> {
        @Override
        public void execute(LifeCycleHookBinding.Operation operation,
                            FieldTestModel elideEntity,
                            com.yahoo.elide.security.RequestScope requestScope,
                            Optional<ChangeSpec> changes) {
            elideEntity.attributeCallback(operation, PRECOMMIT, changes.orElse(null));
        }
    }

    static class AttributePostCommitHook implements LifeCycleHook<FieldTestModel> {
        @Override
        public void execute(LifeCycleHookBinding.Operation operation,
                            FieldTestModel elideEntity,
                            com.yahoo.elide.security.RequestScope requestScope,
                            Optional<ChangeSpec> changes) {
            elideEntity.attributeCallback(operation, POSTCOMMIT, changes.orElse(null));
        }
    }

    static class RelationPreSecurityHook implements LifeCycleHook<FieldTestModel> {
        @Override
        public void execute(LifeCycleHookBinding.Operation operation,
                            FieldTestModel elideEntity,
                            com.yahoo.elide.security.RequestScope requestScope,
                            Optional<ChangeSpec> changes) {
            elideEntity.relationCallback(operation, PRESECURITY, changes.orElse(null));
        }
    }

    static class RelationPreCommitHook implements LifeCycleHook<FieldTestModel> {
        @Override
        public void execute(LifeCycleHookBinding.Operation operation,
                            FieldTestModel elideEntity,
                            com.yahoo.elide.security.RequestScope requestScope,
                            Optional<ChangeSpec> changes) {
            elideEntity.relationCallback(operation, PRECOMMIT, changes.orElse(null));
        }
    }

    static class RelationPostCommitHook implements LifeCycleHook<FieldTestModel> {
        @Override
        public void execute(LifeCycleHookBinding.Operation operation,
                            FieldTestModel elideEntity,
                            com.yahoo.elide.security.RequestScope requestScope,
                            Optional<ChangeSpec> changes) {
            elideEntity.relationCallback(operation, POSTCOMMIT, changes.orElse(null));
        }
    }

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

    public void classAllFieldsCallback(LifeCycleHookBinding.Operation operation,
                                       LifeCycleHookBinding.TransactionPhase phase) {
        //NOOP - this will be mocked to verify hook invocation.
    }
}

/**
 * Model used to mock different lifecycle test scenarios.  This model uses properties instead of fields.
 */
@Include
class PropertyTestModel {
    private String id;

    private Set<PropertyTestModel> models = new HashSet<>();

    static class RelationPostCommitHook implements LifeCycleHook<PropertyTestModel> {
        @Override
        public void execute(LifeCycleHookBinding.Operation operation,
                            PropertyTestModel elideEntity,
                            com.yahoo.elide.security.RequestScope requestScope,
                            Optional<ChangeSpec> changes) {
            elideEntity.relationCallback(operation, POSTCOMMIT, changes.orElse(null));
        }
    }

    @Id
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @ManyToMany
    @LifeCycleHookBinding(hook = PropertyTestModel.RelationPostCommitHook.class,
            operation = CREATE, phase = POSTCOMMIT)
    @LifeCycleHookBinding(hook = PropertyTestModel.RelationPostCommitHook.class,
            operation = UPDATE, phase = POSTCOMMIT)
    public Set<PropertyTestModel> getModels() {
        return models;
    }

    public void setModels(Set<PropertyTestModel> models) {
        this.models = models;
    }

    public void relationCallback(LifeCycleHookBinding.Operation operation,
                                 LifeCycleHookBinding.TransactionPhase phase,
                                 ChangeSpec changes) {
        //NOOP - this will be mocked to verify hook invocation.
    }
}

/**
 * Tests the invocation & sequencing of DataStoreTransaction method invocations and life cycle events.
 */
public class LifeCycleTest {

    private static final AuditLogger MOCK_AUDIT_LOGGER = mock(AuditLogger.class);
    private EntityDictionary dictionary;

    LifeCycleTest() throws Exception {
        dictionary = TestDictionary.getTestDictionary();
        dictionary.bindEntity(FieldTestModel.class);
        dictionary.bindEntity(PropertyTestModel.class);
    }

    @Test
    public void testElideCreate() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        FieldTestModel mockModel = mock(FieldTestModel.class);

        Elide elide = getElide(store, dictionary, MOCK_AUDIT_LOGGER);

        String body = "{\"data\": {\"type\":\"testModel\",\"id\":\"1\",\"attributes\": {\"field\":\"Foo\"}}}";

        when(store.beginTransaction()).thenReturn(tx);
        when(tx.createNewObject(FieldTestModel.class)).thenReturn(mockModel);

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

        verify(mockModel, times(2)).classAllFieldsCallback(any(), any());
        verify(mockModel, times(2)).classAllFieldsCallback(eq(CREATE), eq(PRECOMMIT));

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
        FieldTestModel mockModel = mock(FieldTestModel.class);
        doThrow(RuntimeException.class).when(mockModel).setField(anyString());

        Elide elide = getElide(store, dictionary, MOCK_AUDIT_LOGGER);

        String body = "{\"data\": {\"type\":\"testModel\",\"id\":\"1\",\"attributes\": {\"field\":\"Foo\"}}}";

        when(store.beginTransaction()).thenReturn(tx);
        when(tx.createNewObject(FieldTestModel.class)).thenReturn(mockModel);

        ElideResponse response = elide.post("/testModel", body, null);
        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getResponseCode());
        assertEquals(
                "{\"errors\":[{\"detail\":\"Unexpected exception caught\"}]}",
                response.getBody());

        verify(mockModel, never()).classCallback(any(), any());
        verify(mockModel, never()).attributeCallback(any(), any(), any());
        verify(mockModel, never()).relationCallback(any(), any(), any());
        verify(mockModel, never()).classAllFieldsCallback(any(), any());

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
        FieldTestModel mockModel = mock(FieldTestModel.class);

        Elide elide = getElide(store, dictionary, MOCK_AUDIT_LOGGER);

        when(store.beginReadTransaction()).thenCallRealMethod();
        when(store.beginTransaction()).thenReturn(tx);
        when(tx.loadObject(isA(EntityProjection.class), any(), isA(RequestScope.class))).thenReturn(mockModel);

        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        ElideResponse response = elide.get("/testModel/1", headers, null);
        assertEquals(HttpStatus.SC_OK, response.getResponseCode());

        verify(mockModel, never()).classAllFieldsCallback(any(), any());

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
        FieldTestModel mockModel = mock(FieldTestModel.class);

        Elide elide = getElide(store, dictionary, MOCK_AUDIT_LOGGER);

        when(store.beginReadTransaction()).thenCallRealMethod();
        when(store.beginTransaction()).thenReturn(tx);
        when(tx.loadObject(isA(EntityProjection.class), any(), isA(RequestScope.class))).thenReturn(mockModel);

        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.putSingle("fields[testModel]", "field");
        ElideResponse response = elide.get("/testModel/1", headers, null);
        assertEquals(HttpStatus.SC_OK, response.getResponseCode());

        verify(mockModel, never()).classAllFieldsCallback(any(), any());

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

        verify(mockModel, never()).relationCallback(any(), any(), any());

        verify(tx).preCommit();
        verify(tx).flush(any());
        verify(tx).commit(any());
        verify(tx).close();
    }

    @Test
    public void testElideGetRelationship() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        FieldTestModel mockModel = mock(FieldTestModel.class);
        FieldTestModel child = mock(FieldTestModel.class);
        when(mockModel.getModels()).thenReturn(ImmutableSet.of(child));

        Elide elide = getElide(store, dictionary, MOCK_AUDIT_LOGGER);

        when(store.beginReadTransaction()).thenCallRealMethod();
        when(store.beginTransaction()).thenReturn(tx);
        when(tx.loadObject(isA(EntityProjection.class), any(), isA(RequestScope.class))).thenReturn(mockModel);

        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        ElideResponse response = elide.get("/testModel/1/relationships/models", headers, null);
        assertEquals(HttpStatus.SC_OK, response.getResponseCode());

        verify(mockModel, never()).classAllFieldsCallback(any(), any());

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
        FieldTestModel mockModel = mock(FieldTestModel.class);

        Elide elide = getElide(store, dictionary, MOCK_AUDIT_LOGGER);

        String body = "{\"data\": {\"type\":\"testModel\",\"id\":\"1\",\"attributes\": {\"field\":\"Foo\"}}}";

        dictionary.setValue(mockModel, "id", "1");
        when(store.beginTransaction()).thenReturn(tx);
        when(tx.loadObject(isA(EntityProjection.class), any(), isA(RequestScope.class))).thenReturn(mockModel);

        String contentType = JSONAPI_CONTENT_TYPE;
        ElideResponse response = elide.patch(contentType, contentType, "/testModel/1", body, null);
        assertEquals(HttpStatus.SC_NO_CONTENT, response.getResponseCode());

        verify(mockModel, never()).classAllFieldsCallback(any(), any());

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
        FieldTestModel mockModel = mock(FieldTestModel.class);

        Elide elide = getElide(store, dictionary, MOCK_AUDIT_LOGGER);

        dictionary.setValue(mockModel, "id", "1");
        when(store.beginTransaction()).thenReturn(tx);
        when(tx.loadObject(isA(EntityProjection.class), any(), isA(RequestScope.class))).thenReturn(mockModel);

        ElideResponse response = elide.delete("/testModel/1", "", null);
        assertEquals(HttpStatus.SC_NO_CONTENT, response.getResponseCode());

        verify(mockModel, never()).classAllFieldsCallback(any(), any());

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
        FieldTestModel mockModel = mock(FieldTestModel.class);

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

        verify(mockModel, never()).classAllFieldsCallback(any(), any());

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
    public void testElidePatchExtensionCreate() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        FieldTestModel mockModel = mock(FieldTestModel.class);

        Elide elide = getElide(store, dictionary, MOCK_AUDIT_LOGGER);

        String body = "[{\"op\": \"add\",\"path\": \"/testModel\",\"value\":{"
                + "\"type\":\"testModel\",\"id\":\"1\",\"attributes\": {\"field\":\"Foo\"}}}]";

        when(store.beginTransaction()).thenReturn(tx);
        when(tx.createNewObject(FieldTestModel.class)).thenReturn(mockModel);

        String contentType = JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION;
        ElideResponse response = elide.patch(contentType, contentType, "/", body, null);
        assertEquals(HttpStatus.SC_OK, response.getResponseCode());

        verify(mockModel, times(1)).classCallback(eq(READ), eq(PRESECURITY));
        verify(mockModel, times(1)).classCallback(eq(READ), eq(PRECOMMIT));
        verify(mockModel, times(1)).classCallback(eq(READ), eq(POSTCOMMIT));
        verify(mockModel, times(1)).classCallback(eq(CREATE), eq(PRESECURITY));
        verify(mockModel, times(1)).classCallback(eq(CREATE), eq(PRECOMMIT));
        verify(mockModel, times(1)).classCallback(eq(CREATE), eq(POSTCOMMIT));
        verify(mockModel, never()).classCallback(eq(UPDATE), any());
        verify(mockModel, never()).classCallback(eq(DELETE), any());

        verify(mockModel, times(2)).classAllFieldsCallback(any(), any());
        verify(mockModel, times(2)).classAllFieldsCallback(eq(CREATE), eq(PRECOMMIT));

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
    public void failElidePatchExtensionCreate() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        FieldTestModel mockModel = mock(FieldTestModel.class);

        Elide elide = getElide(store, dictionary, MOCK_AUDIT_LOGGER);

        String body = "[{\"op\": \"add\",\"path\": \"/testModel\",\"value\":{"
                + "\"type\":\"testModel\",\"attributes\": {\"field\":\"Foo\"}}}]";

        when(store.beginTransaction()).thenReturn(tx);
        when(tx.createNewObject(FieldTestModel.class)).thenReturn(mockModel);

        String contentType = JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION;
        ElideResponse response = elide.patch(contentType, contentType, "/", body, null);
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getResponseCode());
        assertTrue(response.getBody().startsWith(
                "[{\"errors\":[{\"detail\":\"Invalid value: Resource(type=testModel, id=null, attributes={field=null}, relationships={models=com.yahoo.elide.jsonapi.models.Relationship@"));

        verify(tx, never()).preCommit();
        verify(tx, never()).flush(isA(RequestScope.class));
        verify(tx, never()).commit(isA(RequestScope.class));
        verify(tx).close();
    }

    @Test
    public void testElidePatchExtensionUpdate() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        FieldTestModel mockModel = mock(FieldTestModel.class);

        Elide elide = getElide(store, dictionary, MOCK_AUDIT_LOGGER);

        String body = "[{\"op\": \"replace\",\"path\": \"/testModel/1\",\"value\":{"
                + "\"type\":\"testModel\",\"id\":\"1\",\"attributes\": {\"field\":\"Foo\"}}}]";

        dictionary.setValue(mockModel, "id", "1");
        when(store.beginTransaction()).thenReturn(tx);
        when(tx.loadObject(isA(EntityProjection.class), any(), isA(RequestScope.class))).thenReturn(mockModel);

        String contentType = JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION;
        ElideResponse response = elide.patch(contentType, contentType, "/", body, null);
        assertEquals(HttpStatus.SC_OK, response.getResponseCode());

        verify(mockModel, never()).classAllFieldsCallback(any(), any());

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
    public void testElidePatchExtensionDelete() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        FieldTestModel mockModel = mock(FieldTestModel.class);

        Elide elide = getElide(store, dictionary, MOCK_AUDIT_LOGGER);

        dictionary.setValue(mockModel, "id", "1");
        when(store.beginTransaction()).thenReturn(tx);
        when(tx.loadObject(isA(EntityProjection.class), any(), isA(RequestScope.class))).thenReturn(mockModel);

        String body = "[{\"op\": \"remove\",\"path\": \"/testModel\",\"value\":{"
                + "\"type\":\"testModel\",\"id\":\"1\"}}]";

        String contentType = JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION;
        ElideResponse response = elide.patch(contentType, contentType, "/", body, null);
        assertEquals(HttpStatus.SC_OK, response.getResponseCode());

        verify(mockModel, never()).classAllFieldsCallback(any(), any());

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
    public void testCreate() {
        FieldTestModel mockModel = mock(FieldTestModel.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        when(tx.createNewObject(FieldTestModel.class)).thenReturn(mockModel);
        RequestScope scope = buildRequestScope(dictionary, tx);
        PersistentResource resource = PersistentResource.createObject(FieldTestModel.class, scope, Optional.of("1"));
        resource.setValueChecked("field", "should not affect calls since this is create!");

        verify(mockModel, never()).classCallback(any(), any());
        verify(mockModel, never()).attributeCallback(any(), any(), any());
        verify(mockModel, never()).relationCallback(any(), any(), any());
        verify(mockModel, never()).classAllFieldsCallback(any(), any());

        scope.runQueuedPreSecurityTriggers();

        verify(mockModel, times(1)).classCallback(any(), any());
        verify(mockModel, times(1)).classCallback(eq(CREATE), eq(PRESECURITY));
        verify(mockModel, times(1)).attributeCallback(any(), any(), any());
        verify(mockModel, times(1)).attributeCallback(eq(CREATE), eq(PRESECURITY), any());
        verify(mockModel, times(1)).relationCallback(any(), any(), any());
        verify(mockModel, times(1)).relationCallback(eq(CREATE), eq(PRESECURITY), any());
        verify(mockModel, never()).classAllFieldsCallback(any(), any());

        clearInvocations(mockModel);
        scope.runQueuedPreCommitTriggers();

        verify(mockModel, times(1)).classCallback(any(), any());
        verify(mockModel, times(1)).classCallback(eq(CREATE), eq(PRECOMMIT));
        verify(mockModel, times(1)).attributeCallback(any(), any(), any());
        verify(mockModel, times(1)).attributeCallback(eq(CREATE), eq(PRECOMMIT), any());
        verify(mockModel, times(1)).relationCallback(any(), any(), any());
        verify(mockModel, times(1)).relationCallback(eq(CREATE), eq(PRECOMMIT), any());
        verify(mockModel, times(2)).classAllFieldsCallback(any(), any());
        verify(mockModel, times(2)).classAllFieldsCallback(eq(CREATE), eq(PRECOMMIT));

        clearInvocations(mockModel);
        scope.getPermissionExecutor().executeCommitChecks();
        scope.runQueuedPostCommitTriggers();

        verify(mockModel, never()).classAllFieldsCallback(any(), any());
        verify(mockModel, times(1)).classCallback(any(), any());
        verify(mockModel, times(1)).classCallback(eq(CREATE), eq(POSTCOMMIT));
        verify(mockModel, times(1)).attributeCallback(any(), any(), any());
        verify(mockModel, times(1)).attributeCallback(eq(CREATE), eq(POSTCOMMIT), any());
        verify(mockModel, times(1)).relationCallback(any(), any(), any());
        verify(mockModel, times(1)).relationCallback(eq(CREATE), eq(POSTCOMMIT), any());
    }

    @Test
    public void testRead() {
        FieldTestModel mockModel = mock(FieldTestModel.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        when(tx.createNewObject(FieldTestModel.class)).thenReturn(mockModel);
        RequestScope scope = buildRequestScope(dictionary, tx);
        PersistentResource resource = new PersistentResource(mockModel, null, "1", scope);

        resource.getValueChecked(Attribute.builder().type(String.class).name("field").build());

        verify(mockModel, times(1)).classCallback(any(), any());
        verify(mockModel, times(1)).classCallback(eq(READ), eq(PRESECURITY));
        verify(mockModel, times(1)).attributeCallback(any(), any(), any());
        verify(mockModel, times(1)).attributeCallback(eq(READ), eq(PRESECURITY), any());
        verify(mockModel, never()).classAllFieldsCallback(any(), any());
        verify(mockModel, never()).relationCallback(any(), any(), any());

        clearInvocations(mockModel);
        scope.runQueuedPreSecurityTriggers();

        verify(mockModel, never()).classCallback(any(), any());
        verify(mockModel, never()).classAllFieldsCallback(any(), any());
        verify(mockModel, never()).relationCallback(any(), any(), any());
        verify(mockModel, never()).attributeCallback(any(), any(), any());

        clearInvocations(mockModel);
        scope.runQueuedPreCommitTriggers();

        verify(mockModel, times(1)).classCallback(any(), any());
        verify(mockModel, times(1)).classCallback(eq(READ), eq(PRECOMMIT));
        verify(mockModel, times(1)).attributeCallback(any(), any(), any());
        verify(mockModel, times(1)).attributeCallback(eq(READ), eq(PRECOMMIT), any());
        verify(mockModel, never()).classAllFieldsCallback(any(), any());
        verify(mockModel, never()).relationCallback(any(), any(), any());

        clearInvocations(mockModel);
        scope.runQueuedPostCommitTriggers();

        verify(mockModel, times(1)).classCallback(any(), any());
        verify(mockModel, times(1)).classCallback(eq(READ), eq(POSTCOMMIT));
        verify(mockModel, times(1)).attributeCallback(any(), any(), any());
        verify(mockModel, times(1)).attributeCallback(eq(READ), eq(POSTCOMMIT), any());
        verify(mockModel, never()).classAllFieldsCallback(any(), any());
        verify(mockModel, never()).relationCallback(any(), any(), any());
    }

    @Test
    public void testDelete() {
        FieldTestModel mockModel = mock(FieldTestModel.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        when(tx.createNewObject(FieldTestModel.class)).thenReturn(mockModel);
        RequestScope scope = buildRequestScope(dictionary, tx);
        PersistentResource resource = new PersistentResource(mockModel, null, "1", scope);

        resource.deleteResource();

        verify(mockModel, times(2)).classCallback(any(), any());
        verify(mockModel, times(1)).classCallback(eq(DELETE), eq(PRESECURITY));

        //TODO - DELETE should not invoke READ.
        verify(mockModel, times(1)).classCallback(eq(READ), eq(PRESECURITY));
        verify(mockModel, times(1)).relationCallback(any(), any(), any());
        verify(mockModel, times(1)).relationCallback(eq(READ), eq(PRESECURITY), any());
        verify(mockModel, never()).classAllFieldsCallback(any(), any());
        verify(mockModel, never()).attributeCallback(any(), any(), any());

        clearInvocations(mockModel);
        scope.runQueuedPreSecurityTriggers();

        verify(mockModel, never()).classAllFieldsCallback(any(), any());
        verify(mockModel, never()).classCallback(any(), any());
        verify(mockModel, never()).attributeCallback(any(), any(), any());
        verify(mockModel, never()).relationCallback(any(), any(), any());

        scope.runQueuedPreCommitTriggers();

        verify(mockModel, times(2)).classCallback(any(), any());
        verify(mockModel, times(1)).classCallback(eq(DELETE), eq(PRECOMMIT));

        //TODO - DELETE should not invoke READ.
        verify(mockModel, times(1)).classCallback(eq(READ), eq(PRECOMMIT));
        verify(mockModel, times(1)).relationCallback(any(), any(), any());
        verify(mockModel, times(1)).relationCallback(eq(READ), eq(PRECOMMIT), any());
        verify(mockModel, never()).classAllFieldsCallback(any(), any());
        verify(mockModel, never()).attributeCallback(any(), any(), any());

        clearInvocations(mockModel);
        scope.getPermissionExecutor().executeCommitChecks();
        scope.runQueuedPostCommitTriggers();

        verify(mockModel, times(2)).classCallback(any(), any());
        verify(mockModel, times(1)).classCallback(eq(DELETE), eq(POSTCOMMIT));

        //TODO - DELETE should not invoke READ.
        verify(mockModel, times(1)).classCallback(eq(READ), eq(POSTCOMMIT));
        verify(mockModel, times(1)).relationCallback(any(), any(), any());
        verify(mockModel, times(1)).relationCallback(eq(READ), eq(POSTCOMMIT), any());
        verify(mockModel, never()).classAllFieldsCallback(any(), any());
        verify(mockModel, never()).attributeCallback(any(), any(), any());
    }

    @Test
    public void testAttributeUpdate() {
        FieldTestModel mockModel = mock(FieldTestModel.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        when(tx.createNewObject(FieldTestModel.class)).thenReturn(mockModel);
        RequestScope scope = buildRequestScope(dictionary, tx);

        PersistentResource resource = new PersistentResource(mockModel, null, scope.getUUIDFor(mockModel), scope);
        resource.setValueChecked("field", "new value");

        verify(mockModel, times(1)).classCallback(any(), any());
        verify(mockModel, times(1)).classCallback(eq(UPDATE), eq(PRESECURITY));
        verify(mockModel, times(1)).attributeCallback(any(), any(), any());
        verify(mockModel, times(1)).attributeCallback(eq(UPDATE), eq(PRESECURITY), notNull());

        verify(mockModel, never()).relationCallback(any(), any(), any());
        verify(mockModel, never()).classAllFieldsCallback(any(), any());

        clearInvocations(mockModel);
        scope.runQueuedPreSecurityTriggers();

        verify(mockModel, never()).classAllFieldsCallback(any(), any());
        verify(mockModel, never()).relationCallback(any(), any(), any());
        verify(mockModel, never()).attributeCallback(any(), any(), any());

        clearInvocations(mockModel);
        scope.runQueuedPreCommitTriggers();

        verify(mockModel, never()).relationCallback(any(), any(), any());
        verify(mockModel, never()).classAllFieldsCallback(any(), any());

        verify(mockModel, times(1)).classCallback(any(), any());
        verify(mockModel, times(1)).classCallback(eq(UPDATE), eq(PRECOMMIT));
        verify(mockModel, times(1)).attributeCallback(any(), any(), any());
        verify(mockModel, times(1)).attributeCallback(eq(UPDATE), eq(PRECOMMIT), notNull());

        clearInvocations(mockModel);
        scope.getPermissionExecutor().executeCommitChecks();
        scope.runQueuedPostCommitTriggers();

        verify(mockModel, never()).relationCallback(any(), any(), any());
        verify(mockModel, never()).classAllFieldsCallback(any(), any());

        verify(mockModel, times(1)).classCallback(any(), any());
        verify(mockModel, times(1)).classCallback(eq(UPDATE), eq(POSTCOMMIT));
        verify(mockModel, times(1)).attributeCallback(any(), any(), any());
        verify(mockModel, times(1)).attributeCallback(eq(UPDATE), eq(POSTCOMMIT), notNull());
    }

    @Test
    public void testRelationshipUpdate() {
        FieldTestModel mockModel = mock(FieldTestModel.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        when(tx.createNewObject(FieldTestModel.class)).thenReturn(mockModel);
        RequestScope scope = buildRequestScope(dictionary, tx);

        FieldTestModel modelToAdd = mock(FieldTestModel.class);

        PersistentResource resource = new PersistentResource(mockModel, null, scope.getUUIDFor(mockModel), scope);
        PersistentResource resourceToAdd = new PersistentResource(modelToAdd, null, scope.getUUIDFor(mockModel), scope);

        resource.addRelation("models", resourceToAdd);

        verify(mockModel, times(1)).classCallback(any(), any());
        verify(mockModel, times(1)).classCallback(eq(UPDATE), eq(PRESECURITY));

        //TODO - this should be only called once.  THis is called twice because the mock has a null collection.
        verify(mockModel, times(2)).relationCallback(any(), any(), any());
        verify(mockModel, times(2)).relationCallback(eq(UPDATE), eq(PRESECURITY), notNull());

        verify(mockModel, never()).attributeCallback(any(), any(), any());
        verify(mockModel, never()).classAllFieldsCallback(any(), any());

        clearInvocations(mockModel);
        scope.runQueuedPreSecurityTriggers();

        verify(mockModel, never()).classAllFieldsCallback(any(), any());
        verify(mockModel, never()).relationCallback(any(), any(), any());
        verify(mockModel, never()).attributeCallback(any(), any(), any());

        clearInvocations(mockModel);
        scope.runQueuedPreCommitTriggers();

        verify(mockModel, never()).attributeCallback(any(), any(), any());
        verify(mockModel, never()).classAllFieldsCallback(any(), any());

        verify(mockModel, times(1)).classCallback(any(), any());
        verify(mockModel, times(1)).classCallback(eq(UPDATE), eq(PRECOMMIT));
        //TODO - this should be only called once.
        verify(mockModel, times(2)).relationCallback(any(), any(), any());
        verify(mockModel, times(2)).relationCallback(eq(UPDATE), eq(PRECOMMIT), notNull());

        clearInvocations(mockModel);
        scope.getPermissionExecutor().executeCommitChecks();
        scope.runQueuedPostCommitTriggers();

        verify(mockModel, never()).attributeCallback(any(), any(), any());
        verify(mockModel, never()).classAllFieldsCallback(any(), any());

        verify(mockModel, times(1)).classCallback(any(), any());
        verify(mockModel, times(1)).classCallback(eq(UPDATE), eq(POSTCOMMIT));
        //TODO - this should be only called once.
        verify(mockModel, times(2)).relationCallback(any(), any(), any());
        verify(mockModel, times(2)).relationCallback(eq(UPDATE), eq(POSTCOMMIT), notNull());
    }

    @Test
    public void testAddToCollectionTrigger() {
        PropertyTestModel mockModel = mock(PropertyTestModel.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        when(tx.createNewObject(PropertyTestModel.class)).thenReturn(mockModel);
        RequestScope scope = buildRequestScope(dictionary, tx);

        PropertyTestModel modelToAdd = mock(PropertyTestModel.class);

        //First we test adding to a newly created object.
        PersistentResource resource = PersistentResource.createObject(PropertyTestModel.class, scope, Optional.of("1"));
        PersistentResource resourceToAdd = new PersistentResource(modelToAdd, null, scope.getUUIDFor(mockModel), scope);

        resource.updateRelation("models", new HashSet<>(Arrays.asList(resourceToAdd)));

        scope.runQueuedPreSecurityTriggers();
        scope.runQueuedPreCommitTriggers();
        scope.runQueuedPostCommitTriggers();

        verify(mockModel, never()).relationCallback(eq(UPDATE), any(), any());
        verify(mockModel, times(1)).relationCallback(eq(CREATE), eq(POSTCOMMIT), notNull());

        //Build another resource, scope & reset the mock to do a pure update (no create):
        scope = buildRequestScope(dictionary, tx);
        resource = new PersistentResource(mockModel, null, scope.getUUIDFor(mockModel), scope);
        reset(mockModel);

        resource.updateRelation("models", new HashSet<>(Arrays.asList(resourceToAdd)));

        scope.runQueuedPreSecurityTriggers();
        scope.runQueuedPreCommitTriggers();
        scope.runQueuedPostCommitTriggers();

        verify(mockModel, never()).relationCallback(eq(CREATE), any(), any());
        verify(mockModel, times(1)).relationCallback(eq(UPDATE), eq(POSTCOMMIT), notNull());
    }

    @Test
    public void testRemoveFromCollectionTrigger() {
        PropertyTestModel mockModel = mock(PropertyTestModel.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        when(tx.createNewObject(PropertyTestModel.class)).thenReturn(mockModel);
        RequestScope scope = buildRequestScope(dictionary, tx);

        PropertyTestModel childModel1 = mock(PropertyTestModel.class);
        PropertyTestModel childModel2 = mock(PropertyTestModel.class);
        when(childModel1.getId()).thenReturn("2");
        when(childModel2.getId()).thenReturn("3");

        //First we test removing from a newly created object.
        PersistentResource resource = PersistentResource.createObject(PropertyTestModel.class, scope, Optional.of("1"));
        PersistentResource childResource1 = new PersistentResource(childModel1, null, "2", scope);
        PersistentResource childResource2 = new PersistentResource(childModel2, null, "3", scope);

        resource.updateRelation("models", new HashSet<>(Arrays.asList(childResource1, childResource2)));

        scope.runQueuedPreSecurityTriggers();
        scope.runQueuedPreCommitTriggers();
        scope.runQueuedPostCommitTriggers();

        verify(mockModel, never()).relationCallback(eq(UPDATE), any(), any());
        verify(mockModel, times(2)).relationCallback(eq(CREATE), eq(POSTCOMMIT), notNull());

        //Build another resource, scope & reset the mock to do a pure update (no create):
        scope = buildRequestScope(dictionary, tx);
        resource = new PersistentResource(mockModel, null, scope.getUUIDFor(mockModel), scope);
        reset(mockModel);
        Relationship relationship = Relationship.builder()
                .projection(EntityProjection.builder()
                    .type(PropertyTestModel.class)
                    .build())
                .name("models")
                .build();

        when(tx.getRelation(tx, mockModel, relationship, scope)).thenReturn(Arrays.asList(childModel1, childModel2));

        resource.updateRelation("models", new HashSet<>(Arrays.asList(childResource1)));

        scope.runQueuedPreSecurityTriggers();
        scope.runQueuedPreCommitTriggers();
        scope.runQueuedPostCommitTriggers();

        verify(mockModel, never()).relationCallback(eq(CREATE), any(), any());
        verify(mockModel, times(1)).relationCallback(eq(UPDATE), eq(POSTCOMMIT), notNull());
    }

    @Test
    public void testPreCommitLifecycleHookException() {
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        FieldTestModel testModel = mock(FieldTestModel.class);

        doThrow(IllegalStateException.class)
                .when(testModel)
                .attributeCallback(eq(UPDATE), eq(PRECOMMIT), any(ChangeSpec.class));

        RequestScope scope = buildRequestScope(dictionary, tx);
        PersistentResource resource = new PersistentResource(testModel, null, "1", scope);
        resource.updateAttribute("field", "New value");
        scope.runQueuedPreSecurityTriggers();
        assertThrows(IllegalStateException.class, () -> scope.runQueuedPreCommitTriggers());
    }

    @Test
    public void testPostCommitLifecycleHookException() {
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        FieldTestModel testModel = mock(FieldTestModel.class);

        doThrow(IllegalStateException.class)
                .when(testModel)
                .attributeCallback(eq(UPDATE), eq(POSTCOMMIT), any(ChangeSpec.class));

        RequestScope scope = buildRequestScope(dictionary, tx);
        PersistentResource resource = new PersistentResource(testModel, null, "1", scope);
        resource.updateAttribute("field", "New value");
        scope.runQueuedPreSecurityTriggers();
        scope.runQueuedPreCommitTriggers();
        assertThrows(IllegalStateException.class, () -> scope.runQueuedPostCommitTriggers());
    }

    @Test
    public void testPreSecurityLifecycleHookException() {
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        FieldTestModel testModel = mock(FieldTestModel.class);

        doThrow(IllegalStateException.class)
                .when(testModel)
                .attributeCallback(eq(UPDATE), eq(PRESECURITY), any(ChangeSpec.class));

        RequestScope scope = buildRequestScope(dictionary, tx);
        PersistentResource resource = new PersistentResource(testModel, null, "1", scope);

        assertThrows(IllegalStateException.class, () -> resource.updateAttribute("field", "New value"));
    }

    private Elide getElide(DataStore dataStore, EntityDictionary dictionary, AuditLogger auditLogger) {
        return new Elide(getElideSettings(dataStore, dictionary, auditLogger));
    }

    private ElideSettings getElideSettings(DataStore dataStore, EntityDictionary dictionary, AuditLogger auditLogger) {
        return new ElideSettingsBuilder(dataStore)
                .withEntityDictionary(dictionary)
                .withAuditLogger(auditLogger)
                .withVerboseErrors()
                .build();
    }

    private RequestScope buildRequestScope(EntityDictionary dict, DataStoreTransaction tx) {
        User user = new TestUser("1");

        return new RequestScope(null, null, tx, user, null,
                getElideSettings(null, dict, MOCK_AUDIT_LOGGER));
    }
}
