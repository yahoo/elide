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

public class EntityMethodTypeTest {

    static class TestModel {
        @OneToOne(targetEntity = String.class)
        public Object getField1() {
            return null;
        }

        @ManyToOne(targetEntity = String.class)
        public Object getField2() {
            return null;
        }

        @OneToMany(targetEntity = String.class)
        public Object getField3() {
            return null;
        }

        @ManyToMany(targetEntity = String.class)
        public Object getField4() {
            return null;
        }

        public Object getField5() {
            return null;
        }
    }

    @Test
    public void testGetReturnType() throws Exception {

        Type<?> type = ClassType.of(TestModel.class);

        String [] methodNames = {"getField1", "getField2", "getField3", "getField4", "getField5"};

        for (String methodName : methodNames) {
            Method method = type.getMethod(methodName);
            assertEquals(ClassType.OBJECT_TYPE, method.getReturnType());
        }
    }

    @Test
    public void testGetParameterizedReturnType() throws Exception {

        Type<?> type = ClassType.of(TestModel.class);

        String [] methodNames = {"getField1", "getField2", "getField3", "getField4"};

        for (String methodName : methodNames) {
            Method method = type.getMethod(methodName);
            assertEquals(ClassType.STRING_TYPE, method.getParameterizedReturnType(type, Optional.of(0)));
        }

        Method method = type.getMethod("getField5");
        assertEquals(ClassType.OBJECT_TYPE, method.getParameterizedReturnType(type, Optional.of(0)));
    }
}
