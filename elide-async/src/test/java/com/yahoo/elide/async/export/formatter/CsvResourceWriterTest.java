/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.export.formatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.request.Attribute;
import com.yahoo.elide.core.request.EntityProjection;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.junit.jupiter.api.Test;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class CsvResourceWriterTest {
    // 2023-12-25T12:30:30.000000010+00:00
    public static OffsetDateTime REFERENCE = OffsetDateTime.of(LocalDate.of(2023, 12, 25), LocalTime.of(12, 30, 30, 10),
            ZoneOffset.UTC);

    enum EnumValue {
        VALUE1,
        VALUE2
    }

    @Data
    public class Nested {
        private EnumValue enumValue = EnumValue.VALUE1;
        private Double doubleNumber = Double.valueOf(1);
        private Float floatNumber = Float.valueOf(1);
        private Long longNumber = Long.valueOf(1L);
        private Integer integerNumber = 1;
        private Instant instant = REFERENCE.toInstant();
        private Date date = Date.from(REFERENCE.toInstant());
        private LocalDateTime localDateTime = REFERENCE.toInstant().atOffset(ZoneOffset.UTC).toLocalDateTime();
        private LocalDate localDate = REFERENCE.toInstant().atOffset(ZoneOffset.UTC).toLocalDate();
        private OffsetDateTime offsetDateTime = REFERENCE;
        private ZonedDateTime zonedDateTime = REFERENCE.toZonedDateTime();
    }

    @Include(name = "export")
    @Entity
    @Data
    public class Export {
        @Id
        private String name = "";

        @Convert(converter = CommaDelimitedStringConverter.class)
        private Set<String> alternatives;

        private Nested nested = new Nested();

        public static class CommaDelimitedStringConverter implements AttributeConverter<Set<String>, String> {

            @Override
            public String convertToDatabaseColumn(Set<String> attribute) {
                if (attribute == null) {
                    return null;
                }
                return String.join(",", attribute);
            }

            @Override
            public Set<String> convertToEntityAttribute(String dbData) {
                if (dbData == null) {
                    return null;
                }
                Set<String> result = new LinkedHashSet<>();
                Collections.addAll(result, dbData.split(","));
                return result;
            }
        }
    }

    @Test
    void set() throws IOException {
        // Prepare EntityProjection
        Set<Attribute> attributes = new LinkedHashSet<>();
        attributes.add(Attribute.builder().type(String.class).name("name").build());
        attributes.add(Attribute.builder().type(Set.class).name("alternatives").build());
        attributes.add(Attribute.builder().type(Nested.class).name("nested").build());
        EntityProjection entityProjection = EntityProjection.builder().type(Export.class).attributes(attributes).build();

        Export export = new Export();
        export.setName("name");
        Set<String> alternatives = new LinkedHashSet<>();
        alternatives.add("a");
        alternatives.add("b");
        alternatives.add("c");
        export.setAlternatives(alternatives);

        Map<String, Object> resourceAttributes = new LinkedHashMap<>();
        resourceAttributes.put("name", export.getName());
        resourceAttributes.put("alternatives", export.getAlternatives());
        resourceAttributes.put("nested", export.getNested());

        RequestScope scope = mock(RequestScope.class);
        PersistentResource<Export> persistentResource = mock(PersistentResource.class);
        when(persistentResource.getObject()).thenReturn(export);
        when(persistentResource.getRequestScope()).thenReturn(scope);
        when(persistentResource.getAttribute(any(Attribute.class))).thenAnswer(key -> {
            return resourceAttributes.get(((Attribute) key.getArgument(0)).getName());
        });
        when(scope.getEntityProjection()).thenReturn(entityProjection);

        byte[] data = process(entityProjection, writer -> {
            writer.write(persistentResource);
        });
        List<String> results = read(data);
        String header = """
               "name","alternatives","nested_enumValue","nested_doubleNumber","nested_floatNumber","nested_longNumber","nested_integerNumber","nested_instant","nested_date","nested_localDateTime","nested_localDate","nested_offsetDateTime","nested_zonedDateTime"\
               """;
        assertEquals(header, results.get(0));
        String row = """
                "name","a;b;c","VALUE1","1.0","1.0","1","1","2023-12-25T12:30:30.000000010Z","2023-12-25T12:30:30.000+00:00","2023-12-25T12:30:30.00000001","2023-12-25","2023-12-25T12:30:30.00000001Z","2023-12-25T12:30:30.00000001Z"\
                """;
        assertEquals(row, results.get(1));
    }

    List<String> read(byte[] data) {
        List<String> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(data)))) {
            result = reader.lines().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return result;
    }

    byte[] process(EntityProjection entityProjection, ResourceWriterProcessor processor) {
        ObjectMapper objectMapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                ResourceWriter writer = new CsvResourceWriter(outputStream, objectMapper, true, entityProjection)) {
            processor.process(writer);
            writer.close();
            outputStream.close();
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @FunctionalInterface
    interface ResourceWriterProcessor {
        void process(ResourceWriter writer) throws IOException;
    }
}
