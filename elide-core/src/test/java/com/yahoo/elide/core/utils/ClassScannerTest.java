/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.utils.ClassScanner;
import org.junit.jupiter.api.Test;
import java.util.Set;

public class ClassScannerTest {

    @Test
    public void testGetAllClasses() {
        Set<Class<?>> classes = ClassScanner.getAllClasses("com.yahoo.elide.core.utils");
        assertEquals(1, classes.size());
        assertEquals(ClassScannerTest.class, classes.iterator().next());
    }

    @Test
    public void testGetAnnotatedClasses() {
        Set<Class<?>> classes = ClassScanner.getAnnotatedClasses("example", ReadPermission.class);
        for (Class<?> cls : classes) {
            assertTrue(cls.isAnnotationPresent(ReadPermission.class));
        }
    }
}
