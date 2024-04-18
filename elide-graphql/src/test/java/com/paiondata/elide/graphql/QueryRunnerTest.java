/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.graphql;

import static com.paiondata.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.paiondata.elide.Elide;
import com.paiondata.elide.ElideResponse;
import com.paiondata.elide.ElideSettings;
import com.paiondata.elide.core.TransactionRegistry;
import com.paiondata.elide.core.datastore.DataStore;
import com.paiondata.elide.core.datastore.DataStoreTransaction;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.exceptions.ExceptionMappers;
import com.paiondata.elide.core.exceptions.Slf4jExceptionLogger;
import com.paiondata.elide.core.type.ClassType;
import com.paiondata.elide.graphql.models.GraphQLErrors;
import com.paiondata.elide.graphql.serialization.GraphQLErrorDeserializer;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;

import example.Book;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import graphql.GraphQLError;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.Set;

public class QueryRunnerTest extends GraphQLTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "#abcd\nmutation",
            "#abcd\n\nmutation",
            "   #abcd\n\nmutation",
            "#abcd\n  #befd\n mutation",
            "mutation"
    })
    public void testIsMutation(String input) {
        assertTrue(QueryRunner.isMutation(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "#abcd\n  #befd\n query",
            "query",
            "QUERY",
            "MUTATION",
            ""
    })
    public void testIsNotMutation(String input) {
        assertFalse(QueryRunner.isMutation(input));
    }

    @Test
    public void testNullMutation() {
        assertFalse(QueryRunner.isMutation(null));
    }

    @Test
    void constraintViolationException() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);

        Elide elide = getElide(store, dictionary, null);

        QueryRunner queryRunner = new QueryRunner(elide, NO_VERSION);

        String body = """
                {"query":"mutation {book(op: UPSERT data: {id:1,title:\\"1984\\",price:{total:10.0,currency:{isoCode:\\"USD\\"}}}) {edges {node {id title authors(op: UPSERT data: {id:1,name:\\"George Orwell\\"}) {edges {node {id name}}}}}}}"}""";

        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        TestObject testObject = new TestObject();
        Set<ConstraintViolation<TestObject>> violations = validator.validate(testObject);
        ConstraintViolationException e = new ConstraintViolationException("message", violations);
        Book mockModel = mock(Book.class);
        when(store.beginTransaction()).thenReturn(tx);
        when(tx.createNewObject(eq(ClassType.of(Book.class)), any())).thenReturn(mockModel);
        doThrow(e).when(tx).preCommit(any());

        ElideResponse<String> response = queryRunner.run("", body, null);
        SimpleModule module = new SimpleModule("GraphQLDeserializer", Version.unknownVersion());
        module.addDeserializer(GraphQLError.class, new GraphQLErrorDeserializer());
        elide.getObjectMapper().registerModule(module);
        GraphQLErrors errorObjects = elide.getObjectMapper().readValue(response.getBody(), GraphQLErrors.class);
        assertEquals(3, errorObjects.getErrors().size());
        for (GraphQLError errorObject : errorObjects.getErrors()) {
            Map<String, Object> extensions = errorObject.getExtensions();
            String expected;
            String actual = elide.getObjectMapper().writeValueAsString(errorObject);
            switch (extensions.get("property").toString()) {
            case "nestedTestObject.nestedNotNullField":
                expected = """
                        {"message":"must not be null","extensions":{"code":"NotNull","type":"ConstraintViolation","property":"nestedTestObject.nestedNotNullField","classification":"DataFetchingException"}}""";
                assertEquals(expected, actual);
                break;
            case "notNullField":
                expected = """
                        {"message":"must not be null","extensions":{"code":"NotNull","type":"ConstraintViolation","property":"notNullField","classification":"DataFetchingException"}}""";
                assertEquals(expected, actual);
                break;
            case "minField":
                expected = """
                        {"message":"must be greater than or equal to 5","extensions":{"code":"Min","type":"ConstraintViolation","property":"minField","classification":"DataFetchingException"}}""";
                assertEquals(expected, actual);
                break;
            }
        }

        verify(tx).close();
    }

    private Elide getElide(DataStore dataStore, EntityDictionary dictionary, ExceptionMappers exceptionMappers) {
        ElideSettings settings = getElideSettings(dataStore, dictionary, exceptionMappers);
        return new Elide(settings, new TransactionRegistry(), settings.getEntityDictionary().getScanner(), false);
    }

    private ElideSettings getElideSettings(DataStore dataStore, EntityDictionary dictionary, ExceptionMappers exceptionMappers) {
        return ElideSettings.builder().dataStore(dataStore)
                .entityDictionary(dictionary)
                .verboseErrors(true)
                .settings(GraphQLSettings.builder().graphqlExceptionHandler(
                        new DefaultGraphQLExceptionHandler(new Slf4jExceptionLogger(), exceptionMappers,
                                new DefaultGraphQLErrorMapper())))
                .build();
    }

    public static class TestObject {
        public static class NestedTestObject {
            @NotNull
            private String nestedNotNullField;
        }

        @NotNull
        private String notNullField;

        @Min(5)
        private int minField = 1;

        @Valid
        private NestedTestObject nestedTestObject = new NestedTestObject();
    }
}
