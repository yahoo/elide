/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import static com.yahoo.elide.annotation.LifeCycleHookBinding.Operation.UPDATE;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.TransactionPhase.PRESECURITY;
import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import com.yahoo.elide.core.audit.LogMessage;
import com.yahoo.elide.core.audit.TestAuditLogger;
import com.yahoo.elide.core.datastore.DataStoreIterableBuilder;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.exceptions.ForbiddenAccessException;
import com.yahoo.elide.core.exceptions.InvalidAttributeException;
import com.yahoo.elide.core.exceptions.InvalidObjectIdentifierException;
import com.yahoo.elide.core.exceptions.InvalidValueException;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.predicates.FilterPredicate;
import com.yahoo.elide.core.lifecycle.CRUDEvent;
import com.yahoo.elide.core.request.Attribute;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.security.ChangeSpec;
import com.yahoo.elide.core.security.TestUser;
import com.yahoo.elide.core.security.User;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.jsonapi.extensions.PatchRequestScope;
import com.yahoo.elide.jsonapi.models.Data;
import com.yahoo.elide.jsonapi.models.Relationship;
import com.yahoo.elide.jsonapi.models.Resource;
import com.yahoo.elide.jsonapi.models.ResourceIdentifier;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import example.Address;
import example.Author;
import example.Book;
import example.Child;
import example.Color;
import example.Company;
import example.ComputedBean;
import example.FirstClassFields;
import example.FunWithPermissions;
import example.GeoLocation;
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
import example.Price;
import example.Right;
import example.Shape;
import example.nontransferable.ContainerWithPackageShare;
import example.nontransferable.NoTransferBiDirectional;
import example.nontransferable.ShareableWithPackageShare;
import example.nontransferable.StrictNoTransfer;
import example.nontransferable.Untransferable;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.ArgumentCaptor;

import io.reactivex.Observable;
import nocreate.NoCreateEntity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Currency;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Test PersistentResource.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PersistentResourceTest extends PersistenceResourceTestSetup {

    private final User goodUser = new TestUser("1");
    private final User badUser = new TestUser("-1");

    private final DataStoreTransaction tx = mock(DataStoreTransaction.class);

    @BeforeEach
    public void beforeTest() {
        reset(tx);
    }

    @Test
    public void testUpdateToOneRelationHookInAddRelation() {
        FunWithPermissions fun = new FunWithPermissions();
        Child child = newChild(1);

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, "3", goodScope);
        PersistentResource<Child> childResource = new PersistentResource<>(child, "1", goodScope);
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

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, "3", goodScope);
        PersistentResource<Child> child2Resource = new PersistentResource<>(child2, "1", goodScope);
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

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, "3", goodScope);
        PersistentResource<Child> childResource = new PersistentResource<>(child, "1", goodScope);
        funResource.removeRelation("relation3", childResource);

        verify(tx, times(1)).updateToOneRelation(eq(tx), eq(fun), any(), any(), eq(goodScope));
        verify(tx, never()).updateToOneRelation(eq(tx), eq(child), any(), any(), eq(goodScope));
    }

    @Test
    public void testUpdateToOneRelationHookInClearRelation() {
        FunWithPermissions fun = new FunWithPermissions();
        Child child1 = newChild(1);
        fun.setRelation3(child1);

        when(tx.getToOneRelation(any(), eq(fun), any(), any())).thenReturn(child1);

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, "3", goodScope);
        funResource.clearRelation("relation3");

        verify(tx, times(1)).updateToOneRelation(eq(tx), eq(fun), any(), any(), eq(goodScope));
        verify(tx, never()).updateToOneRelation(eq(tx), eq(child1), any(), any(), eq(goodScope));
    }

    @Test
    public void testUpdateToManyRelationHookInAddRelationBidirection() {
        Parent parent = new Parent();
        Child child = newChild(1);

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<Parent> parentResource = new PersistentResource<>(parent, "3", goodScope);
        PersistentResource<Child> childResource = new PersistentResource<>(child, "1", goodScope);
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

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<Parent> parentResource = new PersistentResource<>(parent, "3", goodScope);
        PersistentResource<Child> childResource = new PersistentResource<>(child, "1", goodScope);
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

        when(tx.getToManyRelation(any(), eq(parent), any(), any()))
                .thenReturn(new DataStoreIterableBuilder(children).build());

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<Parent> parentResource = new PersistentResource<>(parent, "3", goodScope);
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

        when(tx.getToManyRelation(any(), eq(parent), any(), any()))
                .thenReturn(new DataStoreIterableBuilder(children).build());

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<Parent> parentResource = new PersistentResource<>(parent, "3", goodScope);
        PersistentResource<Child> childResource1 = new PersistentResource<>(child1, "1", goodScope);
        PersistentResource<Child> childResource3 = new PersistentResource<>(child3, "1", goodScope);
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
        ArgumentCaptor<Attribute> attributeArgument = ArgumentCaptor.forClass(Attribute.class);

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<Parent> parentResource = new PersistentResource<>(parent, "1", goodScope);
        parentResource.updateAttribute("firstName", "foobar");

        verify(tx, times(1)).setAttribute(eq(parent), attributeArgument.capture(), eq(goodScope));

        assertEquals(attributeArgument.getValue().getName(), "firstName");
        assertEquals(attributeArgument.getValue().getArguments().iterator().next().getValue(), "foobar");
    }

    @Test
    public void testGetRelationships() {
        FunWithPermissions fun = new FunWithPermissions();
        fun.setRelation1(Sets.newHashSet());
        fun.setRelation2(Sets.newHashSet());
        fun.setRelation3(null);

        RequestScope scope = new TestRequestScope(tx, goodUser, dictionary);

        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, "3", scope);

        Map<String, Relationship> relationships = funResource.getRelationships();

        assertEquals(5, relationships.size(), "All relationships should be returned.");
        assertTrue(relationships.containsKey("relation1"), "relation1 should be present");
        assertTrue(relationships.containsKey("relation2"), "relation2 should be present");
        assertTrue(relationships.containsKey("relation3"), "relation3 should be present");
        assertTrue(relationships.containsKey("relation4"), "relation4 should be present");
        assertTrue(relationships.containsKey("relation5"), "relation5 should be present");

        scope = new TestRequestScope(tx, badUser, dictionary);

        PersistentResource<FunWithPermissions> funResourceWithBadScope = new PersistentResource<>(fun, "3", scope);
        relationships = funResourceWithBadScope.getRelationships();

        assertEquals(0, relationships.size(), "All relationships should be filtered out");
    }

    @Test
    public void testNoCreate() {
        assertNotNull(dictionary);
        NoCreateEntity noCreate = new NoCreateEntity();

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        when(tx.createNewObject(ClassType.of(NoCreateEntity.class), goodScope)).thenReturn(noCreate);

        assertThrows(
                ForbiddenAccessException.class,
                () -> PersistentResource.createObject(
                        ClassType.of(NoCreateEntity.class), goodScope, Optional.of("1"))); // should throw here
    }

    @Test
    public void testGetAttributes() {
        FunWithPermissions fun = new FunWithPermissions();
        fun.setField3("Foobar");
        fun.setField1("blah");
        fun.setField2(null);
        fun.setField4("bar");

        when(tx.getAttribute(any(), any(), any())).thenCallRealMethod();

        RequestScope scope = new TestRequestScope(tx, goodUser, dictionary);
        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, "3", scope);

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

        RequestScope badUserScope = new TestRequestScope(tx, badUser, dictionary);
        PersistentResource<FunWithPermissions> funResourceBad = new PersistentResource<>(fun, "3", badUserScope);

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
            RequestScope scope = new TestRequestScope(tx, goodUser, dictionary);
            PersistentResource<Child> child1Resource = new PersistentResource<>(child1, "1", scope);
            PersistentResource<Child> child2Resource = new PersistentResource<>(child2, "-2", scope);
            PersistentResource<Child> child3Resource = new PersistentResource<>(child3, "3", scope);
            PersistentResource<Child> child4Resource = new PersistentResource<>(child4, "-4", scope);

            Observable<PersistentResource> resources =
                    Observable.fromArray(child1Resource, child2Resource, child3Resource, child4Resource);

            Set<PersistentResource> results =
                    PersistentResource.filter(
                            ReadPermission.class,
                            Optional.empty(), ALL_FIELDS, resources).toList(LinkedHashSet::new).blockingGet();

            assertEquals(2, results.size(), "Only a subset of the children are readable");
            assertTrue(results.contains(child1Resource), "Readable children includes children with positive IDs");
            assertTrue(results.contains(child3Resource), "Readable children includes children with positive IDs");
        }

        {
            RequestScope scope = new TestRequestScope(tx, badUser, dictionary);
            PersistentResource<Child> child1Resource = new PersistentResource<>(child1, "1", scope);
            PersistentResource<Child> child2Resource = new PersistentResource<>(child2, "-2", scope);
            PersistentResource<Child> child3Resource = new PersistentResource<>(child3, "3", scope);
            PersistentResource<Child> child4Resource = new PersistentResource<>(child4, "-4", scope);

            Observable<PersistentResource> resources =
                    Observable.fromArray(child1Resource, child2Resource, child3Resource, child4Resource);

            Set<PersistentResource> results = PersistentResource.filter(ReadPermission.class,
                    Optional.empty(), ALL_FIELDS, resources).toList(LinkedHashSet::new).blockingGet();

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
        assertEquals(
                fun.getRelation1().size(),
                1,
                "setValue should set the relation with the correct number of elements");
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

        RequestScope scope = new TestRequestScope(tx, goodUser, dictionary);
        PersistentResource<Left> leftResource = new PersistentResource<>(left, "3", scope);

        leftResource.deleteInverseRelation("one2one", right);

        assertNull(right.getOne2one(), "The one-2-one inverse relationship should have been unset");
        assertEquals(right, left.getOne2one(), "The owning relationship should NOT have been unset");

        Child child = new Child();
        Parent parent = new Parent();
        child.setParents(Sets.newHashSet(parent));
        parent.setChildren(Sets.newHashSet(child));
        parent.setSpouses(Sets.newHashSet());

        scope = new TestRequestScope(tx, goodUser, dictionary);
        PersistentResource<Child> childResource = new PersistentResource<>(child, "4", scope);

        childResource.deleteInverseRelation("parents", parent);

        assertEquals(parent.getChildren().size(), 0, "The many-2-many inverse collection should have been cleared.");
        assertTrue(child.getParents().contains(parent), "The owning relationship should NOT have been touched");
    }

    @Test
    public void testAddBidirectionalRelation() {
        Left left = new Left();
        Right right = new Right();

        RequestScope scope = new TestRequestScope(tx, goodUser, dictionary);
        PersistentResource<Left> leftResource = new PersistentResource<>(left, "3", scope);

        leftResource.addInverseRelation("one2one", right);

        assertEquals(left, right.getOne2one(), "The one-2-one inverse relationship should have been updated.");

        Child child = new Child();
        Parent parent = new Parent();
        child.setParents(Sets.newHashSet());
        parent.setChildren(Sets.newHashSet());
        parent.setSpouses(Sets.newHashSet());

        scope = new TestRequestScope(tx, goodUser, dictionary);
        PersistentResource<Child> childResource = new PersistentResource<>(child, "4", scope);

        childResource.addInverseRelation("parents", parent);

        assertEquals(
                1,
                parent.getChildren().size(),
                "The many-2-many inverse relationship should have been updated");
        assertTrue(
                parent.getChildren().contains(child),
                "The many-2-many inverse relationship should have been updated");
    }

    @Test
    public void testSuccessfulOneToOneRelationshipAdd() throws Exception {
        Left left = new Left();
        Right right = new Right();
        left.setId(2);
        right.setId(3);

        RequestScope goodScope = buildRequestScope(tx, goodUser);

        PersistentResource<Left> leftResource = new PersistentResource<>(left, "2", goodScope);

        Relationship ids = new Relationship(null, new Data<>(new ResourceIdentifier("right", "3").castToResource()));

        when(tx.loadObject(any(), eq(3L), any())).thenReturn(right);
        boolean updated = leftResource.updateRelation("one2one", ids.toPersistentResources(goodScope));
        goodScope.saveOrCreateObjects();
        verify(tx, times(1)).save(left, goodScope);
        verify(tx, times(1)).save(right, goodScope);
        verify(tx, times(1)).getToOneRelation(tx, left, getRelationship(ClassType.of(Right.class), "one2one"), goodScope);

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
        Left left = new Left();
        left.setId(2);

        RequestScope goodScope = buildRequestScope(tx, goodUser);

        PersistentResource<Left> leftResource = new PersistentResource<>(left, "2", goodScope);

        Relationship ids = new Relationship(null, new Data<>(new Resource("right", null, null, null, null, null)));

        InvalidObjectIdentifierException thrown = assertThrows(
                InvalidObjectIdentifierException.class,
                () -> leftResource.updateRelation("one2one", ids.toPersistentResources(goodScope)));

        assertEquals("Unknown identifier null for right", thrown.getMessage());
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

        when(tx.getToManyRelation(any(), eq(parent), any(), any())).thenReturn(new DataStoreIterableBuilder(allChildren).build());

        PersistentResource<Parent> parentResource = new PersistentResource<>(parent, "1", goodScope);

        //Requested = (3,6)
        List<Resource> idList = new ArrayList<>();
        idList.add(new ResourceIdentifier("child", "3").castToResource());
        idList.add(new ResourceIdentifier("child", "6").castToResource());
        Relationship ids = new Relationship(null, new Data<>(idList));

        when(tx.loadObject(any(), eq(2L), any())).thenReturn(child2);
        when(tx.loadObject(any(), eq(3L), any())).thenReturn(child3);
        when(tx.loadObject(any(), eq(-4L), any())).thenReturn(child4);
        when(tx.loadObject(any(), eq(-5L), any())).thenReturn(child5);
        when(tx.loadObject(any(), eq(6L), any())).thenReturn(child6);

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

    @Test
    /*
     * The following are ids for a hypothetical relationship.
     * GIVEN:
     * all (all the ids in the DB) = 1,2,3,4,5
     * mine (everything the current user has access to) = 1,2,3
     * requested (what the user wants to change to) = 1,2,3
     * THEN:
     * deleted (what gets removed from the DB) = nothing
     * final (what get stored in the relationship) = 1,2,3,4,5
     * BECAUSE:
     * notMine = all - mine
     * updated = (requested UNION mine) - (requested INTERSECT mine)
     * deleted = (mine - requested)
     * final = (notMine) UNION requested
     */
    public void testSuccessfulManyToManyRelationshipNoopUpdate() throws Exception {
        Parent parent = new Parent();
        RequestScope goodScope = buildRequestScope(tx, goodUser);

        Child child1 = newChild(1);
        Child child2 = newChild(2);
        Child child3 = newChild(3);
        Child child4 = newChild(-4); //Not accessible to goodUser
        Child child5 = newChild(-5); //Not accessible to goodUser

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

        when(tx.getToManyRelation(any(), eq(parent), any(), any())).thenReturn(new DataStoreIterableBuilder(allChildren).build());

        PersistentResource<Parent> parentResource = new PersistentResource<>(parent, "1", goodScope);

        //Requested = (1,2,3)
        List<Resource> idList = new ArrayList<>();
        idList.add(new ResourceIdentifier("child", "3").castToResource());
        idList.add(new ResourceIdentifier("child", "2").castToResource());
        idList.add(new ResourceIdentifier("child", "1").castToResource());
        Relationship ids = new Relationship(null, new Data<>(idList));

        when(tx.loadObject(any(), eq(1L), any())).thenReturn(child1);
        when(tx.loadObject(any(), eq(2L), any())).thenReturn(child2);
        when(tx.loadObject(any(), eq(3L), any())).thenReturn(child3);
        when(tx.loadObject(any(), eq(-4L), any())).thenReturn(child4);
        when(tx.loadObject(any(), eq(-5L), any())).thenReturn(child5);

        //Final set after operation = (1,2,3,4,5)
        Set<Child> expected = new HashSet<>();
        expected.add(child1);
        expected.add(child2);
        expected.add(child3);
        expected.add(child4);
        expected.add(child5);

        boolean updated = parentResource.updateRelation("children", ids.toPersistentResources(goodScope));

        goodScope.saveOrCreateObjects();
        verify(tx, never()).save(parent, goodScope);
        verify(tx, never()).save(child1, goodScope);
        verify(tx, never()).save(child2, goodScope);
        verify(tx, never()).save(child4, goodScope);
        verify(tx, never()).save(child5, goodScope);
        verify(tx, never()).save(child3, goodScope);

        assertFalse(updated, "Many-2-many relationship should not be updated.");
        assertTrue(parent.getChildren().containsAll(expected), "All expected members were updated");
        assertTrue(expected.containsAll(parent.getChildren()), "All expected members were updated");

        /*
         * No tests for reference integrity since the parent is the owner and
         * this is a many to many relationship.
         */
    }

    @Test
    /*
     * The following are ids for a hypothetical relationship.
     * GIVEN:
     * all (all the ids in the DB) = null
     * mine (everything the current user has access to) = null
     * requested (what the user wants to change to) = 1,2,3
     * THEN:
     * deleted (what gets removed from the DB) = nothing
     * final (what get stored in the relationship) = 1,2,3
     * BECAUSE:
     * notMine = all - mine
     * updated = (requested UNION mine) - (requested INTERSECT mine)
     * deleted = (mine - requested)
     * final = (notMine) UNION requested
     */
    public void testSuccessfulManyToManyRelationshipNullUpdate() throws Exception {
        Parent parent = new Parent();
        RequestScope goodScope = buildRequestScope(tx, goodUser);

        Child child1 = newChild(1);
        Child child2 = newChild(2);
        Child child3 = newChild(3);

        //All = null
        //Mine = null
        Set<Child> allChildren = new HashSet<>();
        allChildren.add(child1);
        allChildren.add(child2);
        allChildren.add(child3);
        parent.setChildren(null);
        parent.setSpouses(Sets.newHashSet());

        when(tx.getToManyRelation(any(), eq(parent), any(), any())).thenReturn(null);

        PersistentResource<Parent> parentResource = new PersistentResource<>(parent, "1", goodScope);

        //Requested = (1,2,3)
        List<Resource> idList = new ArrayList<>();
        idList.add(new ResourceIdentifier("child", "3").castToResource());
        idList.add(new ResourceIdentifier("child", "2").castToResource());
        idList.add(new ResourceIdentifier("child", "1").castToResource());
        Relationship ids = new Relationship(null, new Data<>(idList));

        when(tx.loadObject(any(), eq(1L), any())).thenReturn(child1);
        when(tx.loadObject(any(), eq(2L), any())).thenReturn(child2);
        when(tx.loadObject(any(), eq(3L), any())).thenReturn(child3);

        //Final set after operation = (1,2,3)
        Set<Child> expected = new HashSet<>();
        expected.add(child1);
        expected.add(child2);
        expected.add(child3);

        boolean updated = parentResource.updateRelation("children", ids.toPersistentResources(goodScope));

        goodScope.saveOrCreateObjects();
        verify(tx, times(1)).save(parent, goodScope);
        verify(tx, times(1)).save(child1, goodScope);
        verify(tx, times(1)).save(child2, goodScope);
        verify(tx, times(1)).save(child3, goodScope);

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
     *
     * @throws Exception
     */
    @Test
    public void testRelationshipMissingData() throws Exception {
        User goodUser = new TestUser("1");

        @SuppressWarnings("resource")
        DataStoreTransaction tx = mock(DataStoreTransaction.class);

        RequestScope goodScope = new RequestScope(
                null,
                null,
                NO_VERSION,
                null,
                tx,
                goodUser,
                null,
                null,
                UUID.randomUUID(),
                elideSettings);

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
        assertTrue(toOneRelationship.getData().get().isEmpty());
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

        when(tx.getAttribute(any(), any(), any())).thenCallRealMethod();

        RequestScope scope = new TestRequestScope(tx, goodUser, dictionary);

        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, "1", scope);

        String result = (String) funResource.getAttribute("field2");
        assertEquals("blah", result, "The correct attribute should be returned.");
        result = (String) funResource.getAttribute("field3");
        assertNull(result, "The correct attribute should be returned.");
    }

    @Test
    public void testGetAttributeInvalidField() {
        FunWithPermissions fun = new FunWithPermissions();

        RequestScope scope = new TestRequestScope(tx, goodUser, dictionary);

        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, "1", scope);

        assertThrows(InvalidAttributeException.class, () -> funResource.getAttribute("invalid"));
    }

    @Test
    public void testGetAttributeInvalidFieldPermissions() {
        FunWithPermissions fun = new FunWithPermissions();
        fun.setField1("foo");

        RequestScope scope = new TestRequestScope(tx, goodUser, dictionary);

        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, "1", scope);

        assertThrows(ForbiddenAccessException.class, () -> funResource.getAttribute("field1"));
    }

    @Test
    public void testGetAttributeInvalidEntityPermissions() {
        NoReadEntity noread = new NoReadEntity();

        RequestScope scope = new TestRequestScope(tx, goodUser, dictionary);

        PersistentResource<NoReadEntity> noreadResource = new PersistentResource<>(noread, "1", scope);

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

        RequestScope scope = new TestRequestScope(tx, goodUser, dictionary);
        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, "3", scope);

        when(scope.getTransaction().getToManyRelation(any(), eq(fun), any(), any()))
                .thenReturn(new DataStoreIterableBuilder(children).build());

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

        RequestScope scope = new TestRequestScope(tx, goodUser, dictionary);
        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, "3", scope);

        when(scope.getTransaction().getToManyRelation(any(), eq(fun), any(), any()))
                .thenReturn(new DataStoreIterableBuilder(children).build());

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

        when(tx.getToManyRelation(eq(tx), any(), any(), any()))
                .thenReturn(new DataStoreIterableBuilder(Sets.newHashSet(child1)).build());

        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.add("filter[child.name]", "paul john");
        RequestScope goodScope = buildRequestScope("/child", tx, goodUser, queryParams);

        PersistentResource<Parent> parentResource = new PersistentResource<>(parent, "1", goodScope);

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

        RequestScope scope = new TestRequestScope(tx, goodUser, dictionary);
        when(scope.getTransaction().getToManyRelation(any(), eq(parent), any(), any()))
                .thenReturn(new DataStoreIterableBuilder(children).build());

        PersistentResource<Parent> parentResource = new PersistentResource<>(parent, "1", scope);

        PersistentResource childResource =
                parentResource.getRelation(getRelationship(ClassType.of(Parent.class), "children"), "2");

        assertEquals("2", childResource.getId());
        assertEquals("john buzzard", ((Child) childResource.getObject()).getName());
    }

    @Test
    public void testGetRelationForbiddenByEntity() {
        NoReadEntity noread = new NoReadEntity();

        RequestScope scope = new TestRequestScope(tx, badUser, dictionary);

        PersistentResource<NoReadEntity> noreadResource = new PersistentResource<>(noread, "3", scope);
        assertThrows(ForbiddenAccessException.class, () -> getRelation(noreadResource, "child"));
    }

    @Test
    public void testGetRelationForbiddenByField() {
        FunWithPermissions fun = new FunWithPermissions();

        RequestScope scope = new TestRequestScope(tx, badUser, dictionary);

        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, "3", scope);

        assertThrows(ForbiddenAccessException.class, () -> getRelation(funResource, "relation1"));
    }

    @Test
    public void testGetRelationForbiddenByEntityAllowedByField() {
        FirstClassFields firstClassFields = new FirstClassFields();

        RequestScope badUserScope = new TestRequestScope(tx, badUser, dictionary);

        PersistentResource<FirstClassFields> fcResource = new PersistentResource<>(firstClassFields, "3", badUserScope);

        getRelation(fcResource, "public2");
    }

    @Test
    public void testGetAttributeForbiddenByEntityAllowedByField() {
        FirstClassFields firstClassFields = new FirstClassFields();

        RequestScope badUserScope = new TestRequestScope(tx, badUser, dictionary);

        PersistentResource<FirstClassFields> fcResource = new PersistentResource<>(firstClassFields, "3", badUserScope);

        fcResource.getAttribute("public1");
    }

    @Test
    public void testGetRelationForbiddenByEntity2() {
        FirstClassFields firstClassFields = new FirstClassFields();

        RequestScope badUserScope = new TestRequestScope(tx, badUser, dictionary);

        PersistentResource<FirstClassFields> fcResource = new PersistentResource<>(firstClassFields, "3", badUserScope);

        assertThrows(ForbiddenAccessException.class, () -> getRelation(fcResource, "private2"));
    }

    @Test
    public void testGetAttributeForbiddenByEntity2() {
        FirstClassFields firstClassFields = new FirstClassFields();

        RequestScope scope = new TestRequestScope(tx, goodUser, dictionary);

        PersistentResource<FirstClassFields> fcResource = new PersistentResource<>(firstClassFields,
                "3", scope);

        assertThrows(ForbiddenAccessException.class, () -> fcResource.getAttribute("private1"));
    }

    @Test
    public void testGetRelationInvalidRelation() {
        FunWithPermissions fun = new FunWithPermissions();

        RequestScope scope = new TestRequestScope(tx, goodUser, dictionary);

        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, "3", scope);

        assertThrows(InvalidAttributeException.class, () -> getRelation(funResource, "invalid"));
    }

    @Test
    public void testGetRelationByIdSuccess() {
        FunWithPermissions fun = new FunWithPermissions();
        Child child1 = newChild(1);
        Child child2 = newChild(2);
        Child child3 = newChild(3);
        fun.setRelation2(Sets.newHashSet(child1, child2, child3));

        when(tx.getToManyRelation(eq(tx), any(), any(), any()))
                .thenReturn(new DataStoreIterableBuilder(Sets.newHashSet(child1)).build());

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, "3", goodScope);

        PersistentResource<?> result = funResource.getRelation(getRelationship(
                ClassType.of(FunWithPermissions.class),
                "relation2"),
                "1");

        assertEquals(1,
                ((Child) result.getObject()).getId(), "The correct relationship element should be returned");
    }

    @Test
    public void testGetRelationByInvalidId() {
        FunWithPermissions fun = new FunWithPermissions();
        Child child1 = newChild(1);
        Child child2 = newChild(2);
        Child child3 = newChild(3);
        fun.setRelation2(Sets.newHashSet(child1, child2, child3));

        when(tx.getToManyRelation(eq(tx), any(), any(), any()))
                .thenReturn(new DataStoreIterableBuilder(Sets.newHashSet(child1)).build());

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, "3", goodScope);

        assertThrows(InvalidObjectIdentifierException.class,
                () -> funResource.getRelation(
                        getRelationship(ClassType.of(FunWithPermissions.class), "relation2"),
                        "-1000"));
    }

    @Test
    public void testGetRelationsNoEntityAccess() {
        FunWithPermissions fun = new FunWithPermissions();

        RequestScope scope = new TestRequestScope(tx, goodUser, dictionary);

        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, "3", scope);

        Set set = getRelation(funResource, "relation4");
        assertEquals(0, set.size());
    }

    @Test
    public void testGetRelationsNoEntityAccess2() {
        FunWithPermissions fun = new FunWithPermissions();

        RequestScope scope = new TestRequestScope(tx, goodUser, dictionary);

        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, "3", scope);

        Set set = getRelation(funResource, "relation5");
        assertEquals(0, set.size());
    }

    @Test
    void testDeleteResourceSuccess() {
        Parent parent = newParent(1);

        RequestScope goodScope = buildRequestScope(tx, goodUser);

        PersistentResource<Parent> parentResource = new PersistentResource<>(parent, "1", goodScope);

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

        RequestScope goodScope = buildRequestScope(tx, goodUser);

        PersistentResource<Invoice> invoiceResource = new PersistentResource<>(invoice, "1", goodScope);

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

        when(tx.getToManyRelation(any(), eq(child), any(), any()))
                .thenReturn(new DataStoreIterableBuilder(parents).build());

        RequestScope goodScope = buildRequestScope(tx, goodUser);

        PersistentResource<Parent> parentResource = new PersistentResource<>(parent, "1", goodScope);
        PersistentResource<Child> childResource =
                new PersistentResource<>(child, parentResource, "children", "1", goodScope);

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

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<NoDeleteEntity> nodeleteResource = new PersistentResource<>(nodelete, "1", goodScope);

        assertThrows(ForbiddenAccessException.class, nodeleteResource::deleteResource);

        verify(tx, never()).delete(nodelete, goodScope);
    }

    @Test
    void testAddRelationSuccess() {
        FunWithPermissions fun = new FunWithPermissions();
        fun.setRelation1(Sets.newHashSet());

        Child child = newChild(1);


        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, "3", goodScope);
        PersistentResource<Child> childResource = new PersistentResource<>(child, "1", goodScope);
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

        RequestScope badScope = buildRequestScope(tx, badUser);
        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, "3", badScope);
        PersistentResource<Child> childResource = new PersistentResource<>(child, "1", badScope);
        assertThrows(ForbiddenAccessException.class, () -> funResource.addRelation("relation1", childResource));
    }

    @Test
    void testAddRelationForbiddenByToManyExistingRelationship() {
        FunWithPermissions fun = new FunWithPermissions();

        Child child = newChild(1);
        fun.setRelation1(Set.of(child));

        RequestScope badScope = buildRequestScope(tx, badUser);
        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, "3", badScope);
        PersistentResource<Child> childResource = new PersistentResource<>(child, "1", badScope);
        assertThrows(ForbiddenAccessException.class, () -> funResource.addRelation("relation1", childResource));
    }

    @Test
    void testAddRelationForbiddenByToOneExistingRelationship() {
        FunWithPermissions fun = new FunWithPermissions();

        Child child = newChild(1);
        fun.setRelation3(child);

        RequestScope badScope = buildRequestScope(tx, badUser);
        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, "3", badScope);
        PersistentResource<Child> childResource = new PersistentResource<>(child, "1", badScope);
        assertThrows(ForbiddenAccessException.class, () -> funResource.addRelation("relation3", childResource));
    }

    @Test
    void testAddRelationForbiddenByEntity() {
        NoUpdateEntity noUpdate = new NoUpdateEntity();
        noUpdate.setId(1);
        Child child = newChild(2);
        noUpdate.setChildren(Sets.newHashSet());

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<NoUpdateEntity> noUpdateResource = new PersistentResource<>(noUpdate, "1", goodScope);
        PersistentResource<Child> childResource = new PersistentResource<>(child, "2", goodScope);
        assertThrows(ForbiddenAccessException.class, () -> noUpdateResource.addRelation("children", childResource));
    }

    @Test
    public void testAddRelationInvalidRelation() {
        FunWithPermissions fun = new FunWithPermissions();

        Child child = newChild(1);

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, "3", goodScope);
        PersistentResource<Child> childResource = new PersistentResource<>(child, "1", goodScope);
        assertThrows(InvalidAttributeException.class, () -> funResource.addRelation("invalid", childResource));
    }

    @Test()
    public void testRemoveToManyRelationSuccess() {
        Child child = newChild(1);
        Parent parent1 = newParent(1, child);
        Parent parent2 = newParent(2, child);
        Parent parent3 = newParent(3, child);
        child.setParents(Sets.newHashSet(parent1, parent2, parent3));

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<Child> childResource = new PersistentResource<>(child, "1", goodScope);
        PersistentResource<Object> removeResource = new PersistentResource<>(parent1, "1", goodScope);
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

        RequestScope goodScope = buildRequestScope(tx, goodUser);

        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, "1", goodScope);
        PersistentResource<Object> removeResource = new PersistentResource<>(child, "1", goodScope);

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

        when(tx.getToOneRelation(any(), eq(fun), eq(com.yahoo.elide.core.request.Relationship.builder()
                .name("relation3")
                .alias("relation3")
                .projection(EntityProjection.builder()
                        .type(Child.class)
                        .build())
                .build()), any())).thenReturn(child);

        when(tx.getToManyRelation(any(), eq(fun), eq(com.yahoo.elide.core.request.Relationship.builder()
                .name("relation1")
                .alias("relation1")
                .projection(EntityProjection.builder()
                        .type(Child.class)
                        .build())
                .build()), any())).thenReturn(new DataStoreIterableBuilder(children1).build());

        when(tx.getToManyRelation(any(), eq(parent), eq(com.yahoo.elide.core.request.Relationship.builder()
                .name("children")
                .alias("children")
                .projection(EntityProjection.builder()
                        .type(Child.class)
                        .build())
                .build()), any())).thenReturn(new DataStoreIterableBuilder(children2).build());

        when(tx.getToOneRelation(any(), eq(child), eq(com.yahoo.elide.core.request.Relationship.builder()
                .name("readNoAccess")
                .alias("readNoAccess")
                .projection(EntityProjection.builder()
                        .type(Child.class)
                        .build())
                .build()), any())).thenReturn(secret);

        RequestScope funScope = new TestRequestScope(tx, goodUser, dictionary);
        RequestScope childScope = new TestRequestScope(tx, goodUser, dictionary);
        RequestScope parentScope = new TestRequestScope(tx, goodUser, dictionary);


        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, "1", funScope);
        PersistentResource<Child> childResource = new PersistentResource<>(child, "1", childScope);
        PersistentResource<Child> secretResource = new PersistentResource<>(secret, "1", childScope);
        PersistentResource<Parent> parentResource = new PersistentResource<>(parent, "1", parentScope);

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

        parentScope.saveOrCreateObjects();
        childScope.saveOrCreateObjects();
        funScope.saveOrCreateObjects();
        verify(tx, never()).save(fun, funScope);
        verify(tx, never()).save(child, childScope);
        verify(tx, never()).save(parent, parentScope);
        verify(tx, never()).save(secret, childScope);
    }

    @Test()
    public void testRemoveNonexistingToOneRelation() {
        FunWithPermissions fun = new FunWithPermissions();
        Child ownedChild = newChild(1);
        Child unownedChild = newChild(2);
        fun.setRelation3(ownedChild);

        RequestScope goodScope = buildRequestScope(tx, goodUser);

        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, "1", goodScope);
        PersistentResource<Object> removeResource = new PersistentResource<>(unownedChild, "1", goodScope);

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

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<Child> childResource = new PersistentResource<>(child, "1", goodScope);
        PersistentResource<Object> removeResource = new PersistentResource<>(unownedParent, "1", goodScope);
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

        when(tx.getToManyRelation(any(), eq(child), any(), any()))
                .thenReturn(new DataStoreIterableBuilder(parents).build());

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        goodScope.setEntityProjection(EntityProjection.builder()
                .type(Child.class)
                .relationship("parents",
                        EntityProjection.builder()
                                .type(Parent.class)
                                .build())
                .build());

        PersistentResource<Child> childResource = new PersistentResource<>(child, "1", goodScope);

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

        when(tx.getToOneRelation(any(), eq(fun), any(), any())).thenReturn(child);

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        goodScope.setEntityProjection(EntityProjection.builder()
                .type(FunWithPermissions.class)
                .relationship("relation3",
                        EntityProjection.builder()
                                .type(Child.class)
                                .build())
                .build());

        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, "1", goodScope);
        funResource.clearRelation("relation3");

        assertNull(fun.getRelation3(), "The one-2-one relationship should be cleared");

        goodScope.saveOrCreateObjects();
        verify(tx, times(1)).save(fun, goodScope);
        verify(tx, times(1)).save(child, goodScope);
    }

    @Test()
    public void testClearRelationFilteredByReadAccess() {
        Parent parent = new Parent();
        RequestScope goodScope = buildRequestScope(tx, goodUser);

        goodScope.setEntityProjection(EntityProjection.builder()
                .type(Parent.class)
                .relationship("children",
                        EntityProjection.builder()
                                .type(Child.class)
                                .build())
                .build());

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

        when(tx.getToManyRelation(any(), eq(parent), any(), any()))
                .thenReturn(new DataStoreIterableBuilder(allChildren).build());

        PersistentResource<Parent> parentResource = new PersistentResource<>(parent, "1", goodScope);

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

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        goodScope.setEntityProjection(EntityProjection.builder()
                .type(Left.class)
                .relationship("noUpdateOne2One",
                        EntityProjection.builder()
                                .type(Right.class)
                                .build())
                .build());

        PersistentResource<Left> leftResource = new PersistentResource<>(left, "1", goodScope);

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

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<Left> leftResource = new PersistentResource<>(left, "1", goodScope);

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

        when(tx.getToManyRelation(any(), eq(left), any(), any()))
                .thenReturn(new DataStoreIterableBuilder(noInverseUpdate).build());

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        goodScope.setEntityProjection(EntityProjection.builder()
                .type(Left.class)
                .relationship("noInverseUpdate",
                        EntityProjection.builder()
                                .type(Right.class)
                                .build())
                .build());

        PersistentResource<Left> leftResource = new PersistentResource<>(left, "1", goodScope);

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

        when(tx.getToOneRelation(any(), eq(left), any(), any())).thenReturn(noDelete);

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        goodScope.setEntityProjection(EntityProjection.builder()
                .type(Left.class)
                .relationship("noDeleteOne2One",
                        EntityProjection.builder()
                                .type(NoDeleteEntity.class)
                                .build())
                .build());

        PersistentResource<Left> leftResource = new PersistentResource<>(left, "1", goodScope);
        assertTrue(leftResource.clearRelation("noDeleteOne2One"));
        assertNull(leftResource.getObject().getNoDeleteOne2One());

    }

    @Test
    public void testClearRelationInvalidRelation() {
        FunWithPermissions fun = new FunWithPermissions();
        Child child = newChild(1);
        fun.setRelation3(child);

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, "1", goodScope);
        assertThrows(InvalidAttributeException.class, () -> funResource.clearRelation("invalid"));
    }

    @Test
    public void testUpdateAttributeSuccess() {
        Parent parent = newParent(1);

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<Parent> parentResource = new PersistentResource<>(parent, "1", goodScope);
        parentResource.updateAttribute("firstName", "foobar");

        assertEquals("foobar", parent.getFirstName(), "The attribute was updated successfully");

        goodScope.saveOrCreateObjects();
        verify(tx, times(1)).save(parent, goodScope);
    }

    @Test
    public void testUpdateComplexAttributeSuccess() {
        Company company = newCompany("abc");
        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<Company> parentResource = new PersistentResource<>(company, "1", goodScope);
        final Address address = new Address();
        address.setStreet1("street1");
        address.setStreet2("street2");
        parentResource.updateAttribute("address", address);

        assertEquals(address, company.getAddress(), "The attribute was updated successfully");

        goodScope.saveOrCreateObjects();
        verify(tx, times(1)).save(company, goodScope);
    }

    @Test
    public void testUpdateComplexAttributeCloneWithHook() {
        reset(bookUpdatePrice);
        Book book = new Book();

        Price originalPrice = new Price();
        originalPrice.setUnits(new BigDecimal(1.0));
        originalPrice.setCurrency(Currency.getInstance("USD"));
        book.setPrice(originalPrice);

        Map<String, Object> newPrice = new HashMap<>();
        newPrice.put("units", new BigDecimal(2.0));
        newPrice.put("currency", Currency.getInstance("CNY"));

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<Book> bookResource = new PersistentResource<>(book, "1", goodScope);
        bookResource.updateAttribute("price", newPrice);

        //check that original value was unmodified.
        assertEquals(Currency.getInstance("USD"), originalPrice.getCurrency());
        assertEquals(new BigDecimal(1.0), originalPrice.getUnits());

        //check that new value matches expected.
        assertEquals(Currency.getInstance("CNY"), book.getPrice().getCurrency());
        assertEquals(new BigDecimal(2.0), book.getPrice().getUnits());

        goodScope.saveOrCreateObjects();
        verify(tx, times(1)).save(book, goodScope);

        ArgumentCaptor<CRUDEvent> eventCapture = ArgumentCaptor.forClass(CRUDEvent.class);
        verify(bookUpdatePrice, times(1)).execute(eq(UPDATE), eq(PRESECURITY),
                eventCapture.capture());

        assertEquals(originalPrice, eventCapture.getValue().getChanges().get().getOriginal());
        assertEquals(book.getPrice(), eventCapture.getValue().getChanges().get().getModified());
    }

    @Test
    public void testUpdateNestedComplexAttributeClone() {
        Company company = newCompany("abc");
        Address originalAddress = new Address();
        originalAddress.setStreet1("street1");
        originalAddress.setStreet2("street2");

        GeoLocation originalGeo = new GeoLocation();
        originalGeo.setLatitude("1");
        originalGeo.setLongitude("2");
        originalAddress.setGeo(originalGeo);

        Map<String, Object> newAddress = new HashMap<>();
        newAddress.put("street1", "Elm");
        newAddress.put("street2", "Maple");
        Map<String, Object> newGeo = new HashMap<>();
        newGeo.put("latitude", "X");
        newGeo.put("longitude", "Y");
        newAddress.put("geo", newGeo);

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<Company> parentResource = new PersistentResource<>(company, "1", goodScope);

        parentResource.updateAttribute("address", newAddress);

        //check that original value was unmodified.
        assertEquals("street1", originalAddress.getStreet1());
        assertEquals("street2", originalAddress.getStreet2());
        assertEquals("1", originalAddress.getGeo().getLatitude());
        assertEquals("2", originalAddress.getGeo().getLongitude());

        //check the new value matches the expected.
        assertEquals("Elm", company.getAddress().getStreet1());
        assertEquals("Maple", company.getAddress().getStreet2());
        assertEquals("X", company.getAddress().getGeo().getLatitude());
        assertEquals("Y", company.getAddress().getGeo().getLongitude());

        goodScope.saveOrCreateObjects();
        verify(tx, times(1)).save(company, goodScope);
    }

    @Test
    public void testUpdateNullComplexAttributeSuccess() {
        Company company = newCompany("abc");
        company.setAddress(new Address());
        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<Company> parentResource = new PersistentResource<>(company, "1", goodScope);
        final Address address = null;

        boolean updated = parentResource.updateAttribute("address", address);

        assertTrue(updated);
        assertNull(company.getAddress(), "The attribute was updated successfully");

        goodScope.saveOrCreateObjects();
        verify(tx, times(1)).save(company, goodScope);
    }

    @Test
    public void testUpdateComplexAttributeNullField() {
        Company company = newCompany("abc");

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<Company> parentResource = new PersistentResource<>(company, "1", goodScope);
        final Address address = new Address();
        address.setStreet1("street1");
        address.setStreet2(null);
        parentResource.updateAttribute("address", address);

        assertEquals(address, company.getAddress(), "The attribute was updated successfully");

        goodScope.saveOrCreateObjects();
        verify(tx, times(1)).save(company, goodScope);
    }

    @Test
    public void testUpdateComplexAttributeAllNullFields() {
        Company company = newCompany("abc");

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<Company> parentResource = new PersistentResource<>(company, "1", goodScope);
        final Address address = new Address();
        address.setStreet1(null);
        address.setStreet2(null);
        parentResource.updateAttribute("address", address);

        assertEquals(address, company.getAddress(), "The attribute was updated successfully");

        goodScope.saveOrCreateObjects();
        verify(tx, times(1)).save(company, goodScope);
    }

    @Test
    public void testUpdateComplexAttributeNested() {
        Company company = newCompany("abc");

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<Company> parentResource = new PersistentResource<>(company, "1", goodScope);
        final Address address = new Address();
        address.setStreet1("street1");
        address.setStreet2("street2");
        final GeoLocation geo = new GeoLocation();
        geo.setLatitude("lat");
        geo.setLongitude("long");
        address.setGeo(geo);
        parentResource.updateAttribute("address", address);

        assertEquals(address, company.getAddress(), "The attribute was updated successfully");

        goodScope.saveOrCreateObjects();
        verify(tx, times(1)).save(company, goodScope);
    }

    @Test
    public void testUpdateComplexAttributeNestedNullField() {
        Company company = newCompany("abc");

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<Company> parentResource = new PersistentResource<>(company, "1", goodScope);
        final Address address = new Address();
        address.setStreet1("street1");
        address.setStreet2("street2");
        final GeoLocation geo = new GeoLocation();
        geo.setLatitude(null);
        geo.setLongitude("long");
        address.setGeo(geo);
        parentResource.updateAttribute("address", address);

        assertEquals(address, company.getAddress(), "The attribute was updated successfully");

        goodScope.saveOrCreateObjects();
        verify(tx, times(1)).save(company, goodScope);
    }

    @Test
    public void testUpdateComplexAttributeNestedAllNullFields() {
        Company company = newCompany("abc");

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<Company> parentResource = new PersistentResource<>(company, "1", goodScope);
        final Address address = new Address();
        address.setStreet1("street1");
        address.setStreet2("street2");
        final GeoLocation geo = new GeoLocation();
        geo.setLatitude(null);
        geo.setLongitude(null);
        address.setGeo(geo);
        parentResource.updateAttribute("address", address);

        assertEquals(address, company.getAddress(), "The attribute was updated successfully");

        goodScope.saveOrCreateObjects();
        verify(tx, times(1)).save(company, goodScope);
    }

    @Test
    public void testUpdateComplexAttributeAllNullFieldsNestedAllNullFields() {
        Company company = newCompany("abc");

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<Company> parentResource = new PersistentResource<>(company, "1", goodScope);
        final Address address = new Address();
        address.setStreet1(null);
        address.setStreet2(null);
        final GeoLocation geo = new GeoLocation();
        geo.setLatitude(null);
        geo.setLongitude(null);
        address.setGeo(geo);
        parentResource.updateAttribute("address", address);

        assertEquals(address, company.getAddress(), "The attribute was updated successfully");

        goodScope.saveOrCreateObjects();
        verify(tx, times(1)).save(company, goodScope);
    }

    @Test
    public void testUpdateComplexAttributeAllNullFieldsNested() {
        Company company = newCompany("abc");

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<Company> parentResource = new PersistentResource<>(company, "1", goodScope);
        final Address address = new Address();
        address.setStreet1(null);
        address.setStreet2(null);
        final GeoLocation geo = new GeoLocation();
        geo.setLatitude("lat");
        geo.setLongitude("long");
        address.setGeo(geo);
        parentResource.updateAttribute("address", address);

        assertEquals(address, company.getAddress(), "The attribute was updated successfully");

        goodScope.saveOrCreateObjects();
        verify(tx, times(1)).save(company, goodScope);
    }

    @Test
    public void testUpdateAttributeInvalidAttribute() {
        Parent parent = newParent(1);

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<Parent> parentResource = new PersistentResource<>(parent, "1", goodScope);
        assertThrows(InvalidAttributeException.class, () -> parentResource.updateAttribute("invalid", "foobar"));
    }

    @Test
    public void testUpdateAttributeInvalidUpdatePermission() {
        FunWithPermissions fun = new FunWithPermissions();
        fun.setId(1);


        RequestScope badScope = buildRequestScope(tx, badUser);

        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, "1", badScope);

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


        RequestScope badScope = buildRequestScope(tx, badUser);
        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, "1", badScope);

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

        EntityProjection collection = EntityProjection.builder()
                .type(Child.class)

                .build();

        when(tx.loadObjects(eq(collection), any(RequestScope.class)))
                .thenReturn(new DataStoreIterableBuilder(
                        Lists.newArrayList(child1, child2, child3, child4, child5)).build());

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        goodScope.setEntityProjection(collection);

        Set<PersistentResource> loaded = PersistentResource.loadRecords(EntityProjection.builder()
                .type(Child.class)
                .build(), new ArrayList<>(), goodScope).toList(LinkedHashSet::new).blockingGet();

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

        EntityProjection collection = EntityProjection.builder()
                .type(Child.class)

                .build();

        when(tx.loadObject(eq(collection), eq(1L), any())).thenReturn(child1);

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        goodScope.setEntityProjection(collection);
        PersistentResource<Child> loaded = PersistentResource.loadRecord(EntityProjection.builder()
                .type(Child.class)
                .build(), "1", goodScope);

        assertEquals(child1, loaded.getObject(), "The load function should return the requested child object");
    }

    @Test
    public void testLoadRecordInvalidId() {
        EntityProjection collection = EntityProjection.builder()
                .type(Child.class)

                .build();

        when(tx.loadObject(eq(collection), eq("1"), any())).thenReturn(null);

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        goodScope.setEntityProjection(collection);
        assertThrows(
                InvalidObjectIdentifierException.class,
                () -> PersistentResource.loadRecord(EntityProjection.builder()

                        .type(Child.class)
                        .build(), "1", goodScope));
    }

    @Test
    public void testLoadRecordForbidden() {
        NoReadEntity noRead = new NoReadEntity();
        noRead.setId(1);
        EntityProjection collection = EntityProjection.builder()
                .type(NoReadEntity.class)

                .build();

        when(tx.loadObject(eq(collection), eq(1L), any())).thenReturn(noRead);

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        goodScope.setEntityProjection(collection);

        assertThrows(
                ForbiddenAccessException.class,
                () -> PersistentResource.loadRecord(EntityProjection.builder().type(NoReadEntity.class).build(),
                        "1", goodScope));
    }

    @Test()
    public void testCreateObjectSuccess() {
        Parent parent = newParent(1);
        RequestScope goodScope = buildRequestScope(tx, goodUser);
        when(tx.createNewObject(ClassType.of(Parent.class), goodScope)).thenReturn(parent);

        PersistentResource<Parent> created = PersistentResource.createObject(ClassType.of(Parent.class), goodScope, Optional.of("uuid"));

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

        final RequestScope goodScope = buildRequestScope(tx, new TestUser("1"));
        when(tx.createNewObject(ClassType.of(Job.class), goodScope)).thenReturn(job);

        PersistentResource<Job> created = PersistentResource.createObject(ClassType.of(Job.class), goodScope, Optional.empty());

        created.getRequestScope().getPermissionExecutor().executeCommitChecks();

        assertEquals("day job", created.getObject().getTitle(),
                "The create function should return the requested job object"
        );
        assertNull(created.getObject().getJobId(), "The create function should not override the ID");

        created = PersistentResource.createObject(ClassType.of(Job.class), goodScope, Optional.of("1234"));
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

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        when(tx.createNewObject(ClassType.of(NoCreateEntity.class), goodScope)).thenReturn(noCreate);

        assertThrows(
                ForbiddenAccessException.class,
                () -> {
                    PersistentResource<NoCreateEntity> created = PersistentResource.createObject(
                            ClassType.of(NoCreateEntity.class),
                            goodScope, Optional.of("1"));
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
        when(tx.getToManyRelation(any(), eq(left), any(), any())).thenReturn(new DataStoreIterableBuilder(rights).build());

        RequestScope badScope = buildRequestScope(tx, badUser);
        PersistentResource<Left> leftResource = new PersistentResource<>(left, badScope.getUUIDFor(left), badScope);

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

        when(tx.getToManyRelation(any(), eq(left), any(), any()))
                .thenReturn(new DataStoreIterableBuilder(rights).build());

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<Left> leftResource = new PersistentResource<>(left, goodScope.getUUIDFor(left), goodScope);

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

        TestAuditLogger logger = new TestAuditLogger();
        RequestScope requestScope = getUserScope(goodUser, logger);
        PersistentResource<Parent> parentResource =
                new PersistentResource<>(parent, requestScope.getUUIDFor(parent), requestScope);
        PersistentResource<Child> childResource = new PersistentResource<>(
                child,
                parentResource,
                "children",
                requestScope.getUUIDFor(child),
                requestScope);

        childResource.auditField(new ChangeSpec(childResource, "name", parent, null));

        assertEquals(1, logger.getMessages().size(), "One message should be logged");

        LogMessage message = logger.getMessages().get(0);
        assertEquals("UPDATE Child 5 Parent 7", message.getMessage(), "Logging template should match");

        assertEquals(1, message.getOperationCode(), "Operation code should match");
        logger.clear(); // tidy up this thread's messages
    }

    @Test
    public void testClassLevelAudit() throws Exception {
        Child child = newChild(5);
        Parent parent = newParent(7);

        TestAuditLogger logger = new TestAuditLogger();
        RequestScope requestScope = getUserScope(goodUser, logger);
        PersistentResource<Parent> parentResource = new PersistentResource<>(
                parent, requestScope.getUUIDFor(parent), requestScope);
        PersistentResource<Child> childResource = new PersistentResource<>(
                child, parentResource, "children", requestScope.getUUIDFor(child), requestScope);

        childResource.auditClass(
                Audit.Action.CREATE,
                new ChangeSpec(childResource, null, null, childResource.getObject()));

        assertEquals(1, logger.getMessages().size(), "One message should be logged");

        LogMessage message = logger.getMessages().get(0);
        assertEquals("CREATE Child 5 Parent 7", message.getMessage(), "Logging template should match");

        assertEquals(0, message.getOperationCode(), "Operation code should match");
        logger.clear(); // tidy up this thread's messages
    }

    @Test
    public void testOwningRelationshipInverseUpdates() {
        Parent parent = newParent(1);
        Child child = newChild(2);

        when(tx.getToManyRelation(any(), eq(parent), any(), any())).thenReturn(new DataStoreIterableBuilder(parent.getChildren()).build());

        RequestScope goodScope = buildRequestScope(tx, goodUser);

        PersistentResource<Parent> parentResource =
                new PersistentResource<>(parent, goodScope.getUUIDFor(parent), goodScope);
        PersistentResource<Child> childResource = new PersistentResource<>(
                child,
                parentResource,
                "children",
                goodScope.getUUIDFor(child),
                goodScope);

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
        when(tx.getToManyRelation(any(), eq(parent), any(), any()))
                .thenReturn(new DataStoreIterableBuilder(parent.getChildren()).build());

        parentResource.clearRelation("children");

        goodScope.saveOrCreateObjects();
        verify(tx, times(1)).save(parent, goodScope);
        verify(tx, times(1)).save(child, goodScope);

        assertEquals(0, parent.getChildren().size(), "The owning relationship should be updated");
        assertEquals(0, child.getParents().size(), "The non-owning relationship should also be updated");
    }

    @Test
    public void testIsIdGenerated() {
        RequestScope scope = new TestRequestScope(tx, goodUser, dictionary);

        PersistentResource<Child> generated = new PersistentResource<>(new Child(), "1", scope);

        assertTrue(generated.isIdGenerated(),
                "isIdGenerated returns true when ID field has the GeneratedValue annotation");

        scope = new TestRequestScope(tx, goodUser, dictionary);

        PersistentResource<NoCreateEntity> notGenerated = new PersistentResource<>(new NoCreateEntity(), "1", scope);

        assertFalse(notGenerated.isIdGenerated(),
                "isIdGenerated returns false when ID field does not have the GeneratedValue annotation");
    }

    @Test
    public void testTransferPermissionErrorOnUpdateSingularRelationship() {
        example.User userModel = new example.User();
        userModel.setId(1);

        NoShareEntity noShare = new NoShareEntity();
        noShare.setId(1);

        List<Resource> idList = new ArrayList<>();
        idList.add(new ResourceIdentifier("noshare", "1").castToResource());
        Relationship ids = new Relationship(null, new Data<>(idList));

        EntityProjection collection = EntityProjection.builder()
                .type(NoShareEntity.class)

                .build();

        when(tx.loadObject(eq(collection), eq(1L), any())).thenReturn(noShare);

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<example.User> userResource =
                new PersistentResource<>(userModel, goodScope.getUUIDFor(userModel), goodScope);

        assertThrows(
                ForbiddenAccessException.class,
                () -> userResource.updateRelation("noShare", ids.toPersistentResources(goodScope)));
    }

    @Test
    public void testTransferPermissionErrorOnUpdateRelationshipPackageLevel() {
        ContainerWithPackageShare containerWithPackageShare = new ContainerWithPackageShare();

        Untransferable untransferable = new Untransferable();
        untransferable.setContainerWithPackageShare(containerWithPackageShare);

        List<Resource> unShareableList = new ArrayList<>();
        unShareableList.add(new ResourceIdentifier("untransferable", "1").castToResource());
        Relationship unShareales = new Relationship(null, new Data<>(unShareableList));

        when(tx.loadObject(any(), eq(1L), any())).thenReturn(untransferable);


        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<ContainerWithPackageShare> containerResource = new PersistentResource<>(
                containerWithPackageShare, goodScope.getUUIDFor(containerWithPackageShare), goodScope);

        assertThrows(
                ForbiddenAccessException.class,
                () -> containerResource.updateRelation(
                        "untransferables", unShareales.toPersistentResources(goodScope)));
    }

    @Test
    public void testTransferPermissionSuccessOnUpdateManyRelationshipPackageLevel() {
        ContainerWithPackageShare containerWithPackageShare = new ContainerWithPackageShare();

        ShareableWithPackageShare shareableWithPackageShare = new ShareableWithPackageShare();
        shareableWithPackageShare.setContainerWithPackageShare(containerWithPackageShare);

        List<Resource> shareableList = new ArrayList<>();
        shareableList.add(new ResourceIdentifier("shareableWithPackageShare", "1").castToResource());
        Relationship shareables = new Relationship(null, new Data<>(shareableList));

        when(tx.loadObject(any(), eq(1L), any())).thenReturn(shareableWithPackageShare);

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<ContainerWithPackageShare> containerResource = new PersistentResource<>(
                containerWithPackageShare, goodScope.getUUIDFor(containerWithPackageShare), goodScope);

        containerResource.updateRelation(
                "shareableWithPackageShares", shareables.toPersistentResources(goodScope));

        assertEquals(1, containerWithPackageShare.getShareableWithPackageShares().size());
        assertTrue(containerWithPackageShare.getShareableWithPackageShares().contains(shareableWithPackageShare));
    }

    @Test
    public void testTransferPermissionErrorOnUpdateManyRelationship() {
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

        when(tx.loadObject(any(), eq(1L), any())).thenReturn(noShare1);
        when(tx.loadObject(any(), eq(2L), any())).thenReturn(noShare2);

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<example.User> userResource =
                new PersistentResource<>(userModel, goodScope.getUUIDFor(userModel), goodScope);

        assertThrows(
                ForbiddenAccessException.class,
                () -> userResource.updateRelation("noShares", ids.toPersistentResources(goodScope)));
    }

    @Test
    public void testTransferPermissionSuccessOnUpdateManyRelationship() {
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

        when(tx.loadObject(any(), eq(1L), any())).thenReturn(noShare1);
        when(tx.getToManyRelation(any(), eq(userModel), any(), any()))
                .thenReturn(new DataStoreIterableBuilder(noshares).build());

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<example.User> userResource =
                new PersistentResource<>(userModel, goodScope.getUUIDFor(userModel), goodScope);

        boolean returnVal = userResource.updateRelation("noShares", ids.toPersistentResources(goodScope));

        assertTrue(returnVal);
        assertEquals(1, userModel.getNoShares().size());
        assertTrue(userModel.getNoShares().contains(noShare1));
    }

    @Test
    public void testTransferPermissionSuccessOnUpdateSingularRelationship() {
        example.User userModel = new example.User();
        userModel.setId(1);

        NoShareEntity noShare = new NoShareEntity();

        /* The noshare already exists so no exception should be thrown */
        userModel.setNoShare(noShare);

        List<Resource> idList = new ArrayList<>();
        idList.add(new ResourceIdentifier("noshare", "1").castToResource());
        Relationship ids = new Relationship(null, new Data<>(idList));

        when(tx.getToOneRelation(any(), eq(userModel), any(), any())).thenReturn(noShare);
        when(tx.loadObject(any(), eq(1L), any())).thenReturn(noShare);

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<example.User> userResource =
                new PersistentResource<>(userModel, goodScope.getUUIDFor(userModel), goodScope);

        boolean returnVal = userResource.updateRelation("noShare", ids.toPersistentResources(goodScope));

        assertFalse(returnVal);
        assertEquals(noShare, userModel.getNoShare());
    }

    @Test
    public void testTransferPermissionSuccessOnClearSingularRelationship() {
        example.User userModel = new example.User();
        userModel.setId(1);

        NoShareEntity noShare = new NoShareEntity();

        /* The noshare already exists so no exception should be thrown */
        userModel.setNoShare(noShare);

        List<Resource> empty = new ArrayList<>();
        Relationship ids = new Relationship(null, new Data<>(empty));

        when(tx.getToOneRelation(any(), eq(userModel), any(), any())).thenReturn(noShare);

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<example.User> userResource =
                new PersistentResource<>(userModel, goodScope.getUUIDFor(userModel), goodScope);

        boolean returnVal = userResource.updateRelation("noShare", ids.toPersistentResources(goodScope));

        assertTrue(returnVal);
        assertNull(userModel.getNoShare());
    }

    @Test
    public void testTransferPermissionErrorOnLineageAncestor() {
        NoTransferBiDirectional a = new NoTransferBiDirectional();
        a.setId(1);
        NoTransferBiDirectional b = new NoTransferBiDirectional();
        b.setId(2);
        NoTransferBiDirectional c = new NoTransferBiDirectional();
        c.setId(3);
        a.setOther(b);
        b.setOther(c);

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource aResource = new PersistentResource(a, "1", goodScope);
        PersistentResource bResource = new PersistentResource(b, aResource, "other", "2", goodScope);
        PersistentResource cResource = new PersistentResource(c, bResource, "other", "3", goodScope);

        assertThrows(
                ForbiddenAccessException.class,
                () -> cResource.addRelation("other", aResource));
    }

    @Test
    public void testTransferPermissionSuccessOnLineageParent() {
        NoTransferBiDirectional a = new NoTransferBiDirectional();
        a.setId(1);
        NoTransferBiDirectional b = new NoTransferBiDirectional();
        b.setId(2);
        NoTransferBiDirectional c = new NoTransferBiDirectional();
        c.setId(3);
        a.setOther(b);
        b.setOther(c);

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource aResource = new PersistentResource(a, "1", goodScope);
        PersistentResource bResource = new PersistentResource(b, aResource, "other", "2", goodScope);
        PersistentResource cResource = new PersistentResource(c, bResource, "other", "3", goodScope);

        cResource.addRelation("other", bResource);
    }

    @Test
    public void testNoTransferStrictPermissionFailure() {
        StrictNoTransfer a = new StrictNoTransfer();
        a.setId(1);
        StrictNoTransfer b = new StrictNoTransfer();
        b.setId(2);
        StrictNoTransfer c = new StrictNoTransfer();
        c.setId(3);
        a.setOther(b);
        b.setOther(c);

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource aResource = new PersistentResource(a, "1", goodScope);
        PersistentResource bResource = new PersistentResource(b, aResource, "other", "2", goodScope);
        PersistentResource cResource = new PersistentResource(c, bResource, "other", "3", goodScope);

        assertThrows(
                ForbiddenAccessException.class,
                () -> cResource.addRelation("other", bResource));
    }

    @Test
    public void testCollectionChangeSpecType() {
        Function<String, BiPredicate<ChangeSpec, BiPredicate<Collection, Collection>>> collectionCheck =
                (fieldName) -> (spec, condFn) -> {
                    assertEquals(fieldName, spec.getFieldName());
                    return condFn.test((Collection) spec.getOriginal(), (Collection) spec.getModified());
                };

        // Ensure that change specs coming from collections work properly

        ChangeSpecModel csModel = new ChangeSpecModel((spec) -> collectionCheck
                .apply("testColl")
                .test(spec, (original, modified) -> original == null && modified.equals(Arrays.asList("a", "b", "c"))));

        PersistentResource<ChangeSpecModel> model = bootstrapPersistentResource(csModel, tx);

        when(tx.getToManyRelation(any(), eq(model.obj), any(), any()))
                .thenReturn(new DataStoreIterableBuilder<>().build());

        /* Attributes */
        // Set new data from null
        assertTrue(model.updateAttribute("testColl", Arrays.asList("a", "b", "c")));

        // Set data to empty
        model.getObject().checkFunction = (spec) -> collectionCheck.apply("testColl").test(spec,
                (original, modified) -> original.equals(Arrays.asList("a", "b", "c")) && modified.isEmpty());
        assertTrue(model.updateAttribute("testColl", Lists.newArrayList()));

        model.getObject().checkFunction = (spec) -> collectionCheck
                .apply("testColl")
                .test(
                        spec,
                        (original, modified) -> original.isEmpty() && modified.equals(Arrays.asList("final", "List")));
        // / Overwrite attribute data
        assertTrue(model.updateAttribute("testColl", Arrays.asList("final", "List")));

        /* ToMany relationships */
        // Learn about the other kids
        model.getObject().checkFunction = (spec) -> collectionCheck
                .apply("otherKids")
                .test(
                        spec,
                        (original, modified) ->
                                CollectionUtils.isEmpty(original)
                                        && modified.size() == 1
                                        && modified.contains(new ChangeSpecChild(1)));

        ChangeSpecChild child1 = new ChangeSpecChild(1);
        assertTrue(model.updateRelation("otherKids", Sets.newHashSet(bootstrapPersistentResource(child1))));

        // Add individual
        model.getObject().checkFunction =
                (spec) -> collectionCheck
                        .apply("otherKids")
                        .test(
                                spec,
                                (original, modified) ->
                                        original.equals(Collections.singletonList(new ChangeSpecChild(1)))
                                                && modified.size() == 2
                                                && modified.contains(new ChangeSpecChild(1))
                                                && modified.contains(new ChangeSpecChild(2)));

        ChangeSpecChild child2 = new ChangeSpecChild(2);
        model.addRelation("otherKids", bootstrapPersistentResource(child2));

        model.getObject().checkFunction = (spec) -> collectionCheck
                .apply("otherKids")
                .test(
                        spec,
                        (original, modified) ->
                                original.size() == 2
                                        && original.contains(new ChangeSpecChild(1))
                                        && original.contains(new ChangeSpecChild(2))
                                        && modified.size() == 3
                                        && modified.contains(new ChangeSpecChild(1))
                                        && modified.contains(new ChangeSpecChild(2))
                                        && modified.contains(new ChangeSpecChild(3)));

        ChangeSpecChild child3 = new ChangeSpecChild(3);
        model.addRelation("otherKids", bootstrapPersistentResource(child3));

        // Remove one
        model.getObject().checkFunction = (spec) -> collectionCheck
                .apply("otherKids")
                .test(
                        spec,
                        (original, modified) ->
                                original.size() == 3
                                        && original.contains(new ChangeSpecChild(1))
                                        && original.contains(new ChangeSpecChild(2))
                                        && original.contains(new ChangeSpecChild(3))
                                        && modified.size() == 2
                                        && modified.contains(new ChangeSpecChild(1))
                                        && modified.contains(new ChangeSpecChild(3)));
        model.removeRelation("otherKids", bootstrapPersistentResource(child2));

        when(tx.getToManyRelation(any(), eq(model.obj), any(), any())).thenReturn(
                new DataStoreIterableBuilder(Sets.newHashSet(child1, child3)).build());

        // Clear the rest
        model.getObject().checkFunction = (spec) -> collectionCheck.apply("otherKids").test(spec, (original, modified)
                -> original.size() <= 2 && modified.size() < original.size());
        model.clearRelation("otherKids");
    }

    @Test
    public void testAttrChangeSpecType() {
        BiPredicate<ChangeSpec, BiPredicate<String, String>> attrCheck = (spec, checkFn) -> {
            assertTrue(spec.getModified() instanceof String || spec.getModified() == null);
            assertEquals("testAttr", spec.getFieldName());
            return checkFn.test((String) spec.getOriginal(), (String) spec.getModified());
        };

        PersistentResource<ChangeSpecModel> model = bootstrapPersistentResource(new ChangeSpecModel(
                (spec) -> attrCheck.test(spec, (original, modified) -> (original == null) && "abc".equals(modified))));
        assertTrue(model.updateAttribute("testAttr", "abc"));

        model.getObject().checkFunction = (spec) -> attrCheck.test(
                spec, (original, modified) -> "abc".equals(original) && "replace".equals(modified));
        assertTrue(model.updateAttribute("testAttr", "replace"));

        model.getObject().checkFunction = (spec) -> attrCheck.test(
                spec, (original, modified) -> "replace".equals(original) && modified == null);
        assertTrue(model.updateAttribute("testAttr", null));
    }

    @Test
    public void testRelationChangeSpecType() {
        BiPredicate<ChangeSpec, BiPredicate<ChangeSpecChild, ChangeSpecChild>> relCheck = (spec, checkFn) -> {
            assertTrue(spec.getModified() instanceof ChangeSpecChild || spec.getModified() == null);
            assertEquals("child", spec.getFieldName());
            return checkFn.test((ChangeSpecChild) spec.getOriginal(), (ChangeSpecChild) spec.getModified());
        };

        PersistentResource<ChangeSpecModel> model = bootstrapPersistentResource(new ChangeSpecModel((spec)
                -> relCheck.test(spec, (original, modified)
                -> (original == null) && new ChangeSpecChild(1).equals(modified))), tx);

        when(tx.getToOneRelation(any(), eq(model.obj), any(), any())).thenReturn(null);

        ChangeSpecChild child1 = new ChangeSpecChild(1);
        assertTrue(model.updateRelation("child", Sets.newHashSet(bootstrapPersistentResource(child1, tx))));
        when(tx.getToOneRelation(any(), eq(model.obj), any(), any())).thenReturn(child1);

        model.getObject().checkFunction = (spec) -> relCheck.test(
                spec,
                (original, modified) ->
                        new ChangeSpecChild(1).equals(original) && new ChangeSpecChild(2).equals(modified));

        ChangeSpecChild child2 = new ChangeSpecChild(2);
        assertTrue(model.updateRelation("child", Sets.newHashSet(bootstrapPersistentResource(child2, tx))));

        when(tx.getToOneRelation(any(), eq(model.obj), any(), any())).thenReturn(child2);

        model.getObject().checkFunction = (spec) -> relCheck
                .test(spec, (original, modified) -> new ChangeSpecChild(2).equals(original) && modified == null);
        assertTrue(model.updateRelation("child", null));
    }

    @Test
    public void testPatchRequestScope() {
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        PatchRequestScope parentScope = new PatchRequestScope(
                null,
                "/book",
                NO_VERSION,
                tx,
                new TestUser("1"),
                UUID.randomUUID(),
                null,
                Collections.emptyMap(),
                elideSettings);
        PatchRequestScope scope = new PatchRequestScope(
                parentScope.getPath(), parentScope.getJsonApiDocument(), parentScope);
        // verify wrap works
        assertEquals(parentScope.getUpdateStatusCode(), scope.getUpdateStatusCode());
        assertEquals(parentScope.getObjectEntityCache(), scope.getObjectEntityCache());

        Parent parent = newParent(7);

        PersistentResource<Parent> parentResource = new PersistentResource<>(parent, "1", scope);
        parentResource.updateAttribute("firstName", "foobar");

        ArgumentCaptor<Attribute> attributeArgument = ArgumentCaptor.forClass(Attribute.class);
        verify(tx, times(1)).setAttribute(eq(parent), attributeArgument.capture(), eq(scope));
        assertEquals(attributeArgument.getValue().getName(), "firstName");
        assertEquals(attributeArgument.getValue().getArguments().iterator().next().getValue(), "foobar");
    }

    @Test
    public void testFilterExpressionByType() {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter[author.name][infix]",
                "Hemingway"
        );

        RequestScope scope = buildRequestScope("/", mock(DataStoreTransaction.class),
                new TestUser("1"), queryParams);

        Optional<FilterExpression> filter = scope.getLoadFilterExpression(ClassType.of(Author.class));
        FilterPredicate predicate = (FilterPredicate) filter.get();
        assertEquals("name", predicate.getField());
        assertEquals("name", predicate.getFieldPath());
        assertEquals(Operator.INFIX, predicate.getOperator());
        assertEquals(Arrays.asList("Hemingway"), predicate.getValues());
        assertEquals("[Author].name", predicate.getPath().toString());
    }

    @Test
    public void testFilterExpressionCollection() {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter[book.authors.name][infix]",
                "Hemingway"
        );

        RequestScope scope = buildRequestScope("/", mock(DataStoreTransaction.class), new TestUser("1"),
                queryParams);

        Optional<FilterExpression> filter = scope.getLoadFilterExpression(ClassType.of(Book.class));
        FilterPredicate predicate = (FilterPredicate) filter.get();
        assertEquals("name", predicate.getField());
        assertEquals("authors.name", predicate.getFieldPath());
        assertEquals(Operator.INFIX, predicate.getOperator());
        assertEquals(Arrays.asList("Hemingway"), predicate.getValues());
        assertEquals("[Book].authors/[Author].name", predicate.getPath().toString());
    }


    @Test
    public void testSparseFields() {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add("fields[author]", "name");

        RequestScope scope = buildRequestScope("/", mock(DataStoreTransaction.class),
                new TestUser("1"), queryParams);
        Map<String, Set<String>> expected = ImmutableMap.of("author", ImmutableSet.of("name"));
        assertEquals(expected, scope.getSparseFields());
    }

    @Test
    public void testEqualsAndHashcode() {
        Child childWithId = newChild(1);
        Child childWithoutId = newChild(0);

        RequestScope scope = new TestRequestScope(tx, goodUser, dictionary);

        PersistentResource resourceWithId = new PersistentResource<>(childWithId, scope.getUUIDFor(childWithId), scope);
        PersistentResource resourceWithDifferentId =
                new PersistentResource<>(childWithoutId, scope.getUUIDFor(childWithoutId), scope);
        PersistentResource resourceWithUUID = new PersistentResource<>(childWithoutId, "abc", scope);
        PersistentResource resourceWithIdAndUUID = new PersistentResource<>(childWithId, "abc", scope);

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
