/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import com.yahoo.elide.annotation.ReadPermission;
import example.Child;
import example.FunWithPermissions;
import example.Parent;
import example.User;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.lang.annotation.Annotation;
import java.util.List;

public class EntityDictionaryTest extends EntityDictionary {
    @BeforeTest
    public void init() {
        this.bindEntity(FunWithPermissions.class);
        this.bindEntity(Parent.class);
        this.bindEntity(Child.class);
        this.bindEntity(User.class);
    }

    @Test
    public void testGetAttributeOrRelationAnnotation() {
        String[] fields = {"field1", "field2", "field3", "relation1", "relation2"};
        Annotation annotation;
        for (String field : fields) {
            annotation = this.getAttributeOrRelationAnnotation(FunWithPermissions.class, ReadPermission.class, field);
            Assert.assertTrue(annotation != null && annotation instanceof ReadPermission, "Every field should return a ReadPermission annotation");
        }
    }

    @Test
    public void testGetParameterizedType() {
        Class<?> type;

        FunWithPermissions fun = new FunWithPermissions();

        type = getParameterizedType(fun, "relation2");
        Assert.assertEquals(type, Child.class, "A set of Child objects should return Child.class");

        type = getParameterizedType(fun, "relation3");
        Assert.assertEquals(type, Child.class, "A Child object should return Child.class");
    }

    @Test
    public void testGetInverseRelationshipOwningSide()  {
        Assert.assertEquals(getRelationInverse(Parent.class, "children"), "parents",
                "The inverse relationship of children should be parents");
    }

    @Test
    public void testGetInverseRelationshipOwnedSide()  {
        Assert.assertEquals(getRelationInverse(Child.class, "parents"), "children",
                "The inverse relationship of children should be parents");
    }

    @Test
    public void testComputedPropertyIsExposed() {
        List<String> attributes = getAttributes(User.class);
        Assert.assertTrue(attributes.contains("password"));
    }
}
