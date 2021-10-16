/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql.subscriptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import com.yahoo.elide.graphql.subscriptions.annotations.SubscriptionField;
import com.yahoo.elide.graphql.subscriptions.hooks.SubscriptionExclusionStrategy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.Test;

import lombok.Data;

import java.util.Date;
import javax.persistence.Id;

public class SubscriptionSerdeTest {

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
        Gson gson = new GsonBuilder().addSerializationExclusionStrategy(new SubscriptionExclusionStrategy())
                .serializeNulls().create();

        TestModel testModel = new TestModel();
        testModel.setId(1);
        testModel.setDate(new Date(0));
        testModel.setNotSerialized("should not be present");
        testModel.setStringField("foo");

        String output = gson.toJson(testModel);
        assertEquals("{\"id\":1,\"stringField\":\"foo\",\"date\":\"Dec 31, 1969, 6:00:00 PM\"}", output);
    }

    @Test
    public void testDeserialization() {
        Gson gson = new GsonBuilder().addSerializationExclusionStrategy(new SubscriptionExclusionStrategy())
                .serializeNulls().create();

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
}
