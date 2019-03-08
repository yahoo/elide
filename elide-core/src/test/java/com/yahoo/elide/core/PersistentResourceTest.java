/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.annotation.Audit;
import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.DeletePermission;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.SharePermission;
import com.yahoo.elide.annotation.UpdatePermission;
import com.yahoo.elide.audit.AuditLogger;
import com.yahoo.elide.audit.LogMessage;
import com.yahoo.elide.audit.TestAuditLogger;
import com.yahoo.elide.core.exceptions.ForbiddenAccessException;
import com.yahoo.elide.core.exceptions.InvalidAttributeException;
import com.yahoo.elide.core.exceptions.InvalidObjectIdentifierException;
import com.yahoo.elide.core.exceptions.InvalidValueException;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.jsonapi.models.Data;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.yahoo.elide.jsonapi.models.Relationship;
import com.yahoo.elide.jsonapi.models.Resource;
import com.yahoo.elide.jsonapi.models.ResourceIdentifier;
import com.yahoo.elide.security.ChangeSpec;
import com.yahoo.elide.security.User;
import com.yahoo.elide.security.checks.OperationCheck;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import example.Child;
import example.Color;
import example.ComputedBean;
import example.FirstClassFields;
import example.FunWithPermissions;
import example.Invoice;
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

import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
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

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;


/**
 * Test PersistentResource.
 */
public class PersistentResourceTest extends PersistenceResourceTestSetup {

    private final RequestScope goodUserScope;
    private final RequestScope badUserScope;

    public PersistentResourceTest() {
        goodUserScope = new RequestScope(null, null, mock(DataStoreTransaction.class),
                new User(1), null, elideSettings, false);
        badUserScope = new RequestScope(null, null, mock(DataStoreTransaction.class),
                new User(-1), null, elideSettings, false);
    }

    @BeforeTest
    public void init() {
        dictionary.bindEntity(Child.class);
        dictionary.bindEntity(Parent.class);
        dictionary.bindEntity(FunWithPermissions.class);
        dictionary.bindEntity(Left.class);
        dictionary.bindEntity(Right.class);
        dictionary.bindEntity(NoReadEntity.class);
        dictionary.bindEntity(NoDeleteEntity.class);
        dictionary.bindEntity(NoUpdateEntity.class);
        dictionary.bindEntity(NoCreateEntity.class);
        dictionary.bindEntity(NoShareEntity.class);
        dictionary.bindEntity(example.User.class);
        dictionary.bindEntity(FirstClassFields.class);
        dictionary.bindEntity(MapColorShape.class);
        dictionary.bindEntity(ChangeSpecModel.class);
        dictionary.bindEntity(ChangeSpecChild.class);
        dictionary.bindEntity(Invoice.class);
        dictionary.bindEntity(LineItem.class);
        dictionary.bindEntity(ComputedBean.class);
        dictionary.bindEntity(ContainerWithPackageShare.class);
        dictionary.bindEntity(ShareableWithPackageShare.class);
        dictionary.bindEntity(UnshareableWithEntityUnshare.class);

        reset(goodUserScope.getTransaction());
    }

    @Test
    public void testUpdateToOneRelationHookInAddRelation() {
        FunWithPermissions fun = new FunWithPermissions();
        Child child = newChild(1);

        User goodUser = new User(1);

        DataStoreTransaction tx = mock(DataStoreTransaction.class);

        RequestScope goodScope = new RequestScope(null, null, tx, goodUser, null, elideSettings, false);
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

        RequestScope goodScope = new RequestScope(null, null, tx, goodUser, null, elideSettings, false);
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

        RequestScope goodScope = new RequestScope(null, null, tx, goodUser, null, elideSettings, false);
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

        RequestScope goodScope = new RequestScope(null, null, tx, goodUser, null, elideSettings, false);
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

        RequestScope goodScope = new RequestScope(null, null, tx, goodUser, null, elideSettings, false);
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

        RequestScope goodScope = new RequestScope(null, null, tx, goodUser, null, elideSettings, false);
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

        RequestScope goodScope = new RequestScope(null, null, tx, goodUser, null, elideSettings, false);
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

        RequestScope goodScope = new RequestScope(null, null, tx, goodUser, null, elideSettings, false);
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

        RequestScope goodScope = new RequestScope(null, null, tx, goodUser, null, elideSettings, false);
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

        Assert.assertEquals(relationships.size(), 5, "All relationships should be returned.");
        Assert.assertTrue(relationships.containsKey("relation1"), "relation1 should be present");
        Assert.assertTrue(relationships.containsKey("relation2"), "relation2 should be present");
        Assert.assertTrue(relationships.containsKey("relation3"), "relation3 should be present");
        Assert.assertTrue(relationships.containsKey("relation4"), "relation4 should be present");
        Assert.assertTrue(relationships.containsKey("relation5"), "relation5 should be present");

        PersistentResource<FunWithPermissions> funResourceWithBadScope = new PersistentResource<>(fun, null, "3", badUserScope);
        relationships = funResourceWithBadScope.getRelationships();

        Assert.assertEquals(relationships.size(), 0, "All relationships should be filtered out");
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testNoCreate() {
        Assert.assertNotNull(dictionary);
        NoCreateEntity noCreate = new NoCreateEntity();
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        User goodUser = new User(1);

        when(tx.createNewObject(NoCreateEntity.class)).thenReturn(noCreate);

        RequestScope goodScope = new RequestScope(null, null, tx, goodUser, null, elideSettings, false);
        PersistentResource.createObject(null, NoCreateEntity.class, goodScope, Optional.of("1")); // should throw here
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

        Assert.assertEquals(attributes.size(), 6,
                "A valid user should have access to all attributes that are readable."
        );

        Assert.assertTrue(attributes.containsKey("field2"), "Readable attributes should include field2");
        Assert.assertTrue(attributes.containsKey("field3"), "Readable attributes should include field3");
        Assert.assertTrue(attributes.containsKey("field4"), "Readable attributes should include field4");
        Assert.assertTrue(attributes.containsKey("field5"), "Readable attributes should include field5");
        Assert.assertTrue(attributes.containsKey("field6"), "Readable attributes should include field6");
        Assert.assertTrue(attributes.containsKey("field8"), "Readable attributes should include field8");
        Assert.assertEquals(attributes.get("field2"), null, "field2 should be set to original value.");
        Assert.assertEquals(attributes.get("field3"), "Foobar", "field3 should be set to original value.");
        Assert.assertEquals(attributes.get("field4"), "bar", "field4 should be set to original value.");

        PersistentResource<FunWithPermissions> funResourceBad = new PersistentResource<>(fun, null, "3", badUserScope);

        attributes = funResourceBad.getAttributes();

        Assert.assertEquals(attributes.size(), 3, "An invalid user should have access to a subset of attributes.");
        Assert.assertTrue(attributes.containsKey("field2"), "Readable attributes should include field2");
        Assert.assertTrue(attributes.containsKey("field4"), "Readable attributes should include field4");
        Assert.assertTrue(attributes.containsKey("field5"), "Readable attributes should include field5");
        Assert.assertEquals(attributes.get("field2"), null, "field2 should be set to original value.");
        Assert.assertEquals(attributes.get("field4"), "bar", "field4 should be set to original value.");
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

            Set<PersistentResource> results = PersistentResource.filter(ReadPermission.class, resources);
            Assert.assertEquals(results.size(), 2, "Only a subset of the children are readable");
            Assert.assertTrue(results.contains(child1Resource), "Readable children includes children with positive IDs");
            Assert.assertTrue(results.contains(child3Resource), "Readable children includes children with positive IDs");
        }

        {
            PersistentResource<Child> child1Resource = new PersistentResource<>(child1, null, "1", badUserScope);
            PersistentResource<Child> child2Resource = new PersistentResource<>(child2, null, "-2", badUserScope);
            PersistentResource<Child> child3Resource = new PersistentResource<>(child3, null, "3", badUserScope);
            PersistentResource<Child> child4Resource = new PersistentResource<>(child4, null, "-4", badUserScope);

            Set<PersistentResource> resources =
                    Sets.newHashSet(child1Resource, child2Resource, child3Resource, child4Resource);

            Set<PersistentResource> results = PersistentResource.filter(ReadPermission.class, resources);
            Assert.assertEquals(results.size(), 0, "No children are readable by an invalid user");
        }
    }

    @Test
    public void testGetValue() throws Exception {
        FunWithPermissions fun = new FunWithPermissions();
        fun.setField3("testValue");
        String result;
        result = (String) getValue(fun, "field3",  getRequestScope());
        Assert.assertEquals(result, "testValue", "getValue should set the appropriate value in the resource");

        fun.setField1("testValue2");

        result = (String) getValue(fun, "field1", getRequestScope());
        Assert.assertEquals(result, "testValue2", "getValue should set the appropriate value in the resource");

        Child testChild = newChild(3);
        fun.setRelation1(Sets.newHashSet(testChild));

        @SuppressWarnings("unchecked")
        Set<Child> children = (Set<Child>) getValue(fun, "relation1", getRequestScope());

        Assert.assertTrue(children.contains(testChild), "getValue should set the correct relation.");
        Assert.assertEquals(children.size(), 1, "getValue should set the relation with the correct number of elements");

        ComputedBean computedBean = new ComputedBean();

        String computedTest1 = (String) getValue(computedBean, "test", getRequestScope());
        String computedTest2 = (String) getValue(computedBean, "testWithScope", getRequestScope());
        String computedTest3 = (String) getValue(computedBean, "testWithSecurityScope", getRequestScope());

        Assert.assertEquals(computedTest1, "test1");
        Assert.assertEquals(computedTest2, "test2");
        Assert.assertEquals(computedTest3, "test3");

        try {
            getValue(computedBean, "NonComputedWithScope", getRequestScope());
            Assert.fail("Getting a bad relation should throw an InvalidAttributeException.");
        } catch (InvalidAttributeException e) {
            // Do nothing
        }

        try {
            getValue(fun, "badRelation", getRequestScope());
            Assert.fail("Getting a bad relation should throw an InvalidAttributeException.");
        } catch (InvalidAttributeException e) {
            return;
        }

        Assert.fail("Getting a bad relation should throw an InvalidAttributeException.");
    }

    @Test
    public void testSetValue() throws Exception {
        FunWithPermissions fun = new FunWithPermissions();
        this.obj = fun;
        setValue("field3", "testValue");
        Assert.assertEquals(fun.getField3(), "testValue", "setValue should set the appropriate value in the resource");

        setValue("field1", "testValue2");
        Assert.assertEquals(fun.getField1(), "testValue2", "setValue should set the appropriate value in the resource");

        Child testChild = newChild(3);
        setValue("relation1", Sets.newHashSet(testChild));

        Assert.assertTrue(fun.getRelation1().contains(testChild), "setValue should set the correct relation.");
        Assert.assertEquals(fun.getRelation1().size(), 1, "setValue should set the relation with the correct number of elements");

        try {
            setValue("badRelation", "badValue");
        } catch (InvalidAttributeException e) {
            return;
        }
        Assert.fail("Setting a bad relation should throw an InvalidAttributeException.");
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

        Assert.assertEquals(mapColorShape.getColorShapeMap().get(Color.Red), Shape.Circle);
        Assert.assertEquals(mapColorShape.getColorShapeMap().get(Color.Green), Shape.Square);
        Assert.assertEquals(mapColorShape.getColorShapeMap().get(Color.Violet), Shape.Triangle);
        Assert.assertEquals(mapColorShape.getColorShapeMap().size(), 3);
    }

    @Test(expectedExceptions = {InvalidValueException.class})
    public void testSetMapInvalidColorEnum() {
        this.obj = new MapColorShape();

        HashMap<Object, Object> coerceable = new HashMap<>();
        coerceable.put("InvalidColor", "Circle");
        setValue("colorShapeMap", coerceable);
    }

    @Test(expectedExceptions = {InvalidValueException.class})
    public void testSetMapInvalidShapeEnum() {
        this.obj = new MapColorShape();

        HashMap<Object, Object> coerceable = new HashMap<>();
        coerceable.put("Red", "InvalidShape");
        setValue("colorShapeMap", coerceable);
    }

    @Test
    public void testDeleteBidirectionalRelation() {
        Left left = new Left();
        Right right = new Right();
        left.setOne2one(right);
        right.setOne2one(left);

        PersistentResource<Left> leftResource = new PersistentResource<>(left, null, "3", goodUserScope);

        leftResource.deleteInverseRelation("one2one", right);

        Assert.assertEquals(right.getOne2one(), null, "The one-2-one inverse relationship should have been unset");
        Assert.assertEquals(left.getOne2one(), right, "The owning relationship should NOT have been unset");

        Child child = new Child();
        Parent parent = new Parent();
        child.setParents(Sets.newHashSet(parent));
        parent.setChildren(Sets.newHashSet(child));
        parent.setSpouses(Sets.newHashSet());

        PersistentResource<Child> childResource = new PersistentResource<>(child, null, "4", goodUserScope);

        childResource.deleteInverseRelation("parents", parent);

        Assert.assertEquals(parent.getChildren().size(), 0, "The many-2-many inverse collection should have been cleared.");
        Assert.assertTrue(child.getParents().contains(parent), "The owning relationship should NOT have been touched");
    }

    @Test
    public void testAddBidirectionalRelation() {
        Left left = new Left();
        Right right = new Right();

        PersistentResource<Left> leftResource = new PersistentResource<>(left, null, "3", goodUserScope);

        leftResource.addInverseRelation("one2one", right);

        Assert.assertEquals(right.getOne2one(), left, "The one-2-one inverse relationship should have been updated.");

        Child child = new Child();
        Parent parent = new Parent();
        child.setParents(Sets.newHashSet());
        parent.setChildren(Sets.newHashSet());
        parent.setSpouses(Sets.newHashSet());

        PersistentResource<Child> childResource = new PersistentResource<>(child, null, "4", goodUserScope);

        childResource.addInverseRelation("parents", parent);

        Assert.assertEquals(parent.getChildren().size(), 1, "The many-2-many inverse relationship should have been updated");
        Assert.assertTrue(parent.getChildren().contains(child), "The many-2-many inverse relationship should have been updated");
    }

    @Test
    public void testSuccessfulOneToOneRelationshipAdd() throws Exception {
        User goodUser = new User(1);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        Left left = new Left();
        Right right = new Right();
        left.setId(2);
        right.setId(3);

        RequestScope goodScope = new RequestScope(null, null, tx, goodUser, null, elideSettings, false);

        PersistentResource<Left> leftResource = new PersistentResource<>(left, null, "2", goodScope);

        Relationship ids = new Relationship(null, new Data<>(new ResourceIdentifier("right", "3").castToResource()));

        when(tx.loadObject(eq(Right.class), eq(3L), any(), any())).thenReturn(right);
        boolean updated = leftResource.updateRelation("one2one", ids.toPersistentResources(goodScope));
        goodScope.saveOrCreateObjects();
        verify(tx, times(1)).save(left, goodScope);
        verify(tx, times(1)).save(right, goodScope);
        Assert.assertEquals(updated, true, "The one-2-one relationship should be added.");
        Assert.assertEquals(left.getOne2one().getId(), 3, "The correct object was set in the one-2-one relationship");
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
        RequestScope goodScope = new RequestScope(null, null, tx, goodUser, null, elideSettings, false);

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

        Assert.assertEquals(updated, true, "Many-2-many relationship should be updated.");
        Assert.assertTrue(parent.getChildren().containsAll(expected), "All expected members were updated");
        Assert.assertTrue(expected.containsAll(parent.getChildren()), "All expected members were updated");

        /*
         * No tests for reference integrity since the parent is the owner and
         * this is a many to many relationship.
         */
    }

    @Test
    public void testGetAttributeSuccess() {
        FunWithPermissions fun = new FunWithPermissions();
        fun.setField2("blah");
        fun.setField3(null);

        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "1", goodUserScope);

        String result = (String) funResource.getAttribute("field2");
        Assert.assertEquals(result, "blah", "The correct attribute should be returned.");
        result = (String) funResource.getAttribute("field3");
        Assert.assertEquals(result, null, "The correct attribute should be returned.");
    }

    @Test(expectedExceptions = InvalidAttributeException.class)
    public void testGetAttributeInvalidField() {
        FunWithPermissions fun = new FunWithPermissions();

        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "1", goodUserScope);

        funResource.getAttribute("invalid");
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testGetAttributeInvalidFieldPermissions() {
        FunWithPermissions fun = new FunWithPermissions();
        fun.setField1("foo");

        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "1", goodUserScope);

        funResource.getAttribute("field1");
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testGetAttributeInvalidEntityPermissions() {
        NoReadEntity noread = new NoReadEntity();

        PersistentResource<NoReadEntity> noreadResource = new PersistentResource<>(noread, null, "1", goodUserScope);

        noreadResource.getAttribute("field");
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

        Assert.assertEquals(results.size(), 3, "All of relation elements should be returned.");
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

        Assert.assertEquals(results.size(), 2, "Only filtered relation elements should be returned.");
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
        RequestScope goodScope = new RequestScope(
                "/child", null, tx, goodUser, queryParams, elideSettings, false);

        PersistentResource<Parent> parentResource = new PersistentResource<>(parent, null, "1", goodScope);

        Set<PersistentResource> results = getRelation(parentResource, "children");

        Assert.assertEquals(results.size(), 1);
        Assert.assertEquals(((Child) results.iterator().next().getObject()).getName(), "paul john");
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

        Assert.assertEquals(childResource.getId(), "2");
        Assert.assertEquals(((Child) childResource.getObject()).getName(), "john buzzard");
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testGetRelationForbiddenByEntity() {
        NoReadEntity noread = new NoReadEntity();

        PersistentResource<NoReadEntity> noreadResource = new PersistentResource<>(noread, null, "3", goodUserScope);
        getRelation(noreadResource, "child");
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testGetRelationForbiddenByField() {
        FunWithPermissions fun = new FunWithPermissions();

        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "3", badUserScope);

        getRelation(funResource, "relation1");
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

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testGetRelationForbiddenByEntity2() {
        FirstClassFields firstClassFields = new FirstClassFields();

        PersistentResource<FirstClassFields> fcResource = new PersistentResource<>(firstClassFields, null, "3", badUserScope);

        getRelation(fcResource, "private2");
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testGetAttributeForbiddenByEntity2() {
        FirstClassFields firstClassFields = new FirstClassFields();

        PersistentResource<FirstClassFields> fcResource = new PersistentResource<>(firstClassFields,
                null, "3", goodUserScope);

        fcResource.getAttribute("private1");
    }

    @Test(expectedExceptions = InvalidAttributeException.class)
    public void testGetRelationInvalidRelation() {
        FunWithPermissions fun = new FunWithPermissions();

        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "3", goodUserScope);

        getRelation(funResource, "invalid");
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

        RequestScope goodScope = new RequestScope(null, null, tx, goodUser, null, elideSettings, false);
        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "3", goodScope);

        PersistentResource<?> result = funResource.getRelation("relation2", "1");

        Assert.assertEquals(((Child) result.getObject()).getId(), 1, "The correct relationship element should be returned");
    }

    @Test(expectedExceptions = InvalidObjectIdentifierException.class)
    public void testGetRelationByInvalidId() {
        FunWithPermissions fun = new FunWithPermissions();
        Child child1 = newChild(1);
        Child child2 = newChild(2);
        Child child3 = newChild(3);
        fun.setRelation2(Sets.newHashSet(child1, child2, child3));

        User goodUser = new User(1);

        DataStoreTransaction tx = mock(DataStoreTransaction.class);

        when(tx.getRelation(eq(tx), any(), any(), any(), any(), any(), any())).thenReturn(Sets.newHashSet(child1));

        RequestScope goodScope = new RequestScope(null, null, tx, goodUser, null, elideSettings, false);
        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "3", goodScope);

        funResource.getRelation("relation2", "-1000");
    }

    @Test
    public void testGetRelationsNoEntityAccess() {
        FunWithPermissions fun = new FunWithPermissions();

        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "3", goodUserScope);

        Set set = getRelation(funResource, "relation4");
        Assert.assertEquals(0,  set.size());
    }

    @Test
    public void testGetRelationsNoEntityAccess2() {
        FunWithPermissions fun = new FunWithPermissions();

        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "3", goodUserScope);

        Set set  = getRelation(funResource, "relation5");
        Assert.assertEquals(0,  set.size());
    }

    @Test
    void testDeleteResourceSuccess() {
        Parent parent = newParent(1);

        User goodUser = new User(1);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        RequestScope goodScope = new RequestScope(null, null, tx, goodUser, null, elideSettings, false);

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
        RequestScope goodScope = new RequestScope(null, null, tx, goodUser, null, elideSettings, false);

        PersistentResource<Invoice> invoiceResource = new PersistentResource<>(invoice, null, "1", goodScope);

        invoiceResource.deleteResource();

        verify(tx).delete(invoice, goodScope);

        /* The inverse relation should not be touched for cascading deletes */
        verify(tx, never()).save(item, goodScope);
        Assert.assertEquals(invoice.getItems().size(), 1);
    }

    @Test
    void testDeleteResourceUpdateRelationshipSuccess() {
        Parent parent = new Parent();
        Child child = newChild(100);
        parent.setChildren(Sets.newHashSet(child));
        parent.setSpouses(Sets.newHashSet());

        Set<Parent> parents = Sets.newHashSet(parent);
        child.setParents(Sets.newHashSet(parent));

        Assert.assertFalse(parent.getChildren().isEmpty());

        User goodUser = new User(1);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        when(tx.getRelation(any(), eq(child), eq("parents"), any(), any(), any(), any())).thenReturn(parents);

        RequestScope goodScope = new RequestScope(null, null, tx, goodUser, null, elideSettings, false);

        PersistentResource<Parent> parentResource = new PersistentResource<>(parent, null, "1", goodScope);
        PersistentResource<Child> childResource = new PersistentResource<>(child, parentResource, "1", goodScope);

        childResource.deleteResource();
        goodScope.saveOrCreateObjects();
        verify(tx, times(1)).delete(child, goodScope);
        verify(tx, times(1)).save(parent, goodScope);
        verify(tx, never()).delete(parent, goodScope);
        Assert.assertTrue(parent.getChildren().isEmpty());
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    void testDeleteResourceForbidden() {
        NoDeleteEntity nodelete = new NoDeleteEntity();
        nodelete.setId(1);

        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        User goodUser = new User(1);

        RequestScope goodScope = new RequestScope(null, null, tx, goodUser, null, elideSettings, false);

        PersistentResource<NoDeleteEntity> nodeleteResource = new PersistentResource<>(nodelete, null, "1", goodScope);

        nodeleteResource.deleteResource();

        verify(tx, never()).delete(nodelete, goodScope);
    }

    @Test
    void testAddRelationSuccess() {
        FunWithPermissions fun = new FunWithPermissions();
        fun.setRelation1(Sets.newHashSet());

        Child child = newChild(1);

        User goodUser = new User(1);

        DataStoreTransaction tx = mock(DataStoreTransaction.class);

        RequestScope goodScope = new RequestScope(null, null, tx, goodUser, null, elideSettings, false);
        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "3", goodScope);
        PersistentResource<Child> childResource = new PersistentResource<>(child, null, "1", goodScope);
        funResource.addRelation("relation1", childResource);

        goodScope.saveOrCreateObjects();
        verify(tx, never()).save(child, goodScope); // Child wasn't modified
        verify(tx, times(1)).save(fun, goodScope);

        Assert.assertTrue(fun.getRelation1().contains(child), "The correct element should be added to the relation");
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    void testAddRelationForbiddenByField() {
        FunWithPermissions fun = new FunWithPermissions();
        fun.setRelation1(Sets.newHashSet());

        Child child = newChild(1);

        User badUser = new User(-1);

        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        RequestScope badScope = new RequestScope(null, null, tx, badUser, null, elideSettings, false);
        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "3", badScope);
        PersistentResource<Child> childResource = new PersistentResource<>(child, null, "1", badScope);
        funResource.addRelation("relation1", childResource);
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    void testAddRelationForbiddenByEntity() {
        NoUpdateEntity noUpdate = new NoUpdateEntity();
        noUpdate.setId(1);
        Child child = newChild(2);
        noUpdate.setChildren(Sets.newHashSet());

        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        User goodUser = new User(1);

        RequestScope goodScope = new RequestScope(null, null, tx, goodUser, null, elideSettings, false);
        PersistentResource<NoUpdateEntity> noUpdateResource = new PersistentResource<>(noUpdate, null, "1", goodScope);
        PersistentResource<Child> childResource = new PersistentResource<>(child, null, "2", goodScope);
        noUpdateResource.addRelation("children", childResource);
    }

    @Test(expectedExceptions = InvalidAttributeException.class)
    public void testAddRelationInvalidRelation() {
        FunWithPermissions fun = new FunWithPermissions();

        Child child = newChild(1);

        User goodUser = new User(1);

        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        RequestScope goodScope = new RequestScope(null, null, tx, goodUser, null, elideSettings, false);
        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "3", goodScope);
        PersistentResource<Child> childResource = new PersistentResource<>(child, null, "1", goodScope);
        funResource.addRelation("invalid", childResource);

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
        RequestScope goodScope = new RequestScope(null, null, tx, goodUser, null, elideSettings, false);
        PersistentResource<Child> childResource = new PersistentResource<>(child, null, "1", goodScope);
        PersistentResource<Object> removeResource = new PersistentResource<>(parent1, null, "1", goodScope);
        childResource.removeRelation("parents", removeResource);

        Assert.assertEquals(child.getParents().size(), 2, "The many-2-many relationship should be cleared");
        Assert.assertEquals(parent1.getChildren().size(), 0, "The many-2-many inverse relationship should be cleared");
        Assert.assertEquals(parent3.getChildren().size(), 1, "The many-2-many inverse relationship should not be cleared");
        Assert.assertEquals(parent3.getChildren().size(), 1, "The many-2-many inverse relationship should not be cleared");

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
        RequestScope goodScope = new RequestScope(null, null, tx, goodUser, null, elideSettings, false);

        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "1", goodScope);
        PersistentResource<Object> removeResource = new PersistentResource<>(child, null, "1", goodScope);

        funResource.removeRelation("relation3", removeResource);

        Assert.assertEquals(fun.getRelation3(), null, "The one-2-one relationship should be cleared");

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
        RequestScope goodScope = new RequestScope(null, null, tx, goodUser, null, elideSettings, false);

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
        RequestScope goodScope = new RequestScope(null, null, tx, goodUser, null, elideSettings, false);

        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "1", goodScope);
        PersistentResource<Object> removeResource = new PersistentResource<>(unownedChild, null, "1", goodScope);

        funResource.removeRelation("relation3", removeResource);

        Assert.assertEquals(fun.getRelation3(), ownedChild, "The one-2-one relationship should NOT be cleared");

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
        RequestScope goodScope = new RequestScope(null, null, tx, goodUser, null, elideSettings, false);
        PersistentResource<Child> childResource = new PersistentResource<>(child, null, "1", goodScope);
        PersistentResource<Object> removeResource = new PersistentResource<>(unownedParent, null, "1", goodScope);
        childResource.removeRelation("parents", removeResource);

        Assert.assertEquals(child.getParents().size(), 3, "The many-2-many relationship should not be cleared");
        Assert.assertEquals(parent1.getChildren().size(), 1, "The many-2-many inverse relationship should not be cleared");
        Assert.assertEquals(parent3.getChildren().size(), 1, "The many-2-many inverse relationship should not be cleared");
        Assert.assertEquals(parent3.getChildren().size(), 1, "The many-2-many inverse relationship should not be cleared");

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
        RequestScope goodScope = new RequestScope(null, null, tx, goodUser, null, elideSettings, false);

        PersistentResource<Child> childResource = new PersistentResource<>(child, null, "1", goodScope);

        childResource.clearRelation("parents");

        Assert.assertEquals(child.getParents().size(), 0, "The many-2-many relationship should be cleared");
        Assert.assertEquals(parent1.getChildren().size(), 0, "The many-2-many inverse relationship should be cleared");
        Assert.assertEquals(parent3.getChildren().size(), 0, "The many-2-many inverse relationship should be cleared");
        Assert.assertEquals(parent3.getChildren().size(), 0, "The many-2-many inverse relationship should be cleared");

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
        RequestScope goodScope = new RequestScope(null, null, tx, goodUser, null, elideSettings, false);
        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "1", goodScope);
        funResource.clearRelation("relation3");

        Assert.assertEquals(fun.getRelation3(), null, "The one-2-one relationship should be cleared");

        goodScope.saveOrCreateObjects();
        verify(tx, times(1)).save(fun, goodScope);
        verify(tx, times(1)).save(child, goodScope);
    }

    @Test()
    public void testClearRelationFilteredByReadAccess() {
        User goodUser = new User(1);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        Parent parent = new Parent();
        RequestScope goodScope = new RequestScope(null, null, tx, goodUser, null, elideSettings, false);

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

        Assert.assertEquals(updated, true, "The relationship should have been partially cleared.");
        Assert.assertTrue(parent.getChildren().containsAll(expected), "The unfiltered remaining members are left");
        Assert.assertTrue(expected.containsAll(parent.getChildren()), "The unfiltered remaining members are left");

        /*
         * No tests for reference integrity since the parent is the owner and
         * this is a many to many relationship.
         */
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testClearRelationInvalidToOneUpdatePermission() {
        Left left = new Left();
        left.setId(1);
        Right right = new Right();
        right.setId(1);
        left.setNoUpdateOne2One(right);
        right.setNoUpdateOne2One(left);

        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        User goodUser = new User(1);
        RequestScope goodScope = new RequestScope(null, null, tx, goodUser, null, elideSettings, false);
        PersistentResource<Left> leftResource = new PersistentResource<>(left, null, "1", goodScope);
        leftResource.clearRelation("noUpdateOne2One");
        // Modifications have a deferred check component:
        leftResource.getRequestScope().getPermissionExecutor().executeCommitChecks();
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testNoChangeRelationInvalidToOneUpdatePermission() {
        Left left = new Left();
        left.setId(1);
        Right right = new Right();
        right.setId(1);
        left.setNoUpdateOne2One(right);
        right.setNoUpdateOne2One(left);

        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        User goodUser = new User(1);
        RequestScope goodScope = new RequestScope(null, null, tx, goodUser, null, elideSettings, false);
        PersistentResource<Left> leftResource = new PersistentResource<>(left, null, "1", goodScope);
        leftResource.updateRelation("noUpdateOne2One", getRelation(leftResource, "noUpdateOne2One"));
        // Modifications have a deferred check component:
        leftResource.getRequestScope().getPermissionExecutor().executeCommitChecks();
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
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
        RequestScope goodScope = new RequestScope(null, null, tx, goodUser, null, elideSettings, false);
        PersistentResource<Left> leftResource = new PersistentResource<>(left, null, "1", goodScope);
        leftResource.clearRelation("noInverseUpdate");
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
        RequestScope goodScope = new RequestScope(null, null, tx, goodUser, null, elideSettings, false);
        PersistentResource<Left> leftResource = new PersistentResource<>(left, null, "1", goodScope);
        Assert.assertTrue(leftResource.clearRelation("noDeleteOne2One"));
        Assert.assertNull(leftResource.getObject().getNoDeleteOne2One());

    }

    @Test(expectedExceptions = InvalidAttributeException.class)
    public void testClearRelationInvalidRelation() {
        FunWithPermissions fun = new FunWithPermissions();
        Child child = newChild(1);
        fun.setRelation3(child);

        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        User goodUser = new User(1);
        RequestScope goodScope = new RequestScope(null, null, tx, goodUser, null, elideSettings, false);
        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "1", goodScope);
        funResource.clearRelation("invalid");
    }

    @Test
    public void testUpdateAttributeSuccess() {
        Parent parent = newParent(1);

        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        User goodUser = new User(1);

        RequestScope goodScope = new RequestScope(null, null, tx, goodUser, null, elideSettings, false);
        PersistentResource<Parent> parentResource = new PersistentResource<>(parent, null, "1", goodScope);
        parentResource.updateAttribute("firstName", "foobar");

        Assert.assertEquals(parent.getFirstName(), "foobar", "The attribute was updated successfully");

        goodScope.saveOrCreateObjects();
        verify(tx, times(1)).save(parent, goodScope);
    }

    @Test(expectedExceptions = InvalidAttributeException.class)
    public void testUpdateAttributeInvalidAttribute() {
        Parent parent = newParent(1);

        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        User goodUser = new User(1);

        RequestScope goodScope = new RequestScope(null, null, tx, goodUser, null, elideSettings, false);
        PersistentResource<Parent> parentResource = new PersistentResource<>(parent, null, "1", goodScope);
        parentResource.updateAttribute("invalid", "foobar");
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testUpdateAttributeInvalidUpdatePermission() {
        FunWithPermissions fun = new FunWithPermissions();
        fun.setId(1);


        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        User badUser = new User(-1);

        RequestScope badScope = new RequestScope(null, null, tx, badUser, null, elideSettings, false);

        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "1", badScope);
        funResource.updateAttribute("field4", "foobar");
        // Updates will defer and wait for the end!
        funResource.getRequestScope().getPermissionExecutor().executeCommitChecks();
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testUpdateAttributeInvalidUpdatePermissionNoChange() {
        FunWithPermissions fun = new FunWithPermissions();
        fun.setId(1);

        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        User badUser = new User(-1);

        RequestScope badScope = new RequestScope(null, null, tx, badUser, null, elideSettings, false);

        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "1", badScope);
        funResource.updateAttribute("field4", funResource.getAttribute("field4"));
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

        RequestScope goodScope = new RequestScope(null, null, tx, goodUser, null, elideSettings, false);
        Set<PersistentResource> loaded = PersistentResource.loadRecords(Child.class, new ArrayList<>(),
                Optional.empty(), Optional.empty(), Optional.empty(), goodScope);

        Set<Child> expected = Sets.newHashSet(child1, child4, child5);

        Set<Object> actual = loaded.stream().map(PersistentResource::getObject).collect(Collectors.toSet());

        Assert.assertEquals(actual.size(), 3,
                "The returned list should be filtered to only include elements that have read permission"
        );
        Assert.assertEquals(expected, actual,
                "The returned list should only include elements with a positive ID"
        );
    }

    @Test()
    public void testLoadRecordSuccess() {
        Child child1 = newChild(1);

        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        User goodUser = new User(1);

        when(tx.loadObject(eq(Child.class), eq(1L), any(), any())).thenReturn(child1);

        RequestScope goodScope = new RequestScope(null, null, tx, goodUser, null, elideSettings, false);
        PersistentResource<Child> loaded = PersistentResource.loadRecord(Child.class, "1", goodScope);

        Assert.assertEquals(loaded.getObject(), child1, "The load function should return the requested child object");
    }

    @Test(expectedExceptions = InvalidObjectIdentifierException.class)
    public void testLoadRecordInvalidId() {
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        User goodUser = new User(1);

        when(tx.loadObject(eq(Child.class), eq("1"), any(), any())).thenReturn(null);

        RequestScope goodScope = new RequestScope(null, null, tx, goodUser, null, elideSettings, false);
        PersistentResource.loadRecord(Child.class, "1", goodScope);
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testLoadRecordForbidden() {
        NoReadEntity noRead = new NoReadEntity();
        noRead.setId(1);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        User goodUser = new User(1);

        when(tx.loadObject(eq(NoReadEntity.class), eq(1L), any(), any())).thenReturn(noRead);

        RequestScope goodScope = new RequestScope(null, null, tx, goodUser, null, elideSettings, false);
        PersistentResource.loadRecord(NoReadEntity.class, "1", goodScope);
    }

    @Test()
    public void testCreateObjectSuccess() {
        Parent parent = newParent(1);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        User goodUser = new User(1);

        when(tx.createNewObject(Parent.class)).thenReturn(parent);

        RequestScope goodScope = new RequestScope(null, null, tx, goodUser, null, elideSettings, false);
        PersistentResource<Parent> created = PersistentResource.createObject(null, Parent.class, goodScope, Optional.of("uuid"));
        parent.setChildren(new HashSet<>());
        created.getRequestScope().getPermissionExecutor().executeCommitChecks();

        Assert.assertEquals(created.getObject(), parent,
                "The create function should return the requested parent object"
        );
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testCreateObjectForbidden() {
        NoCreateEntity noCreate = new NoCreateEntity();
        noCreate.setId(1);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        User goodUser = new User(1);

        when(tx.createNewObject(NoCreateEntity.class)).thenReturn(noCreate);

        RequestScope goodScope = new RequestScope(null, null, tx, goodUser, null, elideSettings, false);
        PersistentResource<NoCreateEntity> created = PersistentResource.createObject(null, NoCreateEntity.class, goodScope, Optional.of("1"));
        created.getRequestScope().getPermissionExecutor().executeCommitChecks();
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

        RequestScope badScope = new RequestScope(null, null, tx, badUser, null, elideSettings, false);
        PersistentResource<Left> leftResource = new PersistentResource<>(left, null, badScope.getUUIDFor(left), badScope);

        Assert.assertTrue(leftResource.clearRelation("fieldLevelDelete"));
        Assert.assertEquals(leftResource.getObject().getFieldLevelDelete().size(), 0);
    }


    @Test(expectedExceptions = ForbiddenAccessException.class)
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

        RequestScope goodScope = new RequestScope(null, null, tx, goodUser, null, elideSettings, false);
        PersistentResource<Left> leftResource = new PersistentResource<>(left, null, goodScope.getUUIDFor(left), goodScope);

        leftResource.updateRelation("noInverseUpdate", ids.toPersistentResources(goodScope));
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

        Assert.assertEquals(logger.getMessages().size(), 1, "One message should be logged");

        LogMessage message = logger.getMessages().get(0);
        Assert.assertEquals(message.getMessage(), "UPDATE Child 5 Parent 7", "Logging template should match");

        Assert.assertEquals(message.getOperationCode(), 1, "Operation code should match");
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

        Assert.assertEquals(logger.getMessages().size(), 1, "One message should be logged");

        LogMessage message = logger.getMessages().get(0);
        Assert.assertEquals(message.getMessage(), "CREATE Child 5 Parent 7", "Logging template should match");

        Assert.assertEquals(message.getOperationCode(), 0, "Operation code should match");
    }

    @Test
    public void testOwningRelationshipInverseUpdates() {
        Parent parent = newParent(1);
        Child child = newChild(2);

        User goodUser = new User(1);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        when(tx.getRelation(any(), eq(parent), eq("children"), any(), any(), any(), any())).thenReturn(parent.getChildren());

        RequestScope goodScope = new RequestScope(null, null, tx, goodUser, null, elideSettings, false);

        PersistentResource<Parent> parentResource = new PersistentResource<>(parent, null, goodScope.getUUIDFor(parent), goodScope);
        PersistentResource<Child> childResource = new PersistentResource<>(child, parentResource, goodScope.getUUIDFor(child), goodScope);

        parentResource.addRelation("children", childResource);

        goodScope.saveOrCreateObjects();
        goodScope.getDirtyResources().clear();
        verify(tx, times(1)).save(parent, goodScope);
        verify(tx, times(1)).save(child, goodScope);

        Assert.assertEquals(parent.getChildren().size(), 1, "The owning relationship should be updated");
        Assert.assertTrue(parent.getChildren().contains(child), "The owning relationship should be updated");

        Assert.assertEquals(child.getParents().size(), 1, "The non-owning relationship should also be updated");
        Assert.assertTrue(child.getParents().contains(parent), "The non-owning relationship should also be updated");

        reset(tx);
        parentResource.clearRelation("children");

        goodScope.saveOrCreateObjects();
        verify(tx, times(1)).save(parent, goodScope);
        verify(tx, times(1)).save(child, goodScope);

        Assert.assertEquals(parent.getChildren().size(), 0, "The owning relationship should be updated");
        Assert.assertEquals(child.getParents().size(), 0, "The non-owning relationship should also be updated");
    }

    @Test
    public void testIsIdGenerated() {

        PersistentResource<Child> generated = new PersistentResource<>(new Child(), null, "1", goodUserScope);

        Assert.assertTrue(generated.isIdGenerated(),
                "isIdGenerated returns true when ID field has the GeneratedValue annotation");

        PersistentResource<NoCreateEntity> notGenerated = new PersistentResource<>(new NoCreateEntity(), null, "1", goodUserScope);

        Assert.assertFalse(notGenerated.isIdGenerated(),
                "isIdGenerated returns false when ID field does not have the GeneratedValue annotation");
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
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

        RequestScope goodScope = new RequestScope(null, null, tx, goodUser, null, elideSettings, false);
        PersistentResource<example.User> userResource = new PersistentResource<>(userModel, null, goodScope.getUUIDFor(userModel), goodScope);

        userResource.updateRelation("noShare", ids.toPersistentResources(goodScope));
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
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

        RequestScope goodScope = new RequestScope(null, null, tx, goodUser, null, elideSettings, false);
        PersistentResource<ContainerWithPackageShare> containerResource = new PersistentResource<>(containerWithPackageShare, null, goodScope.getUUIDFor(containerWithPackageShare), goodScope);

        containerResource.updateRelation("unshareableWithEntityUnshares", unShareales.toPersistentResources(goodScope));
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

        RequestScope goodScope = new RequestScope(null, null, tx, goodUser, null, elideSettings, false);
        PersistentResource<ContainerWithPackageShare> containerResource = new PersistentResource<>(containerWithPackageShare, null, goodScope.getUUIDFor(containerWithPackageShare), goodScope);

        containerResource.updateRelation("shareableWithPackageShares", shareables.toPersistentResources(goodScope));

        Assert.assertEquals(containerWithPackageShare.getShareableWithPackageShares().size(), 1);
        Assert.assertTrue(containerWithPackageShare.getShareableWithPackageShares().contains(shareableWithPackageShare));
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
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

        RequestScope goodScope = new RequestScope(null, null, tx, goodUser, null, elideSettings, false);
        PersistentResource<example.User> userResource = new PersistentResource<>(userModel, null, goodScope.getUUIDFor(userModel), goodScope);

        userResource.updateRelation("noShares", ids.toPersistentResources(goodScope));
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

        RequestScope goodScope = new RequestScope(null, null, tx, goodUser, null, elideSettings, false);
        PersistentResource<example.User> userResource = new PersistentResource<>(userModel, null, goodScope.getUUIDFor(userModel), goodScope);

        boolean returnVal = userResource.updateRelation("noShares", ids.toPersistentResources(goodScope));

        Assert.assertTrue(returnVal);
        Assert.assertEquals(userModel.getNoShares().size(), 1);
        Assert.assertTrue(userModel.getNoShares().contains(noShare1));
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

        RequestScope goodScope = new RequestScope(null, null, tx, goodUser, null, elideSettings, false);
        PersistentResource<example.User> userResource = new PersistentResource<>(userModel, null, goodScope.getUUIDFor(userModel), goodScope);

        boolean returnVal = userResource.updateRelation("noShare", ids.toPersistentResources(goodScope));

        Assert.assertFalse(returnVal);
        Assert.assertEquals(userModel.getNoShare(), noShare);
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

        RequestScope goodScope = new RequestScope(null, null, tx, goodUser, null, elideSettings, false);
        PersistentResource<example.User> userResource = new PersistentResource<>(userModel, null, goodScope.getUUIDFor(userModel), goodScope);

        boolean returnVal = userResource.updateRelation("noShare", ids.toPersistentResources(goodScope));

        Assert.assertTrue(returnVal);
        Assert.assertNull(userModel.getNoShare());
    }

    @Test
    public void testCollectionChangeSpecType() {
        Function<String, BiFunction<ChangeSpec, BiFunction<Collection, Collection, Boolean>, Boolean>> collectionCheck =
                (fieldName) -> (spec, condFn) -> {
                    if (!fieldName.equals(spec.getFieldName())) {
                        throw new IllegalStateException("Wrong field name: '" + spec.getFieldName() + "'. Expected: '" + fieldName + "'");
                    }
                    return condFn.apply((Collection) spec.getOriginal(), (Collection) spec.getModified());
                };

        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        // Ensure that change specs coming from collections work properly
        PersistentResource<ChangeSpecModel> model = bootstrapPersistentResource(new ChangeSpecModel((spec) -> collectionCheck
                .apply("testColl")
                .apply(spec, (original, modified) -> original == null && modified.equals(Arrays.asList("a", "b", "c")))));

        //when(tx.getRelation(any(), eq(model.obj), "otherKids", any(), any(), any(), any())).thenReturn(Sets.newHashSet(child1));

        /* Attributes */
        // Set new data from null
        Assert.assertTrue(model.updateAttribute("testColl", Arrays.asList("a", "b", "c")));

        // Set data to empty
        model.getObject().checkFunction = (spec) -> collectionCheck.apply("testColl").apply(spec,
                (original, modified) -> original.equals(Arrays.asList("a", "b", "c")) && modified.isEmpty());
        Assert.assertTrue(model.updateAttribute("testColl", Lists.newArrayList()));

        model.getObject().checkFunction = (spec) -> collectionCheck.apply("testColl")
                .apply(spec, (original, modified) -> original.isEmpty() && modified.equals(Arrays.asList("final", "List")));
        // / Overwrite attribute data
        Assert.assertTrue(model.updateAttribute("testColl", Arrays.asList("final", "List")));

        /* ToMany relationships */
        // Learn about the other kids
        model.getObject().checkFunction = (spec) -> collectionCheck.apply("otherKids").apply(spec, (original, modified) -> (original == null || original.isEmpty()) && modified.size() == 1 && modified.contains(new ChangeSpecChild(1)));

        Assert.assertTrue(model.updateRelation("otherKids", Sets.newHashSet(bootstrapPersistentResource(new ChangeSpecChild(1)))));

        // Add individual
        model.getObject().checkFunction = (spec) -> collectionCheck.apply("otherKids").apply(spec, (original, modified) -> original.equals(Collections.singletonList(new ChangeSpecChild(1))) && modified.size() == 2 && modified.contains(new ChangeSpecChild(1)) && modified.contains(new ChangeSpecChild(2)));
        model.addRelation("otherKids", bootstrapPersistentResource(new ChangeSpecChild(2)));
        model.getObject().checkFunction = (spec) -> collectionCheck.apply("otherKids").apply(spec, (original, modified) -> original.size() == 2 && original.contains(new ChangeSpecChild(1)) && original.contains(new ChangeSpecChild(2)) && modified.size() == 3 && modified.contains(new ChangeSpecChild(1)) && modified.contains(new ChangeSpecChild(2)) && modified.contains(new ChangeSpecChild(3)));
        model.addRelation("otherKids", bootstrapPersistentResource(new ChangeSpecChild(3)));

        // Remove one
        model.getObject().checkFunction = (spec) -> collectionCheck.apply("otherKids").apply(spec, (original, modified) -> original.size() == 3 && original.contains(new ChangeSpecChild(1)) && original.contains(new ChangeSpecChild(2)) && original.contains(new ChangeSpecChild(3)) && modified.size() == 2 && modified.contains(new ChangeSpecChild(1)) && modified.contains(new ChangeSpecChild(3)));
        model.removeRelation("otherKids", bootstrapPersistentResource(new ChangeSpecChild(2)));

        // Clear the rest
        model.getObject().checkFunction = (spec) -> collectionCheck.apply("otherKids").apply(spec, (original, modified) -> original.size() <= 2 && modified.size() < original.size());
        model.clearRelation("otherKids");
    }

    @Test
    public void testAttrChangeSpecType() {
        BiFunction<ChangeSpec, BiFunction<String, String, Boolean>, Boolean> attrCheck = (spec, checkFn) -> {
            if (!(spec.getModified() instanceof String) && spec.getModified() != null) {
                return false;
            }
            if (!"testAttr".equals(spec.getFieldName())) {
                return false;
            }
            return checkFn.apply((String) spec.getOriginal(), (String) spec.getModified());
        };

        PersistentResource<ChangeSpecModel> model = bootstrapPersistentResource(new ChangeSpecModel((spec) -> attrCheck.apply(spec, (original, modified) -> (original == null) && "abc".equals(modified))));
        Assert.assertTrue(model.updateAttribute("testAttr", "abc"));

        model.getObject().checkFunction = (spec) -> attrCheck.apply(spec, (original, modified) -> "abc".equals(original) && "replace".equals(modified));
        Assert.assertTrue(model.updateAttribute("testAttr", "replace"));

        model.getObject().checkFunction = (spec) -> attrCheck.apply(spec, (original, modified) -> "replace".equals(original) && modified == null);
        Assert.assertTrue(model.updateAttribute("testAttr", null));
    }

    @Test
    public void testRelationChangeSpecType() {
        BiFunction<ChangeSpec, BiFunction<ChangeSpecChild, ChangeSpecChild, Boolean>, Boolean> relCheck = (spec, checkFn) -> {
            if (!(spec.getModified() instanceof ChangeSpecChild) && spec.getModified() != null) {
                return false;
            }
            if (!"child".equals(spec.getFieldName())) {
                return false;
            }
            return checkFn.apply((ChangeSpecChild) spec.getOriginal(), (ChangeSpecChild) spec.getModified());
        };
        PersistentResource<ChangeSpecModel> model = bootstrapPersistentResource(new ChangeSpecModel((spec) -> relCheck.apply(spec, (original, modified) -> (original == null) && new ChangeSpecChild(1).equals(modified))));
        Assert.assertTrue(model.updateRelation("child", Sets.newHashSet(bootstrapPersistentResource(new ChangeSpecChild(1)))));

        model.getObject().checkFunction = (spec) -> relCheck.apply(spec, (original, modified) -> new ChangeSpecChild(1).equals(original) && new ChangeSpecChild(2).equals(modified));
        Assert.assertTrue(model.updateRelation("child", Sets.newHashSet(bootstrapPersistentResource(new ChangeSpecChild(2)))));

        model.getObject().checkFunction = (spec) -> relCheck.apply(spec, (original, modified) -> new ChangeSpecChild(2).equals(original) && modified == null);
        Assert.assertTrue(model.updateRelation("child", null));
    }

    @Test
    public void testEqualsAndHashcode() {
        Child childWithId = newChild(1);
        Child childWithoutId = newChild(0);

        PersistentResource resourceWithId = new PersistentResource<>(childWithId, null, goodUserScope.getUUIDFor(childWithId), goodUserScope);
        PersistentResource resourceWithDifferentId = new PersistentResource<>(childWithoutId, null, goodUserScope.getUUIDFor(childWithoutId), goodUserScope);
        PersistentResource resourceWithUUID = new PersistentResource<>(childWithoutId, null, "abc", goodUserScope);
        PersistentResource resourceWithIdAndUUID = new PersistentResource<>(childWithId, null, "abc", goodUserScope);

        Assert.assertNotEquals(resourceWithId, resourceWithUUID);
        Assert.assertNotEquals(resourceWithUUID, resourceWithId);
        Assert.assertNotEquals(resourceWithId.hashCode(), resourceWithUUID.hashCode(), "Hashcodes were equal...");

        Assert.assertEquals(resourceWithId, resourceWithIdAndUUID);
        Assert.assertEquals(resourceWithIdAndUUID, resourceWithId);
        Assert.assertEquals(resourceWithId.hashCode(), resourceWithIdAndUUID.hashCode());

        // Hashcode's should only ever look at UUID's if no real ID is present (i.e. object id is null or 0)
        Assert.assertNotEquals(resourceWithUUID.hashCode(), resourceWithIdAndUUID.hashCode());

        Assert.assertNotEquals(resourceWithId.hashCode(), resourceWithDifferentId.hashCode());
        Assert.assertNotEquals(resourceWithId, resourceWithDifferentId);
        Assert.assertNotEquals(resourceWithDifferentId, resourceWithId);
    }

    private <T> PersistentResource<T> bootstrapPersistentResource(T obj) {
        return bootstrapPersistentResource(obj, mock(DataStoreTransaction.class));
    }

    private <T> PersistentResource<T> bootstrapPersistentResource(T obj, DataStoreTransaction tx) {
        User goodUser = new User(1);
        RequestScope requestScope = new RequestScope(null, null, tx, goodUser, null, elideSettings, false);
        return new PersistentResource<>(obj, null, requestScope.getUUIDFor(obj), requestScope);
    }

    private RequestScope getUserScope(User user, AuditLogger auditLogger) {
        return new RequestScope(null, new JsonApiDocument(), null, user, null,
                new ElideSettingsBuilder(null)
                    .withEntityDictionary(dictionary)
                    .withAuditLogger(auditLogger)
                    .build(), false);
    }

    // Testing constructor, setId and non-null empty sets
    private static Parent newParent(int id) {
        Parent parent = new Parent();
        parent.setId(id);
        parent.setChildren(new HashSet<>());
        parent.setSpouses(new HashSet<>());
        return parent;
    }

    private Parent newParent(int id, Child child) {
        Parent parent = new Parent();
        parent.setId(id);
        parent.setChildren(Sets.newHashSet(child));
        parent.setSpouses(new HashSet<>());
        return parent;
    }


    private static Child newChild(int id) {
        Child child = new Child();
        child.setId(id);
        child.setParents(new HashSet<>());
        child.setFriends(new HashSet<>());
        return child;
    }

    private static Child newChild(int id, String name) {
        Child child = newChild(id);
        child.setName(name);
        return child;
    }

    /* ChangeSpec-specific test elements */
    @Entity
    @Include
    @CreatePermission(expression = "allow all")
    @ReadPermission(expression = "allow all")
    @UpdatePermission(expression = "deny all")
    @DeletePermission(expression = "allow all")
    public static final class ChangeSpecModel {
        @Id
        public long id;

        @ReadPermission(expression = "deny all")
        @UpdatePermission(expression = "deny all")
        public Function<ChangeSpec, Boolean> checkFunction;

        @UpdatePermission(expression = "changeSpecNonCollection")
        public String testAttr;

        @UpdatePermission(expression = "changeSpecCollection")
        public List<String> testColl;

        @OneToOne
        @UpdatePermission(expression = "changeSpecNonCollection")
        public ChangeSpecChild child;

        @ManyToMany
        @UpdatePermission(expression = "changeSpecCollection")
        public List<Child> otherKids;

        public ChangeSpecModel(final Function<ChangeSpec, Boolean> checkFunction) {
            this.checkFunction = checkFunction;
        }
    }

    @Entity
    @Include
    @EqualsAndHashCode
    @AllArgsConstructor
    @CreatePermission(expression = "allow all")
    @ReadPermission(expression = "allow all")
    @UpdatePermission(expression = "allow all")
    @DeletePermission(expression = "allow all")
    @SharePermission
    public static final class ChangeSpecChild {
        @Id
        public long id;
    }

    public static final class ChangeSpecCollection extends OperationCheck<Object> {

        @Override
        public boolean ok(Object object, com.yahoo.elide.security.RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
            if (changeSpec.isPresent() && (object instanceof ChangeSpecModel)) {
                ChangeSpec spec = changeSpec.get();
                if (!(spec.getModified() instanceof Collection)) {
                    return false;
                }
                return ((ChangeSpecModel) object).checkFunction.apply(spec);
            }
            throw new IllegalStateException("Something is terribly wrong :(");
        }
    }

    public static final class ChangeSpecNonCollection extends OperationCheck<Object> {

        @Override
        public boolean ok(Object object, com.yahoo.elide.security.RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
            if (changeSpec.isPresent() && (object instanceof ChangeSpecModel)) {
                return ((ChangeSpecModel) object).checkFunction.apply(changeSpec.get());
            }
            throw new IllegalStateException("Something is terribly wrong :(");
        }
    }

    public static Set<PersistentResource> getRelation(PersistentResource resource, String relation) {
        Optional<FilterExpression> filterExpression =
                resource.getRequestScope().getExpressionForRelation(resource, relation);

        return resource.getRelationCheckedFiltered(relation, filterExpression, Optional.empty(), Optional.empty());
    }
}
