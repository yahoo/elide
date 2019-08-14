/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.lang.reflect.AccessibleObject;
import java.util.List;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;

public class EntityBindingTest {
    private static EntityBinding entityBinding;

    @Mock
    static
    EntityDictionary entityDictionary;

    @BeforeAll
    public static void init() {
        entityBinding = new EntityBinding(entityDictionary, ChildClass.class, "child", "childBinding");
    }

    @Test
    public void testGetAllFields() throws Exception {
        List<AccessibleObject> allFields = entityBinding.getAllFields();

        assertEquals(2, allFields.size());
        assertEquals(allFields.get(0), ChildClass.class.getDeclaredField("childField"));
        assertEquals(allFields.get(1), ParentClass.class.getDeclaredField("parentField"));
    }

    @Test
    public void testIdField() throws Exception {
        AccessibleObject idField = entityBinding.getIdField();
        assertEquals(idField, ParentClass.class.getDeclaredField("parentField"));
    }

    @Test
    public void testIdGeneratedFalseWhenNoAnnotations() throws Exception {
        assertFalse(entityBinding.isIdGenerated());
    }

    @Test
    public void testIdGeneratedTrueWhenGenerateValue() throws Exception {
        final EntityBinding eb = new EntityBinding(entityDictionary, GeneratedValueClass.class, "test", "testBinding");
        assertTrue(eb.isIdGenerated());
    }

    @Test
    public void testIdGeneratedTrueWhenMapsId() throws Exception {
        final EntityBinding eb = new EntityBinding(entityDictionary, MapsIdClass.class, "test", "testBinding");
        assertTrue(eb.isIdGenerated());
    }

    @Test
    public void testIdGeneratedFalseWhenBadMapsId() throws Exception {
        final EntityBinding eb = new EntityBinding(entityDictionary, BadMapsIdClass.class, "test", "testBinding");
        assertFalse(eb.isIdGenerated());
    }

    private class ParentClass {
        @Id
        String parentField;

        public void ignoredMethod() { }
    }

    private class ChildClass extends ParentClass {
        String childField;
    }

    private class GeneratedValueClass {
        @Id
        @GeneratedValue
        String id;
    }

    private class MapsIdClass extends ParentClass {
        @OneToOne
        @MapsId
        public ParentClass parent;
    }

    private class BadMapsIdClass extends ParentClass {
        @MapsId
        public ParentClass parent;
    }
}
