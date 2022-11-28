/*
 * Copyright 2022, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.type;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import java.util.Optional;
public class EntityFieldTypeTest {

    static class TestModel {
        @OneToOne(targetEntity = String.class)
        Object field1;

        @ManyToOne(targetEntity = String.class)
        Object field2;

        @OneToMany(targetEntity = String.class)
        Object field3;

        @ManyToMany(targetEntity = String.class)
        Object field4;

        Object field5;
    }

    @Test
    public void testGetType() throws Exception {

        Type<?> type = ClassType.of(TestModel.class);

        String [] fieldNames = {"field1", "field2", "field3", "field4", "field5"};

        for (String fieldName : fieldNames) {
            Field field = type.getDeclaredField(fieldName);
            assertEquals(ClassType.OBJECT_TYPE, field.getType());
        }
    }

    @Test
    public void testGetParameterizedReturnType() throws Exception {

        Type<?> type = ClassType.of(TestModel.class);

        String [] fieldNames = {"field1", "field2", "field3", "field4"};

        for (String fieldName : fieldNames) {
            Field field = type.getDeclaredField(fieldName);
            assertEquals(ClassType.STRING_TYPE, field.getParameterizedType(type, Optional.of(0)));
        }

        Field field = type.getDeclaredField("field5");
        assertEquals(ClassType.OBJECT_TYPE, field.getParameterizedType(type, Optional.of(0)));
    }
}
