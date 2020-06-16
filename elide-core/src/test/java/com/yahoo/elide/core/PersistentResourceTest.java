/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yahoo.elide.annotation.Audit;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.audit.LogMessage;
import com.yahoo.elide.audit.TestAuditLogger;
import com.yahoo.elide.core.exceptions.ForbiddenAccessException;
import com.yahoo.elide.core.exceptions.InvalidAttributeException;
import com.yahoo.elide.core.exceptions.InvalidObjectIdentifierException;
import com.yahoo.elide.core.exceptions.InvalidValueException;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.extensions.PatchRequestScope;
import com.yahoo.elide.jsonapi.models.Data;
import com.yahoo.elide.jsonapi.models.Relationship;
import com.yahoo.elide.jsonapi.models.Resource;
import com.yahoo.elide.jsonapi.models.ResourceIdentifier;
import com.yahoo.elide.security.ChangeSpec;
import com.yahoo.elide.security.User;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import example.Author;
import example.Child;
import example.Color;
import example.ComputedBean;
import example.FirstClassFields;
import example.FunWithPermissions;
import example.Invoice;
import example.Job;
import example.Left;
import example.LineItem;
import example.MapColorShape;
import example.NoDeleteEntity;
import example.NoReadEntity;
import example.NoShareEntity;
import example.NoUpdateEntity;
import example.Parent;
import example.Right;
import example.Shape;
import example.packageshareable.ContainerWithPackageShare;
import example.packageshareable.ShareableWithPackageShare;
import example.packageshareable.UnshareableWithEntityUnshare;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import nocreate.NoCreateEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;


/**
 * Test PersistentResource.
 */
public class PersistentResourceTest extends PersistenceResourceTestSetup {

    private final RequestScope goodUserScope;
    private final RequestScope badUserScope;

    public PersistentResourceTest() {
        goodUserScope = buildRequestScope(mock(DataStoreTransaction.class), new User(1));
        badUserScope = buildRequestScope(mock(DataStoreTransaction.class), new User(-1));
        reset(goodUserScope.getTransaction());
    }

    @Test
    public void testUpdateToOneRelationHookInAddRelation() {
        FunWithPermissions fun = new FunWithPermissions();
        Child child = newChild(1);

        User goodUser = new User(1);

        DataStoreTransaction tx = mock(DataStoreTransaction.class);

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "3", goodScope);
        PersistentResource<Child> childResource = new PersistentResource<>(child, null, "1", goodScope);
        funResource.addRelation("relation3", childResource);

        verify(tx, times(1)).updateToOneRelation(eq(tx), eq(fun), any(), any(), eq(goodScope));
        verify(tx, never()).updateToOneRelation(eq(tx), eq(child), any(), any(), eq(goodScope));
    }

    @Test
    public void testUpdateToOneRelationHookInUpdateRelation() {
        FunWithPermissions fun = new FunWithPermissions();
        Child child1 = newChild(1);
        Child child2 = newChild(2);
        fun.setRelation1(Sets.newHashSet(child1));
        User goodUser = new User(1);

        DataStoreTransaction tx = mock(DataStoreTransaction.class);

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "3", goodScope);
        PersistentResource<Child> child2Resource = new PersistentResource<>(child2, null, "1", goodScope);
        funResource.updateRelation("relation3", Sets.newHashSet(child2Resource));

        verify(tx, times(1)).updateToOneRelation(eq(tx), eq(fun), any(), any(), eq(goodScope));
        verify(tx, never()).updateToOneRelation(eq(tx), eq(child1), any(), any(), eq(goodScope));
        verify(tx, never()).updateToOneRelation(eq(tx), eq(child2), any(), any(), eq(goodScope));
    }

    @Test
    public void testUpdateToOneRelationHookInRemoveRelation() {
        FunWithPermissions fun = new FunWithPermissions();
        Child child = newChild(1);
        fun.setRelation3(child);
        User goodUser = new User(1);

        DataStoreTransaction tx = mock(DataStoreTransaction.class);

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "3", goodScope);
        PersistentResource<Child> childResource = new PersistentResource<>(child, null, "1", goodScope);
        funResource.removeRelation("relation3", childResource);

        verify(tx, times(1)).updateToOneRelation(eq(tx), eq(fun), any(), any(), eq(goodScope));
        verify(tx, never()).updateToOneRelation(eq(tx), eq(child), any(), any(), eq(goodScope));
    }

    @Test
    public void testUpdateToOneRelationHookInClearRelation() {
        FunWithPermissions fun = new FunWithPermissions();
        Child child1 = newChild(1);
        fun.setRelation3(child1);
        User goodUser = new User(1);

        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        when(tx.getRelation(any(), eq(fun), eq("relation3"), any(), any(), any(), any())).thenReturn(child1);

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "3", goodScope);
        funResource.clearRelation("relation3");

        verify(tx, times(1)).updateToOneRelation(eq(tx), eq(fun), any(), any(), eq(goodScope));
        verify(tx, never()).updateToOneRelation(eq(tx), eq(child1), any(), any(), eq(goodScope));
    }

    @Test
    public void testUpdateToManyRelationHookInAddRelationBidirection() {
        Parent parent = new Parent();
        Child child = newChild(1);

        User goodUser = new User(1);

        DataStoreTransaction tx = mock(DataStoreTransaction.class);

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<Parent> parentResource = new PersistentResource<>(parent, null, "3", goodScope);
        PersistentResource<Child> childResource = new PersistentResource<>(child, null, "1", goodScope);
        parentResource.addRelation("children", childResource);
        verify(tx, times(1)).updateToManyRelation(eq(tx), eq(parent),
                any(), any(), any(), eq(goodScope));
        verify(tx, times(1)).updateToManyRelation(eq(tx), eq(child),
                any(), any(), any(), eq(goodScope));
    }

    @Test
    public void testUpdateToManyRelationHookInRemoveRelationBidirection() {
        Parent parent = new Parent();
        Child child = newChild(1);
        parent.setChildren(Sets.newHashSet(child));
        child.setParents(Sets.newHashSet(parent));
        User goodUser = new User(1);

        DataStoreTransaction tx = mock(DataStoreTransaction.class);

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<Parent> parentResource = new PersistentResource<>(parent, null, "3", goodScope);
        PersistentResource<Child> childResource = new PersistentResource<>(child, null, "1", goodScope);
        parentResource.removeRelation("children", childResource);

        verify(tx, times(1)).updateToManyRelation(eq(tx), eq(parent),
                any(), any(), any(), eq(goodScope));
        verify(tx, times(1)).updateToManyRelation(eq(tx), eq(child),
                any(), any(), any(), eq(goodScope));
    }

    @Test
    public void testUpdateToManyRelationHookInClearRelationBidirection() {
        Parent parent = new Parent();
        Child child1 = newChild(1);
        Child child2 = newChild(2);
        Set<Child> children = Sets.newHashSet(child1, child2);
        parent.setChildren(children);
        child1.setParents(Sets.newHashSet(parent));
        child2.setParents(Sets.newHashSet(parent));
        User goodUser = new User(1);

        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        when(tx.getRelation(any(), eq(parent), eq("children"), any(), any(), any(), any())).thenReturn(children);

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<Parent> parentResource = new PersistentResource<>(parent, null, "3", goodScope);
        parentResource.clearRelation("children");

        verify(tx, times(1)).updateToManyRelation(eq(tx), eq(parent),
                any(), any(), any(), eq(goodScope));
        verify(tx, times(1)).updateToManyRelation(eq(tx), eq(child1),
                any(), any(), any(), eq(goodScope));
        verify(tx, times(1)).updateToManyRelation(eq(tx), eq(child2),
                any(), any(), any(), eq(goodScope));
    }

    @Test
    public void testUpdateToManyRelationHookInUpdateRelationBidirection() {
        Parent parent = new Parent();
        Child child1 = newChild(1);
        Child child2 = newChild(2);
        Child child3 = newChild(3);
        Set<Child> children = Sets.newHashSet(child1, child2);
        parent.setChildren(children);
        child1.setParents(Sets.newHashSet(parent));
        child2.setParents(Sets.newHashSet(parent));
        User goodUser = new User(1);

        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        when(tx.getRelation(any(), eq(parent), eq("children"), any(), any(), any(), any())).thenReturn(children);

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<Parent> parentResource = new PersistentResource<>(parent, null, "3", goodScope);
        PersistentResource<Child> childResource1 = new PersistentResource<>(child1, null, "1", goodScope);
        PersistentResource<Child> childResource3 = new PersistentResource<>(child3, null, "1", goodScope);
        parentResource.updateRelation("children", Sets.newHashSet(childResource1, childResource3));

        verify(tx, times(1)).updateToManyRelation(eq(tx), eq(parent),
                any(), any(), any(), eq(goodScope));
        verify(tx, never()).updateToManyRelation(eq(tx), eq(child1),
                any(), any(), any(), eq(goodScope));
        verify(tx, times(1)).updateToManyRelation(eq(tx), eq(child2),
                any(), any(), any(), eq(goodScope));
        verify(tx, times(1)).updateToManyRelation(eq(tx), eq(child3),
                any(), any(), any(), eq(goodScope));
    }

    @Test
    public void testSetAttributeHookInUpdateAttribute() {
        Parent parent = newParent(1);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        User goodUser = new User(1);

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<Parent> parentResource = new PersistentResource<>(parent, null, "1", goodScope);
        parentResource.updateAttribute("firstName", "foobar");

        verify(tx, times(1)).setAttribute(parent, "firstName", "foobar", goodScope);
    }

    @Test
    public void testGetRelationships() {
        FunWithPermissions fun = new FunWithPermissions();
        fun.setRelation1(Sets.newHashSet());
        fun.setRelation2(Sets.newHashSet());
        fun.setRelation3(null);

        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "3", goodUserScope);

        Map<String, Relationship> relationships = funResource.getRelationships();

        assertEquals(5, relationships.size(), "All relationships should be returned.");
        assertTrue(relationships.containsKey("relation1"), "relation1 should be present");
        assertTrue(relationships.containsKey("relation2"), "relation2 should be present");
        assertTrue(relationships.containsKey("relation3"), "relation3 should be present");
        assertTrue(relationships.containsKey("relation4"), "relation4 should be present");
        assertTrue(relationships.containsKey("relation5"), "relation5 should be present");

        PersistentResource<FunWithPermissions> funResourceWithBadScope = new PersistentResource<>(fun, null, "3", badUserScope);
        relationships = funResourceWithBadScope.getRelationships();

        assertEquals(0, relationships.size(), "All relationships should be filtered out");
    }

    @Test
    public void testNoCreate() {
        assertNotNull(dictionary);
        NoCreateEntity noCreate = new NoCreateEntity();
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        User goodUser = new User(1);

        when(tx.createNewObject(NoCreateEntity.class)).thenReturn(noCreate);

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        assertThrows(
                ForbiddenAccessException.class,
                () -> PersistentResource.createObject(
                        null, NoCreateEntity.class, goodScope, Optional.of("1"))); // should throw here
    }

    @Test
    public void testGetAttributes() {
        FunWithPermissions fun = new FunWithPermissions();
        fun.setField3("Foobar");
        fun.setField1("blah");
        fun.setField2(null);
        fun.setField4("bar");

        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "3", goodUserScope);

        Map<String, Object> attributes = funResource.getAttributes();

        assertEquals(6, attributes.size(),
                "A valid user should have access to all attributes that are readable."
        );

        assertTrue(attributes.containsKey("field2"), "Readable attributes should include field2");
        assertTrue(attributes.containsKey("field3"), "Readable attributes should include field3");
        assertTrue(attributes.containsKey("field4"), "Readable attributes should include field4");
        assertTrue(attributes.containsKey("field5"), "Readable attributes should include field5");
        assertTrue(attributes.containsKey("field6"), "Readable attributes should include field6");
        assertTrue(attributes.containsKey("field8"), "Readable attributes should include field8");
        assertNull(attributes.get("field2"), "field2 should be set to original value.");
        assertEquals(attributes.get("field3"), "Foobar", "field3 should be set to original value.");
        assertEquals(attributes.get("field4"), "bar", "field4 should be set to original value.");

        PersistentResource<FunWithPermissions> funResourceBad = new PersistentResource<>(fun, null, "3", badUserScope);

        attributes = funResourceBad.getAttributes();

        assertEquals(3, attributes.size(), "An invalid user should have access to a subset of attributes.");
        assertTrue(attributes.containsKey("field2"), "Readable attributes should include field2");
        assertTrue(attributes.containsKey("field4"), "Readable attributes should include field4");
        assertTrue(attributes.containsKey("field5"), "Readable attributes should include field5");
        assertNull(attributes.get("field2"), "field2 should be set to original value.");
        assertEquals(attributes.get("field4"), "bar", "field4 should be set to original value.");
    }

    @Test
    public void testFilter() {
        Child child1 = newChild(1);
        Child child2 = newChild(-2);
        Child child3 = newChild(3);
        Child child4 = newChild(-4);

        {
            PersistentResource<Child> child1Resource = new PersistentResource<>(child1, null, "1", goodUserScope);
            PersistentResource<Child> child2Resource = new PersistentResource<>(child2, null, "-2", goodUserScope);
            PersistentResource<Child> child3Resource = new PersistentResource<>(child3, null, "3", goodUserScope);
            PersistentResource<Child> child4Resource = new PersistentResource<>(child4, null, "-4", goodUserScope);

            Set<PersistentResource> resources =
                    Sets.newHashSet(child1Resource, child2Resource, child3Resource, child4Resource);

            Set<PersistentResource> results = PersistentResource.filter(ReadPermission.class, Optional.empty(), resources);
            assertEquals(2, results.size(), "Only a subset of the children are readable");
            assertTrue(results.contains(child1Resource), "Readable children includes children with positive IDs");
            assertTrue(results.contains(child3Resource), "Readable children includes children with positive IDs");
        }

        {
            PersistentResource<Child> child1Resource = new PersistentResource<>(child1, null, "1", badUserScope);
            PersistentResource<Child> child2Resource = new PersistentResource<>(child2, null, "-2", badUserScope);
            PersistentResource<Child> child3Resource = new PersistentResource<>(child3, null, "3", badUserScope);
            PersistentResource<Child> child4Resource = new PersistentResource<>(child4, null, "-4", badUserScope);

            Set<PersistentResource> resources =
                    Sets.newHashSet(child1Resource, child2Resource, child3Resource, child4Resource);

            Set<PersistentResource> results = PersistentResource.filter(ReadPermission.class, Optional.empty(), resources);
            assertEquals(0, results.size(), "No children are readable by an invalid user");
        }
    }

    @Test
    public void testGetValue() throws Exception {
        FunWithPermissions fun = new FunWithPermissions();
        fun.setField3("testValue");
        String result;
        result = (String) getValue(fun, "field3", getRequestScope());
        assertEquals("testValue", result, "getValue should set the appropriate value in the resource");

        fun.setField1("testValue2");

        result = (String) getValue(fun, "field1", getRequestScope());
        assertEquals(result, "testValue2", "getValue should set the appropriate value in the resource");

        Child testChild = newChild(3);
        fun.setRelation1(Sets.newHashSet(testChild));

        @SuppressWarnings("unchecked")
        Set<Child> children = (Set<Child>) getValue(fun, "relation1", getRequestScope());

        assertTrue(children.contains(testChild), "getValue should set the correct relation.");
        assertEquals(1, children.size(), "getValue should set the relation with the correct number of elements");

        ComputedBean computedBean = new ComputedBean();

        String computedTest1 = (String) getValue(computedBean, "test", getRequestScope());
        String computedTest2 = (String) getValue(computedBean, "testWithScope", getRequestScope());
        String computedTest3 = (String) getValue(computedBean, "testWithSecurityScope", getRequestScope());

        assertEquals("test1", computedTest1);
        assertEquals("test2", computedTest2);
        assertEquals("test3", computedTest3);

        assertThrows(
                InvalidAttributeException.class,
                () -> getValue(computedBean, "NonComputedWithScope", getRequestScope()),
                "Getting a bad relation should throw an InvalidAttributeException.");

        assertThrows(
                InvalidAttributeException.class,
                () -> setValue("badRelation", "badValue"),
                "Getting a bad relation should throw an InvalidAttributeException.");
    }

    @Test
    public void testSetValue() throws Exception {
        FunWithPermissions fun = new FunWithPermissions();
        this.obj = fun;
        setValue("field3", "testValue");
        assertEquals("testValue", fun.getField3(), "setValue should set the appropriate value in the resource");

        setValue("field1", "testValue2");
        assertEquals("testValue2", fun.getField1(), "setValue should set the appropriate value in the resource");

        Child testChild = newChild(3);
        setValue("relation1", Sets.newHashSet(testChild));

        assertTrue(fun.getRelation1().contains(testChild), "setValue should set the correct relation.");
        assertEquals(fun.getRelation1().size(), 1, "setValue should set the relation with the correct number of elements");

        assertThrows(
                InvalidAttributeException.class,
                () -> setValue("badRelation", "badValue"),
                "Getting a bad relation should throw an InvalidAttributeException.");
    }

    @Test
    public void testSetMapValue() {
        MapColorShape mapColorShape = new MapColorShape();
        this.obj = mapColorShape;

        HashMap<Object, Object> coerceable = new HashMap<>();
        coerceable.put("Red", "Circle");
        coerceable.put("Green", "Square");
        coerceable.put("Violet", "Triangle");
        setValue("colorShapeMap", coerceable);

        assertEquals(Shape.Circle, mapColorShape.getColorShapeMap().get(Color.Red));
        assertEquals(Shape.Square, mapColorShape.getColorShapeMap().get(Color.Green));
        assertEquals(Shape.Triangle, mapColorShape.getColorShapeMap().get(Color.Violet));
        assertEquals(3, mapColorShape.getColorShapeMap().size());
    }

    @Test
    public void testSetMapInvalidColorEnum() {
        this.obj = new MapColorShape();

        HashMap<Object, Object> coerceable = new HashMap<>();
        coerceable.put("InvalidColor", "Circle");
        assertThrows(InvalidValueException.class, () -> setValue("colorShapeMap", coerceable));
    }

    @Test
    public void testSetMapInvalidShapeEnum() {
        this.obj = new MapColorShape();

        HashMap<Object, Object> coerceable = new HashMap<>();
        coerceable.put("Red", "InvalidShape");
        assertThrows(InvalidValueException.class, () -> setValue("colorShapeMap", coerceable));
    }

    @Test
    public void testDeleteBidirectionalRelation() {
        Left left = new Left();
        Right right = new Right();
        left.setOne2one(right);
        right.setOne2one(left);

        PersistentResource<Left> leftResource = new PersistentResource<>(left, null, "3", goodUserScope);

        leftResource.deleteInverseRelation("one2one", right);

        assertNull(right.getOne2one(), "The one-2-one inverse relationship should have been unset");
        assertEquals(right, left.getOne2one(), "The owning relationship should NOT have been unset");

        Child child = new Child();
        Parent parent = new Parent();
        child.setParents(Sets.newHashSet(parent));
        parent.setChildren(Sets.newHashSet(child));
        parent.setSpouses(Sets.newHashSet());

        PersistentResource<Child> childResource = new PersistentResource<>(child, null, "4", goodUserScope);

        childResource.deleteInverseRelation("parents", parent);

        assertEquals(parent.getChildren().size(), 0, "The many-2-many inverse collection should have been cleared.");
        assertTrue(child.getParents().contains(parent), "The owning relationship should NOT have been touched");
    }

    @Test
    public void testAddBidirectionalRelation() {
        Left left = new Left();
        Right right = new Right();

        PersistentResource<Left> leftResource = new PersistentResource<>(left, null, "3", goodUserScope);

        leftResource.addInverseRelation("one2one", right);

        assertEquals(left, right.getOne2one(), "The one-2-one inverse relationship should have been updated.");

        Child child = new Child();
        Parent parent = new Parent();
        child.setParents(Sets.newHashSet());
        parent.setChildren(Sets.newHashSet());
        parent.setSpouses(Sets.newHashSet());

        PersistentResource<Child> childResource = new PersistentResource<>(child, null, "4", goodUserScope);

        childResource.addInverseRelation("parents", parent);

        assertEquals(1, parent.getChildren().size(), "The many-2-many inverse relationship should have been updated");
        assertTrue(parent.getChildren().contains(child), "The many-2-many inverse relationship should have been updated");
    }

    @Test
    public void testSuccessfulOneToOneRelationshipAdd() throws Exception {
        User goodUser = new User(1);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        Left left = new Left();
        Right right = new Right();
        left.setId(2);
        right.setId(3);

        RequestScope goodScope = buildRequestScope(tx, goodUser);

        PersistentResource<Left> leftResource = new PersistentResource<>(left, null, "2", goodScope);

        Relationship ids = new Relationship(null, new Data<>(new ResourceIdentifier("right", "3").castToResource()));

        when(tx.loadObject(eq(Right.class), eq(3L), any(), any())).thenReturn(right);
        boolean updated = leftResource.updateRelation("one2one", ids.toPersistentResources(goodScope));
        goodScope.saveOrCreateObjects();
        verify(tx, times(1)).save(left, goodScope);
        verify(tx, times(1)).save(right, goodScope);
        verify(tx, times(1)).getRelation(tx, left, "one2one", Optional.empty(), Optional.empty(), Optional.empty(),
                goodScope);
        assertTrue(updated, "The one-2-one relationship should be added.");
        assertEquals(3, left.getOne2one().getId(), "The correct object was set in the one-2-one relationship");
    }

    /**
     * Avoid NPE when PATCH or POST defines relationship with null id
     * <pre>
     * <code>
     * "relationships": {
     *   "left": {
     *     "data": {
     *       "type": "right",
     *       "id": null
     *     }
     *   }
     * }
     * </code>
     * </pre>
     */
    @Test
    public void testSuccessfulOneToOneRelationshipAddNull() throws Exception {
        User goodUser = new User(1);
        DataStoreTransaction tx = mock(DataStoreTransaction.class, Answers.CALLS_REAL_METHODS);
        Left left = new Left();
        left.setId(2);

        RequestScope goodScope = buildRequestScope(tx, goodUser);

        PersistentResource<Left> leftResource = new PersistentResource<>(left, null, "2", goodScope);

        Relationship ids = new Relationship(null, new Data<>(new Resource("right", null, null, null, null, null)));

        InvalidObjectIdentifierException thrown = assertThrows(
                InvalidObjectIdentifierException.class,
                () -> leftResource.updateRelation("one2one", ids.toPersistentResources(goodScope)));

        assertEquals("Unknown identifier 'null' for right", thrown.getMessage());
    }

    @Test
    /*
     * The following are ids for a hypothetical relationship.
     * GIVEN:
     * all (all the ids in the DB) = 1,2,3,4,5
     * mine (everything the current user has access to) = 1,2,3
     * requested (what the user wants to change to) = 3,6
     * THEN:
     * deleted (what gets removed from the DB) = 1,2
     * final (what get stored in the relationship) = 3,4,5,6
     * BECAUSE:
     * notMine = all - mine
     * updated = (requested UNION mine) - (requested INTERSECT mine)
     * deleted = (mine - requested)
     * final = (notMine) UNION requested
     */
    public void testSuccessfulManyToManyRelationshipUpdate() throws Exception {
        User goodUser = new User(1);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);

        Parent parent = new Parent();
        RequestScope goodScope = buildRequestScope(tx, goodUser);

        Child child1 = newChild(1);
        Child child2 = newChild(2);
        Child child3 = newChild(3);
        Child child4 = newChild(-4); //Not accessible to goodUser
        Child child5 = newChild(-5); //Not accessible to goodUser
        Child child6 = newChild(6);

        //All = (1,2,3,4,5)
        //Mine = (1,2,3)
        Set<Child> allChildren = new HashSet<>();
        allChildren.add(child1);
        allChildren.add(child2);
        allChildren.add(child3);
        allChildren.add(child4);
        allChildren.add(child5);
        parent.setChildren(allChildren);
        parent.setSpouses(Sets.newHashSet());

        when(tx.getRelation(any(), eq(parent), eq("children"), any(), any(), any(), any())).thenReturn(allChildren);

        PersistentResource<Parent> parentResource = new PersistentResource<>(parent, null, "1", goodScope);

        //Requested = (3,6)
        List<Resource> idList = new ArrayList<>();
        idList.add(new ResourceIdentifier("child", "3").castToResource());
        idList.add(new ResourceIdentifier("child", "6").castToResource());
        Relationship ids = new Relationship(null, new Data<>(idList));


        when(tx.loadObject(eq(Child.class), eq(2L), any(), any())).thenReturn(child2);
        when(tx.loadObject(eq(Child.class), eq(3L), any(), any())).thenReturn(child3);
        when(tx.loadObject(eq(Child.class), eq(-4L), any(), any())).thenReturn(child4);
        when(tx.loadObject(eq(Child.class), eq(-5L), any(), any())).thenReturn(child5);
        when(tx.loadObject(eq(Child.class), eq(6L), any(), any())).thenReturn(child6);

        //Final set after operation = (3,4,5,6)
        Set<Child> expected = new HashSet<>();
        expected.add(child3);
        expected.add(child4);
        expected.add(child5);
        expected.add(child6);

        boolean updated = parentResource.updateRelation("children", ids.toPersistentResources(goodScope));

        goodScope.saveOrCreateObjects();
        verify(tx, times(1)).save(parent, goodScope);
        verify(tx, times(1)).save(child1, goodScope);
        verify(tx, times(1)).save(child2, goodScope);
        verify(tx, times(1)).save(child6, goodScope);
        verify(tx, never()).save(child4, goodScope);
        verify(tx, never()).save(child5, goodScope);
        verify(tx, never()).save(child3, goodScope);

        assertTrue(updated, "Many-2-many relationship should be updated.");
        assertTrue(parent.getChildren().containsAll(expected), "All expected members were updated");
        assertTrue(expected.containsAll(parent.getChildren()), "All expected members were updated");

        /*
         * No tests for reference integrity since the parent is the owner and
         * this is a many to many relationship.
         */
    }

    /**
     * Verify that Relationship toMany cannot contain null resources, but toOne can.
     * @throws Exception
     */
    @Test
    public void testRelationshipMissingData() throws Exception {
        User goodUser = new User(1);
        @SuppressWarnings("resource")
        DataStoreTransaction tx = mock(DataStoreTransaction.class);

        RequestScope goodScope = new RequestScope(null, null, tx, goodUser, null, elideSettings);

        // null resource in toMany relationship is not valid
        List<Resource> idList = new ArrayList<>();
        idList.add(new ResourceIdentifier("child", "3").castToResource());
        idList.add(new ResourceIdentifier("child", "6").castToResource());
        idList.add(null);
        assertThrows(
                NullPointerException.class,
                () -> new Relationship(Collections.emptyMap(), new Data<>(idList)));

        // However null toOne relationship is valid
        Relationship toOneRelationship = new Relationship(Collections.emptyMap(), new Data<>((Resource) null));
        assertEquals(Collections.emptySet(), toOneRelationship.getData().get());
        assertNull(toOneRelationship.toPersistentResources(goodScope));

        // no Data
        Relationship nullRelationship = new Relationship(Collections.emptyMap(), null);
        assertNull(nullRelationship.getData());
        assertNull(nullRelationship.toPersistentResources(goodScope));
    }

    @Test
    public void testGetAttributeSuccess() {
        FunWithPermissions fun = new FunWithPermissions();
        fun.setField2("blah");
        fun.setField3(null);

        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "1", goodUserScope);

        String result = (String) funResource.getAttribute("field2");
        assertEquals("blah", result, "The correct attribute should be returned.");
        result = (String) funResource.getAttribute("field3");
        assertNull(result, "The correct attribute should be returned.");
    }

    @Test
    public void testGetAttributeInvalidField() {
        FunWithPermissions fun = new FunWithPermissions();

        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "1", goodUserScope);

        assertThrows(InvalidAttributeException.class, () -> funResource.getAttribute("invalid"));
    }

    @Test
    public void testGetAttributeInvalidFieldPermissions() {
        FunWithPermissions fun = new FunWithPermissions();
        fun.setField1("foo");

        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "1", goodUserScope);

        assertThrows(ForbiddenAccessException.class, () -> funResource.getAttribute("field1"));
    }

    @Test
    public void testGetAttributeInvalidEntityPermissions() {
        NoReadEntity noread = new NoReadEntity();

        PersistentResource<NoReadEntity> noreadResource = new PersistentResource<>(noread, null, "1", goodUserScope);

        assertThrows(ForbiddenAccessException.class, () -> noreadResource.getAttribute("field"));
    }

    @Test
    public void testGetRelationSuccess() {
        FunWithPermissions fun = new FunWithPermissions();
        Child child1 = newChild(1);
        Child child2 = newChild(2);
        Child child3 = newChild(3);
        Set<Child> children = Sets.newHashSet(child1, child2, child3);
        fun.setRelation2(children);

        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "3", goodUserScope);

        when(goodUserScope.getTransaction().getRelation(any(), eq(fun), eq("relation2"), any(), any(), any(), any())).thenReturn(children);

        Set<PersistentResource> results = getRelation(funResource, "relation2");

        assertEquals(3, results.size(), "All of relation elements should be returned.");
    }

    @Test
    public void testGetRelationFilteredSuccess() {
        FunWithPermissions fun = new FunWithPermissions();
        Child child1 = newChild(1);
        Child child2 = newChild(-2);
        Child child3 = newChild(3);

        Set<Child> children = Sets.newHashSet(child1, child2, child3);
        fun.setRelation2(Sets.newHashSet(child1, child2, child3));

        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "3", goodUserScope);

        when(goodUserScope.getTransaction().getRelation(any(), eq(fun), eq("relation2"), any(), any(), any(), any())).thenReturn(children);

        Set<PersistentResource> results = getRelation(funResource, "relation2");

        assertEquals(2, results.size(), "Only filtered relation elements should be returned.");
    }

    @Test
    public void testGetRelationWithPredicateSuccess() {
        Parent parent = newParent(1);
        Child child1 = newChild(1, "paul john");
        Child child2 = newChild(2, "john buzzard");
        Child child3 = newChild(3, "chris smith");
        parent.setChildren(Sets.newHashSet(child1, child2, child3));

        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        when(tx.getRelation(eq(tx), any(), any(), any(), any(), any(), any())).thenReturn(Sets.newHashSet(child1));
        User goodUser = new User(1);

        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.add("filter[child.name]", "paul john");
        RequestScope goodScope = buildRequestScope("/child", tx, goodUser, queryParams);

        PersistentResource<Parent> parentResource = new PersistentResource<>(parent, null, "1", goodScope);

        Set<PersistentResource> results = getRelation(parentResource, "children");

        assertEquals(1, results.size());
        assertEquals("paul john", ((Child) IterableUtils.first(results).getObject()).getName());
    }

    @Test
    public void testGetSingleRelationInMemory() {
        // Ensure we don't break when we try to get a specific relationship in memory (i.e. not yet pushed to datastore)
        Parent parent = newParent(1);
        Child child1 = newChild(1, "paul john");
        Child child2 = newChild(2, "john buzzard");
        Child child3 = newChild(3, "chris smith");
        Set<Child> children = Sets.newHashSet(child1, child2, child3);
        parent.setChildren(children);

        when(goodUserScope.getTransaction().getRelation(any(), eq(parent), eq("children"), any(), any(), any(), any())).thenReturn(children);

        PersistentResource<Parent> parentResource = new PersistentResource<>(parent, null, "1", goodUserScope);

        PersistentResource childResource = parentResource.getRelation("children", "2");

        assertEquals("2", childResource.getId());
        assertEquals("john buzzard", ((Child) childResource.getObject()).getName());
    }

    @Test
    public void testGetRelationForbiddenByEntity() {
        NoReadEntity noread = new NoReadEntity();

        PersistentResource<NoReadEntity> noreadResource = new PersistentResource<>(noread, null, "3", goodUserScope);
        assertThrows(ForbiddenAccessException.class, () -> getRelation(noreadResource, "child"));
    }

    @Test
    public void testGetRelationForbiddenByField() {
        FunWithPermissions fun = new FunWithPermissions();

        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "3", badUserScope);

        assertThrows(ForbiddenAccessException.class, () -> getRelation(funResource, "relation1"));
    }

    @Test
    public void testGetRelationForbiddenByEntityAllowedByField() {
        FirstClassFields firstClassFields = new FirstClassFields();

        PersistentResource<FirstClassFields> fcResource = new PersistentResource<>(firstClassFields, null, "3", badUserScope);

        getRelation(fcResource, "public2");
    }

    @Test
    public void testGetAttributeForbiddenByEntityAllowedByField() {
        FirstClassFields firstClassFields = new FirstClassFields();

        PersistentResource<FirstClassFields> fcResource = new PersistentResource<>(firstClassFields, null, "3", badUserScope);

        fcResource.getAttribute("public1");
    }

    @Test
    public void testGetRelationForbiddenByEntity2() {
        FirstClassFields firstClassFields = new FirstClassFields();

        PersistentResource<FirstClassFields> fcResource = new PersistentResource<>(firstClassFields, null, "3", badUserScope);

        assertThrows(ForbiddenAccessException.class, () -> getRelation(fcResource, "private2"));
    }

    @Test
    public void testGetAttributeForbiddenByEntity2() {
        FirstClassFields firstClassFields = new FirstClassFields();

        PersistentResource<FirstClassFields> fcResource = new PersistentResource<>(firstClassFields,
                null, "3", goodUserScope);

        assertThrows(ForbiddenAccessException.class, () -> fcResource.getAttribute("private1"));
    }

    @Test
    public void testGetRelationInvalidRelation() {
        FunWithPermissions fun = new FunWithPermissions();

        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "3", goodUserScope);

        assertThrows(InvalidAttributeException.class, () -> getRelation(funResource, "invalid"));
    }

    @Test
    public void testGetRelationByIdSuccess() {
        FunWithPermissions fun = new FunWithPermissions();
        Child child1 = newChild(1);
        Child child2 = newChild(2);
        Child child3 = newChild(3);
        fun.setRelation2(Sets.newHashSet(child1, child2, child3));

        User goodUser = new User(1);

        DataStoreTransaction tx = mock(DataStoreTransaction.class);

        when(tx.getRelation(eq(tx), any(), any(), any(), any(), any(), any())).thenReturn(Sets.newHashSet(child1));

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "3", goodScope);

        PersistentResource<?> result = funResource.getRelation("relation2", "1");

        assertEquals(1, ((Child) result.getObject()).getId(), "The correct relationship element should be returned");
    }

    @Test
    public void testGetRelationByInvalidId() {
        FunWithPermissions fun = new FunWithPermissions();
        Child child1 = newChild(1);
        Child child2 = newChild(2);
        Child child3 = newChild(3);
        fun.setRelation2(Sets.newHashSet(child1, child2, child3));

        User goodUser = new User(1);

        DataStoreTransaction tx = mock(DataStoreTransaction.class);

        when(tx.getRelation(eq(tx), any(), any(), any(), any(), any(), any())).thenReturn(Sets.newHashSet(child1));

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "3", goodScope);

        assertThrows(InvalidObjectIdentifierException.class, () -> funResource.getRelation("relation2", "-1000"));
    }

    @Test
    public void testGetRelationsNoEntityAccess() {
        FunWithPermissions fun = new FunWithPermissions();

        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "3", goodUserScope);

        Set set = getRelation(funResource, "relation4");
        assertEquals(0, set.size());
    }

    @Test
    public void testGetRelationsNoEntityAccess2() {
        FunWithPermissions fun = new FunWithPermissions();

        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "3", goodUserScope);

        Set set  = getRelation(funResource, "relation5");
        assertEquals(0, set.size());
    }

    @Test
    void testDeleteResourceSuccess() {
        Parent parent = newParent(1);

        User goodUser = new User(1);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        RequestScope goodScope = buildRequestScope(tx, goodUser);

        PersistentResource<Parent> parentResource = new PersistentResource<>(parent, null, "1", goodScope);

        parentResource.deleteResource();

        verify(tx).delete(parent, goodScope);
    }

    @Test
    void testDeleteCascades() {
        Invoice invoice = new Invoice();
        invoice.setId(1);

        LineItem item = new LineItem();
        invoice.setItems(Sets.newHashSet(item));
        item.setInvoice(invoice);

        User goodUser = new User(1);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        RequestScope goodScope = buildRequestScope(tx, goodUser);

        PersistentResource<Invoice> invoiceResource = new PersistentResource<>(invoice, null, "1", goodScope);

        invoiceResource.deleteResource();

        verify(tx).delete(invoice, goodScope);

        /* The inverse relation should not be touched for cascading deletes */
        verify(tx, never()).save(item, goodScope);
        assertEquals(1, invoice.getItems().size());
    }

    @Test
    void testDeleteResourceUpdateRelationshipSuccess() {
        Parent parent = new Parent();
        Child child = newChild(100);
        parent.setChildren(Sets.newHashSet(child));
        parent.setSpouses(Sets.newHashSet());

        Set<Parent> parents = Sets.newHashSet(parent);
        child.setParents(Sets.newHashSet(parent));

        assertFalse(parent.getChildren().isEmpty());

        User goodUser = new User(1);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        when(tx.getRelation(any(), eq(child), eq("parents"), any(), any(), any(), any())).thenReturn(parents);

        RequestScope goodScope = buildRequestScope(tx, goodUser);

        PersistentResource<Parent> parentResource = new PersistentResource<>(parent, null, "1", goodScope);
        PersistentResource<Child> childResource = new PersistentResource<>(child, parentResource, "1", goodScope);

        childResource.deleteResource();
        goodScope.saveOrCreateObjects();
        verify(tx, times(1)).delete(child, goodScope);
        verify(tx, times(1)).save(parent, goodScope);
        verify(tx, never()).delete(parent, goodScope);
        assertTrue(parent.getChildren().isEmpty());
    }

    @Test
    void testDeleteResourceForbidden() {
        NoDeleteEntity nodelete = new NoDeleteEntity();
        nodelete.setId(1);

        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        User goodUser = new User(1);

        RequestScope goodScope = buildRequestScope(tx, goodUser);

        PersistentResource<NoDeleteEntity> nodeleteResource = new PersistentResource<>(nodelete, null, "1", goodScope);

        assertThrows(ForbiddenAccessException.class, nodeleteResource::deleteResource);

        verify(tx, never()).delete(nodelete, goodScope);
    }

    @Test
    void testAddRelationSuccess() {
        FunWithPermissions fun = new FunWithPermissions();
        fun.setRelation1(Sets.newHashSet());

        Child child = newChild(1);

        User goodUser = new User(1);

        DataStoreTransaction tx = mock(DataStoreTransaction.class);

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "3", goodScope);
        PersistentResource<Child> childResource = new PersistentResource<>(child, null, "1", goodScope);
        funResource.addRelation("relation1", childResource);

        goodScope.saveOrCreateObjects();
        verify(tx, never()).save(child, goodScope); // Child wasn't modified
        verify(tx, times(1)).save(fun, goodScope);

        assertTrue(fun.getRelation1().contains(child), "The correct element should be added to the relation");
    }

    @Test
    void testAddRelationForbiddenByField() {
        FunWithPermissions fun = new FunWithPermissions();
        fun.setRelation1(Sets.newHashSet());

        Child child = newChild(1);

        User badUser = new User(-1);

        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        RequestScope badScope = buildRequestScope(tx, badUser);
        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "3", badScope);
        PersistentResource<Child> childResource = new PersistentResource<>(child, null, "1", badScope);
        assertThrows(ForbiddenAccessException.class, () -> funResource.addRelation("relation1", childResource));
    }

    @Test
    void testAddRelationForbiddenByEntity() {
        NoUpdateEntity noUpdate = new NoUpdateEntity();
        noUpdate.setId(1);
        Child child = newChild(2);
        noUpdate.setChildren(Sets.newHashSet());

        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        User goodUser = new User(1);

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<NoUpdateEntity> noUpdateResource = new PersistentResource<>(noUpdate, null, "1", goodScope);
        PersistentResource<Child> childResource = new PersistentResource<>(child, null, "2", goodScope);
        assertThrows(ForbiddenAccessException.class, () -> noUpdateResource.addRelation("children", childResource));
    }

    @Test
    public void testAddRelationInvalidRelation() {
        FunWithPermissions fun = new FunWithPermissions();

        Child child = newChild(1);

        User goodUser = new User(1);

        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "3", goodScope);
        PersistentResource<Child> childResource = new PersistentResource<>(child, null, "1", goodScope);
        assertThrows(InvalidAttributeException.class, () -> funResource.addRelation("invalid", childResource));
    }

    @Test()
    public void testRemoveToManyRelationSuccess() {
        Child child = newChild(1);
        Parent parent1 = newParent(1, child);
        Parent parent2 = newParent(2, child);
        Parent parent3 = newParent(3, child);
        child.setParents(Sets.newHashSet(parent1, parent2, parent3));

        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        User goodUser = new User(1);
        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<Child> childResource = new PersistentResource<>(child, null, "1", goodScope);
        PersistentResource<Object> removeResource = new PersistentResource<>(parent1, null, "1", goodScope);
        childResource.removeRelation("parents", removeResource);

        assertEquals(2, child.getParents().size(), "The many-2-many relationship should be cleared");
        assertEquals(0, parent1.getChildren().size(), "The many-2-many inverse relationship should be cleared");
        assertEquals(1, parent3.getChildren().size(), "The many-2-many inverse relationship should not be cleared");
        assertEquals(1, parent3.getChildren().size(), "The many-2-many inverse relationship should not be cleared");

        goodScope.saveOrCreateObjects();
        verify(tx, times(1)).save(child, goodScope);
        verify(tx, times(1)).save(parent1, goodScope);
        verify(tx, never()).save(parent2, goodScope);
        verify(tx, never()).save(parent3, goodScope);
    }

    @Test()
    public void testRemoveToOneRelationSuccess() {
        FunWithPermissions fun = new FunWithPermissions();
        Child child = newChild(1);
        fun.setRelation3(child);

        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        User goodUser = new User(1);
        RequestScope goodScope = buildRequestScope(tx, goodUser);

        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "1", goodScope);
        PersistentResource<Object> removeResource = new PersistentResource<>(child, null, "1", goodScope);

        funResource.removeRelation("relation3", removeResource);

        assertNull(fun.getRelation3(), "The one-2-one relationship should be cleared");

        goodScope.saveOrCreateObjects();
        verify(tx, times(1)).save(fun, goodScope);
        verify(tx, never()).save(child, goodScope);
    }

    // Test to ensure that save() is not called on unmodified objects
    @Test
    public void testNoSaveNonModifications() {
        FunWithPermissions fun = new FunWithPermissions();
        Child child = newChild(1);
        Child secret = newChild(2);
        Parent parent = new Parent();

        fun.setRelation3(child);

        Set<Child> children1 = Sets.newHashSet(child);
        fun.setRelation1(children1);

        Set<Child> children2 = Sets.newHashSet(child);
        parent.setChildren(children2);
        parent.setFirstName("Leeroy");

        child.setReadNoAccess(secret);

        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        when(tx.getRelation(any(), eq(fun), eq("relation3"), any(), any(), any(), any())).thenReturn(child);
        when(tx.getRelation(any(), eq(fun), eq("relation1"), any(), any(), any(), any())).thenReturn(children1);
        when(tx.getRelation(any(), eq(parent), eq("children"), any(), any(), any(), any())).thenReturn(children2);
        when(tx.getRelation(any(), eq(child), eq("readNoAccess"), any(), any(), any(), any())).thenReturn(secret);

        User goodUser = new User(1);
        RequestScope goodScope = buildRequestScope(tx, goodUser);

        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "1", goodScope);
        PersistentResource<Child> childResource = new PersistentResource<>(child, null, "1", goodScope);
        PersistentResource<Child> secretResource = new PersistentResource<>(secret, null, "1", goodScope);
        PersistentResource<Parent> parentResource = new PersistentResource<>(parent, null, "1", goodScope);

        // Add an existing to-one relationship
        funResource.addRelation("relation3", childResource);

        // Add an exising to-many relationship
        funResource.addRelation("relation1", childResource);

        // Update set with same data
        funResource.updateRelation("relation1", Sets.newHashSet(childResource));

        // Update to-one relation with same relation with same data
        funResource.updateRelation("relation3", Sets.newHashSet(childResource));

        // Update to-many with bi-directional relationship
        parentResource.updateRelation("children", Sets.newHashSet(childResource));

        // Update to-one with bi-directional relation with same data
        childResource.updateRelation("readNoAccess", Sets.newHashSet(secretResource));

        // Update an attribute with the same value
        parentResource.updateAttribute("firstName", "Leeroy");

        // Remove non-existing to-many relation
        childResource.removeRelation("friends", secretResource);

        // Clear empty to-many relation
        childResource.clearRelation("parents");

        // Clear empty to-one relation
        secretResource.clearRelation("readNoAccess");

        goodScope.saveOrCreateObjects();
        verify(tx, never()).save(fun, goodScope);
        verify(tx, never()).save(child, goodScope);
        verify(tx, never()).save(parent, goodScope);
        verify(tx, never()).save(secret, goodScope);
    }

    @Test()
    public void testRemoveNonexistingToOneRelation() {
        FunWithPermissions fun = new FunWithPermissions();
        Child ownedChild = newChild(1);
        Child unownedChild = newChild(2);
        fun.setRelation3(ownedChild);

        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        User goodUser = new User(1);
        RequestScope goodScope = buildRequestScope(tx, goodUser);

        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "1", goodScope);
        PersistentResource<Object> removeResource = new PersistentResource<>(unownedChild, null, "1", goodScope);

        funResource.removeRelation("relation3", removeResource);

        assertEquals(ownedChild, fun.getRelation3(), "The one-2-one relationship should NOT be cleared");

        verify(tx, never()).save(fun, goodScope);
        verify(tx, never()).save(ownedChild, goodScope);
    }

    @Test()
    public void testRemoveNonexistingToManyRelation() {
        Child child = newChild(1);
        Parent parent1 = newParent(1, child);
        Parent parent2 = newParent(2, child);
        Parent parent3 = newParent(3, child);
        child.setParents(Sets.newHashSet(parent1, parent2, parent3));

        Parent unownedParent = newParent(4, null);

        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        User goodUser = new User(1);
        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<Child> childResource = new PersistentResource<>(child, null, "1", goodScope);
        PersistentResource<Object> removeResource = new PersistentResource<>(unownedParent, null, "1", goodScope);
        childResource.removeRelation("parents", removeResource);

        assertEquals(3, child.getParents().size(), "The many-2-many relationship should not be cleared");
        assertEquals(1, parent1.getChildren().size(), "The many-2-many inverse relationship should not be cleared");
        assertEquals(1, parent3.getChildren().size(), "The many-2-many inverse relationship should not be cleared");
        assertEquals(1, parent3.getChildren().size(), "The many-2-many inverse relationship should not be cleared");

        verify(tx, never()).save(child, goodScope);
        verify(tx, never()).save(parent1, goodScope);
        verify(tx, never()).save(parent2, goodScope);
        verify(tx, never()).save(parent3, goodScope);
    }

    @Test()
    public void testClearToManyRelationSuccess() {
        Child child = newChild(1);
        Parent parent1 = newParent(1, child);
        Parent parent2 = newParent(2, child);
        Parent parent3 = newParent(3, child);
        Set<Parent> parents = Sets.newHashSet(parent1, parent2, parent3);
        child.setParents(parents);

        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        when(tx.getRelation(any(), eq(child), eq("parents"), any(), any(), any(), any())).thenReturn(parents);

        User goodUser = new User(1);
        RequestScope goodScope = buildRequestScope(tx, goodUser);

        PersistentResource<Child> childResource = new PersistentResource<>(child, null, "1", goodScope);

        childResource.clearRelation("parents");

        assertEquals(0, child.getParents().size(), "The many-2-many relationship should be cleared");
        assertEquals(0, parent1.getChildren().size(), "The many-2-many inverse relationship should be cleared");
        assertEquals(0, parent3.getChildren().size(), "The many-2-many inverse relationship should be cleared");
        assertEquals(0, parent3.getChildren().size(), "The many-2-many inverse relationship should be cleared");

        goodScope.saveOrCreateObjects();
        verify(tx, times(1)).save(child, goodScope);
        verify(tx, times(1)).save(parent1, goodScope);
        verify(tx, times(1)).save(parent2, goodScope);
        verify(tx, times(1)).save(parent3, goodScope);
    }

    @Test()
    public void testClearToOneRelationSuccess() {
        FunWithPermissions fun = new FunWithPermissions();
        Child child = newChild(1);
        fun.setRelation3(child);

        DataStoreTransaction tx = mock(DataStoreTransaction.class);

        when(tx.getRelation(any(), eq(fun), eq("relation3"), any(), any(), any(), any())).thenReturn(child);

        User goodUser = new User(1);
        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "1", goodScope);
        funResource.clearRelation("relation3");

        assertNull(fun.getRelation3(), "The one-2-one relationship should be cleared");

        goodScope.saveOrCreateObjects();
        verify(tx, times(1)).save(fun, goodScope);
        verify(tx, times(1)).save(child, goodScope);
    }

    @Test()
    public void testClearRelationFilteredByReadAccess() {
        User goodUser = new User(1);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        Parent parent = new Parent();
        RequestScope goodScope = buildRequestScope(tx, goodUser);

        Child child1 = newChild(1);
        Child child2 = newChild(2);
        Child child3 = newChild(3);
        Child child4 = newChild(-4);
        child4.setId(-4); //Not accessible to goodUser
        Child child5 = newChild(-5);
        child5.setId(-5); //Not accessible to goodUser


        //All = (1,2,3,4,5)
        //Mine = (1,2,3)
        Set<Child> allChildren = new HashSet<>();
        allChildren.add(child1);
        allChildren.add(child2);
        allChildren.add(child3);
        allChildren.add(child4);
        allChildren.add(child5);
        parent.setChildren(allChildren);
        parent.setSpouses(Sets.newHashSet());

        when(tx.getRelation(any(), eq(parent), eq("children"), any(), any(), any(), any())).thenReturn(allChildren);

        PersistentResource<Parent> parentResource = new PersistentResource<>(parent, null, "1", goodScope);

        //Final set after operation = (4,5)
        Set<Child> expected = new HashSet<>();
        expected.add(child4);
        expected.add(child5);

        boolean updated = parentResource.clearRelation("children");

        goodScope.saveOrCreateObjects();
        verify(tx, times(1)).save(parent, goodScope);
        verify(tx, times(1)).save(child1, goodScope);
        verify(tx, times(1)).save(child2, goodScope);
        verify(tx, times(1)).save(child3, goodScope);
        verify(tx, never()).save(child4, goodScope);
        verify(tx, never()).save(child5, goodScope);

        assertTrue(updated, "The relationship should have been partially cleared.");
        assertTrue(parent.getChildren().containsAll(expected), "The unfiltered remaining members are left");
        assertTrue(expected.containsAll(parent.getChildren()), "The unfiltered remaining members are left");

        /*
         * No tests for reference integrity since the parent is the owner and
         * this is a many to many relationship.
         */
    }

    @Test
    public void testClearRelationInvalidToOneUpdatePermission() {
        Left left = new Left();
        left.setId(1);
        Right right = new Right();
        right.setId(1);
        left.setNoUpdateOne2One(right);
        right.setNoUpdateOne2One(left);

        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        User goodUser = new User(1);
        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<Left> leftResource = new PersistentResource<>(left, null, "1", goodScope);

        assertThrows(
                ForbiddenAccessException.class,
                () -> leftResource.clearRelation("noUpdateOne2One"));
        // Modifications have a deferred check component:
        leftResource.getRequestScope().getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void testNoChangeRelationInvalidToOneUpdatePermission() {
        Left left = new Left();
        left.setId(1);
        Right right = new Right();
        right.setId(1);
        left.setNoUpdateOne2One(right);
        right.setNoUpdateOne2One(left);

        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        User goodUser = new User(1);
        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<Left> leftResource = new PersistentResource<>(left, null, "1", goodScope);

        assertThrows(
                ForbiddenAccessException.class,
                () -> leftResource.updateRelation("noUpdateOne2One", getRelation(leftResource, "noUpdateOne2One")));
        // Modifications have a deferred check component:
        leftResource.getRequestScope().getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void testClearRelationInvalidToManyUpdatePermission() {
        Left left = new Left();
        left.setId(1);
        Right right1 = new Right();
        right1.setId(1);
        Right right2 = new Right();
        right2.setId(2);

        Set<Right> noInverseUpdate = Sets.newHashSet(right1, right2);
        left.setNoInverseUpdate(noInverseUpdate);
        right1.setNoUpdate(Sets.newHashSet(left));
        right2.setNoUpdate(Sets.newHashSet(left));

        DataStoreTransaction tx = mock(DataStoreTransaction.class);

        when(tx.getRelation(any(), eq(left), eq("noInverseUpdate"), any(), any(), any(), any())).thenReturn(noInverseUpdate);

        User goodUser = new User(1);
        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<Left> leftResource = new PersistentResource<>(left, null, "1", goodScope);

        assertThrows(
                ForbiddenAccessException.class,
                () -> leftResource.clearRelation("noInverseUpdate"));
        // Modifications have a deferred check component:
        leftResource.getRequestScope().getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void testClearRelationInvalidToOneDeletePermission() {
        Left left = new Left();
        left.setId(1);
        NoDeleteEntity noDelete = new NoDeleteEntity();
        noDelete.setId(1);
        left.setNoDeleteOne2One(noDelete);

        DataStoreTransaction tx = mock(DataStoreTransaction.class);

        when(tx.getRelation(any(), eq(left), eq("noDeleteOne2One"), any(), any(), any(), any())).thenReturn(noDelete);
        User goodUser = new User(1);
        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<Left> leftResource = new PersistentResource<>(left, null, "1", goodScope);
        assertTrue(leftResource.clearRelation("noDeleteOne2One"));
        assertNull(leftResource.getObject().getNoDeleteOne2One());

    }

    @Test
    public void testClearRelationInvalidRelation() {
        FunWithPermissions fun = new FunWithPermissions();
        Child child = newChild(1);
        fun.setRelation3(child);

        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        User goodUser = new User(1);
        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "1", goodScope);
        assertThrows(InvalidAttributeException.class, () -> funResource.clearRelation("invalid"));
    }

    @Test
    public void testUpdateAttributeSuccess() {
        Parent parent = newParent(1);

        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        User goodUser = new User(1);

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<Parent> parentResource = new PersistentResource<>(parent, null, "1", goodScope);
        parentResource.updateAttribute("firstName", "foobar");

        assertEquals("foobar", parent.getFirstName(), "The attribute was updated successfully");

        goodScope.saveOrCreateObjects();
        verify(tx, times(1)).save(parent, goodScope);
    }

    @Test
    public void testUpdateAttributeInvalidAttribute() {
        Parent parent = newParent(1);

        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        User goodUser = new User(1);

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<Parent> parentResource = new PersistentResource<>(parent, null, "1", goodScope);
        assertThrows(InvalidAttributeException.class, () -> parentResource.updateAttribute("invalid", "foobar"));
    }

    @Test
    public void testUpdateAttributeInvalidUpdatePermission() {
        FunWithPermissions fun = new FunWithPermissions();
        fun.setId(1);


        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        User badUser = new User(-1);

        RequestScope badScope = buildRequestScope(tx, badUser);

        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "1", badScope);

        assertThrows(
                ForbiddenAccessException.class,
                () -> funResource.updateAttribute("field4", "foobar"));
        // Updates will defer and wait for the end!
        funResource.getRequestScope().getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void testUpdateAttributeInvalidUpdatePermissionNoChange() {
        FunWithPermissions fun = new FunWithPermissions();
        fun.setId(1);

        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        User badUser = new User(-1);

        RequestScope badScope = buildRequestScope(tx, badUser);

        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "1", badScope);

        assertThrows(
                ForbiddenAccessException.class,
                () -> funResource.updateAttribute("field4", funResource.getAttribute("field4")));
        // Updates will defer and wait for the end!
        funResource.getRequestScope().getPermissionExecutor().executeCommitChecks();
    }

    @Test()
    public void testLoadRecords() {
        Child child1 = newChild(1);
        Child child2 = newChild(-2);
        Child child3 = newChild(-3);
        Child child4 = newChild(4);
        Child child5 = newChild(5);

        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        User goodUser = new User(1);

        when(tx.loadObjects(eq(Child.class), any(), any(), any(), any(RequestScope.class)))
                .thenReturn(Lists.newArrayList(child1, child2, child3, child4, child5));

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        Set<PersistentResource> loaded = PersistentResource.loadRecords(Child.class, new ArrayList<>(),
                Optional.empty(), Optional.empty(), Optional.empty(), goodScope);

        Set<Child> expected = Sets.newHashSet(child1, child4, child5);

        Set<Object> actual = loaded.stream().map(PersistentResource::getObject).collect(Collectors.toSet());

        assertEquals(3, actual.size(),
                "The returned list should be filtered to only include elements that have read permission"
        );
        assertEquals(expected, actual,
                "The returned list should only include elements with a positive ID"
        );
    }

    @Test()
    public void testLoadRecordSuccess() {
        Child child1 = newChild(1);

        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        User goodUser = new User(1);

        when(tx.loadObject(eq(Child.class), eq(1L), any(), any())).thenReturn(child1);

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<Child> loaded = PersistentResource.loadRecord(Child.class, "1", goodScope);

        assertEquals(child1, loaded.getObject(), "The load function should return the requested child object");
    }

    @Test
    public void testLoadRecordInvalidId() {
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        User goodUser = new User(1);

        when(tx.loadObject(eq(Child.class), eq("1"), any(), any())).thenReturn(null);

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        assertThrows(
                InvalidObjectIdentifierException.class,
                () -> PersistentResource.loadRecord(Child.class, "1", goodScope));
    }

    @Test
    public void testLoadRecordForbidden() {
        NoReadEntity noRead = new NoReadEntity();
        noRead.setId(1);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        User goodUser = new User(1);

        when(tx.loadObject(eq(NoReadEntity.class), eq(1L), any(), any())).thenReturn(noRead);

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        assertThrows(
                ForbiddenAccessException.class,
                () -> PersistentResource.loadRecord(NoReadEntity.class, "1", goodScope));
    }

    @Test()
    public void testCreateObjectSuccess() {
        Parent parent = newParent(1);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        User goodUser = new User(1);

        when(tx.createNewObject(Parent.class)).thenReturn(parent);

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<Parent> created = PersistentResource.createObject(null, Parent.class, goodScope, Optional.of("uuid"));
        parent.setChildren(new HashSet<>());
        created.getRequestScope().getPermissionExecutor().executeCommitChecks();

        assertEquals(parent, created.getObject(),
                "The create function should return the requested parent object"
        );
        assertTrue(goodScope.isNewResource(parent));
    }

    @Test()
    public void testCreateMappedIdObjectSuccess() {
        final Job job = new Job();
        job.setTitle("day job");
        job.setParent(newParent(1));

        final DataStoreTransaction tx = mock(DataStoreTransaction.class);
        when(tx.createNewObject(Job.class)).thenReturn(job);

        final RequestScope goodScope = buildRequestScope(tx, new User(1));
        PersistentResource<Job> created = PersistentResource.createObject(null, Job.class, goodScope, Optional.empty());
        created.getRequestScope().getPermissionExecutor().executeCommitChecks();

        assertEquals("day job", created.getObject().getTitle(),
                "The create function should return the requested job object"
        );
        assertNull(created.getObject().getJobId(), "The create function should not override the ID");

        created = PersistentResource.createObject(null, Job.class, goodScope, Optional.of("1234"));
        created.getRequestScope().getPermissionExecutor().executeCommitChecks();

        assertEquals("day job", created.getObject().getTitle(),
                "The create function should return the requested job object"
        );
        assertNull(created.getObject().getJobId(), "The create function should not override the ID");
    }

    @Test
    public void testCreateObjectForbidden() {
        NoCreateEntity noCreate = new NoCreateEntity();
        noCreate.setId(1);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        User goodUser = new User(1);

        when(tx.createNewObject(NoCreateEntity.class)).thenReturn(noCreate);

        RequestScope goodScope = buildRequestScope(tx, goodUser);

        assertThrows(
                ForbiddenAccessException.class,
                () -> {
                    PersistentResource<NoCreateEntity> created = PersistentResource.createObject(null, NoCreateEntity.class, goodScope, Optional.of("1"));
                    created.getRequestScope().getPermissionExecutor().executeCommitChecks();
                }
        );
    }

    @Test
    public void testDeletePermissionCheckedOnInverseRelationship() {
        Left left = new Left();
        left.setId(1);
        Right right = new Right();
        right.setId(2);

        Set<Right> rights = Sets.newHashSet(right);
        left.setFieldLevelDelete(Sets.newHashSet(right));
        right.setAllowDeleteAtFieldLevel(Sets.newHashSet(left));

        //Bad User triggers the delete permission failure
        User badUser = new User(-1);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        when(tx.getRelation(any(), eq(left), eq("fieldLevelDelete"), any(), any(), any(), any())).thenReturn(rights);

        RequestScope badScope = buildRequestScope(tx, badUser);
        PersistentResource<Left> leftResource = new PersistentResource<>(left, null, badScope.getUUIDFor(left), badScope);

        assertTrue(leftResource.clearRelation("fieldLevelDelete"));
        assertEquals(0, leftResource.getObject().getFieldLevelDelete().size());
    }


    @Test
    public void testUpdatePermissionCheckedOnInverseRelationship() {
        Left left = new Left();
        left.setId(1);
        Right right = new Right();

        Set<Right> rights = Sets.newHashSet(right);
        left.setNoInverseUpdate(rights);
        right.setNoUpdate(Sets.newHashSet(left));

        List<Resource> empty = new ArrayList<>();
        Relationship ids = new Relationship(null, new Data<>(empty));

        User goodUser = new User(1);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        when(tx.getRelation(any(), eq(left), eq("noInverseUpdate"), any(), any(), any(), any())).thenReturn(rights);

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<Left> leftResource = new PersistentResource<>(left, null, goodScope.getUUIDFor(left), goodScope);

        assertThrows(
                ForbiddenAccessException.class,
                () -> leftResource.updateRelation("noInverseUpdate", ids.toPersistentResources(goodScope)));
        // Modifications have a deferred check component:
        leftResource.getRequestScope().getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void testFieldLevelAudit() throws Exception {
        Child child = newChild(5);

        Parent parent = newParent(7);

        User goodUser = new User(1);
        TestAuditLogger logger = new TestAuditLogger();
        RequestScope requestScope = getUserScope(goodUser, logger);
        PersistentResource<Parent> parentResource = new PersistentResource<>(parent, null, requestScope.getUUIDFor(parent), requestScope);
        PersistentResource<Child> childResource = new PersistentResource<>(child, parentResource, requestScope.getUUIDFor(child), requestScope);

        childResource.auditField(new ChangeSpec(childResource, "name", parent, null));

        assertEquals(1, logger.getMessages().size(), "One message should be logged");

        LogMessage message = logger.getMessages().get(0);
        assertEquals("UPDATE Child 5 Parent 7", message.getMessage(), "Logging template should match");

        assertEquals(1, message.getOperationCode(), "Operation code should match");
    }

    @Test
    public void testClassLevelAudit() throws Exception {
        Child child = newChild(5);
        Parent parent = newParent(7);

        User goodUser = new User(1);
        TestAuditLogger logger = new TestAuditLogger();
        RequestScope requestScope = getUserScope(goodUser, logger);
        PersistentResource<Parent> parentResource = new PersistentResource<>(
                parent, null, requestScope.getUUIDFor(parent), requestScope);
        PersistentResource<Child> childResource = new PersistentResource<>(
                child, parentResource, requestScope.getUUIDFor(child), requestScope);

        childResource.auditClass(Audit.Action.CREATE, new ChangeSpec(childResource, null, null, childResource.getObject()));

        assertEquals(1, logger.getMessages().size(), "One message should be logged");

        LogMessage message = logger.getMessages().get(0);
        assertEquals("CREATE Child 5 Parent 7", message.getMessage(), "Logging template should match");

        assertEquals(0, message.getOperationCode(), "Operation code should match");
    }

    @Test
    public void testOwningRelationshipInverseUpdates() {
        Parent parent = newParent(1);
        Child child = newChild(2);

        User goodUser = new User(1);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        when(tx.getRelation(any(), eq(parent), eq("children"), any(), any(), any(), any())).thenReturn(parent.getChildren());

        RequestScope goodScope = buildRequestScope(tx, goodUser);

        PersistentResource<Parent> parentResource = new PersistentResource<>(parent, null, goodScope.getUUIDFor(parent), goodScope);
        PersistentResource<Child> childResource = new PersistentResource<>(child, parentResource, goodScope.getUUIDFor(child), goodScope);

        parentResource.addRelation("children", childResource);

        goodScope.saveOrCreateObjects();
        goodScope.getDirtyResources().clear();
        verify(tx, times(1)).save(parent, goodScope);
        verify(tx, times(1)).save(child, goodScope);

        assertEquals(1, parent.getChildren().size(), "The owning relationship should be updated");
        assertTrue(parent.getChildren().contains(child), "The owning relationship should be updated");

        assertEquals(1, child.getParents().size(), "The non-owning relationship should also be updated");
        assertTrue(child.getParents().contains(parent), "The non-owning relationship should also be updated");

        reset(tx);
        when(tx.getRelation(any(), eq(parent), eq("children"), any(), any(), any(), any())).thenReturn(parent.getChildren());

        parentResource.clearRelation("children");

        goodScope.saveOrCreateObjects();
        verify(tx, times(1)).save(parent, goodScope);
        verify(tx, times(1)).save(child, goodScope);

        assertEquals(0, parent.getChildren().size(), "The owning relationship should be updated");
        assertEquals(0, child.getParents().size(), "The non-owning relationship should also be updated");
    }

    @Test
    public void testIsIdGenerated() {

        PersistentResource<Child> generated = new PersistentResource<>(new Child(), null, "1", goodUserScope);

        assertTrue(generated.isIdGenerated(),
                "isIdGenerated returns true when ID field has the GeneratedValue annotation");

        PersistentResource<NoCreateEntity> notGenerated = new PersistentResource<>(new NoCreateEntity(), null, "1", goodUserScope);

        assertFalse(notGenerated.isIdGenerated(),
                "isIdGenerated returns false when ID field does not have the GeneratedValue annotation");
    }

    @Test
    public void testSharePermissionErrorOnUpdateSingularRelationship() {
        example.User userModel = new example.User();
        userModel.setId(1);

        NoShareEntity noShare = new NoShareEntity();
        noShare.setId(1);

        List<Resource> idList = new ArrayList<>();
        idList.add(new ResourceIdentifier("noshare", "1").castToResource());
        Relationship ids = new Relationship(null, new Data<>(idList));

        User goodUser = new User(1);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        when(tx.loadObject(eq(NoShareEntity.class), eq(1L), any(), any())).thenReturn(noShare);

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<example.User> userResource = new PersistentResource<>(userModel, null, goodScope.getUUIDFor(userModel), goodScope);

        assertThrows(
                ForbiddenAccessException.class,
                () -> userResource.updateRelation("noShare", ids.toPersistentResources(goodScope)));
    }

    @Test
    public void testSharePermissionErrorOnUpdateRelationshipPackageLevel() {
        ContainerWithPackageShare containerWithPackageShare = new ContainerWithPackageShare();

        UnshareableWithEntityUnshare unshareableWithEntityUnshare = new UnshareableWithEntityUnshare();
        unshareableWithEntityUnshare.setContainerWithPackageShare(containerWithPackageShare);

        List<Resource> unShareableList = new ArrayList<>();
        unShareableList.add(new ResourceIdentifier("unshareableWithEntityUnshare", "1").castToResource());
        Relationship unShareales = new Relationship(null, new Data<>(unShareableList));

        User goodUser = new User(1);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        when(tx.loadObject(eq(UnshareableWithEntityUnshare.class), eq(1L), any(), any())).thenReturn(unshareableWithEntityUnshare);

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<ContainerWithPackageShare> containerResource = new PersistentResource<>(containerWithPackageShare, null, goodScope.getUUIDFor(containerWithPackageShare), goodScope);

        assertThrows(
                ForbiddenAccessException.class,
                () -> containerResource.updateRelation(
                        "unshareableWithEntityUnshares", unShareales.toPersistentResources(goodScope)));
    }

    @Test
    public void testSharePermissionSuccessOnUpdateManyRelationshipPackageLevel() {
        ContainerWithPackageShare containerWithPackageShare = new ContainerWithPackageShare();

        ShareableWithPackageShare shareableWithPackageShare = new ShareableWithPackageShare();
        shareableWithPackageShare.setContainerWithPackageShare(containerWithPackageShare);

        List<Resource> shareableList = new ArrayList<>();
        shareableList.add(new ResourceIdentifier("shareableWithPackageShare", "1").castToResource());
        Relationship shareables = new Relationship(null, new Data<>(shareableList));

        User goodUser = new User(1);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        when(tx.loadObject(eq(ShareableWithPackageShare.class), eq(1L), any(), any())).thenReturn(shareableWithPackageShare);

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<ContainerWithPackageShare> containerResource = new PersistentResource<>(containerWithPackageShare, null, goodScope.getUUIDFor(containerWithPackageShare), goodScope);

        containerResource.updateRelation("shareableWithPackageShares", shareables.toPersistentResources(goodScope));

        assertEquals(1, containerWithPackageShare.getShareableWithPackageShares().size());
        assertTrue(containerWithPackageShare.getShareableWithPackageShares().contains(shareableWithPackageShare));
    }

    @Test
    public void testSharePermissionErrorOnUpdateManyRelationship() {
        example.User userModel = new example.User();
        userModel.setId(1);

        NoShareEntity noShare1 = new NoShareEntity();
        noShare1.setId(1);
        NoShareEntity noShare2 = new NoShareEntity();
        noShare2.setId(2);

        List<Resource> idList = new ArrayList<>();
        idList.add(new ResourceIdentifier("noshare", "1").castToResource());
        idList.add(new ResourceIdentifier("noshare", "2").castToResource());
        Relationship ids = new Relationship(null, new Data<>(idList));

        User goodUser = new User(1);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        when(tx.loadObject(eq(NoShareEntity.class), eq(1L), any(), any())).thenReturn(noShare1);
        when(tx.loadObject(eq(NoShareEntity.class), eq(2L), any(), any())).thenReturn(noShare2);

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<example.User> userResource = new PersistentResource<>(userModel, null, goodScope.getUUIDFor(userModel), goodScope);

        assertThrows(
                ForbiddenAccessException.class,
                () -> userResource.updateRelation("noShares", ids.toPersistentResources(goodScope)));
    }

    @Test
    public void testSharePermissionSuccessOnUpdateManyRelationship() {
        example.User userModel = new example.User();
        userModel.setId(1);

        NoShareEntity noShare1 = new NoShareEntity();
        noShare1.setId(1);
        NoShareEntity noShare2 = new NoShareEntity();
        noShare2.setId(2);
        HashSet<NoShareEntity> noshares = Sets.newHashSet(noShare1, noShare2);

        /* The no shares already exist so no exception should be thrown */
        userModel.setNoShares(noshares);

        List<Resource> idList = new ArrayList<>();
        idList.add(new ResourceIdentifier("noshare", "1").castToResource());
        Relationship ids = new Relationship(null, new Data<>(idList));

        User goodUser = new User(1);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        when(tx.loadObject(eq(NoShareEntity.class), eq(1L), any(), any())).thenReturn(noShare1);
        when(tx.getRelation(any(), eq(userModel), eq("noShares"), any(), any(), any(), any())).thenReturn(noshares);

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<example.User> userResource = new PersistentResource<>(userModel, null, goodScope.getUUIDFor(userModel), goodScope);

        boolean returnVal = userResource.updateRelation("noShares", ids.toPersistentResources(goodScope));

        assertTrue(returnVal);
        assertEquals(1, userModel.getNoShares().size());
        assertTrue(userModel.getNoShares().contains(noShare1));
    }

    @Test
    public void testSharePermissionSuccessOnUpdateSingularRelationship() {
        example.User userModel = new example.User();
        userModel.setId(1);

        NoShareEntity noShare = new NoShareEntity();

        /* The noshare already exists so no exception should be thrown */
        userModel.setNoShare(noShare);

        List<Resource> idList = new ArrayList<>();
        idList.add(new ResourceIdentifier("noshare", "1").castToResource());
        Relationship ids = new Relationship(null, new Data<>(idList));

        User goodUser = new User(1);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);

        when(tx.getRelation(any(), eq(userModel), eq("noShare"), any(), any(), any(), any())).thenReturn(noShare);
        when(tx.loadObject(eq(NoShareEntity.class), eq(1L), any(), any())).thenReturn(noShare);

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<example.User> userResource = new PersistentResource<>(userModel, null, goodScope.getUUIDFor(userModel), goodScope);

        boolean returnVal = userResource.updateRelation("noShare", ids.toPersistentResources(goodScope));

        assertFalse(returnVal);
        assertEquals(noShare, userModel.getNoShare());
    }

    @Test
    public void testSharePermissionSuccessOnClearSingularRelationship() {
        example.User userModel = new example.User();
        userModel.setId(1);

        NoShareEntity noShare = new NoShareEntity();

        /* The noshare already exists so no exception should be thrown */
        userModel.setNoShare(noShare);

        List<Resource> empty = new ArrayList<>();
        Relationship ids = new Relationship(null, new Data<>(empty));

        User goodUser = new User(1);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        when(tx.getRelation(any(), eq(userModel), eq("noShare"), any(), any(), any(), any())).thenReturn(noShare);

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<example.User> userResource = new PersistentResource<>(userModel, null, goodScope.getUUIDFor(userModel), goodScope);

        boolean returnVal = userResource.updateRelation("noShare", ids.toPersistentResources(goodScope));

        assertTrue(returnVal);
        assertNull(userModel.getNoShare());
    }

    @Test
    public void testCollectionChangeSpecType() {
        Function<String, BiFunction<ChangeSpec, BiFunction<Collection, Collection, Boolean>, Boolean>> collectionCheck =
                (fieldName) -> (spec, condFn) -> {
                    if (!fieldName.equals(spec.getFieldName())) {
                        fail("Should not reach here");
                        throw new IllegalStateException();
                    }
                    return condFn.apply((Collection) spec.getOriginal(), (Collection) spec.getModified());
                };

        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        // Ensure that change specs coming from collections work properly

        ChangeSpecModel csModel = new ChangeSpecModel((spec) -> collectionCheck
                .apply("testColl")
                .apply(spec, (original, modified) -> original == null && modified.equals(Arrays.asList("a", "b", "c"))));

        PersistentResource<ChangeSpecModel> model = bootstrapPersistentResource(csModel, tx);

        when(tx.getRelation(any(), eq(model.obj), eq("otherKids"), any(), any(), any(), any())).thenReturn(new HashSet<>());

        /* Attributes */
        // Set new data from null
        assertTrue(model.updateAttribute("testColl", Arrays.asList("a", "b", "c")));

        // Set data to empty
        model.getObject().checkFunction = (spec) -> collectionCheck.apply("testColl").apply(spec,
                (original, modified) -> original.equals(Arrays.asList("a", "b", "c")) && modified.isEmpty());
        assertTrue(model.updateAttribute("testColl", Lists.newArrayList()));

        model.getObject().checkFunction = (spec) -> collectionCheck.apply("testColl")
                .apply(spec, (original, modified) -> original.isEmpty() && modified.equals(Arrays.asList("final", "List")));
        // / Overwrite attribute data
        assertTrue(model.updateAttribute("testColl", Arrays.asList("final", "List")));

        /* ToMany relationships */
        // Learn about the other kids
        model.getObject().checkFunction = (spec) -> collectionCheck.apply("otherKids").apply(spec, (original, modified) -> CollectionUtils.isEmpty(original) && modified.size() == 1 && modified.contains(new ChangeSpecChild(1)));

        ChangeSpecChild child1 = new ChangeSpecChild(1);
        assertTrue(model.updateRelation("otherKids", Sets.newHashSet(bootstrapPersistentResource(child1))));

        // Add individual
        model.getObject().checkFunction = (spec) -> collectionCheck.apply("otherKids").apply(spec, (original, modified) -> original.equals(Collections.singletonList(new ChangeSpecChild(1))) && modified.size() == 2 && modified.contains(new ChangeSpecChild(1)) && modified.contains(new ChangeSpecChild(2)));

        ChangeSpecChild child2 = new ChangeSpecChild(2);
        model.addRelation("otherKids", bootstrapPersistentResource(child2));

        model.getObject().checkFunction = (spec) -> collectionCheck.apply("otherKids").apply(spec, (original, modified) -> original.size() == 2 && original.contains(new ChangeSpecChild(1)) && original.contains(new ChangeSpecChild(2)) && modified.size() == 3 && modified.contains(new ChangeSpecChild(1)) && modified.contains(new ChangeSpecChild(2)) && modified.contains(new ChangeSpecChild(3)));

        ChangeSpecChild child3 = new ChangeSpecChild(3);
        model.addRelation("otherKids", bootstrapPersistentResource(child3));

        // Remove one
        model.getObject().checkFunction = (spec) -> collectionCheck.apply("otherKids").apply(spec, (original, modified) -> original.size() == 3 && original.contains(new ChangeSpecChild(1)) && original.contains(new ChangeSpecChild(2)) && original.contains(new ChangeSpecChild(3)) && modified.size() == 2 && modified.contains(new ChangeSpecChild(1)) && modified.contains(new ChangeSpecChild(3)));
        model.removeRelation("otherKids", bootstrapPersistentResource(child2));

        when(tx.getRelation(any(), eq(model.obj), eq("otherKids"), any(), any(), any(), any())).thenReturn(Sets.newHashSet(child1, child3));
        // Clear the rest
        model.getObject().checkFunction = (spec) -> collectionCheck.apply("otherKids").apply(spec, (original, modified)
                -> original.size() <= 2 && modified.size() < original.size());
        model.clearRelation("otherKids");
    }

    @Test
    public void testAttrChangeSpecType() {
        BiFunction<ChangeSpec, BiFunction<String, String, Boolean>, Boolean> attrCheck = (spec, checkFn) -> {
            if (!(spec.getModified() instanceof String) && spec.getModified() != null) {
                fail("Should not reach here");
                return false;
            }
            if (!"testAttr".equals(spec.getFieldName())) {
                fail("Should not reach here");
                return false;
            }
            return checkFn.apply((String) spec.getOriginal(), (String) spec.getModified());
        };

        PersistentResource<ChangeSpecModel> model = bootstrapPersistentResource(new ChangeSpecModel((spec) -> attrCheck.apply(spec, (original, modified) -> (original == null) && "abc".equals(modified))));
        assertTrue(model.updateAttribute("testAttr", "abc"));

        model.getObject().checkFunction = (spec) -> attrCheck.apply(spec, (original, modified) -> "abc".equals(original) && "replace".equals(modified));
        assertTrue(model.updateAttribute("testAttr", "replace"));

        model.getObject().checkFunction = (spec) -> attrCheck.apply(spec, (original, modified) -> "replace".equals(original) && modified == null);
        assertTrue(model.updateAttribute("testAttr", null));
    }

    @Test
    public void testRelationChangeSpecType() {
            BiFunction<ChangeSpec, BiFunction<ChangeSpecChild, ChangeSpecChild, Boolean>, Boolean> relCheck = (spec, checkFn) -> {
                if (!(spec.getModified() instanceof ChangeSpecChild) && spec.getModified() != null) {
                    fail("Should not reach here");
                    return false;
                }
                if (!"child".equals(spec.getFieldName())) {
                    fail("Should not reach here");
                    return false;
                }
                return checkFn.apply((ChangeSpecChild) spec.getOriginal(), (ChangeSpecChild) spec.getModified());
            };
            DataStoreTransaction tx = mock(DataStoreTransaction.class);

            PersistentResource<ChangeSpecModel> model = bootstrapPersistentResource(new ChangeSpecModel((spec)
                    -> relCheck.apply(spec, (original, modified)
                    -> (original == null) && new ChangeSpecChild(1).equals(modified))), tx);

            when(tx.getRelation(any(), eq(model.obj), eq("child"), any(), any(), any(), any())).thenReturn(null);

            ChangeSpecChild child1 = new ChangeSpecChild(1);
            assertTrue(model.updateRelation("child", Sets.newHashSet(bootstrapPersistentResource(child1, tx))));
            when(tx.getRelation(any(), eq(model.obj), eq("child"), any(), any(), any(), any())).thenReturn(child1);

            model.getObject().checkFunction = (spec) -> relCheck.apply(spec, (original, modified) -> new ChangeSpecChild(1).equals(original) && new ChangeSpecChild(2).equals(modified));

            ChangeSpecChild child2 = new ChangeSpecChild(2);
            assertTrue(model.updateRelation("child", Sets.newHashSet(bootstrapPersistentResource(child2, tx))));

            when(tx.getRelation(any(), eq(model.obj), eq("child"), any(), any(), any(), any())).thenReturn(child2);

            model.getObject().checkFunction = (spec) -> relCheck.apply(spec, (original, modified) -> new ChangeSpecChild(2).equals(original) && modified == null);
            assertTrue(model.updateRelation("child", null));
    }

    @Test
    public void testPatchRequestScope() {
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        PatchRequestScope parentScope =
                new PatchRequestScope(null, tx, new User(1), elideSettings);
        PatchRequestScope scope = new PatchRequestScope(
                parentScope.getPath(), parentScope.getJsonApiDocument(), parentScope);
        // verify wrap works
        assertEquals(parentScope.isUseFilterExpressions(), scope.isUseFilterExpressions());
        assertEquals(parentScope.getSorting(), scope.getSorting());
        assertEquals(parentScope.getUpdateStatusCode(), scope.getUpdateStatusCode());
        assertEquals(parentScope.getObjectEntityCache(), scope.getObjectEntityCache());

        Parent parent = newParent(7);

        PersistentResource<Parent> parentResource = new PersistentResource<>(parent, null, "1", scope);
        parentResource.updateAttribute("firstName", "foobar");

        verify(tx, times(1)).setAttribute(parent, "firstName", "foobar", scope);
    }

    @Test
    public void testFilterExpressionByType() {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter[author.name][infix]",
                "Hemingway"
        );

        RequestScope scope = buildRequestScope("/", mock(DataStoreTransaction.class), new User(1), queryParams);

        Optional<FilterExpression> filter = scope.getLoadFilterExpression(Author.class);
        FilterPredicate predicate = (FilterPredicate) filter.get();
        assertEquals("name", predicate.getField());
        assertEquals("name", predicate.getFieldPath());
        assertEquals(Operator.INFIX, predicate.getOperator());
        assertEquals(Arrays.asList("Hemingway"), predicate.getValues());
        assertEquals("[Author].name", predicate.getPath().toString());
    }

    @Test
    public void testSparseFields() {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add("fields[author]", "name");

        RequestScope scope = buildRequestScope("/", mock(DataStoreTransaction.class), new User(1), queryParams);
        Map<String, Set<String>> expected = ImmutableMap.of("author", ImmutableSet.of("name"));
        assertEquals(expected, scope.getSparseFields());
        assertEquals(10, scope.getPagination().getLimit());
        assertEquals(0, scope.getPagination().getPageTotals());
    }

    @Test
    public void testEqualsAndHashcode() {
        Child childWithId = newChild(1);
        Child childWithoutId = newChild(0);

        PersistentResource resourceWithId = new PersistentResource<>(childWithId, null, goodUserScope.getUUIDFor(childWithId), goodUserScope);
        PersistentResource resourceWithDifferentId = new PersistentResource<>(childWithoutId, null, goodUserScope.getUUIDFor(childWithoutId), goodUserScope);
        PersistentResource resourceWithUUID = new PersistentResource<>(childWithoutId, null, "abc", goodUserScope);
        PersistentResource resourceWithIdAndUUID = new PersistentResource<>(childWithId, null, "abc", goodUserScope);

        assertNotEquals(resourceWithUUID, resourceWithId);
        assertNotEquals(resourceWithId, resourceWithUUID);
        assertNotEquals(resourceWithId.hashCode(), resourceWithUUID.hashCode(), "Hashcodes were equal...");

        assertEquals(resourceWithIdAndUUID, resourceWithId);
        assertEquals(resourceWithId, resourceWithIdAndUUID);
        assertEquals(resourceWithIdAndUUID.hashCode(), resourceWithId.hashCode());

        // Hashcode's should only ever look at UUID's if no real ID is present (i.e. object id is null or 0)
        assertNotEquals(resourceWithIdAndUUID.hashCode(), resourceWithUUID.hashCode());

        assertNotEquals(resourceWithDifferentId.hashCode(), resourceWithId.hashCode());
        assertNotEquals(resourceWithDifferentId, resourceWithId);
        assertNotEquals(resourceWithId, resourceWithDifferentId);
    }
}
