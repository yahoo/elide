/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import static org.mockito.Mockito.mock;

import com.yahoo.elide.annotation.ComputedAttribute;
import com.yahoo.elide.annotation.Exclude;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.MappedInterface;
import com.yahoo.elide.annotation.OnUpdatePreSecurity;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.functions.LifeCycleHook;
import com.yahoo.elide.models.generics.Employee;
import com.yahoo.elide.models.generics.Manager;
import com.yahoo.elide.security.checks.prefab.Collections.AppendOnly;
import com.yahoo.elide.security.checks.prefab.Collections.RemoveOnly;
import com.yahoo.elide.security.checks.prefab.Common.UpdateOnCreate;
import com.yahoo.elide.security.checks.prefab.Role;

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

import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Transient;

public class EntityDictionaryTest extends EntityDictionary {

    //Test class to validate inheritance logic
    @Include(rootLevel = true, type = "friend")
    private class Friend extends Child { }

    public EntityDictionaryTest() {
        super(Collections.EMPTY_MAP);
    }

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
        this.bindEntity(Manager.class);
        this.bindEntity(Employee.class);

        checkNames.forcePut("user has all access", Role.ALL.class);
    }

    @Test
    public void testFindCheckByExpression() {
        Assert.assertEquals(getCheckIdentifier(Role.ALL.class), "user has all access");
        Assert.assertEquals(getCheckIdentifier(Role.NONE.class), "Prefab.Role.None");
        Assert.assertEquals(getCheckIdentifier(AppendOnly.class), "Prefab.Collections.AppendOnly");
        Assert.assertEquals(getCheckIdentifier(RemoveOnly.class), "Prefab.Collections.RemoveOnly");
        Assert.assertEquals(getCheckIdentifier(UpdateOnCreate.class), "Prefab.Common.UpdateOnCreate");
    }

    @Test
    public void testGetAttributeOrRelationAnnotation() {
        String[] fields = {"field1", "field2", "field3", "relation1", "relation2"};
        Annotation annotation;
        for (String field : fields) {
            annotation = this.getAttributeOrRelationAnnotation(FunWithPermissions.class, ReadPermission.class, field);
            Assert.assertTrue(annotation instanceof ReadPermission, "Every field should return a ReadPermission annotation");
        }
    }

    @Test
    public void testBindingInitializerPriorToBindingEntityClass() {
        @Entity
        @Include
        class Foo {
            @Id
            private long id;

            private int bar;
        }

        Initializer<Foo> initializer = mock(Initializer.class);
        this.bindInitializer(initializer, Foo.class);

        Assert.assertEquals(this.getAllFields(Foo.class).size(), 1);
    }

    @Test
    public void testBindingTriggerPriorToBindingEntityClass1() {
        @Entity
        @Include
        class Foo2 {
            @Id
            private long id;

            private int bar;
        }

        LifeCycleHook<Foo2> trigger = mock(LifeCycleHook.class);

        this.bindTrigger(Foo2.class, OnUpdatePreSecurity.class, "bar", trigger);
        Assert.assertEquals(this.getAllFields(Foo2.class).size(), 1);
    }

    @Test
    public void testBindingTriggerPriorToBindingEntityClass2() {
        @Entity
        @Include
        class Foo3 {
            @Id
            private long id;

            private int bar;
        }

        LifeCycleHook<Foo3> trigger = mock(LifeCycleHook.class);

        this.bindTrigger(Foo3.class, OnUpdatePreSecurity.class, trigger, true);
        Assert.assertEquals(this.getAllFields(Foo3.class).size(), 1);
    }

    @Test
    public void testBindingTriggerPriorToBindingEntityClass3() {
        @Entity
        @Include
        class Foo4 {
            @Id
            private long id;

            private int bar;
        }

        LifeCycleHook<Foo4> trigger = mock(LifeCycleHook.class);

        this.bindTrigger(Foo4.class, OnUpdatePreSecurity.class, trigger);
        Assert.assertEquals(this.getAllFields(Foo4.class).size(), 1);
    }

    @Test
    public void testJPAFieldLevelAccess() {
        @Entity
        @Include
        class FieldLevelTest {
            @Id
            private long id;

            private int bar;

            @Exclude
            private int excluded;

            @Transient
            @ComputedAttribute
            private int computedField;

            @Transient
            @ComputedAttribute
            public int getComputedProperty() {
                return 1;
            }

            public void setComputedProperty() {
                //NOOP
            }
        }
        this.bindEntity(FieldLevelTest.class);

        Assert.assertEquals(getAccessType(FieldLevelTest.class), AccessType.FIELD);

        List<String> fields = this.getAllFields(FieldLevelTest.class);
        Assert.assertEquals(fields.size(), 3);
        Assert.assertTrue(fields.contains("bar"));
        Assert.assertTrue(fields.contains("computedField"));
        Assert.assertTrue(fields.contains("computedProperty"));
    }

    @Test
    public void testJPAPropertyLevelAccess() {
        @Entity
        @Include
        class PropertyLevelTest {
            private long id;

            private int excluded;
            public int bar;


            @Exclude
            public int getExcluded() {
                return excluded;
            }

            public void setExcluded(int unused) {
                //noop
            }

            @Transient
            @ComputedAttribute
            private int computedField;

            @Transient
            @ComputedAttribute
            public int getComputedProperty() {
                return 1;
            }

            public void setComputedProperty() {
                //NOOP
            }
        }
        this.bindEntity(PropertyLevelTest.class);

        Assert.assertEquals(getAccessType(PropertyLevelTest.class), AccessType.PROPERTY);

        List<String> fields = this.getAllFields(PropertyLevelTest.class);
        Assert.assertEquals(fields.size(), 2);
        Assert.assertTrue(fields.contains("bar"));
        Assert.assertTrue(fields.contains("computedProperty"));
    }


    @Test
    public void testGetParameterizedType() {
        Class<?> type;

        FunWithPermissions fun = new FunWithPermissions();

        type = getParameterizedType(fun, "relation2");
        Assert.assertEquals(type, Child.class, "A set of Child objects should return Child.class");

        type = getParameterizedType(fun, "relation3");
        Assert.assertEquals(type, Child.class, "A Child object should return Child.class");

        Assert.assertEquals(getParameterizedType(FieldAnnotations.class, "children"), FieldAnnotations.class,
                "getParameterizedType return the type of a private field relationship");

        Assert.assertEquals(getParameterizedType(Parent.class, "children"), Child.class,
            "getParameterizedType returns the type of relationship fields");

        Assert.assertEquals(getParameterizedType(Manager.class, "minions"), Employee.class,
            "getParameterizedType returns the correct generic type of a to-many relationship");
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
    public void testExcludedAttributeIsNotExposed() {
        List<String> attributes = getAttributes(User.class);
        Assert.assertFalse(attributes.contains("reversedPassword"));
    }

    @Test
    public void testDetectCascadeRelations() {
        Assert.assertFalse(cascadeDeletes(FunWithPermissions.class, "relation1"));
        Assert.assertFalse(cascadeDeletes(FunWithPermissions.class, "relation2"));
        Assert.assertTrue(cascadeDeletes(FunWithPermissions.class, "relation3"));
        Assert.assertFalse(cascadeDeletes(FunWithPermissions.class, "relation4"));
        Assert.assertFalse(cascadeDeletes(FunWithPermissions.class, "relation5"));
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

        Assert.assertNull(getIdType(NoId.class),
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

        Assert.assertNull(getType(FieldAnnotations.class, "missingField"),
                "getId returns null if attribute is missing"
        );

        Assert.assertEquals(getType(FieldAnnotations.class, "parent"), FieldAnnotations.class,
                "getType return the type of a private field relationship");

        Assert.assertEquals(getType(FieldAnnotations.class, "children"), Set.class,
                "getType return the type of a private field relationship");

        Assert.assertEquals(getType(Parent.class, "children"), Set.class,
            "getType returns the type of relationship fields");

        Assert.assertEquals(getType(Friend.class, "name"), String.class,
                "getType returns the type of attribute when defined in a super class");

        Assert.assertEquals(getType(Employee.class, "boss"), Manager.class,
            "getType returns the correct generic type of a to-one relationship");

        Assert.assertEquals(getType(Manager.class, "minions"), Set.class,
            "getType returns the correct generic type of a to-many relationship");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testGetTypUnknownEntityException() {
        getType(Object.class, "id");
    }

    @Test
    public void testNoExcludedFieldsReturned() {
        List<String> attrs = getAttributes(Child.class);
        List<String> rels = getRelationships(Child.class);
        Assert.assertTrue(!attrs.contains("excludedEntity") && !attrs.contains("excludedRelationship")
            && !attrs.contains("excludedEntityList"));
        Assert.assertTrue(!rels.contains("excludedEntity") && !rels.contains("excludedRelationship")
            && !rels.contains("excludedEntityList"));
    }

    @MappedInterface
    public interface SuitableInterface { }

    public interface BadInterface { }

    @Test
    public void testMappedInterface() {
        Assert.assertEquals(getEntityBinding(SuitableInterface.class), EntityBinding.EMPTY_BINDING);
    }

    @Test(expectedExceptions = java.lang.IllegalArgumentException.class)
    public void testBadInterface() {
        getEntityBinding(BadInterface.class);
    }
}
