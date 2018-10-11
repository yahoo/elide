/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import static org.testng.Assert.assertEquals;

import org.mockito.Mock;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.lang.reflect.AccessibleObject;
import java.util.List;

import javax.persistence.Id;

public class EntityBindingTest {
    private EntityBinding entityBinding;

    @Mock
    EntityDictionary entityDictionary;

    @BeforeTest
    public void init() {
        entityBinding = new EntityBinding(entityDictionary, ChildClass.class, "child", "childBinding");
    }

    @Test
    public void testGetAllFields() throws Exception {
        List<AccessibleObject> allFields = entityBinding.getAllFields();

        assertEquals(allFields.size(), 2);
        assertEquals(allFields.get(0), ChildClass.class.getDeclaredField("childField"));
        assertEquals(allFields.get(1), ParentClass.class.getDeclaredField("parentField"));
    }

    @Test
    public void testIdField() throws Exception {
        AccessibleObject idField = entityBinding.getIdField();
        assertEquals(idField, ParentClass.class.getDeclaredField("parentField"));
    }

    private class ParentClass {
        @Id
        String parentField;

        public void ignoredMethod() { }
    }

    private class ChildClass extends ParentClass {
        String childField;
    }
}
