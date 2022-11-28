/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql.subscriptions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yahoo.elide.core.utils.coerce.converters.ISO8601DateSerde;
import com.yahoo.elide.graphql.subscriptions.annotations.SubscriptionField;
import com.yahoo.elide.graphql.subscriptions.hooks.SubscriptionExclusionStrategy;
import com.yahoo.elide.graphql.subscriptions.hooks.SubscriptionFieldSerde;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Id;
import lombok.Data;

import java.util.Date;
import java.util.TimeZone;

public class SubscriptionSerdeTest {

    private Gson gson;

    public SubscriptionSerdeTest() {
        ISO8601DateSerde serde = new ISO8601DateSerde("yyyy-MM-dd'T'HH:mm'Z'", TimeZone.getTimeZone("UTC"));

        gson = new GsonBuilder()
                .addSerializationExclusionStrategy(new SubscriptionExclusionStrategy())
                .registerTypeAdapter(Date.class, new SubscriptionFieldSerde<>(serde))
                .serializeNulls().create();
    }

    @Data
    public static class TestModel {
        @Id
        long id;

        @SubscriptionField
        String stringField;

        @SubscriptionField
        Date date;

        String notSerialized;
    }

    @Test
    public void testSerialization() {
        TestModel testModel = new TestModel();
        testModel.setId(1);
        testModel.setDate(new Date(0));
        testModel.setNotSerialized("should not be present");
        testModel.setStringField("foo");

        String output = gson.toJson(testModel);
        assertEquals("{\"id\":1,\"stringField\":\"foo\",\"date\":\"1970-01-01T00:00Z\"}", output);
    }

    @Test
    public void testSerializationWithNull() {
        TestModel testModel = new TestModel();
        testModel.setId(1);
        testModel.setDate(null);
        testModel.setNotSerialized("should not be present");
        testModel.setStringField("foo");

        String output = gson.toJson(testModel);
        assertEquals("{\"id\":1,\"stringField\":\"foo\",\"date\":null}", output);
    }

    @Test
    public void testDeserialization() {
        TestModel testModel = new TestModel();
        testModel.setId(1);
        testModel.setDate(new Date(0));
        testModel.setNotSerialized("should not be present");
        testModel.setStringField("foo");

        String output = gson.toJson(testModel);

        TestModel deserialized = gson.fromJson(output, TestModel.class);

        testModel.setNotSerialized(null);
        assertEquals(testModel, deserialized);
    }

    @Test
    public void testDeserializationWithNull() {
        TestModel testModel = new TestModel();
        testModel.setId(1);
        testModel.setDate(null);
        testModel.setNotSerialized("should not be present");
        testModel.setStringField("foo");

        String output = gson.toJson(testModel);

        TestModel deserialized = gson.fromJson(output, TestModel.class);

        testModel.setNotSerialized(null);
        assertEquals(testModel, deserialized);
    }
}
