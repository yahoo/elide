/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.exceptions;

import static com.paiondata.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.paiondata.elide.Elide;
import com.paiondata.elide.ElideErrorResponse;
import com.paiondata.elide.ElideErrors;
import com.paiondata.elide.ElideResponse;
import com.paiondata.elide.ElideSettings;
import com.paiondata.elide.core.TransactionRegistry;
import com.paiondata.elide.core.datastore.DataStore;
import com.paiondata.elide.core.datastore.DataStoreTransaction;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.dictionary.TestDictionary;
import com.paiondata.elide.core.lifecycle.FieldTestModel;
import com.paiondata.elide.core.lifecycle.LegacyTestModel;
import com.paiondata.elide.core.lifecycle.PropertyTestModel;
import com.paiondata.elide.core.request.route.Route;
import com.paiondata.elide.core.type.ClassType;
import com.paiondata.elide.jsonapi.DefaultJsonApiErrorMapper;
import com.paiondata.elide.jsonapi.DefaultJsonApiExceptionHandler;
import com.paiondata.elide.jsonapi.JsonApi;
import com.paiondata.elide.jsonapi.JsonApiSettings;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * Tests the error mapping logic.
 */
public class ExceptionMapperTest {

    private final String baseUrl = "http://localhost:8080/api/v1";
    private static final ExceptionMappers MOCK_EXCEPTION_MAPPER = mock(ExceptionMappers.class);
    private static final Exception EXPECTED_EXCEPTION = new IllegalStateException("EXPECTED_EXCEPTION");
    private static final ErrorResponseException MAPPED_EXCEPTION = new ErrorResponseException(
            422,
            "MAPPED_EXCEPTION",
            ElideErrors.builder()
                    .error(error -> error.attribute("code", "SOME_ERROR"))
                    .build()
    );
    private EntityDictionary dictionary;

    ExceptionMapperTest() throws Exception {
        dictionary = TestDictionary.getTestDictionary();
        dictionary.bindEntity(FieldTestModel.class);
        dictionary.bindEntity(PropertyTestModel.class);
        dictionary.bindEntity(LegacyTestModel.class);
    }

    @AfterEach
    public void afterEach() {
        reset(MOCK_EXCEPTION_MAPPER);
    }

    @Test
    public void testElideRuntimeExceptionNoErrorResponseMapper() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        FieldTestModel mockModel = mock(FieldTestModel.class);

        Elide elide = getElide(store, dictionary, null);
        JsonApi jsonApi = new JsonApi(elide);

        String body = "{\"data\": {\"type\":\"testModel\",\"id\":\"1\",\"attributes\": {\"field\":\"Foo\"}}}";

        when(store.beginTransaction()).thenReturn(tx);
        when(tx.createNewObject(eq(ClassType.of(FieldTestModel.class)), any())).thenReturn(mockModel);
        doThrow(EXPECTED_EXCEPTION).when(tx).preCommit(any());

        Route route = Route.builder().baseUrl(baseUrl).path("/testModel").apiVersion(NO_VERSION).build();
        RuntimeException result = assertThrows(RuntimeException.class, () -> jsonApi.post(route, body, null, null));
        assertEquals(EXPECTED_EXCEPTION, result);

        verify(tx).close();
    }

    @Test
    public void testElideIOExceptionNoErrorResponseMapper() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        FieldTestModel mockModel = mock(FieldTestModel.class);

        Elide elide = getElide(store, dictionary, null);
        JsonApi jsonApi = new JsonApi(elide);

        //Invalid JSON
        String body = "{\"data\": {\"type\":\"testModel\"\"id\":\"1\",\"attributes\": {\"field\":\"Foo\"}}}";

        when(store.beginTransaction()).thenReturn(tx);
        when(tx.createNewObject(eq(ClassType.of(FieldTestModel.class)), any())).thenReturn(mockModel);

        Route route = Route.builder().baseUrl(baseUrl).path("/testModel").apiVersion(NO_VERSION).build();
        ElideResponse<String> response = jsonApi.post(route, body, null, null);
        assertEquals(400, response.getStatus());
        assertEquals(
                "{\"errors\":[{\"detail\":\"Unexpected character (&#39;&#34;&#39; (code 34)): was expecting comma to separate Object entries\"}]}",
                response.getBody());

        verify(tx).close();
    }

    @Test
    public void testElideRuntimeExceptionWithErrorResponseMapperUnmapped() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        FieldTestModel mockModel = mock(FieldTestModel.class);

        Elide elide = getElide(store, dictionary, MOCK_EXCEPTION_MAPPER);
        JsonApi jsonApi = new JsonApi(elide);

        String body = "{\"data\": {\"type\":\"testModel\",\"id\":\"1\",\"attributes\": {\"field\":\"Foo\"}}}";

        when(store.beginTransaction()).thenReturn(tx);
        when(tx.createNewObject(eq(ClassType.of(FieldTestModel.class)), any())).thenReturn(mockModel);
        doThrow(EXPECTED_EXCEPTION).when(tx).preCommit(any());

        Route route = Route.builder().baseUrl(baseUrl).path("/testModel").apiVersion(NO_VERSION).build();
        RuntimeException result = assertThrows(RuntimeException.class, () -> jsonApi.post(route, body, null, null));
        assertEquals(EXPECTED_EXCEPTION, result);

        verify(tx).close();
    }

    @Test
    public void testElideIOExceptionWithErrorResponseMapperUnmapped() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        FieldTestModel mockModel = mock(FieldTestModel.class);

        Elide elide = getElide(store, dictionary, MOCK_EXCEPTION_MAPPER);
        JsonApi jsonApi = new JsonApi(elide);

        //Invalid JSON
        String body = "{\"data\": {\"type\":\"testModel\"\"id\":\"1\",\"attributes\": {\"field\":\"Foo\"}}}";

        when(store.beginTransaction()).thenReturn(tx);
        when(tx.createNewObject(eq(ClassType.of(FieldTestModel.class)), any())).thenReturn(mockModel);

        Route route = Route.builder().baseUrl(baseUrl).path("/testModel").apiVersion(NO_VERSION).build();
        ElideResponse<String> response = jsonApi.post(route, body, null, null);
        assertEquals(400, response.getStatus());
        assertEquals(
                "{\"errors\":[{\"detail\":\"Unexpected character (&#39;&#34;&#39; (code 34)): was expecting comma to separate Object entries\"}]}",
                response.getBody());

        verify(tx).close();
    }

    @Test
    void testElideRuntimeExceptionWithErrorResponseMapperMapped() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        FieldTestModel mockModel = mock(FieldTestModel.class);

        Elide elide = getElide(store, dictionary, MOCK_EXCEPTION_MAPPER);
        JsonApi jsonApi = new JsonApi(elide);

        String body = "{\"data\": {\"type\":\"testModel\",\"id\":\"1\",\"attributes\": {\"field\":\"Foo\"}}}";

        when(store.beginTransaction()).thenReturn(tx);
        when(tx.createNewObject(eq(ClassType.of(FieldTestModel.class)), any())).thenReturn(mockModel);
        doThrow(EXPECTED_EXCEPTION).when(tx).preCommit(any());
        when(MOCK_EXCEPTION_MAPPER.toErrorResponse(eq(EXPECTED_EXCEPTION), any())).thenReturn((ElideErrorResponse<Object>) MAPPED_EXCEPTION.getErrorResponse());

        Route route = Route.builder().baseUrl(baseUrl).path("/testModel").apiVersion(NO_VERSION).build();
        ElideResponse<String> response = jsonApi.post(route, body, null, null);
        assertEquals(422, response.getStatus());
        assertEquals(
                "{\"errors\":[{\"code\":\"SOME_ERROR\"}]}",
                response.getBody());

        verify(tx).close();
    }

    @Test
    void testElideIOExceptionWithErrorResponseMapperMapped() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        FieldTestModel mockModel = mock(FieldTestModel.class);

        Elide elide = getElide(store, dictionary, MOCK_EXCEPTION_MAPPER);
        JsonApi jsonApi = new JsonApi(elide);

        //Invalid JSON:
        String body = "{\"data\": {\"type\":\"testModel\"\"id\":\"1\",\"attributes\": {\"field\":\"Foo\"}}}";

        when(store.beginTransaction()).thenReturn(tx);
        when(tx.createNewObject(eq(ClassType.of(FieldTestModel.class)), any())).thenReturn(mockModel);

        when(MOCK_EXCEPTION_MAPPER.toErrorResponse(isA(IOException.class), any())).thenReturn((ElideErrorResponse<Object>) MAPPED_EXCEPTION.getErrorResponse());

        Route route = Route.builder().baseUrl(baseUrl).path("/testModel").apiVersion(NO_VERSION).build();
        ElideResponse<String> response = jsonApi.post(route, body, null, null);
        assertEquals(422, response.getStatus());
        assertEquals(
                "{\"errors\":[{\"code\":\"SOME_ERROR\"}]}",
                response.getBody());

        verify(tx).close();
    }

    private Elide getElide(DataStore dataStore, EntityDictionary dictionary, ExceptionMappers exceptionMappers) {
        ElideSettings settings = getElideSettings(dataStore, dictionary, exceptionMappers);
        return new Elide(settings, new TransactionRegistry(), settings.getEntityDictionary().getScanner(), false);
    }

    private ElideSettings getElideSettings(DataStore dataStore, EntityDictionary dictionary, ExceptionMappers exceptionMappers) {
        JsonApiSettings.JsonApiSettingsBuilder jsonApiSettings = JsonApiSettings.builder()
                .jsonApiExceptionHandler(new DefaultJsonApiExceptionHandler(new Slf4jExceptionLogger(),
                        exceptionMappers, new DefaultJsonApiErrorMapper()));
        return ElideSettings.builder().dataStore(dataStore)
                .entityDictionary(dictionary)
                .verboseErrors(true)
                .settings(jsonApiSettings)
                .objectMapper(jsonApiSettings.build().getJsonApiMapper().getObjectMapper())
                .build();
    }
}
