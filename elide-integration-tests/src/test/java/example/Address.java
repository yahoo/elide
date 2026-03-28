/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import lombok.Data;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;

@Embeddable
@Data
public class Address {

    private String street1;
    private String street2;

    @Column(name = "properties", columnDefinition = "TEXT")
    @Convert(attributeName = "key", converter = MapConverter.class)
    Map<Object, Object> properties;

    public static class MapConverter implements AttributeConverter<Map<Object, Object>, String> {
        ObjectMapper mapper = new ObjectMapper();

        @Override
        public String convertToDatabaseColumn(Map<Object, Object> attribute) {
            try {
                return mapper.writeValueAsString(attribute);
            } catch (JacksonException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public Map<Object, Object> convertToEntityAttribute(String dbData) {
            TypeReference<HashMap<Object, Object>> typeRef = new TypeReference<>() {
            };
            return mapper.readValue(dbData, typeRef);
        }
    }
}
