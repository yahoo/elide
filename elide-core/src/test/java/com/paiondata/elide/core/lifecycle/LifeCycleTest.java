/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.lifecycle;

import static com.paiondata.elide.annotation.LifeCycleHookBinding.Operation.CREATE;
import static com.paiondata.elide.annotation.LifeCycleHookBinding.Operation.DELETE;
import static com.paiondata.elide.annotation.LifeCycleHookBinding.Operation.UPDATE;
import static com.paiondata.elide.annotation.LifeCycleHookBinding.TransactionPhase.POSTCOMMIT;
import static com.paiondata.elide.annotation.LifeCycleHookBinding.TransactionPhase.PRECOMMIT;
import static com.paiondata.elide.annotation.LifeCycleHookBinding.TransactionPhase.PREFLUSH;
import static com.paiondata.elide.annotation.LifeCycleHookBinding.TransactionPhase.PRESECURITY;
import static com.paiondata.elide.core.dictionary.EntityDictionary.NO_VERSION;
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

import com.paiondata.elide.Elide;
import com.paiondata.elide.ElideResponse;
import com.paiondata.elide.ElideSettings;
import com.paiondata.elide.core.PersistentResource;
import com.paiondata.elide.core.RequestScope;
import com.paiondata.elide.core.TransactionRegistry;
import com.paiondata.elide.core.audit.AuditLogger;
import com.paiondata.elide.core.datastore.DataStore;
import com.paiondata.elide.core.datastore.DataStoreIterable;
import com.paiondata.elide.core.datastore.DataStoreIterableBuilder;
import com.paiondata.elide.core.datastore.DataStoreTransaction;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.dictionary.TestDictionary;
import com.paiondata.elide.core.exceptions.HttpStatus;
import com.paiondata.elide.core.request.Attribute;
import com.paiondata.elide.core.request.EntityProjection;
import com.paiondata.elide.core.request.Relationship;
import com.paiondata.elide.core.request.route.Route;
import com.paiondata.elide.core.security.ChangeSpec;
import com.paiondata.elide.core.security.TestUser;
import com.paiondata.elide.core.security.User;
import com.paiondata.elide.core.type.ClassType;
import com.paiondata.elide.jsonapi.JsonApi;
import com.paiondata.elide.jsonapi.JsonApiRequestScope;
import com.paiondata.elide.jsonapi.JsonApiSettings;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import jakarta.validation.ConstraintViolationException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

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

    private Map<String, List<String>> getHeaders(String mediaType) {
        Map<String, List<String>> headers = new TreeMap<>();
        headers.put("accept", Arrays.asList(mediaType));
        headers.put("content-type", Arrays.asList(mediaType));
        return headers;
    }


    @Test
    public void testLifecycleError() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        ErrorTestModel mockModel = mock(ErrorTestModel.class);

        Elide elide = getElide(store, dictionary, MOCK_AUDIT_LOGGER);
        JsonApi jsonApi = new JsonApi(elide);

        String body = "{\"data\": {\"type\":\"errorTestModel\",\"id\":\"1\",\"attributes\": {\"field\":\"Foo\"}}}";

        when(store.beginTransaction()).thenReturn(tx);
        when(tx.createNewObject(eq(ClassType.of(ErrorTestModel.class)), any())).thenReturn(mockModel);

        Route route = Route.builder().baseUrl(baseUrl).path("/errorTestModel").apiVersion(NO_VERSION).build();
        ElideResponse<String> response = jsonApi.post(route, body, null, null);
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
        assertEquals("{\"errors\":[{\"detail\":\"Invalid\"}]}", response.getBody());
    }

    @Test
    public void testElideCreate() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        FieldTestModel mockModel = mock(FieldTestModel.class);

        Elide elide = getElide(store, dictionary, MOCK_AUDIT_LOGGER);
        JsonApi jsonApi = new JsonApi(elide);

        String body = "{\"data\": {\"type\":\"testModel\",\"id\":\"1\",\"attributes\": {\"field\":\"Foo\"}}}";

        when(store.beginTransaction()).thenReturn(tx);
        when(tx.createNewObject(eq(ClassType.of(FieldTestModel.class)), any())).thenReturn(mockModel);

        Route route = Route.builder().baseUrl(baseUrl).path("/testModel").apiVersion(NO_VERSION).build();
        ElideResponse<String> response = jsonApi.post(route, body, null, null);
        assertEquals(HttpStatus.SC_CREATED, response.getStatus());

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
        JsonApi jsonApi = new JsonApi(elide);

        String body = "{\"data\": {\"type\":\"legacyTestModel\",\"id\":\"1\",\"attributes\": {\"field\":\"Foo\"}}}";

        when(store.beginTransaction()).thenReturn(tx);
        when(tx.createNewObject(eq(ClassType.of(LegacyTestModel.class)), any())).thenReturn(mockModel);

        Route route = Route.builder().baseUrl(baseUrl).path("/legacyTestModel").apiVersion(NO_VERSION).build();
        ElideResponse<String> response = jsonApi.post(route, body, null, null);
        assertEquals(HttpStatus.SC_CREATED, response.getStatus());

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
        JsonApi jsonApi = new JsonApi(elide);

        String body = "{\"data\": {\"type\":\"testModel\",\"id\":\"1\",\"attributes\": {\"field\":\"Foo\"}}}";

        when(store.beginTransaction()).thenReturn(tx);
        when(tx.createNewObject(eq(ClassType.of(FieldTestModel.class)), any())).thenReturn(mockModel);

        Route route = Route.builder().baseUrl(baseUrl).path("/testModel").apiVersion(NO_VERSION).build();
        ElideResponse<String> response = jsonApi.post(route, body, null, null);
        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
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
        JsonApi jsonApi = new JsonApi(elide);

        when(store.beginReadTransaction()).thenCallRealMethod();
        when(store.beginTransaction()).thenReturn(tx);
        when(tx.loadObject(isA(EntityProjection.class), any(), isA(RequestScope.class))).thenReturn(mockModel);

        Route route = Route.builder().baseUrl(baseUrl).path("/testModel/1").apiVersion(NO_VERSION).build();
        ElideResponse<String> response = jsonApi.get(route, null, null);
        assertEquals(HttpStatus.SC_OK, response.getStatus());

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
        JsonApi jsonApi = new JsonApi(elide);

        when(store.beginReadTransaction()).thenCallRealMethod();
        when(store.beginTransaction()).thenReturn(tx);
        when(tx.loadObject(isA(EntityProjection.class), any(), isA(RequestScope.class))).thenReturn(mockModel);

        Route route = Route.builder().baseUrl(baseUrl).path("/legacyTestModel/1").apiVersion(NO_VERSION).build();
        ElideResponse<String> response = jsonApi.get(route, null, null);
        assertEquals(HttpStatus.SC_OK, response.getStatus());

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
        JsonApi jsonApi = new JsonApi(elide);

        when(store.beginReadTransaction()).thenCallRealMethod();
        when(store.beginTransaction()).thenReturn(tx);
        when(tx.loadObject(isA(EntityProjection.class), any(), isA(RequestScope.class))).thenReturn(mockModel);

        Map<String, List<String>> queryParams = new LinkedHashMap<>();
        queryParams.put("fields[testModel]", List.of("field"));
        Route route = Route.builder().baseUrl(baseUrl).path("/testModel/1").apiVersion(NO_VERSION).parameters(queryParams).build();
        ElideResponse<String> response = jsonApi.get(route, null, null);
        assertEquals(HttpStatus.SC_OK, response.getStatus());

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
        JsonApi jsonApi = new JsonApi(elide);

        Map<String, List<String>> queryParams = new LinkedHashMap<>();
        queryParams.put("fields[testModel]", List.of("field"));
        queryParams.put("?filter", List.of("field")); // Key starts with '?'
        queryParams.put("Sort", List.of("field")); // Valid Key is sort
        queryParams.put("INCLUDE", List.of("field")); // Valid Key is include
        queryParams.put("fields.testModel", List.of("field")); // fields is not followed by [
        queryParams.put("page.size", List.of("10")); // page is not followed by [
        Route route = Route.builder().baseUrl(baseUrl).path("/testModel/1").apiVersion(NO_VERSION).parameters(queryParams).build();
        ElideResponse<String> response = jsonApi.get(route, null, null);
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
        assertEquals("{\"errors\":[{\"detail\":\"Found undefined keys in request: ?filter, Sort, INCLUDE, fields.testModel, page.size\"}]}",
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
        JsonApi jsonApi = new JsonApi(elide);

        when(store.beginReadTransaction()).thenCallRealMethod();
        when(store.beginTransaction()).thenReturn(tx);
        when(tx.loadObject(isA(EntityProjection.class), any(), isA(RequestScope.class))).thenReturn(mockModel);

        Route route = Route.builder().baseUrl(baseUrl).path("/testModel/1/relationships/models").apiVersion(NO_VERSION).build();
        ElideResponse<String> response = jsonApi.get(route, null, null);
        assertEquals(HttpStatus.SC_OK, response.getStatus());

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
        JsonApi jsonApi = new JsonApi(elide);

        String body = "{\"data\": {\"type\":\"testModel\",\"id\":\"1\",\"attributes\": {\"field\":\"Foo\"}}}";

        dictionary.setValue(mockModel, "id", "1");
        when(store.beginTransaction()).thenReturn(tx);
        when(tx.loadObject(isA(EntityProjection.class), any(), isA(RequestScope.class))).thenReturn(mockModel);

        Route route = Route.builder().baseUrl(baseUrl).path("/testModel/1").apiVersion(NO_VERSION)
                .headers(getHeaders(JsonApi.MEDIA_TYPE)).build();
        ElideResponse<String> response = jsonApi.patch(route, body, null, null);
        assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatus());

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
        JsonApi jsonApi = new JsonApi(elide);

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

        Route route = Route.builder().baseUrl(baseUrl).path("/testModel/1").apiVersion(NO_VERSION)
                .headers(getHeaders(JsonApi.MEDIA_TYPE)).build();

        ElideResponse<String> response = jsonApi.patch(route, body, null, null);
        assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatus());

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
        JsonApi jsonApi = new JsonApi(elide);

        String body = "{\"data\": {\"type\":\"legacyTestModel\",\"id\":\"1\",\"attributes\": {\"field\":\"Foo\"}}}";

        dictionary.setValue(mockModel, "id", "1");
        when(store.beginTransaction()).thenReturn(tx);
        when(tx.loadObject(isA(EntityProjection.class), any(), isA(RequestScope.class))).thenReturn(mockModel);

        Route route = Route.builder().baseUrl(baseUrl).path("/legacyTestModel/1").apiVersion(NO_VERSION)
                .headers(getHeaders(JsonApi.MEDIA_TYPE)).build();
        ElideResponse<String> response = jsonApi.patch(route, body, null, null);
        assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatus());

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
        JsonApi jsonApi = new JsonApi(elide);

        dictionary.setValue(mockModel, "id", "1");
        when(store.beginTransaction()).thenReturn(tx);
        when(tx.loadObject(isA(EntityProjection.class), any(), isA(RequestScope.class))).thenReturn(mockModel);

        Route route = Route.builder().baseUrl(baseUrl).path("/testModel/1").apiVersion(NO_VERSION).build();
        ElideResponse<String> response = jsonApi.delete(route, "", null, null);
        assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatus());

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
        JsonApi jsonApi = new JsonApi(elide);

        dictionary.setValue(mockModel, "id", "1");
        when(store.beginTransaction()).thenReturn(tx);
        when(tx.loadObject(isA(EntityProjection.class), any(), isA(RequestScope.class))).thenReturn(mockModel);

        Route route = Route.builder().baseUrl(baseUrl).path("/legacyTestModel/1").apiVersion(NO_VERSION).build();
        ElideResponse<String> response = jsonApi.delete(route, "", null, null);
        assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatus());

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
        JsonApi jsonApi = new JsonApi(elide);

        String body = "[{\"op\": \"add\",\"path\": \"/testModel\",\"value\":{"
                + "\"type\":\"testModel\",\"id\": \"1\",\"attributes\": {\"field\":\"Foo\"}}}]";


        when(store.beginTransaction()).thenReturn(tx);
        when(tx.createNewObject(eq(ClassType.of(FieldTestModel.class)), any())).thenReturn(mockModel);

        String contentType = JsonApi.JsonPatch.MEDIA_TYPE;
        Route route = Route.builder().baseUrl(baseUrl).path("/").apiVersion(NO_VERSION).headers(getHeaders(contentType))
                .build();
        ElideResponse<String> response = jsonApi.patch(route, body, null, null);
        assertEquals(HttpStatus.SC_OK, response.getStatus());

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
        JsonApi jsonApi = new JsonApi(elide);

        String body = "[{\"op\": \"add\",\"path\": \"/legacyTestModel\",\"value\":{"
                + "\"type\":\"legacyTestModel\",\"id\": \"1\",\"attributes\": {\"field\":\"Foo\"}}}]";

        when(store.beginTransaction()).thenReturn(tx);
        when(tx.createNewObject(eq(ClassType.of(LegacyTestModel.class)), any())).thenReturn(mockModel);

        String contentType = JsonApi.JsonPatch.MEDIA_TYPE;
        Route route = Route.builder().baseUrl(baseUrl).path("/").apiVersion(NO_VERSION).headers(getHeaders(contentType))
                .build();
        ElideResponse<String> response = jsonApi.patch(route, body, null, null);
        assertEquals(HttpStatus.SC_OK, response.getStatus());

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
        JsonApi jsonApi = new JsonApi(elide);

        String body = "[{\"op\": \"add\",\"path\": \"/testModel\",\"value\":{"
                + "\"type\":\"testModel\",\"attributes\": {\"field\":\"Foo\"}}}]";

        RequestScope scope = buildRequestScope(dictionary, tx);
        when(store.beginTransaction()).thenReturn(tx);
        when(tx.createNewObject(ClassType.of(FieldTestModel.class), scope)).thenReturn(mockModel);

        String contentType = JsonApi.JsonPatch.MEDIA_TYPE;
        Route route = Route.builder().baseUrl(baseUrl).path("/").apiVersion(NO_VERSION).headers(getHeaders(contentType))
                .build();
        ElideResponse<String> response = jsonApi.patch(route, body, null, null);
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
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
        JsonApi jsonApi = new JsonApi(elide);

        String body = "[{\"op\": \"replace\",\"path\": \"/testModel/1\",\"value\":{"
                + "\"type\":\"testModel\",\"id\": \"1\",\"attributes\": {\"field\":\"Foo\"}}}]";

        dictionary.setValue(mockModel, "id", "1");
        when(store.beginTransaction()).thenReturn(tx);
        when(tx.loadObject(any(), any(), isA(RequestScope.class))).thenReturn(mockModel);

        String contentType = JsonApi.JsonPatch.MEDIA_TYPE;
        Route route = Route.builder().baseUrl(baseUrl).path("/").apiVersion(NO_VERSION).headers(getHeaders(contentType))
                .build();
        ElideResponse<String> response = jsonApi.patch(route, body, null, null);
        assertEquals(HttpStatus.SC_OK, response.getStatus());
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

        // Twice because the patch extension request is broken into attributes &
        // relationships separately.
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
        JsonApi jsonApi = new JsonApi(elide);

        String body = "[{\"op\": \"remove\",\"path\": \"/testModel\",\"value\":{"
                + "\"type\":\"testModel\",\"id\": \"1\"}}]";

        dictionary.setValue(mockModel, "id", "1");
        when(store.beginTransaction()).thenReturn(tx);
        when(tx.loadObject(any(), any(), isA(RequestScope.class))).thenReturn(mockModel);

        String contentType = JsonApi.JsonPatch.MEDIA_TYPE;
        Route route = Route.builder().baseUrl(baseUrl).path("/").apiVersion(NO_VERSION).headers(getHeaders(contentType))
                .build();
        ElideResponse<String> response = jsonApi.patch(route, body, null, null);
        assertEquals(HttpStatus.SC_OK, response.getStatus());

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
        JsonApi jsonApi = new JsonApi(elide);

        String body = "{\"data\": {\"type\":\"testModel\",\"id\":\"1\",\"attributes\": {\"field\":\"Foo\"}}}";

        dictionary.setValue(mockModel, "id", "1");
        when(store.beginTransaction()).thenReturn(tx);
        when(tx.loadObject(isA(EntityProjection.class), any(), isA(RequestScope.class))).thenReturn(mockModel);
        doThrow(ConstraintViolationException.class).when(tx).flush(any());

        String contentType = JsonApi.MEDIA_TYPE;
        Route route = Route.builder().baseUrl(baseUrl).path("/testModel/1").apiVersion(NO_VERSION).headers(getHeaders(contentType))
                .build();
        ElideResponse<String> response = jsonApi.patch(route, body, null, null);
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
        assertEquals("{\"errors\":[{\"detail\":\"Constraint violation\"}]}", response.getBody());

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
    public void testElideAtomicOperationsExtensionCreate() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        FieldTestModel mockModel = mock(FieldTestModel.class);

        Elide elide = getElide(store, dictionary, MOCK_AUDIT_LOGGER);
        JsonApi jsonApi = new JsonApi(elide);

        String body = """
                {
                  "atomic:operations": [{
                    "op": "add",
                    "href": "/testModel",
                    "data": {
                      "type": "testModel",
                      "id": "1",
                      "attributes": {
                        "field": "Foo"
                      }
                    }
                  }]
                }
                      """;
        when(store.beginTransaction()).thenReturn(tx);
        when(tx.createNewObject(eq(ClassType.of(FieldTestModel.class)), any())).thenReturn(mockModel);

        String contentType = JsonApi.AtomicOperations.MEDIA_TYPE;
        Route route = Route.builder().baseUrl(baseUrl).path("/").apiVersion(NO_VERSION).headers(getHeaders(contentType))
                .build();
        ElideResponse<String> response = jsonApi.operations(route, body, null, null);
        assertEquals(HttpStatus.SC_OK, response.getStatus());

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
    public void testElideAtomicOperationsExtensionCreateRef() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        FieldTestModel mockModel = mock(FieldTestModel.class);

        Elide elide = getElide(store, dictionary, MOCK_AUDIT_LOGGER);
        JsonApi jsonApi = new JsonApi(elide);

        String body = """
                {
                  "atomic:operations": [{
                    "op": "add",
                    "data": {
                      "type": "testModel",
                      "id": "1",
                      "attributes": {
                        "field": "Foo"
                      }
                    }
                  }]
                }
                      """;
        when(store.beginTransaction()).thenReturn(tx);
        when(tx.createNewObject(eq(ClassType.of(FieldTestModel.class)), any())).thenReturn(mockModel);

        String contentType = JsonApi.AtomicOperations.MEDIA_TYPE;
        Route route = Route.builder().baseUrl(baseUrl).path("/").apiVersion(NO_VERSION).headers(getHeaders(contentType))
                .build();
        ElideResponse<String> response = jsonApi.operations(route, body, null, null);
        assertEquals(HttpStatus.SC_OK, response.getStatus());

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
    public void failElideAtomicOperationsExtensionCreate() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        FieldTestModel mockModel = mock(FieldTestModel.class);

        Elide elide = getElide(store, dictionary, MOCK_AUDIT_LOGGER);
        JsonApi jsonApi = new JsonApi(elide);

        String body = """
                {
                  "atomic:operations": [{
                    "op": "add",
                    "href": "/testModel",
                    "data": {
                      "type": "testModel",
                      "attributes": {
                        "field": "Foo"
                      }
                    }
                  }]
                }
                      """;

        RequestScope scope = buildRequestScope(dictionary, tx);
        when(store.beginTransaction()).thenReturn(tx);
        when(tx.createNewObject(ClassType.of(FieldTestModel.class), scope)).thenReturn(mockModel);

        String contentType = JsonApi.AtomicOperations.MEDIA_TYPE;
        Route route = Route.builder().baseUrl(baseUrl).path("/").apiVersion(NO_VERSION).headers(getHeaders(contentType))
                .build();
        ElideResponse<String> response = jsonApi.operations(route, body, null, null);
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
        String expected = """
                [{"errors":[{"detail":"Bad Request Body&#39;Atomic Operations extension requires all objects to have an assigned ID (temporary or permanent) when assigning relationships.&#39;","status":"400"}]}]""";
        assertEquals(expected, response.getBody());

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
    public void testElideAtomicOperationsExtensionUpdate() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        FieldTestModel mockModel = mock(FieldTestModel.class);

        Elide elide = getElide(store, dictionary, MOCK_AUDIT_LOGGER);
        JsonApi jsonApi = new JsonApi(elide);

        String body = """
                {
                  "atomic:operations": [{
                    "op": "update",
                    "href": "/testModel/1",
                    "data": {
                      "type": "testModel",
                      "id": "1",
                      "attributes": {
                        "field": "Foo"
                      }
                    }
                  }]
                }
                      """;

        dictionary.setValue(mockModel, "id", "1");
        when(store.beginTransaction()).thenReturn(tx);
        when(tx.loadObject(any(), any(), isA(RequestScope.class))).thenReturn(mockModel);

        String contentType = JsonApi.AtomicOperations.MEDIA_TYPE;
        Route route = Route.builder().baseUrl(baseUrl).path("/").apiVersion(NO_VERSION).headers(getHeaders(contentType))
                .build();
        ElideResponse<String> response = jsonApi.operations(route, body, null, null);
        assertEquals(HttpStatus.SC_OK, response.getStatus());

        String expected = """
                {"atomic:results":[{"data":null}]}""";
        assertEquals(expected, response.getBody());

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

        // Twice because the patch extension request is broken into attributes &
        // relationships separately.
        verify(tx, times(2)).loadObject(any(), any(), isA(RequestScope.class));
        verify(tx).flush(isA(RequestScope.class));
        verify(tx).commit(isA(RequestScope.class));
        verify(tx).close();
    }

    @Test
    public void testElideAtomicOperationsExtensionUpdateRef() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        FieldTestModel mockModel = mock(FieldTestModel.class);

        Elide elide = getElide(store, dictionary, MOCK_AUDIT_LOGGER);
        JsonApi jsonApi = new JsonApi(elide);

        String body = """
                {
                  "atomic:operations": [{
                    "op": "update",
                    "data": {
                      "type": "testModel",
                      "id": "1",
                      "attributes": {
                        "field": "Foo"
                      }
                    }
                  }]
                }
                      """;

        dictionary.setValue(mockModel, "id", "1");
        when(store.beginTransaction()).thenReturn(tx);
        when(tx.loadObject(any(), any(), isA(RequestScope.class))).thenReturn(mockModel);

        String contentType = JsonApi.AtomicOperations.MEDIA_TYPE;
        Route route = Route.builder().baseUrl(baseUrl).path("/").apiVersion(NO_VERSION).headers(getHeaders(contentType))
                .build();
        ElideResponse<String> response = jsonApi.operations(route, body, null, null);
        assertEquals(HttpStatus.SC_OK, response.getStatus());

        String expected = """
                {"atomic:results":[{"data":null}]}""";
        assertEquals(expected, response.getBody());

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

        // Twice because the patch extension request is broken into attributes &
        // relationships separately.
        verify(tx, times(2)).loadObject(any(), any(), isA(RequestScope.class));
        verify(tx).flush(isA(RequestScope.class));
        verify(tx).commit(isA(RequestScope.class));
        verify(tx).close();
    }

    @Test
    public void testElideAtomicOperationsExtensionDelete() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        FieldTestModel mockModel = mock(FieldTestModel.class);

        Elide elide = getElide(store, dictionary, MOCK_AUDIT_LOGGER);
        JsonApi jsonApi = new JsonApi(elide);

        String body = """
                {
                  "atomic:operations": [{
                    "op": "remove",
                    "href": "/testModel",
                    "data": {
                      "type": "testModel",
                      "id": "1"
                    }
                  }]
                }
                      """;
        dictionary.setValue(mockModel, "id", "1");
        when(store.beginTransaction()).thenReturn(tx);
        when(tx.loadObject(any(), any(), isA(RequestScope.class))).thenReturn(mockModel);

        String contentType = JsonApi.AtomicOperations.MEDIA_TYPE;
        Route route = Route.builder().baseUrl(baseUrl).path("/").apiVersion(NO_VERSION).headers(getHeaders(contentType))
                .build();
        ElideResponse<String> response = jsonApi.operations(route, body, null, null);
        assertEquals(HttpStatus.SC_OK, response.getStatus());

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

    @Test
    public void testElideAtomicOperationsExtensionDeleteRef() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        FieldTestModel mockModel = mock(FieldTestModel.class);

        Elide elide = getElide(store, dictionary, MOCK_AUDIT_LOGGER);
        JsonApi jsonApi = new JsonApi(elide);

        String body = """
                {
                  "atomic:operations": [{
                    "op": "remove",
                    "ref": {
                      "type": "testModel",
                      "id": "1"
                    }
                  }]
                }
                      """;
        dictionary.setValue(mockModel, "id", "1");
        when(store.beginTransaction()).thenReturn(tx);
        when(tx.loadObject(any(), any(), isA(RequestScope.class))).thenReturn(mockModel);

        String contentType = JsonApi.AtomicOperations.MEDIA_TYPE;
        Route route = Route.builder().baseUrl(baseUrl).path("/").apiVersion(NO_VERSION).headers(getHeaders(contentType))
                .build();
        ElideResponse<String> response = jsonApi.operations(route, body, null, null);
        assertEquals(HttpStatus.SC_OK, response.getStatus());

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
        return new Elide(settings, new TransactionRegistry(), settings.getEntityDictionary().getScanner(), false);
    }

    private ElideSettings getElideSettings(DataStore dataStore, EntityDictionary dictionary, AuditLogger auditLogger) {
        JsonApiSettings.JsonApiSettingsBuilder jsonApiSettings = JsonApiSettings.builder();
        return ElideSettings.builder().dataStore(dataStore)
                .entityDictionary(dictionary)
                .auditLogger(auditLogger)
                .verboseErrors(true)
                .objectMapper(jsonApiSettings.build().getJsonApiMapper().getObjectMapper())
                .settings(jsonApiSettings)
                .build();
    }

    private RequestScope buildRequestScope(EntityDictionary dict, DataStoreTransaction tx) {
        User user = new TestUser("1");
        Route route = Route.builder().apiVersion(NO_VERSION).build();
        ElideSettings elideSettings = getElideSettings(null, dict, MOCK_AUDIT_LOGGER);
        return JsonApiRequestScope.builder().route(route).dataStoreTransaction(tx).user(user)
                .requestId(UUID.randomUUID()).elideSettings(elideSettings).build();
    }
}
