/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.Collections;

/**
 * Test for ObjectCloner.
 */
public class ObjectClonersTest {
    public static class NoDefaultConstructorObject {
        private Long id;

        public NoDefaultConstructorObject(NoDefaultConstructorObject copy) {
            this.id = copy.id;
        }

        public NoDefaultConstructorObject(Long id) {
            this.id = id;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
    }

    @Test
    void testClone() {
        ObjectClonerTestObject object = new ObjectClonerTestObject();
        object.setId(1L);
        object.setAdmin(true);
        object.setName("name");
        object.setList(Collections.singletonList("value"));

        ObjectClonerTestObject clone = ObjectCloners.clone(object);
        assertTrue(clone.isAdmin());
        assertEquals("name", clone.getName());
        assertEquals("value", clone.getList().get(0));
        assertEquals(1L, clone.id());
        assertTrue(clone != object);
    }

    @Test
    void testShouldReturnOriginalObject() {
        NoDefaultConstructorObject object = new NoDefaultConstructorObject(1L);
        NoDefaultConstructorObject clone = ObjectCloners.clone(object);
        assertEquals(1L, clone.getId());
        assertTrue(clone == object);
    }
}
