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

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

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
    public void testComputedAttributeIsExposed() {
        List<String> attributes = getAttributes(User.class);
        Assert.assertTrue(attributes.contains("password"));
    }

    @Test
    public void testGetIdAnnotations() throws Exception {

        Collection<Class> expectedAnnotationClasses = Arrays.asList(new Class[]{Id.class, GeneratedValue.class});
        Collection<Class> actualAnnotationsClasses  = getIdAnnotations(new Parent()).stream()
            .map(Annotation::annotationType)
        .collect(Collectors.toList());

        Assert.assertEquals(actualAnnotationsClasses, expectedAnnotationClasses,
                "getIdAnnotations returns annotations on the ID field of the given class");
    }

    @Test
    public void testGetIdAnnotationsNoId() throws Exception {

        Collection<Annotation> expectedAnnotation = Collections.emptyList();
        Collection<Annotation> actualAnnotations  = getIdAnnotations(new Object());

        Assert.assertEquals(actualAnnotations, expectedAnnotation,
                "getIdAnnotations returns an empty collection if there is no ID field for given class");
    }

    @Test
    public void testGetIdAnnotationsSubClass() throws Exception {

        class Friend extends Child { }

        Collection<Class> expectedAnnotationClasses = Arrays.asList(new Class[]{Id.class, GeneratedValue.class});
        Collection<Class> actualAnnotationsClasses  = getIdAnnotations(new Friend()).stream()
                .map(Annotation::annotationType)
        .collect(Collectors.toList());

        Assert.assertEquals(actualAnnotationsClasses, expectedAnnotationClasses,
                "getIdAnnotations returns annotations on the ID field when defined in a super class");
    }
}
