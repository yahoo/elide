/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.dictionary;

import static com.yahoo.elide.annotation.LifeCycleHookBinding.Operation.UPDATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.yahoo.elide.annotation.ComputedAttribute;
import com.yahoo.elide.annotation.Exclude;
import com.yahoo.elide.annotation.FilterExpressionPath;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.LifeCycleHookBinding;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.SecurityCheck;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.exceptions.InvalidAttributeException;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.lifecycle.LifeCycleHook;
import com.yahoo.elide.core.security.checks.FilterExpressionCheck;
import com.yahoo.elide.core.security.checks.UserCheck;
import com.yahoo.elide.core.security.checks.prefab.Collections.AppendOnly;
import com.yahoo.elide.core.security.checks.prefab.Collections.RemoveOnly;
import com.yahoo.elide.core.security.checks.prefab.Role;
import com.yahoo.elide.core.type.AccessibleObject;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.core.utils.DefaultClassScanner;
import com.yahoo.elide.core.utils.coerce.CoerceUtil;
import com.yahoo.elide.core.utils.coerce.converters.ISO8601DateSerde;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import example.Address;
import example.Author;
import example.Book;
import example.Child;
import example.CoerceBean;
import example.Editor;
import example.FieldAnnotations;
import example.FunWithPermissions;
import example.GeoLocation;
import example.Job;
import example.Left;
import example.Parent;
import example.Price;
import example.Publisher;
import example.Right;
import example.StringId;
import example.User;
import example.models.generics.Employee;
import example.models.generics.Manager;
import example.models.packageinfo.ExcludedPackageLevel;
import example.models.packageinfo.IncludedPackageLevel;
import example.models.packageinfo.excluded.ExcludedSubPackage;
import example.models.packageinfo.included.ExcludedBySuperClass;
import example.models.packageinfo.included.IncludedSubPackage;
import example.models.versioned.BookV2;
import example.nontransferable.NoTransferBiDirectional;
import example.nontransferable.StrictNoTransfer;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AccessType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Transient;

import java.lang.annotation.Annotation;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

public class EntityDictionaryTest extends EntityDictionary {

    //Test class to validate inheritance logic
    @Include(name = "friend")
    private class Friend extends Child {
    }

    public EntityDictionaryTest() {
        super(
                Collections.emptyMap(), //checks
                Collections.emptyMap(), //role Checks
                DEFAULT_INJECTOR,
                CoerceUtil::lookup,
                Collections.emptySet(),
                DefaultClassScanner.getInstance()
        );
        init();
    }

    private void init() {
        bindEntity(FunWithPermissions.class);
        bindEntity(Parent.class);
        bindEntity(Child.class);
        bindEntity(Publisher.class);
        bindEntity(User.class);
        bindEntity(Left.class);
        bindEntity(Right.class);
        bindEntity(StringId.class);
        bindEntity(Friend.class);
        bindEntity(FieldAnnotations.class);
        bindEntity(Manager.class);
        bindEntity(Employee.class);
        bindEntity(Job.class);
        bindEntity(NoId.class);
        bindEntity(BookV2.class);
        bindEntity(Book.class);
        bindEntity(Author.class);
        bindEntity(Editor.class);
        bindEntity(IncludedPackageLevel.class);
        bindEntity(IncludedSubPackage.class);
        bindEntity(ExcludedPackageLevel.class);
        bindEntity(ExcludedSubPackage.class);
        bindEntity(ExcludedBySuperClass.class);
        bindEntity(StrictNoTransfer.class);
        bindEntity(NoTransferBiDirectional.class);

        checkNames.forcePut("user has all access", Role.ALL.class);
    }

    @Test
    public void testGetInjector() {
        assertNotNull(getInjector());
    }

    @Test
    public void testSetId() {
        Parent parent = new Parent();
        setId(parent, "123");
        assertEquals(parent.getId(), 123);
    }

    @Test
    public void testFindCheckByExpression() {
        assertEquals("user has all access", getCheckIdentifier(Role.ALL.class));
        assertEquals("Prefab.Role.None", getCheckIdentifier(Role.NONE.class));
        assertEquals("Prefab.Collections.AppendOnly", getCheckIdentifier(AppendOnly.class));
        assertEquals("Prefab.Collections.RemoveOnly", getCheckIdentifier(RemoveOnly.class));
    }

    @SecurityCheck("User is Admin")
    public static class Bar extends UserCheck {

        @Override
        public boolean ok(com.yahoo.elide.core.security.User user) {
            return false;
        }
    }

    @Test
    public void testBindingNoExcludeSet() {

        EntityDictionary testDictionary = EntityDictionary.builder().build();
        testDictionary.bindEntity(Employee.class);
        // Finds the Binding
        assertNotNull(testDictionary.entityBindings.get(ClassType.of(Employee.class)));
    }

    @Test
    public void testCheckScan() {

        EntityDictionary testDictionary = EntityDictionary.builder().build();
        testDictionary.scanForSecurityChecks();

        assertEquals("User is Admin", testDictionary.getCheckIdentifier(Bar.class));
    }

    @SecurityCheck("Filter Expression Injection Test")
    public static class Foo extends FilterExpressionCheck {

        @Inject
        Long testLong;

        @Override
        public FilterExpression getFilterExpression(Type entityClass,
                                                    com.yahoo.elide.core.security.RequestScope requestScope) {
            assertEquals(testLong, 123L);
            return null;
        }
    }

    @Test
    public void testCheckInjection() {
        EntityDictionary testDictionary = EntityDictionary.builder()
                .injector(new Injector() {
                    @Override
                    public void inject(Object entity) {
                        if (entity instanceof Foo) {
                            ((Foo) entity).testLong = 123L;
                        }
                    }
                })
                .build();
        testDictionary.scanForSecurityChecks();

        assertEquals("Filter Expression Injection Test", testDictionary.getCheckIdentifier(Foo.class));
    }

    @Test
    public void testSerdeId() {

        @Include(rootLevel = false)
        class EntityWithDateId {
            @Id
            private Date id;
        }

        EntityDictionary testDictionary = new EntityDictionary(
                new HashMap<>(),
                null,
                DEFAULT_INJECTOR,
                unused -> new ISO8601DateSerde(),
                Collections.emptySet(),
                DefaultClassScanner.getInstance());

        testDictionary.bindEntity(EntityWithDateId.class);

        EntityWithDateId testModel = new EntityWithDateId();
        testModel.id = new Date(0);
        assertEquals("1970-01-01T00:00Z", testDictionary.getId(testModel));
    }

    @Test
    public void testGetAttributeOrRelationAnnotation() {
        String[] fields = {"field1", "field2", "field3", "relation1", "relation2"};
        Annotation annotation;
        for (String field : fields) {
            annotation = getAttributeOrRelationAnnotation(ClassType.of(FunWithPermissions.class), ReadPermission.class, field);
            assertTrue(annotation instanceof ReadPermission, "Every field should return a ReadPermission annotation");
        }
    }

    @Test
    public void testHasAnnotation() {
        assertTrue(hasAnnotation(ClassType.of(Book.class), Include.class));
        assertTrue(hasAnnotation(ClassType.of(Book.class), Transient.class));
        assertFalse(hasAnnotation(ClassType.of(Book.class), Exclude.class));
    }

    @Test
    public void testBindingTriggerPriorToBindingEntityClass1() {
        @Entity
        @Include(rootLevel = false)
        class Foo2 {
            @Id
            private long id;

            private int bar;
        }

        LifeCycleHook<Foo2> trigger = mock(LifeCycleHook.class);

        bindTrigger(Foo2.class, "bar", UPDATE, LifeCycleHookBinding.TransactionPhase.PRESECURITY, trigger);
        assertEquals(1, getAllExposedFields(ClassType.of(Foo2.class)).size());
    }

    @Test
    public void testBindingTriggerPriorToBindingEntityClass2() {
        @Entity
        @Include(rootLevel = false)
        class Foo3 {
            @Id
            private long id;

            private int bar;
        }

        LifeCycleHook<Foo3> trigger = mock(LifeCycleHook.class);

        bindTrigger(Foo3.class, UPDATE, LifeCycleHookBinding.TransactionPhase.PRESECURITY, trigger, true);
        assertEquals(1, getAllExposedFields(ClassType.of(Foo3.class)).size());
    }

    @Test
    public void testBindingTriggerPriorToBindingEntityClass3() {
        @Entity
        @Include(rootLevel = false)
        class Foo4 {
            @Id
            private long id;

            private int bar;
        }

        LifeCycleHook<Foo4> trigger = mock(LifeCycleHook.class);

        bindTrigger(Foo4.class, UPDATE, LifeCycleHookBinding.TransactionPhase.PRESECURITY, trigger, false);
        assertEquals(1, getAllExposedFields(ClassType.of(Foo4.class)).size());
    }

    @Test
    public void testJPAFieldLevelAccess() {
        @Entity
        @Include(rootLevel = false)
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
                // NOOP
            }
        }
        bindEntity(FieldLevelTest.class);

        assertEquals(AccessType.FIELD, getAccessType(ClassType.of(FieldLevelTest.class)));

        List<String> fields = getAllExposedFields(ClassType.of(FieldLevelTest.class));
        assertEquals(3, fields.size());
        assertTrue(fields.contains("bar"));
        assertTrue(fields.contains("computedField"));
        assertTrue(fields.contains("computedProperty"));
    }

    @Test
    public void testJPAPropertyLevelAccess() {
        @Entity
        @Include(rootLevel = false)
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
        bindEntity(PropertyLevelTest.class);

        assertEquals(AccessType.PROPERTY, getAccessType(ClassType.of(PropertyLevelTest.class)));

        List<String> fields = getAllExposedFields(ClassType.of(PropertyLevelTest.class));
        assertEquals(2, fields.size());
        assertTrue(fields.contains("bar"));
        assertTrue(fields.contains("computedProperty"));
    }


    @Test
    public void testGetParameterizedType() {
        Type<?> type;

        FunWithPermissions fun = new FunWithPermissions();

        type = getParameterizedType(fun, "relation2");
        assertEquals(ClassType.of(Child.class), type, "A set of Child objects should return Child.class");

        type = getParameterizedType(fun, "relation3");
        assertEquals(ClassType.of(Child.class), type, "A Child object should return Child.class");

        assertEquals(
                ClassType.of(FieldAnnotations.class),
                getParameterizedType(ClassType.of(FieldAnnotations.class), "children"),
                "getParameterizedType return the type of a private field relationship");

        assertEquals(
                ClassType.of(Child.class),
                getParameterizedType(ClassType.of(Parent.class), "children"),
                "getParameterizedType returns the type of relationship fields");

        assertEquals(
                ClassType.of(Employee.class),
                getParameterizedType(ClassType.of(Manager.class), "minions"),
                "getParameterizedType returns the correct generic type of a to-many relationship");

        assertEquals(
                ClassType.of(Book.class),
                getParameterizedType(ClassType.of(Author.class), "products"),
                "getParameterizedType returns the correct targetEntity type of a to-many relationship");

        assertEquals(ClassType.of(Manager.class), getParameterizedType(ClassType.of(Employee.class), "boss"),
                "getParameterizedType returns the correct generic type of a to-one relationship");
    }

    @Test
    public void testIsGeneratedId() {
        @Include(rootLevel = false)
        class GeneratedIdModel {
            @Id
            @GeneratedValue
            private long id;

        }

        @Include(rootLevel = false)
        class NonGeneratedIdModel {
            @Id
            private long id;

        }
        bindEntity(GeneratedIdModel.class);
        bindEntity(NonGeneratedIdModel.class);

        assertTrue(isIdGenerated(ClassType.of(GeneratedIdModel.class)));
        assertFalse(isIdGenerated(ClassType.of(NonGeneratedIdModel.class)));
    }

    @Test
    public void testHiddenFields() {
        @Include
        class Model {
            @Id
            private long id;

            private String field1;
            private String field2;
        }

        bindEntity(Model.class, (field) -> field.getName().equals("field1"));

        Type<?> modelType = ClassType.of(Model.class);

        assertEquals(List.of("field2"), getAllExposedFields(modelType));

        EntityBinding binding = getEntityBinding(modelType);
        assertEquals(List.of("id", "field1", "field2"), binding.getAllFields().stream()
                .map(AccessibleObject::getName)
                .collect(Collectors.toList()));
    }

    @Test
    public void testGetInverseRelationshipOwningSide() {
        assertEquals(
                "parents",
                getRelationInverse(ClassType.of(Parent.class), "children"),
                "The inverse relationship of children should be parents");
    }

    @Test
    public void testGetInverseRelationshipOwnedSide() {
        assertEquals(
                "children",
                getRelationInverse(ClassType.of(Child.class), "parents"),
                "The inverse relationship of children should be parents");
    }

    @Test
    public void testComputedAttributeIsExposed() {
        List<String> attributes = getAttributes(ClassType.of(User.class));
        assertTrue(attributes.contains("password"));
    }

    @Test
    public void testExcludedAttributeIsNotExposed() {
        List<String> attributes = getAttributes(ClassType.of(User.class));
        assertFalse(attributes.contains("reversedPassword"));
    }

    @Test
    public void testDetectCascadeRelations() {
        assertFalse(cascadeDeletes(ClassType.of(FunWithPermissions.class), "relation1"));
        assertFalse(cascadeDeletes(ClassType.of(FunWithPermissions.class), "relation2"));
        assertTrue(cascadeDeletes(ClassType.of(FunWithPermissions.class), "relation3"));
        assertFalse(cascadeDeletes(ClassType.of(FunWithPermissions.class), "relation4"));
        assertFalse(cascadeDeletes(ClassType.of(FunWithPermissions.class), "relation5"));
    }

    @Test
    public void testGetIdAnnotations() throws Exception {

        Collection<Class> expectedAnnotationClasses = Arrays.asList(new Class[]{Id.class, GeneratedValue.class});
        Collection<Class> actualAnnotationsClasses = getIdAnnotations(new Parent()).stream()
                .map(Annotation::annotationType)
                .collect(Collectors.toList());

        assertEquals(actualAnnotationsClasses, expectedAnnotationClasses,
                "getIdAnnotations returns annotations on the ID field of the given class");
    }

    @Test
    public void testGetIdAnnotationsNoId() throws Exception {

        Collection<Annotation> expectedAnnotation = Collections.emptyList();
        Collection<Annotation> actualAnnotations = getIdAnnotations(new NoId());

        assertEquals(actualAnnotations, expectedAnnotation,
                "getIdAnnotations returns an empty collection if there is no ID field for given class");
    }

    @Include(rootLevel = false)
    class NoId {

    }

    @Test
    public void testGetIdAnnotationsSubClass() throws Exception {

        Collection<Class> expectedAnnotationClasses = Arrays.asList(new Class[]{Id.class, GeneratedValue.class});
        Collection<Class> actualAnnotationsClasses = getIdAnnotations(new Friend()).stream()
                .map(Annotation::annotationType)
                .collect(Collectors.toList());

        assertEquals(actualAnnotationsClasses, expectedAnnotationClasses,
                "getIdAnnotations returns annotations on the ID field when defined in a super class");
    }

    @Test
    public void testIsSharableTrue() throws Exception {
        assertTrue(isTransferable(ClassType.of(Right.class)));
        assertFalse(isStrictNonTransferable(ClassType.of(Right.class)));
    }

    @Test
    public void testIsSharableFalse() throws Exception {
        assertFalse(isTransferable(ClassType.of(Left.class)));
        assertFalse(isStrictNonTransferable(ClassType.of(Left.class)));
    }

    @Test
    public void testIsStrictNonTransferable() throws Exception {
        assertTrue(isStrictNonTransferable(ClassType.of(StrictNoTransfer.class)));
        assertFalse(isStrictNonTransferable(ClassType.of(NoTransferBiDirectional.class)));
    }

    @Test
    public void testGetIdType() throws Exception {
        assertEquals(getIdType(ClassType.of(Parent.class)), ClassType.of(long.class),
                "getIdType returns the type of the ID field of the given class");

        assertEquals(getIdType(ClassType.of(StringId.class)), ClassType.of(String.class),
                "getIdType returns the type of the ID field of the given class");

        assertNull(getIdType(ClassType.of(NoId.class)),
                "getIdType returns null if ID field is missing");

        assertEquals(getIdType(ClassType.of(Friend.class)), ClassType.of(long.class),
                "getIdType returns the type of the ID field when defined in a super class");
    }

    @Test
    public void testGetType() throws Exception {
        assertEquals(
                ClassType.of(Long.class),
                getType(ClassType.of(FieldAnnotations.class), "id"),
                "getType returns the type of the ID field of the given class");

        assertEquals(
                ClassType.of(long.class),
                getType(ClassType.of(FieldAnnotations.class), "publicField"),
                "getType returns the type of attribute when Column annotation is on a field");

        assertEquals(
                ClassType.of(Boolean.class),
                getType(ClassType.of(FieldAnnotations.class), "privateField"),
                "getType returns the type of attribute when Column annotation is on a getter");

        assertNull(getType(ClassType.of(FieldAnnotations.class), "missingField"),
                "getId returns null if attribute is missing"
        );

        assertEquals(
                ClassType.of(FieldAnnotations.class),
                getType(ClassType.of(FieldAnnotations.class), "parent"),
                "getType return the type of a private field relationship");

        assertEquals(
                ClassType.of(Set.class),
                getType(ClassType.of(FieldAnnotations.class), "children"),
                "getType return the type of a private field relationship");

        assertEquals(
                ClassType.of(Set.class),
                getType(ClassType.of(Parent.class), "children"),
                "getType returns the type of relationship fields");

        assertEquals(ClassType.of(String.class), getType(ClassType.of(Friend.class), "name"),
                "getType returns the type of attribute when defined in a super class");

        assertEquals(ClassType.of(Object.class), getType(ClassType.of(Employee.class), "boss"),
                "getType returns the correct generic type of a to-one relationship");

        assertEquals(ClassType.of(Set.class), getType(ClassType.of(Manager.class), "minions"),
                "getType returns the correct generic type of a to-many relationship");

        // ID is "id"
        assertEquals(ClassType.of(long.class), getType(ClassType.of(Parent.class), "id"),
                "getType returns the type of surrogate key");

        // ID is not "id"
        assertEquals(ClassType.of(Long.class), getType(ClassType.of(Job.class), "jobId"),
                "getType returns the type of surrogate key");
        assertEquals(ClassType.of(Long.class), getType(ClassType.of(Job.class), "id"),
                "getType returns the type of surrogate key");
        assertEquals(ClassType.of(String.class), getType(ClassType.of(StringId.class), "surrogateKey"),
                "getType returns the type of surrogate key");
        assertEquals(ClassType.of(String.class), getType(ClassType.of(StringId.class), "id"),
                "getType returns the type of surrogate key");

        // Test targetEntity on a method.
        assertEquals(ClassType.of(Collection.class), getType(ClassType.of(Author.class), "products"));
    }

    @Test
    public void testGetTypUnknownEntityException() {
        assertThrows(IllegalArgumentException.class, () -> getType(ClassType.of(Object.class), "id"));
    }

    @Test
    public void testNoExcludedFieldsReturned() {
        List<String> attrs = getAttributes(ClassType.of(Child.class));
        List<String> rels = getRelationships(ClassType.of(Child.class));
        assertTrue(!attrs.contains("excludedEntity") && !attrs.contains("excludedRelationship")
                && !attrs.contains("excludedEntityList"));
        assertTrue(!rels.contains("excludedEntity") && !rels.contains("excludedRelationship")
                && !rels.contains("excludedEntityList"));
    }

    public interface BadInterface {
    }

    @Test
    public void testBadInterface() {
        assertThrows(IllegalArgumentException.class, () -> getEntityBinding(ClassType.of(BadInterface.class)));
    }

    @Test
    public void testEntityInheritanceBinding() {
        @Entity
        @Include(rootLevel = false)
        class SuperclassBinding {
            @Id
            private long id;
        }

        class SubclassBinding extends SuperclassBinding {
        }

        class SubsubclassBinding extends SubclassBinding {
        }

        bindEntity(SuperclassBinding.class);
        bindEntity(SubclassBinding.class);
        bindEntity(SubsubclassBinding.class);

        assertEquals(ClassType.of(SuperclassBinding.class), getEntityBinding(ClassType.of(SubclassBinding.class)).entityClass);
        assertEquals(ClassType.of(SuperclassBinding.class), getEntityBinding(ClassType.of(SuperclassBinding.class)).entityClass);

        assertEquals(ClassType.of(SuperclassBinding.class), lookupEntityClass(ClassType.of(SuperclassBinding.class)));
        assertEquals(ClassType.of(SuperclassBinding.class), lookupEntityClass(ClassType.of(SubclassBinding.class)));
        assertEquals(ClassType.of(SuperclassBinding.class), lookupEntityClass(ClassType.of(SubsubclassBinding.class)));

        assertNull(getEntityClass("subclassBinding", NO_VERSION));
        assertEquals(ClassType.of(SuperclassBinding.class), getEntityClass("superclassBinding", NO_VERSION));

        assertEquals("superclassBinding", getJsonAliasFor(ClassType.of(SubclassBinding.class)));
        assertEquals("superclassBinding", getJsonAliasFor(ClassType.of(SuperclassBinding.class)));
    }

    @Test
    public void testEntityInheritanceBindingOverride() {
        @Entity
        @Include(rootLevel = false)
        class SuperclassBinding {
            @Id
            private long id;
        }

        class SubclassBinding extends SuperclassBinding {
        }

        @Entity
        @Include(rootLevel = false)
        class SubsubclassBinding extends SubclassBinding {
            @Id
            private long id;
        }

        bindEntity(SuperclassBinding.class);
        bindEntity(SubsubclassBinding.class);

        assertEquals(ClassType.of(SuperclassBinding.class), getEntityBinding(ClassType.of(SuperclassBinding.class)).entityClass);
        assertEquals(ClassType.of(SuperclassBinding.class), getEntityBinding(ClassType.of(SubclassBinding.class)).entityClass);
        assertEquals(ClassType.of(SubsubclassBinding.class), getEntityBinding(ClassType.of(SubsubclassBinding.class)).entityClass);

        assertEquals(ClassType.of(SuperclassBinding.class), lookupEntityClass(ClassType.of(SuperclassBinding.class)));
        assertEquals(ClassType.of(SuperclassBinding.class), lookupEntityClass(ClassType.of(SubclassBinding.class)));
        assertEquals(ClassType.of(SubsubclassBinding.class), lookupEntityClass(ClassType.of(SubsubclassBinding.class)));
    }

    @Test
    public void testMissingEntityBinding() {
        @Entity
        class SuperclassBinding {
            @Id
            private long id;
        }

        bindEntity(SuperclassBinding.class);

        assertNull(getEntityBinding(ClassType.of(SuperclassBinding.class)).entityClass);
        assertEquals(ClassType.of(SuperclassBinding.class), lookupEntityClass(ClassType.of(SuperclassBinding.class)));
    }

    @Test
    public void testNonEntityInheritanceBinding() {
        @Include(rootLevel = false)
        class SuperclassBinding {
            @Id
            private long id;
        }

        class SubclassBinding extends SuperclassBinding {
        }

        class SubsubclassBinding extends SubclassBinding {
        }

        bindEntity(SuperclassBinding.class);

        assertEquals(ClassType.of(SuperclassBinding.class), getEntityBinding(ClassType.of(SubclassBinding.class)).entityClass);
        assertEquals(ClassType.of(SuperclassBinding.class), getEntityBinding(ClassType.of(SuperclassBinding.class)).entityClass);

        assertEquals(ClassType.of(SuperclassBinding.class), lookupIncludeClass(ClassType.of(SuperclassBinding.class)));
        assertEquals(ClassType.of(SuperclassBinding.class), lookupIncludeClass(ClassType.of(SubclassBinding.class)));
        assertEquals(ClassType.of(SuperclassBinding.class), lookupIncludeClass(ClassType.of(SubsubclassBinding.class)));
    }

    @Test
    public void testNonEntityInheritanceBindingOverride() {
        @Include(rootLevel = false)
        class SuperclassBinding {
            @Id
            private long id;
        }

        class SubclassBinding extends SuperclassBinding {
        }

        @Include(rootLevel = false)
        class SubsubclassBinding extends SubclassBinding {
            @Id
            private long id;
        }

        bindEntity(SuperclassBinding.class);
        bindEntity(SubsubclassBinding.class);

        assertEquals(ClassType.of(SuperclassBinding.class), getEntityBinding(ClassType.of(SubclassBinding.class)).entityClass);
        assertEquals(ClassType.of(SuperclassBinding.class), getEntityBinding(ClassType.of(SuperclassBinding.class)).entityClass);
        assertEquals(ClassType.of(SubsubclassBinding.class), getEntityBinding(ClassType.of(SubsubclassBinding.class)).entityClass);

        assertEquals(ClassType.of(SuperclassBinding.class), lookupIncludeClass(ClassType.of(SuperclassBinding.class)));
        assertEquals(ClassType.of(SuperclassBinding.class), lookupIncludeClass(ClassType.of(SubclassBinding.class)));
        assertEquals(ClassType.of(SubsubclassBinding.class), lookupIncludeClass(ClassType.of(SubsubclassBinding.class)));
    }

    @Test
    public void testNonEntityInheritanceBindingExclusion() {
        @Include(rootLevel = false)
        class SuperclassBinding {
            @Id
            private long id;
        }

        class SubclassBinding extends SuperclassBinding {
        }

        @Exclude
        class SubsubclassBinding extends SubclassBinding {
        }

        bindEntity(SuperclassBinding.class);
        bindEntity(SubsubclassBinding.class);

        assertEquals(ClassType.of(SuperclassBinding.class), getEntityBinding(ClassType.of(SubclassBinding.class)).entityClass);
        assertEquals(ClassType.of(SuperclassBinding.class), getEntityBinding(ClassType.of(SuperclassBinding.class)).entityClass);
        assertThrows(IllegalArgumentException.class, () ->
            getEntityBinding(ClassType.of(SubsubclassBinding.class))
        );

        assertEquals(ClassType.of(SuperclassBinding.class), lookupIncludeClass(ClassType.of(SuperclassBinding.class)));
        assertEquals(ClassType.of(SuperclassBinding.class), lookupIncludeClass(ClassType.of(SubclassBinding.class)));
        assertNull(lookupIncludeClass(ClassType.of(SubsubclassBinding.class)));
    }

    @Test
    public void testGetFirstAnnotation() {
        @Exclude
        class Foo {
        }

        @Include(rootLevel = false)
        class Bar extends Foo {

        }

        class Baz extends Bar {

        }

        Annotation first = getFirstAnnotation(ClassType.of(Baz.class), Arrays.asList(Exclude.class, Include.class));
        assertTrue(first instanceof Include);
    }

    @Test
    public void testGetFirstAnnotationConflict() {
        @Exclude
        @Include(rootLevel = false)
        class Foo {
        }

        Annotation first = getFirstAnnotation(ClassType.of(Foo.class), Arrays.asList(Exclude.class, Include.class));
        assertTrue(first instanceof Exclude);
    }

    @Test
    public void testAnnotationNoSuchMethod() {
        bindEntity(Book.class);
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> getMethodAnnotation(ClassType.of(Book.class), "NoMethod", FilterExpressionPath.class));
        assertTrue(e.getCause() instanceof NoSuchMethodException, e.toString());
    }

    @Test
    public void testAnnotationFilterExpressionPath() {
        bindEntity(Book.class);
        FilterExpressionPath fe =
                getMethodAnnotation(ClassType.of(Book.class), "getEditor", FilterExpressionPath.class);
        assertEquals("publisher.editor", fe.value());
    }

    @Test
    public void testBadLookupEntityClass() {
        assertThrows(IllegalArgumentException.class, () -> lookupEntityClass(null));
        assertThrows(IllegalArgumentException.class, () -> lookupEntityClass(ClassType.of(Object.class)));
    }

    @Test
    public void testFieldIsInjected() {
        EntityDictionary testDictionary = EntityDictionary.builder().build();

        @Include(rootLevel = false)
        class FieldInject {
            @Inject
            private String field;
        }

        testDictionary.bindEntity(FieldInject.class);

        assertTrue(testDictionary.getEntityBinding(ClassType.of(FieldInject.class)).isInjected());
    }

    @Test
    public void testInheritedFieldIsInjected() {
        EntityDictionary testDictionary = EntityDictionary.builder().build();
        class BaseClass {
            @Inject
            private String field;
        }

        @Include(rootLevel = false)
        class SubClass extends BaseClass {
            private String anotherField;
        }

        testDictionary.bindEntity(SubClass.class);

        assertTrue(testDictionary.getEntityBinding(ClassType.of(SubClass.class)).isInjected());
    }

    @Test
    public void testMethodIsInjected() {
        EntityDictionary testDictionary = EntityDictionary.builder().build();

        @Include(rootLevel = false)
        class MethodInject {
            @Inject
            private void setField(String field) {
                //NOOP
            }
        }

        testDictionary.bindEntity(MethodInject.class);

        assertTrue(testDictionary.getEntityBinding(ClassType.of(MethodInject.class)).isInjected());
    }

    @Test
    public void testInhertedMethodIsInjected() {
        EntityDictionary testDictionary = EntityDictionary.builder().build();
        class BaseClass {
            @Inject
            private void setField(String field) {
                //NOOP
            }
        }

        @Include(rootLevel = false)
        class SubClass extends BaseClass {
            private String anotherField;
        }

        testDictionary.bindEntity(SubClass.class);

        assertTrue(testDictionary.getEntityBinding(ClassType.of(SubClass.class)).isInjected());
    }

    @Test
    public void testConstructorIsInjected() {
        EntityDictionary testDictionary = EntityDictionary.builder().build();

        @Include(rootLevel = false)
        class ConstructorInject {
            @Inject
            public ConstructorInject(String field) {
                //NOOP
            }
        }

        testDictionary.bindEntity(ConstructorInject.class);

        assertTrue(testDictionary.getEntityBinding(ClassType.of(ConstructorInject.class)).isInjected());
    }

    @Test
    public void testFieldLookup() throws Exception {
        Book book = new Book() {
            @Override
            public String toString() {
                return "ProxyBook";
            }
        };
        book.setId(1234L);
        Author author = new Author();

        initializeEntity(book);

        RequestScope scope = mock(RequestScope.class);

        assertEquals("Book", getSimpleName(ClassType.of(Book.class)));
        assertEquals("getEditor",
                findMethod(ClassType.of(Book.class), "getEditor").getName());
        assertEquals("setGenre",
                findMethod(ClassType.of(Book.class), "setGenre", ClassType.of(String.class)).getName());

        setValue(book, "genre", "Elide");
        assertEquals("Elide", getValue(book, "genre", scope));
        setValue(book, "authors", ImmutableSet.of(author));
        assertEquals(ImmutableSet.of(author), getValue(book, "authors", scope));

        assertThrows(InvalidAttributeException.class, () -> setValue(book, "badfield", "Elide"));
        assertEquals("1234", getId(book));
        assertTrue(isRoot(ClassType.of(Book.class)));

        assertEquals(ClassType.of(Book.class), lookupBoundClass(ClassType.of(Book.class)));
        assertNull(lookupBoundClass(ClassType.of(String.class)));
        // check proxy lookup
        assertNotEquals(ClassType.of(Book.class), book.getClass());
        assertEquals(ClassType.of(Book.class), lookupBoundClass(ClassType.of(Book.class)));

        assertFalse(isComputed(ClassType.of(Book.class), "genre"));
        assertTrue(isComputed(ClassType.of(Book.class), "editor"));
        assertTrue(isComputed(ClassType.of(Editor.class), "fullName"));
        assertFalse(isComputed(ClassType.of(Editor.class), "badfield"));

        assertEquals(
                ImmutableSet.of("awards", "genre", "language", "title"),
                getFieldsOfType(ClassType.of(Book.class), ClassType.of(String.class)));

        assertTrue(isRelation(ClassType.of(Book.class), "editor"));
        assertTrue(isAttribute(ClassType.of(Book.class), "title"));
        assertEquals(
                Arrays.asList(ClassType.of(Book.class), ClassType.of(Author.class),
                        ClassType.of(Editor.class), ClassType.of(Publisher.class)),
                walkEntityGraph(ImmutableSet.of(ClassType.of(Book.class)), x -> x));

        assertTrue(hasBinding(ClassType.of(Book.class)));
        assertFalse(hasBinding(ClassType.of(String.class)));
    }

    @Test
    public void testCoerce() throws Exception {
        bindEntity(CoerceBean.class);
        CoerceBean bean = new CoerceBean();

        setValue(bean, "string", 1L);
        setValue(bean, "list", ImmutableSet.of(true, false));
        setValue(bean, "map", ImmutableMap.of("one", "1", "two", "2"));
        setValue(bean, "set", ImmutableList.of(3L, 4L));

        assertEquals("1", bean.string);
        assertEquals(Arrays.asList(true, false), bean.list);
        assertEquals(
                ImmutableMap.of("one", 1L, "two", 2L),
                bean.map);
        assertEquals(ImmutableSet.of(3.0, 4.0), bean.set);
    }

    public static class TestCheck extends UserCheck {

        @Override
        public boolean ok(com.yahoo.elide.core.security.User user) {
            throw new IllegalStateException();
        }
    }

    @Test
    public void testCheckLookup() throws Exception {
        assertEquals(Role.ALL.class, this.getCheck("user has all access"));

        assertEquals(TestCheck.class, this.getCheck("com.yahoo.elide.core.dictionary.EntityDictionaryTest$TestCheck"));

        assertThrows(IllegalArgumentException.class, () -> this.getCheck("UnknownClassName"));

        assertThrows(IllegalArgumentException.class, () -> this.getCheck(String.class.getName()));
    }

    @Test
    public void testAttributeOrRelationAnnotationExists() {
        assertTrue(attributeOrRelationAnnotationExists(ClassType.of(Job.class), "jobId", Id.class));
        assertFalse(attributeOrRelationAnnotationExists(ClassType.of(Job.class), "title", OneToOne.class));
    }

    @Test
    public void testIsValidField() {
        assertTrue(isValidField(ClassType.of(Job.class), "title"));
        assertFalse(isValidField(ClassType.of(Job.class), "foo"));
    }

    @Test
    public void testBindingHiddenAttribute() {
        @Include
        class Book {
            @Id
            long id;

            String notHidden;

            String hidden;
        }

        bindEntity(Book.class, (field) -> field.getName().equals("hidden") ? true : false);

        assertFalse(isAttribute(ClassType.of(Book.class), "hidden"));
        assertTrue(isAttribute(ClassType.of(Book.class), "notHidden"));
    }

    @Test
    public void testGetBoundByVersion() {
        Set<Type<?>> models = getBoundClassesByVersion("1.0");
        assertEquals(3, models.size());  //Also includes com.yahoo.elide inner classes from this file.
        assertTrue(models.contains(ClassType.of(BookV2.class)));

        models = getBoundClassesByVersion(NO_VERSION);
        assertEquals(21, models.size());
    }

    @Test
    public void testGetEntityClassByVersion() {
        Type<?> model = getEntityClass("book", NO_VERSION);
        assertEquals(ClassType.of(Book.class), model);

        model = getEntityClass("book", "1.0");
        assertEquals(ClassType.of(BookV2.class), model);
    }

    @Test
    public void testGetModelVersion() {
        assertEquals("1.0", getModelVersion(ClassType.of(BookV2.class)));
        assertEquals(NO_VERSION, getModelVersion(ClassType.of(Book.class)));
    }

    @Test
    public void testIsComplexAttribute() {
        //Test complex attribute
        assertTrue(isComplexAttribute(ClassType.of(Author.class), "homeAddress"));
        //Test nested complex attribute
        assertTrue(isComplexAttribute(ClassType.of(Address.class), "geo"));
        //Test another complex attribute.
        assertTrue(isComplexAttribute(ClassType.of(Book.class), "price"));
        //Test Java Type with no default constructor.
        assertFalse(isComplexAttribute(ClassType.of(Price.class), "currency"));
        //Test embedded Elide model
        assertFalse(isComplexAttribute(ClassType.of(Price.class), "book"));
        //Test String
        assertFalse(isComplexAttribute(ClassType.of(Book.class), "title"));
        //Test primitive
        assertFalse(isComplexAttribute(ClassType.of(Book.class), "publishDate"));
        //Test primitive wrapper
        assertFalse(isComplexAttribute(ClassType.of(FieldAnnotations.class), "privateField"));
        //Test collection
        assertFalse(isComplexAttribute(ClassType.of(Book.class), "awards"));
        //Test relationship
        assertFalse(isComplexAttribute(ClassType.of(Book.class), "authors"));
        //Test enum
        assertFalse(isComplexAttribute(ClassType.of(Author.class), "authorType"));
        //Test collection of complex type
        assertFalse(isComplexAttribute(ClassType.of(Author.class), "vacationHomes"));
        //Test map of objects
        assertFalse(isComplexAttribute(ClassType.of(Author.class), "stuff"));
    }

    @Test
    public void testHasBinding() {
        assertTrue(hasBinding(ClassType.of(FunWithPermissions.class)));
        assertTrue(hasBinding(ClassType.of(Parent.class)));
        assertTrue(hasBinding(ClassType.of(Child.class)));
        assertTrue(hasBinding(ClassType.of(User.class)));
        assertTrue(hasBinding(ClassType.of(Left.class)));
        assertTrue(hasBinding(ClassType.of(Right.class)));
        assertTrue(hasBinding(ClassType.of(StringId.class)));
        assertTrue(hasBinding(ClassType.of(Friend.class)));
        assertTrue(hasBinding(ClassType.of(FieldAnnotations.class)));
        assertTrue(hasBinding(ClassType.of(Manager.class)));
        assertTrue(hasBinding(ClassType.of(Employee.class)));
        assertTrue(hasBinding(ClassType.of(Job.class)));
        assertTrue(hasBinding(ClassType.of(NoId.class)));
        assertTrue(hasBinding(ClassType.of(BookV2.class)));
        assertTrue(hasBinding(ClassType.of(Book.class)));
        assertTrue(hasBinding(ClassType.of((IncludedPackageLevel.class))));
        assertTrue(hasBinding(ClassType.of((IncludedSubPackage.class))));
        assertFalse(hasBinding(ClassType.of((ExcludedPackageLevel.class))));
        assertFalse(hasBinding(ClassType.of((ExcludedSubPackage.class))));
        assertFalse(hasBinding(ClassType.of((ExcludedBySuperClass.class))));

        //Test bindings for complex attribute types
        assertTrue(hasBinding(ClassType.of(Address.class)));
        assertTrue(hasBinding(ClassType.of(GeoLocation.class)));
        assertFalse(hasBinding(ClassType.of(String.class)));
        assertFalse(hasBinding(ClassType.of(Author.AuthorType.class)));
        assertFalse(hasBinding(ClassType.of(Boolean.class)));
        assertFalse(hasBinding(ClassType.of(Date.class)));
        assertFalse(hasBinding(ClassType.of(Map.class)));
        assertFalse(hasBinding(ClassType.of(BigDecimal.class)));
    }

    @Test
    public void testEntityPrefix() {
        assertEquals("example_includedPackageLevel",
                getJsonAliasFor(ClassType.of(IncludedPackageLevel.class)));
    }

    @Test
    public void testEntityDescription() {
        assertEquals("A book publisher", EntityDictionary.getEntityDescription(ClassType.of(Publisher.class)));
        assertNull(EntityDictionary.getEntityDescription(ClassType.of(Book.class)));
    }
}
