/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.UpdatePermission;
import org.junit.jupiter.api.Test;

import java.util.Set;

public class ClassScannerTest {

    @Test
    public void testGetAllClasses() {
        Set<Class<?>> classes = ClassScanner.getAllClasses("com.yahoo.elide.core.utils");
        assertEquals(31, classes.size());
        assertTrue(classes.contains(ClassScannerTest.class));
    }

    @Test
    public void testGetAnnotatedClasses() {
        Set<Class<?>> classes = ClassScanner.getAnnotatedClasses("example", ReadPermission.class);
        assertEquals(6, classes.size(), "Actual: " + classes);
        classes.forEach(cls -> assertTrue(cls.isAnnotationPresent(ReadPermission.class)));
    }

    @Test
    public void testGetAllAnnotatedClasses() {
        Set<Class<?>> classes = ClassScanner.getAnnotatedClasses(ReadPermission.class);
        assertEquals(12, classes.size(), "Actual: " + classes);
        classes.forEach(cls -> assertTrue(cls.isAnnotationPresent(ReadPermission.class)));
    }

    @Test
    public void testGetAnyAnnotatedClasses() {
        Set<Class<?>> classes = ClassScanner.getAnnotatedClasses(ReadPermission.class, UpdatePermission.class);
        assertEquals(17, classes.size());
        for (Class<?> cls : classes) {
            assertTrue(cls.isAnnotationPresent(ReadPermission.class)
                    || cls.isAnnotationPresent(UpdatePermission.class));
        }
    }
}
