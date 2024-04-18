/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ObjectPropertiesTest {
    public static class PrimitiveFieldProtectedModel {
        protected int field = 10;
    }

    public static class PrimitiveMethodPublicModel {
        private int privatefield = 10;
        private boolean privateboolean = true;

        public int getField() {
            return privatefield;
        }

        public void setField(int field) {
            this.privatefield = field;
        }

        public boolean isBoolean() {
            return privateboolean;
        }

        public void setBoolean(boolean privateboolean) {
            this.privateboolean = privateboolean;
        }
    }

    public static class PrimitiveFieldPublicModel {
        public int field = 10;
    }

    public static class PrimitiveMethodProtectedModel {
        private int privatefield = 10;
        private boolean privateboolean = true;

        protected int getField() {
            return privatefield;
        }

        protected void setField(int field) {
            this.privatefield = field;
        }

        protected boolean isBoolean() {
            return privateboolean;
        }

        protected void setBoolean(boolean privateboolean) {
            this.privateboolean = privateboolean;
        }
    }

    @Test
    void primitiveProtectedField() {
        Integer value = ObjectProperties.getProperty(new PrimitiveFieldProtectedModel(), "field");
        assertEquals(10, value);
    }

    @Test
    void primitivePublicMethod() {
        Integer value = ObjectProperties.getProperty(new PrimitiveMethodPublicModel(), "field");
        assertEquals(10, value);
    }

    @Test
    void primitivePublicField() {
        Integer value = ObjectProperties.getProperty(new PrimitiveFieldPublicModel(), "field");
        assertEquals(10, value);
    }

    @Test
    void primitiveProtectedMethod() {
        Integer value = ObjectProperties.getProperty(new PrimitiveMethodProtectedModel(), "field");
        assertEquals(10, value);
    }

    @Test
    void primitivePublicBooleanMethod() {
        Boolean value = ObjectProperties.getProperty(new PrimitiveMethodPublicModel(), "boolean");
        assertTrue(value);
    }

    @Test
    void primitiveProtectedBooleanMethod() {
        Boolean value = ObjectProperties.getProperty(new PrimitiveMethodProtectedModel(), "boolean");
        assertTrue(value);
    }

    @Test
    void primitiveMethodClass() {
        PrimitiveMethodPublicModel object = new PrimitiveMethodPublicModel();
        Integer value = ObjectProperties.getProperty(object, "field", PrimitiveMethodPublicModel.class);
        assertEquals(10, value);
    }
}
