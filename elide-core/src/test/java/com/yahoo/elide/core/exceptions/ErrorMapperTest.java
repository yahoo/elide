/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.exceptions;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.dictionary.TestDictionary;
import com.yahoo.elide.core.lifecycle.FieldTestModel;
import com.yahoo.elide.core.lifecycle.LegacyTestModel;
import com.yahoo.elide.core.lifecycle.PropertyTestModel;
import com.yahoo.elide.core.security.TestUser;
import com.yahoo.elide.core.security.User;
import com.yahoo.elide.core.type.ClassType;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

/**
 * Tests the error mapping logic.
 */
public class ErrorMapperTest {

    private final String baseUrl = "http://localhost:8080/api/v1";
    private static final ErrorMapper MOCK_ERROR_MAPPER = mock(ErrorMapper.class);
    private static final Exception EXPECTED_EXCEPTION = new IllegalStateException("EXPECTED_EXCEPTION");
    private static final CustomErrorException MAPPED_EXCEPTION = new CustomErrorException(
            422,
            "MAPPED_EXCEPTION",
            ErrorObjects.builder()
                    .addError()
                    .withCode("SOME_ERROR")
                    .build()
    );
    private EntityDictionary dictionary;

    ErrorMapperTest() throws Exception {
        dictionary = TestDictionary.getTestDictionary();
        dictionary.bindEntity(FieldTestModel.class);
        dictionary.bindEntity(PropertyTestModel.class);
        dictionary.bindEntity(LegacyTestModel.class);
    }

    @AfterEach
    private void afterEach() {
        reset(MOCK_ERROR_MAPPER);
    }

    @Test
    public void testElideCreateNoErrorMapper() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        FieldTestModel mockModel = mock(FieldTestModel.class);

        Elide elide = getElide(store, dictionary, null);

        String body = "{\"data\": {\"type\":\"testModel\",\"id\":\"1\",\"attributes\": {\"field\":\"Foo\"}}}";

        when(store.beginTransaction()).thenReturn(tx);
        when(tx.createNewObject(eq(ClassType.of(FieldTestModel.class)), any())).thenReturn(mockModel);
        doThrow(EXPECTED_EXCEPTION).when(tx).preCommit(any());

        RuntimeException result = assertThrows(RuntimeException.class, () -> elide.post(baseUrl, "/testModel", body, null, NO_VERSION));
        assertEquals(EXPECTED_EXCEPTION, result.getCause());

        verify(tx).close();
    }

    @Test
    public void testElideCreateWithErrorMapperUnmapped() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        FieldTestModel mockModel = mock(FieldTestModel.class);

        Elide elide = getElide(store, dictionary, MOCK_ERROR_MAPPER);

        String body = "{\"data\": {\"type\":\"testModel\",\"id\":\"1\",\"attributes\": {\"field\":\"Foo\"}}}";

        when(store.beginTransaction()).thenReturn(tx);
        when(tx.createNewObject(eq(ClassType.of(FieldTestModel.class)), any())).thenReturn(mockModel);
        doThrow(EXPECTED_EXCEPTION).when(tx).preCommit(any());

        RuntimeException result = assertThrows(RuntimeException.class, () -> elide.post(baseUrl, "/testModel", body, null, NO_VERSION));
        assertEquals(EXPECTED_EXCEPTION, result.getCause());

        verify(tx).close();
    }

    @Test
    public void testElideCreateWithErrorMapperMapped() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        FieldTestModel mockModel = mock(FieldTestModel.class);

        Elide elide = getElide(store, dictionary, MOCK_ERROR_MAPPER);

        String body = "{\"data\": {\"type\":\"testModel\",\"id\":\"1\",\"attributes\": {\"field\":\"Foo\"}}}";

        when(store.beginTransaction()).thenReturn(tx);
        when(tx.createNewObject(eq(ClassType.of(FieldTestModel.class)), any())).thenReturn(mockModel);
        doThrow(EXPECTED_EXCEPTION).when(tx).preCommit(any());
        when(MOCK_ERROR_MAPPER.map(EXPECTED_EXCEPTION)).thenReturn(MAPPED_EXCEPTION);

        ElideResponse response = elide.post(baseUrl, "/testModel", body, null, NO_VERSION);
        assertEquals(422, response.getResponseCode());
        assertEquals(
                "{\"errors\":[{\"code\":\"SOME_ERROR\"}]}",
                response.getBody());

        verify(tx).close();
    }

    private Elide getElide(DataStore dataStore, EntityDictionary dictionary, ErrorMapper errorMapper) {
        return new Elide(getElideSettings(dataStore, dictionary, errorMapper));
    }

    private ElideSettings getElideSettings(DataStore dataStore, EntityDictionary dictionary, ErrorMapper errorMapper) {
        return new ElideSettingsBuilder(dataStore)
                .withEntityDictionary(dictionary)
                .withErrorMapper(errorMapper)
                .withVerboseErrors()
                .build();
    }

    private RequestScope buildRequestScope(EntityDictionary dict, DataStoreTransaction tx) {
        User user = new TestUser("1");

        return new RequestScope(null, null, NO_VERSION, null, tx, user, null, null, UUID.randomUUID(),
                getElideSettings(null, dict, MOCK_ERROR_MAPPER));
    }
}
