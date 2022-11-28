/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.lifecycle;

import static com.yahoo.elide.Elide.JSONAPI_CONTENT_TYPE;
import static com.yahoo.elide.Elide.JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.Operation.CREATE;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.Operation.DELETE;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.Operation.UPDATE;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.TransactionPhase.POSTCOMMIT;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.TransactionPhase.PRECOMMIT;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.TransactionPhase.PREFLUSH;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.TransactionPhase.PRESECURITY;
import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.TransactionRegistry;
import com.yahoo.elide.core.audit.AuditLogger;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.datastore.DataStoreIterable;
import com.yahoo.elide.core.datastore.DataStoreIterableBuilder;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.dictionary.TestDictionary;
import com.yahoo.elide.core.exceptions.HttpStatus;
import com.yahoo.elide.core.request.Attribute;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.request.Relationship;
import com.yahoo.elide.core.security.ChangeSpec;
import com.yahoo.elide.core.security.TestUser;
import com.yahoo.elide.core.security.User;
import com.yahoo.elide.core.type.ClassType;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.validation.ConstraintViolationException;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Tests the invocation & sequencing of DataStoreTransaction method invocations and life cycle events.
 */
public class LifeCycleTest {

    private final String baseUrl = "http://localhost:8080/api/v1";
    private static final AuditLogger MOCK_AUDIT_LOGGER = mock(AuditLogger.class);
    private EntityDictionary dictionary;

    LifeCycleTest() throws Exception {
        dictionary = TestDictionary.getTestDictionary();
        dictionary.bindEntity(FieldTestModel.class);
        dictionary.bindEntity(PropertyTestModel.class);
        dictionary.bindEntity(LegacyTestModel.class);
        dictionary.bindEntity(ErrorTestModel.class);
    }

    @Test
    public void testLifecycleError() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        ErrorTestModel mockModel = mock(ErrorTestModel.class);

        Elide elide = getElide(store, dictionary, MOCK_AUDIT_LOGGER);

        String body = "{\"data\": {\"type\":\"errorTestModel\",\"id\":\"1\",\"attributes\": {\"field\":\"Foo\"}}}";

        when(store.beginTransaction()).thenReturn(tx);
        when(tx.createNewObject(eq(ClassType.of(ErrorTestModel.class)), any())).thenReturn(mockModel);

        ElideResponse response = elide.post(baseUrl, "/errorTestModel", body, null, NO_VERSION);
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getResponseCode());
        assertEquals("{\"errors\":[{\"detail\":\"Invalid\"}]}", response.getBody());
    }

    @Test
    public void testElideCreate() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        FieldTestModel mockModel = mock(FieldTestModel.class);

        Elide elide = getElide(store, dictionary, MOCK_AUDIT_LOGGER);

        String body = "{\"data\": {\"type\":\"testModel\",\"id\":\"1\",\"attributes\": {\"field\":\"Foo\"}}}";

        when(store.beginTransaction()).thenReturn(tx);
        when(tx.createNewObject(eq(ClassType.of(FieldTestModel.class)), any())).thenReturn(mockModel);

        ElideResponse response = elide.post(baseUrl, "/testModel", body, null, NO_VERSION);
        assertEquals(HttpStatus.SC_CREATED, response.getResponseCode());

        verify(mockModel, times(1)).classCallback(eq(CREATE), eq(PRESECURITY));
        verify(mockModel, times(1)).classCallback(eq(CREATE), eq(PREFLUSH));
        verify(mockModel, times(1)).classCallback(eq(CREATE), eq(PRECOMMIT));
        verify(mockModel, times(1)).classCallback(eq(CREATE), eq(POSTCOMMIT));
        verify(mockModel, never()).classCallback(eq(UPDATE), any());
        verify(mockModel, never()).classCallback(eq(DELETE), any());

        verify(mockModel, times(2)).classAllFieldsCallback(any(), any());
        verify(mockModel, times(2)).classAllFieldsCallback(eq(CREATE), eq(PRECOMMIT));

        verify(mockModel, times(1)).attributeCallback(eq(CREATE), eq(PRESECURITY), any());
        verify(mockModel, times(1)).attributeCallback(eq(CREATE), eq(PREFLUSH), any());
        verify(mockModel, times(1)).attributeCallback(eq(CREATE), eq(PRECOMMIT), any());
        verify(mockModel, times(1)).attributeCallback(eq(CREATE), eq(POSTCOMMIT), any());
        verify(mockModel, never()).attributeCallback(eq(UPDATE), any(), any());
        verify(mockModel, never()).attributeCallback(eq(DELETE), any(), any());

        verify(mockModel, times(1)).relationCallback(eq(CREATE), eq(PRESECURITY), any());
        verify(mockModel, times(1)).relationCallback(eq(CREATE), eq(PREFLUSH), any());
        verify(mockModel, times(1)).relationCallback(eq(CREATE), eq(PRECOMMIT), any());
        verify(mockModel, times(1)).relationCallback(eq(CREATE), eq(POSTCOMMIT), any());
        verify(mockModel, never()).relationCallback(eq(UPDATE), any(), any());
        verify(mockModel, never()).relationCallback(eq(DELETE), any(), any());

        verify(tx).preCommit(any());
        verify(tx, times(1)).createObject(eq(mockModel), isA(RequestScope.class));
        verify(tx).flush(isA(RequestScope.class));
        verify(tx).commit(isA(RequestScope.class));
        verify(tx).close();
    }

    @Test
    public void testLegacyElideCreate() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        LegacyTestModel mockModel = mock(LegacyTestModel.class);

        Elide elide = getElide(store, dictionary, MOCK_AUDIT_LOGGER);

        String body = "{\"data\": {\"type\":\"legacyTestModel\",\"id\":\"1\",\"attributes\": {\"field\":\"Foo\"}}}";

        when(store.beginTransaction()).thenReturn(tx);
        when(tx.createNewObject(eq(ClassType.of(LegacyTestModel.class)), any())).thenReturn(mockModel);

        ElideResponse response = elide.post(baseUrl, "/legacyTestModel", body, null, NO_VERSION);
        assertEquals(HttpStatus.SC_CREATED, response.getResponseCode());

        verify(mockModel, times(1)).classCreatePreCommitAllUpdates();
        verify(mockModel, times(1)).classCreatePreSecurity();
        verify(mockModel, times(1)).classCreatePreCommit();
        verify(mockModel, times(1)).classCreatePostCommit();
        verify(mockModel, times(3)).classMultiple();
        verify(mockModel, never()).classUpdatePreCommit();
        verify(mockModel, never()).classUpdatePostCommit();
        verify(mockModel, never()).classUpdatePreSecurity();
        verify(mockModel, never()).classDeletePreCommit();
        verify(mockModel, never()).classDeletePostCommit();
        verify(mockModel, never()).classDeletePreSecurity();

        verify(mockModel, times(1)).fieldCreatePreSecurity();
        verify(mockModel, times(1)).fieldCreatePreCommit();
        verify(mockModel, times(1)).fieldCreatePostCommit();
        verify(mockModel, times(3)).fieldMultiple();
        verify(mockModel, never()).fieldUpdatePreCommit();
        verify(mockModel, never()).fieldUpdatePostCommit();
        verify(mockModel, never()).fieldUpdatePreSecurity();

        verify(tx).preCommit(any());
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
        when(tx.createNewObject(eq(ClassType.of(FieldTestModel.class)), any())).thenReturn(mockModel);

        ElideResponse response = elide.post(baseUrl, "/testModel", body, null, NO_VERSION);
        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getResponseCode());
        assertEquals(
                "{\"errors\":[{\"detail\":\"Unexpected exception caught\"}]}",
                response.getBody());

        verify(mockModel, never()).classCallback(any(), any());
        verify(mockModel, never()).attributeCallback(any(), any(), any());
        verify(mockModel, never()).relationCallback(any(), any(), any());
        verify(mockModel, never()).classAllFieldsCallback(any(), any());

        verify(tx, never()).preCommit(any());
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

        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        ElideResponse response = elide.get(baseUrl, "/testModel/1", queryParams, null, NO_VERSION);
        assertEquals(HttpStatus.SC_OK, response.getResponseCode());

        verify(mockModel, never()).classAllFieldsCallback(any(), any());

        verify(mockModel, never()).classCallback(eq(CREATE), any());
        verify(mockModel, never()).classCallback(eq(UPDATE), any());
        verify(mockModel, never()).classCallback(eq(DELETE), any());

        verify(mockModel, never()).attributeCallback(eq(CREATE), any(), any());
        verify(mockModel, never()).attributeCallback(eq(UPDATE), any(), any());
        verify(mockModel, never()).attributeCallback(eq(DELETE), any(), any());

        verify(mockModel, never()).relationCallback(eq(CREATE), any(), any());
        verify(mockModel, never()).relationCallback(eq(UPDATE), any(), any());
        verify(mockModel, never()).relationCallback(eq(DELETE), any(), any());
        verify(tx).preCommit(any());
        verify(tx).flush(any());
        verify(tx).commit(any());
        verify(tx).close();
    }

    @Test
    public void testLegacyElideGet() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        LegacyTestModel mockModel = mock(LegacyTestModel.class);

        Elide elide = getElide(store, dictionary, MOCK_AUDIT_LOGGER);

        when(store.beginReadTransaction()).thenCallRealMethod();
        when(store.beginTransaction()).thenReturn(tx);
        when(tx.loadObject(isA(EntityProjection.class), any(), isA(RequestScope.class))).thenReturn(mockModel);

        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        ElideResponse response = elide.get(baseUrl, "/legacyTestModel/1", queryParams, null, NO_VERSION);
        assertEquals(HttpStatus.SC_OK, response.getResponseCode());

        verify(mockModel, never()).classMultiple();
        verify(mockModel, never()).classUpdatePreSecurity();
        verify(mockModel, never()).classUpdatePreCommit();
        verify(mockModel, never()).classUpdatePostCommit();
        verify(mockModel, never()).classCreatePreCommitAllUpdates();
        verify(mockModel, never()).classCreatePreSecurity();
        verify(mockModel, never()).classCreatePreCommit();
        verify(mockModel, never()).classCreatePostCommit();
        verify(mockModel, never()).classDeletePreSecurity();
        verify(mockModel, never()).classDeletePreCommit();
        verify(mockModel, never()).classDeletePostCommit();

        verify(mockModel, never()).fieldMultiple();
        verify(mockModel, never()).fieldUpdatePreSecurity();
        verify(mockModel, never()).fieldUpdatePreCommit();
        verify(mockModel, never()).fieldUpdatePostCommit();
        verify(mockModel, never()).fieldCreatePreSecurity();
        verify(mockModel, never()).fieldCreatePreCommit();
        verify(mockModel, never()).fieldCreatePostCommit();

        verify(tx).preCommit(any());
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

        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.putSingle("fields[testModel]", "field");
        ElideResponse response = elide.get(baseUrl, "/testModel/1", queryParams, null, NO_VERSION);
        assertEquals(HttpStatus.SC_OK, response.getResponseCode());

        verify(mockModel, never()).classAllFieldsCallback(any(), any());

        verify(mockModel, never()).classCallback(eq(CREATE), any());
        verify(mockModel, never()).classCallback(eq(UPDATE), any());
        verify(mockModel, never()).classCallback(eq(DELETE), any());

        verify(mockModel, never()).attributeCallback(eq(CREATE), any(), any());
        verify(mockModel, never()).attributeCallback(eq(UPDATE), any(), any());
        verify(mockModel, never()).attributeCallback(eq(DELETE), any(), any());

        verify(mockModel, never()).relationCallback(any(), any(), any());

        verify(tx).preCommit(any());
        verify(tx).flush(any());
        verify(tx).commit(any());
        verify(tx).close();
    }

    @Test
    public void testElideGetInvalidKey() throws Exception {
        DataStore store = mock(DataStore.class);
        Elide elide = getElide(store, dictionary, MOCK_AUDIT_LOGGER);

        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.putSingle("fields[testModel]", "field");
        queryParams.putSingle("?filter", "field"); // Key starts with '?'
        queryParams.putSingle("Sort", "field"); // Valid Key is sort
        queryParams.putSingle("INCLUDE", "field"); // Valid Key is include
        queryParams.putSingle("fields.testModel", "field"); // fields is not followed by [
        queryParams.putSingle("page.size", "10"); // page is not followed by [
        ElideResponse response = elide.get(baseUrl, "/testModel/1", queryParams, null, NO_VERSION);
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getResponseCode());
        assertEquals("{\"errors\":[{\"detail\":\"Found undefined keys in request: fields.testModel, ?filter, Sort, page.size, INCLUDE\"}]}",
                        response.getBody());
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

        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        ElideResponse response = elide.get(baseUrl, "/testModel/1/relationships/models", queryParams, null, NO_VERSION);
        assertEquals(HttpStatus.SC_OK, response.getResponseCode());

        verify(mockModel, never()).classAllFieldsCallback(any(), any());

        verify(mockModel, never()).classCallback(eq(CREATE), any());
        verify(mockModel, never()).classCallback(eq(UPDATE), any());
        verify(mockModel, never()).classCallback(eq(DELETE), any());

        verify(mockModel, never()).attributeCallback(eq(CREATE), any(), any());
        verify(mockModel, never()).attributeCallback(eq(UPDATE), any(), any());
        verify(mockModel, never()).attributeCallback(eq(DELETE), any(), any());

        verify(mockModel, never()).relationCallback(eq(CREATE), any(), any());
        verify(mockModel, never()).relationCallback(eq(UPDATE), any(), any());
        verify(mockModel, never()).relationCallback(eq(DELETE), any(), any());

        verify(tx).preCommit(any());
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
        ElideResponse response = elide.patch(baseUrl, contentType, contentType, "/testModel/1", body, null, NO_VERSION);
        assertEquals(HttpStatus.SC_NO_CONTENT, response.getResponseCode());

        verify(mockModel, never()).classAllFieldsCallback(any(), any());

        verify(mockModel, never()).classCallback(eq(CREATE), any());
        verify(mockModel, never()).classCallback(eq(DELETE), any());
        verify(mockModel, times(1)).classCallback(eq(UPDATE), eq(PRESECURITY));
        verify(mockModel, times(1)).classCallback(eq(UPDATE), eq(PREFLUSH));
        verify(mockModel, times(1)).classCallback(eq(UPDATE), eq(PRECOMMIT));
        verify(mockModel, times(1)).classCallback(eq(UPDATE), eq(POSTCOMMIT));

        verify(mockModel, never()).attributeCallback(eq(CREATE), any(), any());
        verify(mockModel, never()).attributeCallback(eq(DELETE), any(), any());
        verify(mockModel, times(1)).attributeCallback(eq(UPDATE), eq(PRESECURITY), any());
        verify(mockModel, times(1)).attributeCallback(eq(UPDATE), eq(PREFLUSH), any());
        verify(mockModel, times(1)).attributeCallback(eq(UPDATE), eq(PRECOMMIT), any());
        verify(mockModel, times(1)).attributeCallback(eq(UPDATE), eq(POSTCOMMIT), any());

        verify(mockModel, never()).relationCallback(eq(CREATE), any(), any());
        verify(mockModel, never()).relationCallback(eq(DELETE), any(), any());
        verify(mockModel, never()).relationCallback(eq(UPDATE), any(), any());

        verify(tx).preCommit(any());
        verify(tx).save(eq(mockModel), isA(RequestScope.class));
        verify(tx).flush(isA(RequestScope.class));
        verify(tx).commit(isA(RequestScope.class));
        verify(tx).close();
    }

    @Test
    public void testElidePatchRelationshipAddMultiple() {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        FieldTestModel parent = mock(FieldTestModel.class);
        FieldTestModel child1 = mock(FieldTestModel.class);
        FieldTestModel child2 = mock(FieldTestModel.class);
        FieldTestModel child3 = mock(FieldTestModel.class);

        Elide elide = getElide(store, dictionary, MOCK_AUDIT_LOGGER);

        String body = "{\"data\": {\"type\":\"testModel\",\"id\":\"1\",\"relationships\": { \"models\": { \"data\": [ { \"type\": \"testModel\", \"id\": \"2\" }, {\"type\": \"testModel\", \"id\": \"3\" } ] } } } }";

        dictionary.setValue(parent, "id", "1");
        dictionary.setValue(child1, "id", "2");
        dictionary.setValue(child2, "id", "3");
        dictionary.setValue(child3, "id", "4");
        when(store.beginTransaction()).thenReturn(tx);
        when(tx.loadObject(isA(EntityProjection.class), eq("1"), isA(RequestScope.class))).thenReturn(parent);
        when(tx.loadObject(isA(EntityProjection.class), eq("2"), isA(RequestScope.class))).thenReturn(child1);
        when(tx.loadObject(isA(EntityProjection.class), eq("3"), isA(RequestScope.class))).thenReturn(child2);
        when(tx.loadObject(isA(EntityProjection.class), eq("4"), isA(RequestScope.class))).thenReturn(child3);

        DataStoreIterable iterable = new DataStoreIterableBuilder(List.of(child3)).build();
        when(tx.getToManyRelation(any(), any(), isA(Relationship.class), isA(RequestScope.class))).thenReturn(iterable);

        String contentType = JSONAPI_CONTENT_TYPE;
        ElideResponse response = elide.patch(baseUrl, contentType, contentType, "/testModel/1", body, null, NO_VERSION);
        assertEquals(HttpStatus.SC_NO_CONTENT, response.getResponseCode());

        verify(parent, times(1)).relationCallback(eq(UPDATE), eq(POSTCOMMIT), notNull());

        verify(parent, times(4)).classCallback(eq(UPDATE), any());
        verify(parent, never()).classAllFieldsCallback(any(), any());
        verify(parent, never()).attributeCallback(any(), any(), any());
    }

    @Test
    public void testLegacyElidePatch() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        LegacyTestModel mockModel = mock(LegacyTestModel.class);

        Elide elide = getElide(store, dictionary, MOCK_AUDIT_LOGGER);

        String body = "{\"data\": {\"type\":\"legacyTestModel\",\"id\":\"1\",\"attributes\": {\"field\":\"Foo\"}}}";

        dictionary.setValue(mockModel, "id", "1");
        when(store.beginTransaction()).thenReturn(tx);
        when(tx.loadObject(isA(EntityProjection.class), any(), isA(RequestScope.class))).thenReturn(mockModel);

        String contentType = JSONAPI_CONTENT_TYPE;
        ElideResponse response = elide.patch(baseUrl, contentType, contentType, "/legacyTestModel/1", body, null, NO_VERSION);
        assertEquals(HttpStatus.SC_NO_CONTENT, response.getResponseCode());

        verify(mockModel, never()).classCreatePreCommitAllUpdates();
        verify(mockModel, never()).classCreatePreSecurity();
        verify(mockModel, never()).classCreatePreCommit();
        verify(mockModel, never()).classCreatePostCommit();
        verify(mockModel, never()).classDeletePreSecurity();
        verify(mockModel, never()).classDeletePreCommit();
        verify(mockModel, never()).classDeletePostCommit();

        verify(mockModel, times(1)).classUpdatePreSecurity();
        verify(mockModel, times(1)).classUpdatePreCommit();
        verify(mockModel, times(1)).classUpdatePostCommit();
        verify(mockModel, times(3)).classMultiple();

        verify(mockModel, never()).fieldCreatePreSecurity();
        verify(mockModel, never()).fieldCreatePreCommit();
        verify(mockModel, never()).fieldCreatePostCommit();

        verify(mockModel, times(1)).fieldUpdatePreSecurity();
        verify(mockModel, times(1)).fieldUpdatePreCommit();
        verify(mockModel, times(1)).fieldUpdatePostCommit();
        verify(mockModel, times(3)).fieldMultiple();

        verify(tx).preCommit(any());
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

        ElideResponse response = elide.delete(baseUrl, "/testModel/1", "", null, NO_VERSION);
        assertEquals(HttpStatus.SC_NO_CONTENT, response.getResponseCode());

        verify(mockModel, never()).classAllFieldsCallback(any(), any());

        verify(mockModel, never()).classCallback(eq(UPDATE), any());
        verify(mockModel, never()).classCallback(eq(CREATE), any());

        verify(mockModel, times(1)).classCallback(eq(DELETE), eq(PRESECURITY));
        verify(mockModel, times(1)).classCallback(eq(DELETE), eq(PREFLUSH));
        verify(mockModel, times(1)).classCallback(eq(DELETE), eq(PRECOMMIT));
        verify(mockModel, times(1)).classCallback(eq(DELETE), eq(POSTCOMMIT));

        verify(mockModel, never()).attributeCallback(eq(UPDATE), any(), any());
        verify(mockModel, never()).attributeCallback(eq(CREATE), any(), any());
        verify(mockModel, never()).attributeCallback(eq(DELETE), any(), any());

        verify(mockModel, never()).relationCallback(eq(UPDATE), any(), any());
        verify(mockModel, never()).relationCallback(eq(CREATE), any(), any());
        verify(mockModel, never()).relationCallback(eq(DELETE), any(), any());

        verify(tx).preCommit(any());
        verify(tx).delete(eq(mockModel), isA(RequestScope.class));
        verify(tx).flush(isA(RequestScope.class));
        verify(tx).commit(isA(RequestScope.class));
        verify(tx).close();
    }

    @Test
    public void testLegacyElideDelete() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        LegacyTestModel mockModel = mock(LegacyTestModel.class);

        Elide elide = getElide(store, dictionary, MOCK_AUDIT_LOGGER);

        dictionary.setValue(mockModel, "id", "1");
        when(store.beginTransaction()).thenReturn(tx);
        when(tx.loadObject(isA(EntityProjection.class), any(), isA(RequestScope.class))).thenReturn(mockModel);

        ElideResponse response = elide.delete(baseUrl, "/legacyTestModel/1", "", null, NO_VERSION);
        assertEquals(HttpStatus.SC_NO_CONTENT, response.getResponseCode());

        verify(mockModel, never()).classUpdatePostCommit();
        verify(mockModel, never()).classUpdatePreSecurity();
        verify(mockModel, never()).classUpdatePreCommit();
        verify(mockModel, never()).classCreatePreCommitAllUpdates();
        verify(mockModel, never()).classCreatePostCommit();
        verify(mockModel, never()).classCreatePreSecurity();
        verify(mockModel, never()).classCreatePreCommit();

        verify(mockModel, times(1)).classDeletePreSecurity();
        verify(mockModel, times(1)).classDeletePreCommit();
        verify(mockModel, times(1)).classDeletePostCommit();
        verify(mockModel, times(3)).classMultiple();

        verify(mockModel, never()).fieldUpdatePostCommit();
        verify(mockModel, never()).fieldUpdatePreSecurity();
        verify(mockModel, never()).fieldUpdatePreCommit();
        verify(mockModel, never()).fieldCreatePostCommit();
        verify(mockModel, never()).fieldCreatePreSecurity();
        verify(mockModel, never()).fieldCreatePreCommit();
        verify(mockModel, never()).fieldMultiple();

        verify(tx).preCommit(any());
        verify(tx).delete(eq(mockModel), isA(RequestScope.class));
        verify(tx).flush(isA(RequestScope.class));
        verify(tx).commit(isA(RequestScope.class));
        verify(tx).close();
    }

    @Test
    public void testElidePatchExtensionCreate() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        FieldTestModel mockModel = mock(FieldTestModel.class);

        Elide elide = getElide(store, dictionary, MOCK_AUDIT_LOGGER);

        String body = "[{\"op\": \"add\",\"path\": \"/testModel\",\"value\":{"
                + "\"type\":\"testModel\",\"id\": \"1\",\"attributes\": {\"field\":\"Foo\"}}}]";


        when(store.beginTransaction()).thenReturn(tx);
        when(tx.createNewObject(eq(ClassType.of(FieldTestModel.class)), any())).thenReturn(mockModel);

        String contentType = JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION;
        ElideResponse response =
                elide.patch(baseUrl, contentType, contentType, "/", body, null, NO_VERSION);
        assertEquals(HttpStatus.SC_OK, response.getResponseCode());

        verify(mockModel, times(1)).classCallback(eq(CREATE), eq(PRESECURITY));
        verify(mockModel, times(1)).classCallback(eq(CREATE), eq(PREFLUSH));
        verify(mockModel, times(1)).classCallback(eq(CREATE), eq(PRECOMMIT));
        verify(mockModel, times(1)).classCallback(eq(CREATE), eq(POSTCOMMIT));
        verify(mockModel, never()).classCallback(eq(UPDATE), any());
        verify(mockModel, never()).classCallback(eq(DELETE), any());

        verify(mockModel, times(2)).classAllFieldsCallback(any(), any());
        verify(mockModel, times(2)).classAllFieldsCallback(eq(CREATE), eq(PRECOMMIT));

        verify(mockModel, times(1)).attributeCallback(eq(CREATE), eq(PRESECURITY), any());
        verify(mockModel, times(1)).attributeCallback(eq(CREATE), eq(PREFLUSH), any());
        verify(mockModel, times(1)).attributeCallback(eq(CREATE), eq(PRECOMMIT), any());
        verify(mockModel, times(1)).attributeCallback(eq(CREATE), eq(POSTCOMMIT), any());
        verify(mockModel, never()).attributeCallback(eq(UPDATE), any(), any());
        verify(mockModel, never()).attributeCallback(eq(DELETE), any(), any());

        verify(mockModel, times(1)).relationCallback(eq(CREATE), eq(PRESECURITY), any());
        verify(mockModel, times(1)).relationCallback(eq(CREATE), eq(PREFLUSH), any());
        verify(mockModel, times(1)).relationCallback(eq(CREATE), eq(PRECOMMIT), any());
        verify(mockModel, times(1)).relationCallback(eq(CREATE), eq(POSTCOMMIT), any());
        verify(mockModel, never()).relationCallback(eq(UPDATE), any(), any());
        verify(mockModel, never()).relationCallback(eq(DELETE), any(), any());

        verify(tx).preCommit(any());
        verify(tx, times(1)).createObject(eq(mockModel), isA(RequestScope.class));
        verify(tx).flush(isA(RequestScope.class));
        verify(tx).commit(isA(RequestScope.class));
        verify(tx).close();
    }

    @Test
    public void testLegacyElidePatchExtensionCreate() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        LegacyTestModel mockModel = mock(LegacyTestModel.class);

        Elide elide = getElide(store, dictionary, MOCK_AUDIT_LOGGER);

        String body = "[{\"op\": \"add\",\"path\": \"/legacyTestModel\",\"value\":{"
                + "\"type\":\"legacyTestModel\",\"id\": \"1\",\"attributes\": {\"field\":\"Foo\"}}}]";

        when(store.beginTransaction()).thenReturn(tx);
        when(tx.createNewObject(eq(ClassType.of(LegacyTestModel.class)), any())).thenReturn(mockModel);

        String contentType = JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION;
        ElideResponse response =
                elide.patch(baseUrl, contentType, contentType, "/", body, null, NO_VERSION);
        assertEquals(HttpStatus.SC_OK, response.getResponseCode());

        verify(mockModel, times(1)).classCreatePreSecurity();
        verify(mockModel, times(1)).classCreatePreCommit();
        verify(mockModel, times(1)).classCreatePreCommitAllUpdates();
        verify(mockModel, times(1)).classCreatePostCommit();
        verify(mockModel, times(3)).classMultiple();
        verify(mockModel, never()).classUpdatePreCommit();
        verify(mockModel, never()).classUpdatePostCommit();
        verify(mockModel, never()).classUpdatePreSecurity();
        verify(mockModel, never()).classDeletePreCommit();
        verify(mockModel, never()).classDeletePostCommit();
        verify(mockModel, never()).classDeletePreSecurity();

        verify(mockModel, times(1)).fieldCreatePostCommit();
        verify(mockModel, times(1)).fieldCreatePreCommit();
        verify(mockModel, times(1)).fieldCreatePreSecurity();
        verify(mockModel, times(3)).fieldMultiple();
        verify(mockModel, never()).fieldUpdatePreCommit();
        verify(mockModel, never()).fieldUpdatePostCommit();
        verify(mockModel, never()).fieldUpdatePreSecurity();

        verify(tx).preCommit(any());
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

      RequestScope scope = buildRequestScope(dictionary, tx);
      when(store.beginTransaction()).thenReturn(tx);
      when(tx.createNewObject(ClassType.of(FieldTestModel.class), scope)).thenReturn(mockModel);

      String contentType = JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION;
      ElideResponse response =
              elide.patch(baseUrl, contentType, contentType, "/", body, null, NO_VERSION);
      assertEquals(HttpStatus.SC_BAD_REQUEST, response.getResponseCode());
      assertEquals(
              "[{\"errors\":[{\"detail\":\"Bad Request Body&#39;Patch extension requires all objects to have an assigned ID (temporary or permanent) when assigning relationships.&#39;\",\"status\":\"400\"}]}]",
              response.getBody());

      verify(mockModel, never()).classCallback(eq(CREATE), eq(PRESECURITY));
      verify(mockModel, never()).classCallback(eq(CREATE), eq(PREFLUSH));
      verify(mockModel, never()).classCallback(eq(CREATE), eq(PRECOMMIT));
      verify(mockModel, never()).classCallback(eq(CREATE), eq(POSTCOMMIT));
      verify(mockModel, never()).classCallback(eq(UPDATE), any());
      verify(mockModel, never()).classCallback(eq(DELETE), any());

      verify(mockModel, never()).classAllFieldsCallback(any(), any());
      verify(mockModel, never()).classAllFieldsCallback(eq(CREATE), eq(PRECOMMIT));

      verify(mockModel, never()).attributeCallback(eq(CREATE), eq(PRESECURITY), any());
      verify(mockModel, never()).attributeCallback(eq(CREATE), eq(PREFLUSH), any());
      verify(mockModel, never()).attributeCallback(eq(CREATE), eq(PRECOMMIT), any());
      verify(mockModel, never()).attributeCallback(eq(CREATE), eq(POSTCOMMIT), any());
      verify(mockModel, never()).attributeCallback(eq(UPDATE), any(), any());
      verify(mockModel, never()).attributeCallback(eq(DELETE), any(), any());

      verify(mockModel, never()).relationCallback(eq(CREATE), eq(PRESECURITY), any());
      verify(mockModel, never()).relationCallback(eq(CREATE), eq(PREFLUSH), any());
      verify(mockModel, never()).relationCallback(eq(CREATE), eq(PRECOMMIT), any());
      verify(mockModel, never()).relationCallback(eq(CREATE), eq(POSTCOMMIT), any());
      verify(mockModel, never()).relationCallback(eq(UPDATE), any(), any());
      verify(mockModel, never()).relationCallback(eq(DELETE), any(), any());

      verify(tx, never()).preCommit(any());
      verify(tx, never()).createObject(eq(mockModel), isA(RequestScope.class));
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
              + "\"type\":\"testModel\",\"id\": \"1\",\"attributes\": {\"field\":\"Foo\"}}}]";

      dictionary.setValue(mockModel, "id", "1");
      when(store.beginTransaction()).thenReturn(tx);
      when(tx.loadObject(any(), any(), isA(RequestScope.class))).thenReturn(mockModel);

      String contentType = JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION;
      ElideResponse response =
              elide.patch(baseUrl, contentType, contentType, "/", body, null, NO_VERSION);
      assertEquals(HttpStatus.SC_OK, response.getResponseCode());
      assertEquals("[{\"data\":null}]", response.getBody());

      verify(mockModel, times(1)).classCallback(eq(UPDATE), eq(PRESECURITY));
      verify(mockModel, times(1)).classCallback(eq(UPDATE), eq(PREFLUSH));
      verify(mockModel, times(1)).classCallback(eq(UPDATE), eq(PRECOMMIT));
      verify(mockModel, times(1)).classCallback(eq(UPDATE), eq(POSTCOMMIT));
      verify(mockModel, never()).classCallback(eq(CREATE), any());
      verify(mockModel, never()).classCallback(eq(DELETE), any());

      verify(mockModel, never()).classAllFieldsCallback(any(), any());
      verify(mockModel, never()).classAllFieldsCallback(eq(CREATE), eq(PRECOMMIT));

      verify(mockModel, times(1)).attributeCallback(eq(UPDATE), eq(PRESECURITY), any());
      verify(mockModel, times(1)).attributeCallback(eq(UPDATE), eq(PREFLUSH), any());
      verify(mockModel, times(1)).attributeCallback(eq(UPDATE), eq(PRECOMMIT), any());
      verify(mockModel, times(1)).attributeCallback(eq(UPDATE), eq(POSTCOMMIT), any());
      verify(mockModel, never()).attributeCallback(eq(CREATE), any(), any());
      verify(mockModel, never()).attributeCallback(eq(DELETE), any(), any());

      verify(mockModel, never()).relationCallback(eq(CREATE), any(), any());
      verify(mockModel, never()).relationCallback(eq(UPDATE), any(), any());
      verify(mockModel, never()).relationCallback(eq(DELETE), any(), any());

      verify(tx).preCommit(any());

      //Twice because the patch extension request is broken into attributes & relationships separately.
      verify(tx, times(2)).loadObject(any(), any(), isA(RequestScope.class));
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

      String body = "[{\"op\": \"remove\",\"path\": \"/testModel\",\"value\":{"
              + "\"type\":\"testModel\",\"id\": \"1\"}}]";

      dictionary.setValue(mockModel, "id", "1");
      when(store.beginTransaction()).thenReturn(tx);
      when(tx.loadObject(any(), any(), isA(RequestScope.class))).thenReturn(mockModel);

      String contentType = JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION;
      ElideResponse response =
              elide.patch(baseUrl, contentType, contentType, "/", body, null, NO_VERSION);
      assertEquals(HttpStatus.SC_OK, response.getResponseCode());

      verify(mockModel, never()).classAllFieldsCallback(any(), any());

      verify(mockModel, never()).classCallback(eq(UPDATE), any());
      verify(mockModel, never()).classCallback(eq(CREATE), any());
      verify(mockModel, times(1)).classCallback(eq(DELETE), eq(PRESECURITY));
      verify(mockModel, times(1)).classCallback(eq(DELETE), eq(PREFLUSH));
      verify(mockModel, times(1)).classCallback(eq(DELETE), eq(PRECOMMIT));
      verify(mockModel, times(1)).classCallback(eq(DELETE), eq(POSTCOMMIT));

      verify(mockModel, never()).attributeCallback(eq(UPDATE), any(), any());
      verify(mockModel, never()).attributeCallback(eq(CREATE), any(), any());
      verify(mockModel, never()).attributeCallback(eq(DELETE), any(), any());

      // TODO - Read should not be called for a delete.
      verify(mockModel, never()).relationCallback(eq(UPDATE), any(), any());
      verify(mockModel, never()).relationCallback(eq(CREATE), any(), any());
      verify(mockModel, never()).relationCallback(eq(DELETE), any(), any());

      verify(tx).preCommit(any());
      verify(tx).delete(eq(mockModel), isA(RequestScope.class));
      verify(tx).flush(isA(RequestScope.class));
      verify(tx).commit(isA(RequestScope.class));
      verify(tx).close();
  }

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
      ElideResponse response =
              elide.patch(baseUrl, contentType, contentType, "/testModel/1", body, null, NO_VERSION);
      assertEquals(HttpStatus.SC_BAD_REQUEST, response.getResponseCode());
      assertEquals(
              "{\"errors\":[{\"detail\":\"Constraint violation\"}]}",
              response.getBody());

      verify(mockModel, never()).classAllFieldsCallback(any(), any());

      verify(mockModel, never()).classCallback(eq(CREATE), any());
      verify(mockModel, never()).classCallback(eq(DELETE), any());

      verify(mockModel, times(1)).classCallback(eq(UPDATE), eq(PRESECURITY));
      verify(mockModel, times(1)).classCallback(eq(UPDATE), eq(PREFLUSH));
      verify(mockModel, never()).classCallback(eq(UPDATE), eq(PRECOMMIT));
      verify(mockModel, never()).classCallback(eq(UPDATE), eq(POSTCOMMIT));

      verify(mockModel, never()).attributeCallback(eq(CREATE), any(), any());
      verify(mockModel, never()).attributeCallback(eq(DELETE), any(), any());
      verify(mockModel, times(1)).attributeCallback(eq(UPDATE), eq(PRESECURITY), any());
      verify(mockModel, times(1)).attributeCallback(eq(UPDATE), eq(PREFLUSH), any());
      verify(mockModel, never()).attributeCallback(eq(UPDATE), eq(PRECOMMIT), any());
      verify(mockModel, never()).attributeCallback(eq(UPDATE), eq(POSTCOMMIT), any());

      verify(mockModel, never()).relationCallback(eq(UPDATE), any(), any());
      verify(mockModel, never()).relationCallback(eq(CREATE), any(), any());
      verify(mockModel, never()).relationCallback(eq(DELETE), any(), any());

      verify(tx).preCommit(any());
      verify(tx).save(eq(mockModel), isA(RequestScope.class));
      verify(tx).flush(isA(RequestScope.class));
      verify(tx, never()).commit(isA(RequestScope.class));
      verify(tx).close();
  }

    @Test
    public void testCreate() {
        FieldTestModel mockModel = mock(FieldTestModel.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        RequestScope scope = buildRequestScope(dictionary, tx);
        when(tx.createNewObject(ClassType.of(FieldTestModel.class), scope)).thenReturn(mockModel);
        PersistentResource resource = PersistentResource.createObject(ClassType.of(FieldTestModel.class), scope, Optional.of("1"));
        resource.updateAttribute("field", "should not affect calls since this is create!");

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
        scope.runQueuedPreFlushTriggers();

        verify(mockModel, times(1)).classCallback(any(), any());
        verify(mockModel, times(1)).classCallback(eq(CREATE), eq(PREFLUSH));
        verify(mockModel, times(1)).attributeCallback(any(), any(), any());
        verify(mockModel, times(1)).attributeCallback(eq(CREATE), eq(PREFLUSH), any());
        verify(mockModel, times(1)).relationCallback(any(), any(), any());
        verify(mockModel, times(1)).relationCallback(eq(CREATE), eq(PREFLUSH), any());
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
        RequestScope scope = buildRequestScope(dictionary, tx);
        when(tx.createNewObject(ClassType.of(FieldTestModel.class), scope)).thenReturn(mockModel);
        PersistentResource resource = new PersistentResource(mockModel, "1", scope);

        resource.getAttribute(Attribute.builder().type(String.class).name("field").build());

        verify(mockModel, never()).classCallback(any(), any());
        verify(mockModel, never()).attributeCallback(any(), any(), any());
        verify(mockModel, never()).classAllFieldsCallback(any(), any());
        verify(mockModel, never()).relationCallback(any(), any(), any());

        clearInvocations(mockModel);
        scope.runQueuedPreSecurityTriggers();

        verify(mockModel, never()).classCallback(any(), any());
        verify(mockModel, never()).classAllFieldsCallback(any(), any());
        verify(mockModel, never()).relationCallback(any(), any(), any());
        verify(mockModel, never()).attributeCallback(any(), any(), any());

        clearInvocations(mockModel);
        scope.runQueuedPreFlushTriggers();

        verify(mockModel, never()).classCallback(any(), any());
        verify(mockModel, never()).attributeCallback(any(), any(), any());
        verify(mockModel, never()).classAllFieldsCallback(any(), any());
        verify(mockModel, never()).relationCallback(any(), any(), any());

        clearInvocations(mockModel);
        scope.runQueuedPreCommitTriggers();

        verify(mockModel, never()).classCallback(any(), any());
        verify(mockModel, never()).attributeCallback(any(), any(), any());
        verify(mockModel, never()).classAllFieldsCallback(any(), any());
        verify(mockModel, never()).relationCallback(any(), any(), any());

        clearInvocations(mockModel);
        scope.runQueuedPostCommitTriggers();

        verify(mockModel, never()).classCallback(any(), any());
        verify(mockModel, never()).attributeCallback(any(), any(), any());
        verify(mockModel, never()).classAllFieldsCallback(any(), any());
        verify(mockModel, never()).relationCallback(any(), any(), any());
    }

    @Test
    public void testDelete() {
        FieldTestModel mockModel = mock(FieldTestModel.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        RequestScope scope = buildRequestScope(dictionary, tx);
        when(tx.createNewObject(ClassType.of(FieldTestModel.class), scope)).thenReturn(mockModel);
        PersistentResource resource = new PersistentResource(mockModel, "1", scope);

        resource.deleteResource();

        verify(mockModel, times(1)).classCallback(any(), any());
        verify(mockModel, times(1)).classCallback(eq(DELETE), eq(PRESECURITY));

        verify(mockModel, never()).relationCallback(any(), any(), any());
        verify(mockModel, never()).classAllFieldsCallback(any(), any());
        verify(mockModel, never()).attributeCallback(any(), any(), any());

        clearInvocations(mockModel);
        scope.runQueuedPreSecurityTriggers();

        verify(mockModel, never()).classAllFieldsCallback(any(), any());
        verify(mockModel, never()).classCallback(any(), any());
        verify(mockModel, never()).attributeCallback(any(), any(), any());
        verify(mockModel, never()).relationCallback(any(), any(), any());

        clearInvocations(mockModel);
        scope.runQueuedPreFlushTriggers();

        verify(mockModel, times(1)).classCallback(any(), any());
        verify(mockModel, times(1)).classCallback(eq(DELETE), eq(PREFLUSH));
        verify(mockModel, never()).relationCallback(any(), any(), any());
        verify(mockModel, never()).classAllFieldsCallback(any(), any());
        verify(mockModel, never()).attributeCallback(any(), any(), any());

        clearInvocations(mockModel);
        scope.runQueuedPreCommitTriggers();

        verify(mockModel, times(1)).classCallback(any(), any());
        verify(mockModel, times(1)).classCallback(eq(DELETE), eq(PRECOMMIT));
        verify(mockModel, never()).relationCallback(any(), any(), any());
        verify(mockModel, never()).classAllFieldsCallback(any(), any());
        verify(mockModel, never()).attributeCallback(any(), any(), any());

        clearInvocations(mockModel);
        scope.getPermissionExecutor().executeCommitChecks();
        scope.runQueuedPostCommitTriggers();

        verify(mockModel, times(1)).classCallback(any(), any());
        verify(mockModel, times(1)).classCallback(eq(DELETE), eq(POSTCOMMIT));
        verify(mockModel, never()).relationCallback(any(), any(), any());
        verify(mockModel, never()).classAllFieldsCallback(any(), any());
        verify(mockModel, never()).attributeCallback(any(), any(), any());
    }

    @Test
    public void testAttributeUpdate() {
        FieldTestModel mockModel = mock(FieldTestModel.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        RequestScope scope = buildRequestScope(dictionary, tx);
        when(tx.createNewObject(ClassType.of(FieldTestModel.class), scope)).thenReturn(mockModel);

        PersistentResource resource = new PersistentResource(mockModel, scope.getUUIDFor(mockModel), scope);
        resource.updateAttribute("field", "new value");

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
        scope.runQueuedPreFlushTriggers();

        verify(mockModel, times(1)).classCallback(any(), any());
        verify(mockModel, times(1)).classCallback(eq(UPDATE), eq(PREFLUSH));
        verify(mockModel, times(1)).attributeCallback(any(), any(), any());
        verify(mockModel, times(1)).attributeCallback(eq(UPDATE), eq(PREFLUSH), notNull());

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
        RequestScope scope = buildRequestScope(dictionary, tx);
        when(tx.createNewObject(ClassType.of(FieldTestModel.class), scope)).thenReturn(mockModel);

        FieldTestModel modelToAdd = mock(FieldTestModel.class);

        PersistentResource resource = new PersistentResource(mockModel, scope.getUUIDFor(mockModel), scope);
        PersistentResource resourceToAdd = new PersistentResource(modelToAdd, scope.getUUIDFor(mockModel), scope);

        resource.addRelation("models", resourceToAdd);

        verify(mockModel, times(1)).classCallback(any(), any());
        verify(mockModel, times(1)).classCallback(eq(UPDATE), eq(PRESECURITY));

        verify(mockModel, times(1)).relationCallback(any(), any(), any());
        verify(mockModel, times(1)).relationCallback(eq(UPDATE), eq(PRESECURITY), notNull());

        verify(mockModel, never()).attributeCallback(any(), any(), any());
        verify(mockModel, never()).classAllFieldsCallback(any(), any());

        clearInvocations(mockModel);
        scope.runQueuedPreSecurityTriggers();

        verify(mockModel, never()).classAllFieldsCallback(any(), any());
        verify(mockModel, never()).relationCallback(any(), any(), any());
        verify(mockModel, never()).attributeCallback(any(), any(), any());

        clearInvocations(mockModel);
        scope.runQueuedPreFlushTriggers();

        verify(mockModel, times(1)).classCallback(any(), any());
        verify(mockModel, times(1)).classCallback(eq(UPDATE), eq(PREFLUSH));

        verify(mockModel, times(1)).relationCallback(any(), any(), any());
        verify(mockModel, times(1)).relationCallback(eq(UPDATE), eq(PREFLUSH), notNull());

        clearInvocations(mockModel);
        scope.runQueuedPreCommitTriggers();

        verify(mockModel, never()).attributeCallback(any(), any(), any());
        verify(mockModel, never()).classAllFieldsCallback(any(), any());

        verify(mockModel, times(1)).classCallback(any(), any());
        verify(mockModel, times(1)).classCallback(eq(UPDATE), eq(PRECOMMIT));
        verify(mockModel, times(1)).relationCallback(any(), any(), any());
        verify(mockModel, times(1)).relationCallback(eq(UPDATE), eq(PRECOMMIT), notNull());

        clearInvocations(mockModel);
        scope.getPermissionExecutor().executeCommitChecks();
        scope.runQueuedPostCommitTriggers();

        verify(mockModel, never()).attributeCallback(any(), any(), any());
        verify(mockModel, never()).classAllFieldsCallback(any(), any());

        verify(mockModel, times(1)).classCallback(any(), any());
        verify(mockModel, times(1)).classCallback(eq(UPDATE), eq(POSTCOMMIT));
        verify(mockModel, times(1)).relationCallback(any(), any(), any());
        verify(mockModel, times(1)).relationCallback(eq(UPDATE), eq(POSTCOMMIT), notNull());
    }

    @Test
    public void testAddToCollectionTrigger() {
        PropertyTestModel mockModel = mock(PropertyTestModel.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        RequestScope scope = buildRequestScope(dictionary, tx);
        when(tx.createNewObject(ClassType.of(PropertyTestModel.class), scope)).thenReturn(mockModel);

        PropertyTestModel modelToAdd1 = mock(PropertyTestModel.class);
        PropertyTestModel modelToAdd2 = mock(PropertyTestModel.class);

        //First we test adding to a newly created object.
        PersistentResource resource = PersistentResource.createObject(ClassType.of(PropertyTestModel.class), scope, Optional.of("1"));
        PersistentResource resourceToAdd1 = new PersistentResource(modelToAdd1, scope.getUUIDFor(mockModel), scope);
        PersistentResource resourceToAdd2 = new PersistentResource(modelToAdd2, scope.getUUIDFor(mockModel), scope);

        resource.updateRelation("models", new HashSet<>(Arrays.asList(resourceToAdd1, resourceToAdd2)));

        scope.runQueuedPreSecurityTriggers();
        scope.runQueuedPreCommitTriggers();
        scope.runQueuedPostCommitTriggers();

        verify(mockModel, never()).relationCallback(eq(UPDATE), any(), any());
        verify(mockModel, times(1)).relationCallback(eq(CREATE), eq(POSTCOMMIT), notNull());

        //Build another resource, scope & reset the mock to do a pure update (no create):
        scope = buildRequestScope(dictionary, tx);
        resource = new PersistentResource(mockModel, scope.getUUIDFor(mockModel), scope);
        reset(mockModel);

        resource.updateRelation("models", new HashSet<>(Arrays.asList(resourceToAdd1, resourceToAdd2)));

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
        RequestScope scope = buildRequestScope(dictionary, tx);
        when(tx.createNewObject(ClassType.of(PropertyTestModel.class), scope)).thenReturn(mockModel);

        PropertyTestModel childModel1 = mock(PropertyTestModel.class);
        PropertyTestModel childModel2 = mock(PropertyTestModel.class);
        PropertyTestModel childModel3 = mock(PropertyTestModel.class);
        when(childModel1.getId()).thenReturn("2");
        when(childModel2.getId()).thenReturn("3");
        when(childModel3.getId()).thenReturn("4");

        //First we test removing from a newly created object.
        PersistentResource resource = PersistentResource.createObject(ClassType.of(PropertyTestModel.class), scope, Optional.of("1"));
        PersistentResource childResource1 = new PersistentResource(childModel1, "2", scope);
        PersistentResource childResource2 = new PersistentResource(childModel2, "3", scope);
        PersistentResource childResource3 = new PersistentResource(childModel3, "3", scope);

        resource.updateRelation("models", new HashSet<>(Arrays.asList(childResource1, childResource2)));

        scope.runQueuedPreSecurityTriggers();
        scope.runQueuedPreCommitTriggers();
        scope.runQueuedPostCommitTriggers();

        verify(mockModel, never()).relationCallback(eq(UPDATE), any(), any());

        ArgumentCaptor<ChangeSpec> changes = ArgumentCaptor.forClass(ChangeSpec.class);
        verify(mockModel, times(1)).relationCallback(eq(CREATE),
                eq(POSTCOMMIT), changes.capture());

        changes.getValue().getModified().equals(List.of(childModel1, childModel2));
        changes.getValue().getOriginal().equals(List.of());

        //Build another resource, scope & reset the mock to do a pure update (no create):
        scope = buildRequestScope(dictionary, tx);
        resource = new PersistentResource(mockModel, scope.getUUIDFor(mockModel), scope);
        reset(mockModel);
        Relationship relationship = Relationship.builder()
                .projection(EntityProjection.builder()
                    .type(PropertyTestModel.class)
                    .build())
                .name("models")
                .build();

        when(tx.getToManyRelation(tx, mockModel, relationship, scope))
                .thenReturn(new DataStoreIterableBuilder<Object>(Arrays.asList(childModel1, childModel2)).build());

        when(mockModel.getModels()).thenReturn(new HashSet<>(Arrays.asList(childModel1, childModel2)));
        resource.updateRelation("models", new HashSet<>(Arrays.asList(childResource1, childResource3)));

        scope.runQueuedPreSecurityTriggers();
        scope.runQueuedPreCommitTriggers();
        scope.runQueuedPostCommitTriggers();

        verify(mockModel, never()).relationCallback(eq(CREATE), any(), any());

        changes = ArgumentCaptor.forClass(ChangeSpec.class);
        verify(mockModel, times(1)).relationCallback(eq(UPDATE), eq(POSTCOMMIT), changes.capture());
        changes.getValue().getModified().equals(List.of(childModel1, childModel3));
        changes.getValue().getOriginal().equals(List.of(childModel1, childModel2));
    }

    @Test
    public void testPreCommitLifecycleHookException() {
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        FieldTestModel testModel = mock(FieldTestModel.class);

        doThrow(IllegalStateException.class)
                .when(testModel)
                .attributeCallback(eq(UPDATE), eq(PRECOMMIT), any(ChangeSpec.class));

        RequestScope scope = buildRequestScope(dictionary, tx);
        PersistentResource resource = new PersistentResource(testModel, "1", scope);
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
        PersistentResource resource = new PersistentResource(testModel, "1", scope);
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
        PersistentResource resource = new PersistentResource(testModel, "1", scope);

        assertThrows(IllegalStateException.class, () -> resource.updateAttribute("field", "New value"));
    }

    @Test
    public void testPreFlushLifecycleHookException() {
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        FieldTestModel testModel = mock(FieldTestModel.class);

        doThrow(IllegalStateException.class)
                .when(testModel)
                .attributeCallback(eq(UPDATE), eq(PREFLUSH), any(ChangeSpec.class));

        RequestScope scope = buildRequestScope(dictionary, tx);
        PersistentResource resource = new PersistentResource(testModel, "1", scope);

        resource.updateAttribute("field", "New value");
        scope.runQueuedPreSecurityTriggers();
        assertThrows(IllegalStateException.class, () -> scope.runQueuedPreFlushTriggers());
    }

    private Elide getElide(DataStore dataStore, EntityDictionary dictionary, AuditLogger auditLogger) {
        ElideSettings settings = getElideSettings(dataStore, dictionary, auditLogger);
        return new Elide(settings, new TransactionRegistry(), settings.getDictionary().getScanner(), false);
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

        return new RequestScope(null, null, NO_VERSION, null, tx, user, null, null, UUID.randomUUID(),
                getElideSettings(null, dict, MOCK_AUDIT_LOGGER));
    }
}
