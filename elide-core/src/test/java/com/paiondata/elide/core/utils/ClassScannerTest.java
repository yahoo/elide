/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.paiondata.elide.annotation.Include;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;

import java.util.Set;

public class ClassScannerTest {

    private final ClassScanner scanner;

    public ClassScannerTest() {
        scanner = new DefaultClassScanner();
    }

    @Test
    public void testGetAllClasses() {
        Set<Class<?>> classes = scanner.getAllClasses("com.paiondata.elide.core.utils");
        assertEquals(44, classes.size());
        assertTrue(classes.contains(ClassScannerTest.class));
    }

    @Test
    public void testGetAnnotatedClasses() {
        Set<Class<?>> classes = scanner.getAnnotatedClasses("example", Include.class);
        assertEquals(36, classes.size(), "Actual: " + classes);
        classes.forEach(cls -> assertTrue(cls.isAnnotationPresent(Include.class)));
    }

    @Test
    public void testGetAllAnnotatedClasses() {
        Set<Class<?>> classes = scanner.getAnnotatedClasses(Include.class);
        assertEquals(48, classes.size(), "Actual: " + classes);
        classes.forEach(cls -> assertTrue(cls.isAnnotationPresent(Include.class)));
    }

    @Test
    public void testGetAnyAnnotatedClasses() {
        Set<Class<?>> classes = scanner.getAnnotatedClasses(Include.class, Entity.class);
        assertEquals(59, classes.size());
        for (Class<?> cls : classes) {
            assertTrue(cls.isAnnotationPresent(Include.class)
                    || cls.isAnnotationPresent(Entity.class));
        }
    }

    @Test
    public void testGetAnnotatedClassesNoClassesFound() {
        assertThrows(IllegalArgumentException.class, () -> {
            scanner.getAnnotatedClasses("nonexistent.package", Include.class);
        }, "No annotated classes found in the specified package: nonexistent.package");
    }
}
