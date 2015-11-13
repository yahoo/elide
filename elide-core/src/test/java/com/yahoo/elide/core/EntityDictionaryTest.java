/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import com.yahoo.elide.annotation.ReadPermission;

import example.Child;
import example.FieldAnnotations;
import example.FunWithPermissions;
import example.Left;
import example.Parent;
import example.Right;
import example.StringId;
import example.User;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

public class EntityDictionaryTest extends EntityDictionary {

    //Test class to validate inheritance logic
    private class Friend extends Child { }

    @BeforeTest
    public void init() {
        this.bindEntity(FunWithPermissions.class);
        this.bindEntity(Parent.class);
        this.bindEntity(Child.class);
        this.bindEntity(User.class);
        this.bindEntity(Left.class);
        this.bindEntity(Right.class);
        this.bindEntity(StringId.class);
        this.bindEntity(Friend.class);
        this.bindEntity(FieldAnnotations.class);
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
        Collection<Annotation> actualAnnotations  = getIdAnnotations(new NoId());

        Assert.assertEquals(actualAnnotations, expectedAnnotation,
                "getIdAnnotations returns an empty collection if there is no ID field for given class");
    }

    @Entity
    class NoId {

    }

    @Test
    public void testGetIdAnnotationsSubClass() throws Exception {

        Collection<Class> expectedAnnotationClasses = Arrays.asList(new Class[]{Id.class, GeneratedValue.class});
        Collection<Class> actualAnnotationsClasses  = getIdAnnotations(new Friend()).stream()
                .map(Annotation::annotationType)
        .collect(Collectors.toList());

        Assert.assertEquals(actualAnnotationsClasses, expectedAnnotationClasses,
                "getIdAnnotations returns annotations on the ID field when defined in a super class");
    }

    @Test
    public void testIsSharableTrue() throws Exception {
        Assert.assertTrue(isShareable(Right.class));
    }

    @Test
    public void testIsSharableFalse() throws Exception {
        Assert.assertFalse(isShareable(Left.class));
    }

    @Test
    public void testGetIdType() throws Exception {

        Assert.assertEquals(getIdType(Parent.class), long.class,
                "getIdType returns the type of the ID field of the given class");

        Assert.assertEquals(getIdType(StringId.class), String.class,
                "getIdType returns the type of the ID field of the given class");

        Assert.assertEquals(getIdType(NoId.class), null,
                "getIdType returns null if ID field is missing");

        Assert.assertEquals(getIdType(Friend.class), long.class,
                "getIdType returns the type of the ID field when defined in a super class");
    }

    @Test
    public void testGetType() throws Exception {

        Assert.assertEquals(getType(FieldAnnotations.class, "id"), Long.class,
            "getType returns the type of the ID field of the given class");

        Assert.assertEquals(getType(FieldAnnotations.class, "publicField"), long.class,
            "getType returns the type of attribute when Column annotation is on a field");

        Assert.assertEquals(getType(FieldAnnotations.class, "privateField"), Boolean.class,
            "getType returns the type of attribute when Column annotation is on a getter");

        Assert.assertEquals(getType(FieldAnnotations.class, "missingField"), null,
            "getId returns null if attribute is missing");

        Assert.assertEquals(getType(Parent.class, "children"), Set.class,
            "getType returns the type of relationship fields");

        Assert.assertEquals(getType(Friend.class, "name"), String.class,
                "getType returns the type of attribute when defined in a super class");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testGetTypUnknownEntityException() {
        getType(Object.class, "id");
    }
}
