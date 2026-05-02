/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql.subscriptions.serialization;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yahoo.elide.graphql.subscriptions.annotations.SubscriptionField;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Id;
import lombok.Data;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

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
    void serializeFields() {
        FieldsModel model = new FieldsModel();
        model.setId("id");
        model.setField("field");
        model.setIgnore("ignore");

        ObjectMapper objectMapper = JsonMapper.builder()
                .disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                .addModule(new GraphQLSubscriptionModule()).build();
        String actual = objectMapper.writeValueAsString(model);
        String expected = """
                {"id":"id","field":"field"}""";
        assertEquals(expected, actual);
    }

    @Test
    void serializeProperties() {
        PropertiesModel model = new PropertiesModel();
        model.setId("id");
        model.setField("field");
        model.setIgnore("ignore");

        ObjectMapper objectMapper = JsonMapper.builder()
                .disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                .addModule(new GraphQLSubscriptionModule()).build();
        String actual = objectMapper.writeValueAsString(model);
        String expected = """
                {"id":"id","field":"field"}""";
        assertEquals(expected, actual);
    }
}
