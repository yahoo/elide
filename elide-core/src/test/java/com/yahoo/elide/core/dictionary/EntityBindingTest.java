/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.dictionary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yahoo.elide.core.type.AccessibleObject;
import com.yahoo.elide.core.type.ClassType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;

import java.util.List;
import java.util.stream.Collectors;

public class EntityBindingTest {
    private static EntityBinding entityBinding;

    @BeforeAll
    public static void init() {
        entityBinding = new EntityBinding(
                null, ClassType.of(ChildClass.class), "childBinding");
    }

    @Test
    public void testGetAllFields() throws Exception {
        List<String> allFields = entityBinding.getAllFields().stream()
                .map(AccessibleObject::getName)
                .collect(Collectors.toList());

        assertEquals(2, allFields.size());
        assertEquals(allFields.get(0), "childField");
        assertEquals(allFields.get(1), "parentField");
    }

    @Test
    public void testIdField() throws Exception {
        AccessibleObject idField = entityBinding.getIdField();
        assertEquals(idField, ClassType.of(ParentClass.class).getDeclaredField("parentField"));
    }

    @Test
    public void testIdGeneratedFalseWhenNoAnnotations() throws Exception {
        assertFalse(entityBinding.isIdGenerated());
    }

    @Test
    public void testIdGeneratedTrueWhenGenerateValue() throws Exception {
        final EntityBinding eb = new EntityBinding(null,
                ClassType.of(GeneratedValueClass.class), "testBinding");
        assertTrue(eb.isIdGenerated());
    }

    @Test
    public void testIdGeneratedTrueWhenMapsId() throws Exception {
        final EntityBinding eb = new EntityBinding(null,
                ClassType.of(MapsIdClass.class), "testBinding");
        assertTrue(eb.isIdGenerated());
    }

    @Test
    public void testIdGeneratedFalseWhenBadMapsId() throws Exception {
        final EntityBinding eb = new EntityBinding(null,
                ClassType.of(BadMapsIdClass.class), "testBinding");
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
