/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.graphql.subscriptions.serialization;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.paiondata.elide.graphql.subscriptions.annotations.SubscriptionField;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Id;
import lombok.Data;

class SubscriptionBeanSerializerModifierTest {
    @Data
    public static class FieldsModel {
        @Id
        private String id;

        @SubscriptionField
        private String field;

        private String ignore;
    }

    public static class PropertiesModel {
        private String id;

        private String field;

        private String ignore;

        @Id
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        @SubscriptionField
        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public String getIgnore() {
            return ignore;
        }

        public void setIgnore(String ignore) {
            this.ignore = ignore;
        }
    }

    @Test
    void serializeFields() throws JsonProcessingException {
        FieldsModel model = new FieldsModel();
        model.setId("id");
        model.setField("field");
        model.setIgnore("ignore");

        ObjectMapper objectMapper = JsonMapper.builder()
                .addModule(new GraphQLSubscriptionModule()).build();
        String actual = objectMapper.writeValueAsString(model);
        String expected = """
                {"id":"id","field":"field"}""";
        assertEquals(expected, actual);
    }

    @Test
    void serializeProperties() throws JsonProcessingException {
        PropertiesModel model = new PropertiesModel();
        model.setId("id");
        model.setField("field");
        model.setIgnore("ignore");

        ObjectMapper objectMapper = JsonMapper.builder()
                .addModule(new GraphQLSubscriptionModule()).build();
        String actual = objectMapper.writeValueAsString(model);
        String expected = """
                {"id":"id","field":"field"}""";
        assertEquals(expected, actual);
    }
}
