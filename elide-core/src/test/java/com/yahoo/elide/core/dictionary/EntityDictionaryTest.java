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
import com.yahoo.elide.annotation.MappedInterface;
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
import com.yahoo.elide.core.utils.coerce.converters.ISO8601DateSerde;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import example.Author;
import example.Book;
import example.Child;
import example.Editor;
import example.FieldAnnotations;
import example.FunWithPermissions;
import example.Job;
import example.Left;
import example.Parent;
import example.Right;
import example.StringId;
import example.User;
import example.models.generics.Employee;
import example.models.generics.Manager;
import example.models.versioned.BookV2;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
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
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

public class EntityDictionaryTest extends EntityDictionary {

    //Test class to validate inheritance logic
    @Include(type = "friend")
    private class Friend extends Child {
    }

    public EntityDictionaryTest() {
        super(Collections.EMPTY_MAP, mock(Injector.class));
        init();
    }

    private void init() {
        bindEntity(FunWithPermissions.class);
        bindEntity(Parent.class);
        bindEntity(Child.class);
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
    public class Bar extends UserCheck {

        @Override
        public boolean ok(com.yahoo.elide.core.security.User user) {
            return false;
        }
    }

    @Test
    public void testCheckScan() {

        EntityDictionary testDictionary = new EntityDictionary(new HashMap<>());
        testDictionary.scanForSecurityChecks();

        assertEquals("User is Admin", testDictionary.getCheckIdentifier(Bar.class));
    }

    @SecurityCheck("Filter Expression Injection Test")
    public class Foo extends FilterExpressionCheck {

        @Inject
        Long testLong;

        @Override
        public FilterExpression getFilterExpression(Class entityClass,
                                                    com.yahoo.elide.core.security.RequestScope requestScope) {
            assertEquals(testLong, 123L);
            return null;
        }
    }

    @Test
    public void testCheckInjection() {
        EntityDictionary testDictionary = new EntityDictionary(new HashMap<>(), new Injector() {
            @Override
            public void inject(Object entity) {
                ((Foo) entity).testLong = 123L;
            }
        });
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
                (unused) -> { return new ISO8601DateSerde(); });

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
            annotation = getAttributeOrRelationAnnotation(FunWithPermissions.class, ReadPermission.class, field);
            assertTrue(annotation instanceof ReadPermission, "Every field should return a ReadPermission annotation");
        }
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
        assertEquals(1, getAllFields(Foo2.class).size());
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
        assertEquals(1, getAllFields(Foo3.class).size());
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
        assertEquals(1, getAllFields(Foo4.class).size());
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

        assertEquals(AccessType.FIELD, getAccessType(FieldLevelTest.class));

        List<String> fields = getAllFields(FieldLevelTest.class);
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

        assertEquals(AccessType.PROPERTY, getAccessType(PropertyLevelTest.class));

        List<String> fields = getAllFields(PropertyLevelTest.class);
        assertEquals(2, fields.size());
        assertTrue(fields.contains("bar"));
        assertTrue(fields.contains("computedProperty"));
    }


    @Test
    public void testGetParameterizedType() {
        Class<?> type;

        FunWithPermissions fun = new FunWithPermissions();

        type = getParameterizedType(fun, "relation2");
        assertEquals(Child.class, type, "A set of Child objects should return Child.class");

        type = getParameterizedType(fun, "relation3");
        assertEquals(Child.class, type, "A Child object should return Child.class");

        assertEquals(
                FieldAnnotations.class,
                getParameterizedType(FieldAnnotations.class, "children"),
                "getParameterizedType return the type of a private field relationship");

        assertEquals(
                Child.class,
                getParameterizedType(Parent.class, "children"),
                "getParameterizedType returns the type of relationship fields");

        assertEquals(
                Employee.class,
                getParameterizedType(Manager.class, "minions"),
                "getParameterizedType returns the correct generic type of a to-many relationship");
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

        assertTrue(isIdGenerated(GeneratedIdModel.class));
        assertFalse(isIdGenerated(NonGeneratedIdModel.class));
    }

    @Test
    public void testGetInverseRelationshipOwningSide() {
        assertEquals(
                "parents",
                getRelationInverse(Parent.class, "children"),
                "The inverse relationship of children should be parents");
    }

    @Test
    public void testGetInverseRelationshipOwnedSide() {
        assertEquals(
                "children",
                getRelationInverse(Child.class, "parents"),
                "The inverse relationship of children should be parents");
    }

    @Test
    public void testComputedAttributeIsExposed() {
        List<String> attributes = getAttributes(User.class);
        assertTrue(attributes.contains("password"));
    }

    @Test
    public void testExcludedAttributeIsNotExposed() {
        List<String> attributes = getAttributes(User.class);
        assertFalse(attributes.contains("reversedPassword"));
    }

    @Test
    public void testDetectCascadeRelations() {
        assertFalse(cascadeDeletes(FunWithPermissions.class, "relation1"));
        assertFalse(cascadeDeletes(FunWithPermissions.class, "relation2"));
        assertTrue(cascadeDeletes(FunWithPermissions.class, "relation3"));
        assertFalse(cascadeDeletes(FunWithPermissions.class, "relation4"));
        assertFalse(cascadeDeletes(FunWithPermissions.class, "relation5"));
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
        assertTrue(isTransferable(Right.class));
    }

    @Test
    public void testIsSharableFalse() throws Exception {
        assertFalse(isTransferable(Left.class));
    }

    @Test
    public void testGetIdType() throws Exception {
        assertEquals(getIdType(Parent.class), long.class,
                "getIdType returns the type of the ID field of the given class");

        assertEquals(getIdType(StringId.class), String.class,
                "getIdType returns the type of the ID field of the given class");

        assertNull(getIdType(NoId.class),
                "getIdType returns null if ID field is missing");

        assertEquals(getIdType(Friend.class), long.class,
                "getIdType returns the type of the ID field when defined in a super class");
    }

    @Test
    public void testGetType() throws Exception {
        assertEquals(
                Long.class,
                getType(FieldAnnotations.class, "id"),
                "getType returns the type of the ID field of the given class");

        assertEquals(
                long.class,
                getType(FieldAnnotations.class, "publicField"),
                "getType returns the type of attribute when Column annotation is on a field");

        assertEquals(
                Boolean.class,
                getType(FieldAnnotations.class, "privateField"),
                "getType returns the type of attribute when Column annotation is on a getter");

        assertNull(getType(FieldAnnotations.class, "missingField"),
                "getId returns null if attribute is missing"
        );

        assertEquals(
                FieldAnnotations.class,
                getType(FieldAnnotations.class, "parent"),
                "getType return the type of a private field relationship");

        assertEquals(
                Set.class,
                getType(FieldAnnotations.class, "children"),
                "getType return the type of a private field relationship");

        assertEquals(
                Set.class,
                getType(Parent.class, "children"),
                "getType returns the type of relationship fields");

        assertEquals(String.class, getType(Friend.class, "name"),
                "getType returns the type of attribute when defined in a super class");

        assertEquals(Manager.class, getType(Employee.class, "boss"),
                "getType returns the correct generic type of a to-one relationship");

        assertEquals(Set.class, getType(Manager.class, "minions"),
                "getType returns the correct generic type of a to-many relationship");

        // ID is "id"
        assertEquals(long.class, getType(Parent.class, "id"),
                "getType returns the type of surrogate key");

        // ID is not "id"
        assertEquals(Long.class, getType(Job.class, "jobId"),
                "getType returns the type of surrogate key");
        assertEquals(Long.class, getType(Job.class, "id"),
                "getType returns the type of surrogate key");
        assertEquals(String.class, getType(StringId.class, "surrogateKey"),
                "getType returns the type of surrogate key");
        assertEquals(String.class, getType(StringId.class, "id"),
                "getType returns the type of surrogate key");
    }

    @Test
    public void testGetTypUnknownEntityException() {
        assertThrows(IllegalArgumentException.class, () -> getType(Object.class, "id"));
    }

    @Test
    public void testNoExcludedFieldsReturned() {
        List<String> attrs = getAttributes(Child.class);
        List<String> rels = getRelationships(Child.class);
        assertTrue(!attrs.contains("excludedEntity") && !attrs.contains("excludedRelationship")
                && !attrs.contains("excludedEntityList"));
        assertTrue(!rels.contains("excludedEntity") && !rels.contains("excludedRelationship")
                && !rels.contains("excludedEntityList"));
    }

    @MappedInterface
    public interface SuitableInterface {
    }

    public interface BadInterface {
    }

    @Test
    public void testMappedInterface() {
        assertEquals(EntityBinding.EMPTY_BINDING, getEntityBinding(SuitableInterface.class));
    }

    @Test
    public void testBadInterface() {
        assertThrows(IllegalArgumentException.class, () -> getEntityBinding(BadInterface.class));
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

        assertEquals(SuperclassBinding.class, getEntityBinding(SubclassBinding.class).entityClass);
        assertEquals(SuperclassBinding.class, getEntityBinding(SuperclassBinding.class).entityClass);

        assertEquals(SuperclassBinding.class, lookupEntityClass(SuperclassBinding.class));
        assertEquals(SuperclassBinding.class, lookupEntityClass(SubclassBinding.class));
        assertEquals(SuperclassBinding.class, lookupEntityClass(SubsubclassBinding.class));

        assertNull(getEntityClass("subclassBinding", NO_VERSION));
        assertEquals(SuperclassBinding.class, getEntityClass("superclassBinding", NO_VERSION));

        assertEquals("superclassBinding", getJsonAliasFor(SubclassBinding.class));
        assertEquals("superclassBinding", getJsonAliasFor(SuperclassBinding.class));
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

        assertEquals(SuperclassBinding.class, getEntityBinding(SuperclassBinding.class).entityClass);
        assertEquals(SuperclassBinding.class, getEntityBinding(SubclassBinding.class).entityClass);
        assertEquals(SubsubclassBinding.class, getEntityBinding(SubsubclassBinding.class).entityClass);

        assertEquals(SuperclassBinding.class, lookupEntityClass(SuperclassBinding.class));
        assertEquals(SuperclassBinding.class, lookupEntityClass(SubclassBinding.class));
        assertEquals(SubsubclassBinding.class, lookupEntityClass(SubsubclassBinding.class));
    }

    @Test
    public void testMissingEntityBinding() {
        @Entity
        class SuperclassBinding {
            @Id
            private long id;
        }

        bindEntity(SuperclassBinding.class);

        assertEquals(null, getEntityBinding(SuperclassBinding.class).entityClass);
        assertEquals(SuperclassBinding.class, lookupEntityClass(SuperclassBinding.class));
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

        assertEquals(SuperclassBinding.class, getEntityBinding(SubclassBinding.class).entityClass);
        assertEquals(SuperclassBinding.class, getEntityBinding(SuperclassBinding.class).entityClass);

        assertEquals(SuperclassBinding.class, lookupIncludeClass(SuperclassBinding.class));
        assertEquals(SuperclassBinding.class, lookupIncludeClass(SubclassBinding.class));
        assertEquals(SuperclassBinding.class, lookupIncludeClass(SubsubclassBinding.class));
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

        assertEquals(SuperclassBinding.class, getEntityBinding(SubclassBinding.class).entityClass);
        assertEquals(SuperclassBinding.class, getEntityBinding(SuperclassBinding.class).entityClass);
        assertEquals(SubsubclassBinding.class, getEntityBinding(SubsubclassBinding.class).entityClass);

        assertEquals(SuperclassBinding.class, lookupIncludeClass(SuperclassBinding.class));
        assertEquals(SuperclassBinding.class, lookupIncludeClass(SubclassBinding.class));
        assertEquals(SubsubclassBinding.class, lookupIncludeClass(SubsubclassBinding.class));
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

        assertEquals(SuperclassBinding.class, getEntityBinding(SubclassBinding.class).entityClass);
        assertEquals(SuperclassBinding.class, getEntityBinding(SuperclassBinding.class).entityClass);
        assertThrows(IllegalArgumentException.class, () -> {
            getEntityBinding(SubsubclassBinding.class);
        });

        assertEquals(SuperclassBinding.class, lookupIncludeClass(SuperclassBinding.class));
        assertEquals(SuperclassBinding.class, lookupIncludeClass(SubclassBinding.class));
        assertEquals(null, lookupIncludeClass(SubsubclassBinding.class));
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

        Annotation first = getFirstAnnotation(Baz.class, Arrays.asList(Exclude.class, Include.class));
        assertTrue(first instanceof Include);
    }

    @Test
    public void testGetFirstAnnotationConflict() {
        @Exclude
        @Include(rootLevel = false)
        class Foo {
        }

        Annotation first = getFirstAnnotation(Foo.class, Arrays.asList(Exclude.class, Include.class));
        assertTrue(first instanceof Exclude);
    }

    @Test
    public void testAnnotationNoSuchMethod() {
        bindEntity(Book.class);
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> getMethodAnnotation(Book.class, "NoMethod", FilterExpressionPath.class));
        assertTrue(e.getCause() instanceof NoSuchMethodException, e.toString());
    }

    @Test
    public void testAnnotationFilterExpressionPath() {
        bindEntity(Book.class);
        FilterExpressionPath fe =
                getMethodAnnotation(Book.class, "getEditor", FilterExpressionPath.class);
        assertEquals("publisher.editor", fe.value());
    }

    @Test
    public void testBadLookupEntityClass() {
        assertThrows(IllegalArgumentException.class, () -> lookupEntityClass(null));
        assertThrows(IllegalArgumentException.class, () -> lookupEntityClass(Object.class));
    }

    @Test
    public void testFieldIsInjected() {
        EntityDictionary testDictionary = new EntityDictionary(new HashMap<>());

        @Include(rootLevel = false)
        class FieldInject {
            @Inject
            private String field;
        }

        testDictionary.bindEntity(FieldInject.class);

        assertTrue(testDictionary.getEntityBinding(FieldInject.class).isInjected());
    }

    @Test
    public void testInheritedFieldIsInjected() {
        EntityDictionary testDictionary = new EntityDictionary(new HashMap<>());
        class BaseClass {
            @Inject
            private String field;
        }

        @Include(rootLevel = false)
        class SubClass extends BaseClass {
            private String anotherField;
        }

        testDictionary.bindEntity(SubClass.class);

        assertTrue(testDictionary.getEntityBinding(SubClass.class).isInjected());
    }

    @Test
    public void testMethodIsInjected() {
        EntityDictionary testDictionary = new EntityDictionary(new HashMap<>());

        @Include(rootLevel = false)
        class MethodInject {
            @Inject
            private void setField(String field) {
                //NOOP
            }
        }

        testDictionary.bindEntity(MethodInject.class);

        assertTrue(testDictionary.getEntityBinding(MethodInject.class).isInjected());
    }

    @Test
    public void testInhertedMethodIsInjected() {
        EntityDictionary testDictionary = new EntityDictionary(new HashMap<>());
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

        assertTrue(testDictionary.getEntityBinding(SubClass.class).isInjected());
    }

    @Test
    public void testConstructorIsInjected() {
        EntityDictionary testDictionary = new EntityDictionary(new HashMap<>());

        @Include(rootLevel = false)
        class ConstructorInject {
            @Inject
            public ConstructorInject(String field) {
                //NOOP
            }
        }

        testDictionary.bindEntity(ConstructorInject.class);

        assertTrue(testDictionary.getEntityBinding(ConstructorInject.class).isInjected());
    }

    @Test
    public void testFieldLookup() throws Exception {
        bindEntity(Book.class);
        bindEntity(Editor.class);
        bindEntity(Author.class);

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

        assertEquals("Book", getSimpleName(Book.class));
        assertEquals("getEditor",
                findMethod(Book.class, "getEditor").getName());
        assertEquals("setGenre",
                findMethod(Book.class, "setGenre", String.class).getName());

        setValue(book, "genre", "Elide");
        assertEquals("Elide", getValue(book, "genre", scope));
        setValue(book, "authors", ImmutableSet.of(author));
        assertEquals(ImmutableSet.of(author), getValue(book, "authors", scope));

        assertThrows(InvalidAttributeException.class, () -> setValue(book, "badfield", "Elide"));
        assertEquals("1234", getId(book));
        assertEquals(true, isRoot(Book.class));

        assertEquals(Book.class, lookupBoundClass(Book.class));
        assertNull(lookupBoundClass(String.class));
        // check proxy lookup
        assertNotEquals(Book.class, book.getClass());
        assertEquals(Book.class, lookupBoundClass(book.getClass()));

        assertFalse(isComputed(Book.class, "genre"));
        assertTrue(isComputed(Book.class, "editor"));
        assertTrue(isComputed(Editor.class, "fullName"));
        assertFalse(isComputed(Editor.class, "badfield"));

        assertEquals(
                ImmutableSet.of("awards", "genre", "language", "title"),
                getFieldsOfType(Book.class, String.class));

        assertTrue(isRelation(Book.class, "editor"));
        assertTrue(isAttribute(Book.class, "title"));
        assertEquals(
                Arrays.asList(Book.class, Author.class, Editor.class),
                walkEntityGraph(ImmutableSet.of(Book.class), x -> x));

        assertTrue(hasBinding(Book.class));
        assertFalse(hasBinding(String.class));
    }

    @Test
    public void testCoerce() throws Exception {
        @Entity
        @Include(rootLevel = false)
        class CoerceBean {
            public String string;
            public List<Boolean> list;
            public Map<String, Long> map;
            public Set<Double> set;
        }

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
        assertTrue(attributeOrRelationAnnotationExists(Job.class, "jobId", Id.class));
        assertFalse(attributeOrRelationAnnotationExists(Job.class, "title", OneToOne.class));
    }

    @Test
    public void testIsValidField() {
        assertTrue(isValidField(Job.class, "title"));
        assertFalse(isValidField(Job.class, "foo"));
    }

    @Test
    public void testGetBoundByVersion() {
        Set<Class<?>> models = getBoundClassesByVersion("1.0");
        assertEquals(3, models.size());  //Also includes com.yahoo.elide inner classes from this file.
        assertTrue(models.contains(BookV2.class));

        models = getBoundClassesByVersion(NO_VERSION);
        assertEquals(14, models.size());
    }

    @Test
    public void testGetEntityClassByVersion() {
        Class<?> model = getEntityClass("book", NO_VERSION);
        assertEquals(Book.class, model);

        model = getEntityClass("book", "1.0");
        assertEquals(BookV2.class, model);
    }

    @Test
    public void testGetModelVersion() {
        assertEquals("1.0", getModelVersion(BookV2.class));
        assertEquals(NO_VERSION, getModelVersion(Book.class));
    }

    @Test
    public void testHasBinding() {
        assertTrue(hasBinding(FunWithPermissions.class));
        assertTrue(hasBinding(Parent.class));
        assertTrue(hasBinding(Child.class));
        assertTrue(hasBinding(User.class));
        assertTrue(hasBinding(Left.class));
        assertTrue(hasBinding(Right.class));
        assertTrue(hasBinding(StringId.class));
        assertTrue(hasBinding(Friend.class));
        assertTrue(hasBinding(FieldAnnotations.class));
        assertTrue(hasBinding(Manager.class));
        assertTrue(hasBinding(Employee.class));
        assertTrue(hasBinding(Job.class));
        assertTrue(hasBinding(NoId.class));
        assertTrue(hasBinding(BookV2.class));
        assertTrue(hasBinding(Book.class));
    }
}
