/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
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
import com.yahoo.elide.jsonapi.models.Data;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.yahoo.elide.jsonapi.models.Relationship;
import com.yahoo.elide.jsonapi.models.Resource;
import com.yahoo.elide.jsonapi.models.ResourceIdentifier;
import com.yahoo.elide.security.ChangeSpec;
import com.yahoo.elide.security.checks.prefab.Role;
import com.yahoo.elide.security.User;
import com.yahoo.elide.security.checks.OperationCheck;
import example.Child;
import example.Color;
import example.FirstClassFields;
import example.FunWithPermissions;
import example.Left;
import example.MapColorShape;
import example.NoCreateEntity;
import example.NoDeleteEntity;
import example.NoReadEntity;
import example.NoShareEntity;
import example.NoUpdateEntity;
import example.Parent;
import example.Right;
import example.Shape;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
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
import java.util.stream.Collectors;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 * Test PersistentResource.
 */
public class PersistentResourceTest extends PersistentResource {
    private final RequestScope goodUserScope;
    private final RequestScope badUserScope;
    private static final AuditLogger MOCK_AUDIT_LOGGER = mock(AuditLogger.class);

    public PersistentResourceTest() {
        super(new Child(), null, new RequestScope(null, null, null, new EntityDictionary(), null, MOCK_AUDIT_LOGGER));
        goodUserScope = new RequestScope(null, null, new User(1), dictionary, null, MOCK_AUDIT_LOGGER);
        badUserScope = new RequestScope(null, null, new User(-1), dictionary, null, MOCK_AUDIT_LOGGER);
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
    }

    @Test
    public void testGetRelationships() {
        FunWithPermissions fun = new FunWithPermissions();
        fun.setRelation1(Sets.newHashSet());
        fun.relation2 = Sets.newHashSet();
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

    @Test
    public void testGetAttributes() {
        FunWithPermissions fun = new FunWithPermissions();
        fun.field3 = "Foobar";
        fun.setField1("blah");
        fun.setField2(null);
        fun.field4 = "bar";

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

            Set<PersistentResource<Child>> resources =
                    Sets.newHashSet(child1Resource, child2Resource, child3Resource, child4Resource);

            Set<PersistentResource<Child>> results = filter(ReadPermission.class, resources);
            Assert.assertEquals(results.size(), 2, "Only a subset of the children are readable");
            Assert.assertTrue(results.contains(child1Resource), "Readable children includes children with positive IDs");
            Assert.assertTrue(results.contains(child3Resource), "Readable children includes children with positive IDs");
        }

        {
            PersistentResource<Child> child1Resource = new PersistentResource<>(child1, null, "1", badUserScope);
            PersistentResource<Child> child2Resource = new PersistentResource<>(child2, null, "-2", badUserScope);
            PersistentResource<Child> child3Resource = new PersistentResource<>(child3, null, "3", badUserScope);
            PersistentResource<Child> child4Resource = new PersistentResource<>(child4, null, "-4", badUserScope);

            Set<PersistentResource<Child>> resources =
                    Sets.newHashSet(child1Resource, child2Resource, child3Resource, child4Resource);

            Set<PersistentResource<Child>> results = filter(ReadPermission.class, resources);
            Assert.assertEquals(results.size(), 0, "No children are readable by an invalid user");
        }
    }

    @Test
    public void testGetValue() throws Exception {
        FunWithPermissions fun = new FunWithPermissions();
        fun.field3 = "testValue";
        String result;
        result = (String) getValue(fun, "field3",  dictionary);
        Assert.assertEquals(result, "testValue", "getValue should set the appropriate value in the resource");

        fun.setField1("testValue2");

        result = (String) getValue(fun, "field1", dictionary);
        Assert.assertEquals(result, "testValue2", "getValue should set the appropriate value in the resource");

        Child testChild = newChild(3);
        fun.setRelation1(Sets.newHashSet(testChild));

        @SuppressWarnings("unchecked")
        Set<Child> children = (Set<Child>) getValue(fun, "relation1", dictionary);

        Assert.assertTrue(children.contains(testChild), "getValue should set the correct relation.");
        Assert.assertEquals(children.size(), 1, "getValue should set the relation with the correct number of elements");

        try {
            getValue(fun, "badRelation", dictionary);
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
        Assert.assertEquals(fun.field3, "testValue", "setValue should set the appropriate value in the resource");

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
        MapColorShape mapColorShape = new MapColorShape();
        this.obj = mapColorShape;

        HashMap<Object, Object> coerceable = new HashMap<>();
        coerceable.put("InvalidColor", "Circle");
        setValue("colorShapeMap", coerceable);
    }

    @Test(expectedExceptions = {InvalidValueException.class})
    public void testSetMapInvalidShapeEnum() {
        MapColorShape mapColorShape = new MapColorShape();
        this.obj = mapColorShape;

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

        RequestScope goodScope = new RequestScope(null, tx, goodUser, dictionary, null, MOCK_AUDIT_LOGGER);

        PersistentResource<Left> leftResource = new PersistentResource<>(left, null, "2", goodScope);

        Relationship ids = new Relationship(null, new Data<>(new ResourceIdentifier("right", "3").castToResource()));

        when(tx.loadObject(Right.class, 3L)).thenReturn(right);
        boolean updated = leftResource.updateRelation("one2one", ids.toPersistentResources(goodScope));
        verify(tx).save(left);
        verify(tx).save(right);

        Assert.assertEquals(updated, true, "The one-2-one relationship should be added.");
        Assert.assertEquals(left.getOne2one().getId(), 3, "The correct object was set in the one-2-one relationship");
    }

    @Test
    /**
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
        RequestScope goodScope = new RequestScope(null, tx, goodUser, dictionary, null, MOCK_AUDIT_LOGGER);

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

        PersistentResource<Parent> parentResource = new PersistentResource<>(parent, null, "1", goodScope);

        //Requested = (3,6)
        List<Resource> idList = new ArrayList<>();
        idList.add(new ResourceIdentifier("child", "3").castToResource());
        idList.add(new ResourceIdentifier("child", "6").castToResource());
        Relationship ids = new Relationship(null, new Data<>(idList));


        when(tx.loadObject(Child.class, 2L)).thenReturn(child2);
        when(tx.loadObject(Child.class, 3L)).thenReturn(child3);
        when(tx.loadObject(Child.class, -4L)).thenReturn(child4);
        when(tx.loadObject(Child.class, -5L)).thenReturn(child5);
        when(tx.loadObject(Child.class, 6L)).thenReturn(child6);

        //Final set after operation = (3,4,5,6)
        Set<Child> expected = new HashSet<>();
        expected.add(child3);
        expected.add(child4);
        expected.add(child5);
        expected.add(child6);

        boolean updated = parentResource.updateRelation("children", ids.toPersistentResources(goodScope));

        verify(tx).save(parent);
        verify(tx).save(child1);
        verify(tx).save(child2);
        verify(tx).save(child6);
        verify(tx, never()).save(child4);
        verify(tx, never()).save(child5);
        verify(tx, never()).save(child3);

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
        fun.field3 = null;

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
        fun.relation2 = Sets.newHashSet(child1, child2, child3);

        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "3", goodUserScope);

        Set<PersistentResource> results = funResource.getRelationCheckedFiltered("relation2");

        Assert.assertEquals(results.size(), 3, "All of relation elements should be returned.");
    }

    @Test
    public void testGetRelationFilteredSuccess() {
        FunWithPermissions fun = new FunWithPermissions();
        Child child1 = newChild(1);
        Child child2 = newChild(-2);
        Child child3 = newChild(3);
        fun.relation2 = Sets.newHashSet(child1, child2, child3);

        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "3", goodUserScope);

        Set<PersistentResource> results = funResource.getRelationCheckedFiltered("relation2");

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
        when(tx.filterCollection(anyCollection(), any(), any()))
                .thenReturn(Sets.newHashSet(child1));
        User goodUser = new User(1);

        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.add("filter[child.name]", "paul john");
        RequestScope goodScope = new RequestScope(null, tx, goodUser, dictionary, null, MOCK_AUDIT_LOGGER, queryParams);

        PersistentResource<Parent> parentResource = new PersistentResource<>(parent, null, "1", goodScope);

        Set<PersistentResource> results = parentResource.getRelationCheckedFiltered("children");

        Assert.assertEquals(results.size(), 1);
        Assert.assertEquals(((Child) results.iterator().next().getObject()).getName(), "paul john");
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testGetRelationForbiddenByEntity() {
        NoReadEntity noread = new NoReadEntity();

        PersistentResource<NoReadEntity> noreadResource = new PersistentResource<>(noread, null, "3", goodUserScope);
        noreadResource.getRelationCheckedFiltered("child");
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testGetRelationForbiddenByField() {
        FunWithPermissions fun = new FunWithPermissions();

        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "3", badUserScope);

        funResource.getRelationCheckedFiltered("relation1");
    }

    @Test
    public void testGetRelationForbiddenByEntityAllowedByField() {
        FirstClassFields firstClassFields = new FirstClassFields();

        PersistentResource<FirstClassFields> fcResource = new PersistentResource<>(firstClassFields, null, "3", badUserScope);

        fcResource.getRelationCheckedFiltered("public2");
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

        fcResource.getRelationCheckedFiltered("private2");
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

        funResource.getRelationCheckedFiltered("invalid");
    }

    @Test
    public void testGetRelationByIdSuccess() {
        FunWithPermissions fun = new FunWithPermissions();
        Child child1 = newChild(1);
        Child child2 = newChild(2);
        Child child3 = newChild(3);
        fun.relation2 = Sets.newHashSet(child1, child2, child3);

        User goodUser = new User(1);

        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        when(tx.filterCollection(anyCollection(), any(), any()))
                .thenReturn(Sets.newHashSet(child1));

        RequestScope goodScope = new RequestScope(null, tx, goodUser, dictionary, null, MOCK_AUDIT_LOGGER);
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
        fun.relation2 = Sets.newHashSet(child1, child2, child3);

        User goodUser = new User(1);

        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        when(tx.filterCollection(anyCollection(), any(), any())).thenReturn(Collections.emptySet());

        RequestScope goodScope = new RequestScope(null, tx, goodUser, dictionary, null, MOCK_AUDIT_LOGGER);
        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "3", goodScope);

        funResource.getRelation("relation2", "-1000");
    }

    @Test
    public void testGetRelationsNoEntityAccess() {
        FunWithPermissions fun = new FunWithPermissions();

        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "3", goodUserScope);

        Set set = funResource.getRelationCheckedFiltered("relation4");
        Assert.assertEquals(0,  set.size());
    }

    @Test
    public void testGetRelationsNoEntityAccess2() {
        FunWithPermissions fun = new FunWithPermissions();

        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "3", goodUserScope);

        Set set = funResource.getRelationCheckedFiltered("relation5");
        Assert.assertEquals(0,  set.size());
    }

    @Test
    void testDeleteResourceSuccess() {
        Parent parent = newParent(1);

        User goodUser = new User(1);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        RequestScope goodScope = new RequestScope(null, tx, goodUser, dictionary, null, MOCK_AUDIT_LOGGER);

        PersistentResource<Parent> parentResource = new PersistentResource<>(parent, null, "1", goodScope);

        parentResource.deleteResource();

        verify(tx).delete(parent);
    }

    @Test
    void testDeleteResourceUpdateRelationshipSuccess() {
        Parent parent = new Parent();
        Child child = newChild(100);
        parent.setChildren(Sets.newHashSet(child));
        parent.setSpouses(Sets.newHashSet());
        child.setParents(Sets.newHashSet(parent));

        Assert.assertFalse(parent.getChildren().isEmpty());

        User goodUser = new User(1);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        RequestScope goodScope = new RequestScope(null, tx, goodUser, dictionary, null, MOCK_AUDIT_LOGGER);

        PersistentResource<Parent> parentResource = new PersistentResource<>(parent, null, "1", goodScope);
        PersistentResource<Child> childResource = new PersistentResource<>(child, parentResource, "1", goodScope);

        childResource.deleteResource();

        verify(tx).delete(child);
        verify(tx).save(parent);
        verify(tx, never()).delete(parent);
        Assert.assertTrue(parent.getChildren().isEmpty());
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    void testDeleteResourceForbidden() {
        NoDeleteEntity nodelete = new NoDeleteEntity();
        nodelete.setId(1);

        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        User goodUser = new User(1);

        RequestScope goodScope = new RequestScope(null, tx, goodUser, dictionary, null, MOCK_AUDIT_LOGGER);

        PersistentResource<NoDeleteEntity> nodeleteResource = new PersistentResource<>(nodelete, null, "1", goodScope);

        nodeleteResource.deleteResource();

        verify(tx, never()).delete(nodelete);
    }

    @Test
    void testAddRelationSuccess() {
        FunWithPermissions fun = new FunWithPermissions();
        fun.setRelation1(Sets.newHashSet());

        Child child = newChild(1);

        User goodUser = new User(1);

        DataStoreTransaction tx = mock(DataStoreTransaction.class);

        RequestScope goodScope = new RequestScope(null, tx, goodUser, dictionary, null, MOCK_AUDIT_LOGGER);
        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "3", goodScope);
        PersistentResource<Child> childResource = new PersistentResource<>(child, null, "1", goodScope);
        funResource.addRelation("relation1", childResource);

        verify(tx).save(child);
        verify(tx).save(fun);

        Assert.assertTrue(fun.getRelation1().contains(child), "The correct element should be added to the relation");
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    void testAddRelationForbiddenByField() {
        FunWithPermissions fun = new FunWithPermissions();
        fun.setRelation1(Sets.newHashSet());

        Child child = newChild(1);

        User badUser = new User(-1);

        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        RequestScope badScope = new RequestScope(null, tx, badUser, dictionary, null, MOCK_AUDIT_LOGGER);
        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "3", badScope);
        PersistentResource<Child> childResource = new PersistentResource<>(child, null, "1", badScope);
        funResource.addRelation("relation1", childResource);
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    void testAddRelationForbiddenByEntity() {
        NoUpdateEntity noUpdate = new NoUpdateEntity();
        noUpdate.setId(1);
        Child child = newChild(2);
        noUpdate.children = Sets.newHashSet();

        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        User goodUser = new User(1);

        RequestScope goodScope = new RequestScope(null, tx, goodUser, dictionary, null, MOCK_AUDIT_LOGGER);
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
        RequestScope goodScope = new RequestScope(null, tx, goodUser, dictionary, null, MOCK_AUDIT_LOGGER);
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
        RequestScope goodScope = new RequestScope(null, tx, goodUser, dictionary, null, MOCK_AUDIT_LOGGER);
        PersistentResource<Child> childResource = new PersistentResource<>(child, null, "1", goodScope);
        PersistentResource<Object> removeResource = new PersistentResource<>(parent1, null, "1", goodScope);
        childResource.removeRelation("parents", removeResource);

        Assert.assertEquals(child.getParents().size(), 2, "The many-2-many relationship should be cleared");
        Assert.assertEquals(parent1.getChildren().size(), 0, "The many-2-many inverse relationship should be cleared");
        Assert.assertEquals(parent3.getChildren().size(), 1, "The many-2-many inverse relationship should not be cleared");
        Assert.assertEquals(parent3.getChildren().size(), 1, "The many-2-many inverse relationship should not be cleared");

        verify(tx).save(child);
        verify(tx).save(parent1);
        verify(tx, never()).save(parent2);
        verify(tx, never()).save(parent3);
    }

    @Test()
    public void testRemoveToOneRelationSuccess() {
        FunWithPermissions fun = new FunWithPermissions();
        Child child = newChild(1);
        fun.setRelation3(child);

        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        User goodUser = new User(1);
        RequestScope goodScope = new RequestScope(null, tx, goodUser, dictionary, null, MOCK_AUDIT_LOGGER);

        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "1", goodScope);
        PersistentResource<Object> removeResource = new PersistentResource<>(child, null, "1", goodScope);

        funResource.removeRelation("relation3", removeResource);

        Assert.assertEquals(fun.getRelation3(), null, "The one-2-one relationship should be cleared");

        verify(tx).save(fun);
        verify(tx).save(child);
    }

    @Test()
    public void testRemoveNonexistingToOneRelation() {
        FunWithPermissions fun = new FunWithPermissions();
        Child ownedChild = newChild(1);
        Child unownedChild = newChild(2);
        fun.setRelation3(ownedChild);

        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        User goodUser = new User(1);
        RequestScope goodScope = new RequestScope(null, tx, goodUser, dictionary, null, MOCK_AUDIT_LOGGER);

        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "1", goodScope);
        PersistentResource<Object> removeResource = new PersistentResource<>(unownedChild, null, "1", goodScope);

        funResource.removeRelation("relation3", removeResource);

        Assert.assertEquals(fun.getRelation3(), ownedChild, "The one-2-one relationship should NOT be cleared");

        verify(tx, never()).save(fun);
        verify(tx, never()).save(ownedChild);
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
        RequestScope goodScope = new RequestScope(null, tx, goodUser, dictionary, null, MOCK_AUDIT_LOGGER);
        PersistentResource<Child> childResource = new PersistentResource<>(child, null, "1", goodScope);
        PersistentResource<Object> removeResource = new PersistentResource<>(unownedParent, null, "1", goodScope);
        childResource.removeRelation("parents", removeResource);

        Assert.assertEquals(child.getParents().size(), 3, "The many-2-many relationship should not be cleared");
        Assert.assertEquals(parent1.getChildren().size(), 1, "The many-2-many inverse relationship should not be cleared");
        Assert.assertEquals(parent3.getChildren().size(), 1, "The many-2-many inverse relationship should not be cleared");
        Assert.assertEquals(parent3.getChildren().size(), 1, "The many-2-many inverse relationship should not be cleared");

        verify(tx, never()).save(child);
        verify(tx, never()).save(parent1);
        verify(tx, never()).save(parent2);
        verify(tx, never()).save(parent3);
    }

    @Test()
    public void testClearToManyRelationSuccess() {
        Child child = newChild(1);
        Parent parent1 = newParent(1, child);
        Parent parent2 = newParent(2, child);
        Parent parent3 = newParent(3, child);
        child.setParents(Sets.newHashSet(parent1, parent2, parent3));

        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        User goodUser = new User(1);
        RequestScope goodScope = new RequestScope(null, tx, goodUser, dictionary, null, MOCK_AUDIT_LOGGER);

        PersistentResource<Child> childResource = new PersistentResource<>(child, null, "1", goodScope);

        childResource.clearRelation("parents");

        Assert.assertEquals(child.getParents().size(), 0, "The many-2-many relationship should be cleared");
        Assert.assertEquals(parent1.getChildren().size(), 0, "The many-2-many inverse relationship should be cleared");
        Assert.assertEquals(parent3.getChildren().size(), 0, "The many-2-many inverse relationship should be cleared");
        Assert.assertEquals(parent3.getChildren().size(), 0, "The many-2-many inverse relationship should be cleared");

        verify(tx).save(child);
        verify(tx).save(parent1);
        verify(tx).save(parent2);
        verify(tx).save(parent3);
    }

    @Test()
    public void testClearToOneRelationSuccess() {
        FunWithPermissions fun = new FunWithPermissions();
        Child child = newChild(1);
        fun.setRelation3(child);

        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        User goodUser = new User(1);
        RequestScope goodScope = new RequestScope(null, tx, goodUser, dictionary, null, MOCK_AUDIT_LOGGER);
        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "1", goodScope);
        funResource.clearRelation("relation3");

        Assert.assertEquals(fun.getRelation3(), null, "The one-2-one relationship should be cleared");

        verify(tx).save(fun);
        verify(tx).save(child);
    }

    @Test()
    public void testClearRelationFilteredByReadAccess() {
        User goodUser = new User(1);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        Parent parent = new Parent();
        RequestScope goodScope = new RequestScope(null, tx, goodUser, dictionary, null, MOCK_AUDIT_LOGGER);

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

        PersistentResource<Parent> parentResource = new PersistentResource<>(parent, null, "1", goodScope);

        //Final set after operation = (4,5)
        Set<Child> expected = new HashSet<>();
        expected.add(child4);
        expected.add(child5);

        boolean updated = parentResource.clearRelation("children");

        verify(tx).save(parent);
        verify(tx).save(child1);
        verify(tx).save(child2);
        verify(tx).save(child3);
        verify(tx, never()).save(child4);
        verify(tx, never()).save(child5);

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
        left.noUpdateOne2One = right;
        right.noUpdateOne2One = left;

        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        User goodUser = new User(1);
        RequestScope goodScope = new RequestScope(null, tx, goodUser, dictionary, null, MOCK_AUDIT_LOGGER);
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
        left.noUpdateOne2One = right;
        right.noUpdateOne2One = left;

        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        User goodUser = new User(1);
        RequestScope goodScope = new RequestScope(null, tx, goodUser, dictionary, null, MOCK_AUDIT_LOGGER);
        PersistentResource<Left> leftResource = new PersistentResource<>(left, null, "1", goodScope);
        leftResource.updateRelation("noUpdateOne2One", leftResource.getRelationCheckedFiltered("noUpdateOne2One"));
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
        left.noInverseUpdate = Sets.newHashSet(right1, right2);
        right1.noUpdate = Sets.newHashSet(left);
        right2.noUpdate = Sets.newHashSet(left);


        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        User goodUser = new User(1);
        RequestScope goodScope = new RequestScope(null, tx, goodUser, dictionary, null, MOCK_AUDIT_LOGGER);
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
        left.noDeleteOne2One = noDelete;

        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        User goodUser = new User(1);
        RequestScope goodScope = new RequestScope(null, tx, goodUser, dictionary, null, MOCK_AUDIT_LOGGER);
        PersistentResource<Left> leftResource = new PersistentResource<>(left, null, "1", goodScope);
        Assert.assertTrue(leftResource.clearRelation("noDeleteOne2One"));
        Assert.assertNull(leftResource.getObject().noDeleteOne2One);

    }

    @Test(expectedExceptions = InvalidAttributeException.class)
    public void testClearRelationInvalidRelation() {
        FunWithPermissions fun = new FunWithPermissions();
        Child child = newChild(1);
        fun.setRelation3(child);

        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        User goodUser = new User(1);
        RequestScope goodScope = new RequestScope(null, tx, goodUser, dictionary, null, MOCK_AUDIT_LOGGER);
        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "1", goodScope);
        funResource.clearRelation("invalid");
    }

    @Test
    public void testUpdateAttributeSuccess() {
        Parent parent = newParent(1);

        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        User goodUser = new User(1);

        RequestScope goodScope = new RequestScope(null, tx, goodUser, dictionary, null, MOCK_AUDIT_LOGGER);
        PersistentResource<Parent> parentResource = new PersistentResource<>(parent, null, "1", goodScope);
        parentResource.updateAttribute("firstName", "foobar");

        Assert.assertEquals(parent.getFirstName(), "foobar", "The attribute was updated successfully");
        verify(tx).save(parent);
    }

    @Test(expectedExceptions = InvalidAttributeException.class)
    public void testUpdateAttributeInvalidAttribute() {
        Parent parent = newParent(1);

        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        User goodUser = new User(1);

        RequestScope goodScope = new RequestScope(null, tx, goodUser, dictionary, null, MOCK_AUDIT_LOGGER);
        PersistentResource<Parent> parentResource = new PersistentResource<>(parent, null, "1", goodScope);
        parentResource.updateAttribute("invalid", "foobar");
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testUpdateAttributeInvalidUpdatePermission() {
        FunWithPermissions fun = new FunWithPermissions();
        fun.setId(1);


        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        User badUser = new User(-1);

        RequestScope badScope = new RequestScope(null, tx, badUser, dictionary, null, MOCK_AUDIT_LOGGER);

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

        RequestScope badScope = new RequestScope(null, tx, badUser, dictionary, null, MOCK_AUDIT_LOGGER);

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

        when(tx.loadObjects(eq(Child.class), anyObject()))
                .thenReturn(Lists.newArrayList(child1, child2, child3, child4, child5));

        RequestScope goodScope = new RequestScope(null, tx, goodUser, dictionary, null, MOCK_AUDIT_LOGGER);
        Set<PersistentResource<Child>> loaded = PersistentResource.loadRecords(Child.class, goodScope);

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

        when(tx.loadObject(Child.class, 1L)).thenReturn(child1);

        RequestScope goodScope = new RequestScope(null, tx, goodUser, dictionary, null, MOCK_AUDIT_LOGGER);
        PersistentResource<Child> loaded = PersistentResource.loadRecord(Child.class, "1", goodScope);

        Assert.assertEquals(loaded.getObject(), child1,
                "The load function should return the requested child object"
        );
    }

    @Test(expectedExceptions = InvalidObjectIdentifierException.class)
    public void testLoadRecordInvalidId() {
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        User goodUser = new User(1);

        when(tx.loadObject(Child.class, "1")).thenReturn(null);

        RequestScope goodScope = new RequestScope(null, tx, goodUser, dictionary, null, MOCK_AUDIT_LOGGER);
        PersistentResource.loadRecord(Child.class, "1", goodScope);
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testLoadRecordForbidden() {
        NoReadEntity noRead = new NoReadEntity();
        noRead.setId(1);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        User goodUser = new User(1);

        when(tx.loadObject(NoReadEntity.class, 1L)).thenReturn(noRead);

        RequestScope goodScope = new RequestScope(null, tx, goodUser, dictionary, null, MOCK_AUDIT_LOGGER);
        PersistentResource.loadRecord(NoReadEntity.class, "1", goodScope);
    }

    @Test()
    public void testCreateObjectSuccess() {
        Parent parent = newParent(1);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        User goodUser = new User(1);

        when(tx.createObject(Parent.class)).thenReturn(parent);

        RequestScope goodScope = new RequestScope(null, tx, goodUser, dictionary, null, MOCK_AUDIT_LOGGER);
        PersistentResource<Parent> created = PersistentResource.createObject(Parent.class, goodScope, "uuid");
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

        when(tx.createObject(NoCreateEntity.class)).thenReturn(noCreate);

        RequestScope goodScope = new RequestScope(null, tx, goodUser, dictionary, null, MOCK_AUDIT_LOGGER);
        PersistentResource<NoCreateEntity> created = PersistentResource.createObject(NoCreateEntity.class, goodScope, "uuid");
        created.getRequestScope().getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void testDeletePermissionCheckedOnInverseRelationship() {
        Left left = new Left();
        left.setId(1);
        Right right = new Right();
        right.setId(2);

        left.fieldLevelDelete = Sets.newHashSet(right);
        right.allowDeleteAtFieldLevel = Sets.newHashSet(left);

        //Bad User triggers the delete permission failure
        User badUser = new User(-1);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        RequestScope badScope = new RequestScope(null, tx, badUser, dictionary, null, MOCK_AUDIT_LOGGER);
        PersistentResource<Left> leftResource = new PersistentResource<>(left, null, badScope);

        Assert.assertTrue(leftResource.clearRelation("fieldLevelDelete"));
        Assert.assertEquals(leftResource.getObject().fieldLevelDelete.size(), 0);
    }


    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testUpdatePermissionCheckedOnInverseRelationship() {
        Left left = new Left();
        left.setId(1);
        Right right = new Right();

        left.noInverseUpdate = Sets.newHashSet(right);
        right.noUpdate = Sets.newHashSet(left);

        List<Resource> empty = new ArrayList<>();
        Relationship ids = new Relationship(null, new Data<>(empty));

        User goodUser = new User(1);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        RequestScope goodScope = new RequestScope(null, tx, goodUser, dictionary, null, MOCK_AUDIT_LOGGER);
        PersistentResource<Left> leftResource = new PersistentResource<>(left, null, goodScope);

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
        PersistentResource<Parent> parentResource = new PersistentResource<>(
                parent, getUserScope(goodUser, logger));
        PersistentResource<Child> childResource = new PersistentResource<>(
                parentResource, child, getUserScope(goodUser, logger));

        childResource.audit("name");

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
        PersistentResource<Parent> parentResource = new PersistentResource<>(
                parent, getUserScope(goodUser, logger));
        PersistentResource<Child> childResource = new PersistentResource<>(
                parentResource, child, getUserScope(goodUser, logger));

        childResource.audit(Audit.Action.CREATE);

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
        RequestScope goodScope = new RequestScope(null, tx, goodUser, dictionary, null, MOCK_AUDIT_LOGGER);

        PersistentResource<Parent> parentResource = new PersistentResource<>(parent, goodScope);
        PersistentResource<Child> childResource = new PersistentResource<>(parentResource, child, goodScope);

        parentResource.addRelation("children", childResource);

        verify(tx).save(parent);
        verify(tx).save(child);

        Assert.assertEquals(parent.getChildren().size(), 1, "The owning relationship should be updated");
        Assert.assertTrue(parent.getChildren().contains(child), "The owning relationship should be updated");

        Assert.assertEquals(child.getParents().size(), 1, "The non-owning relationship should also be updated");
        Assert.assertTrue(child.getParents().contains(parent), "The non-owning relationship should also be updated");

        reset(tx);
        parentResource.clearRelation("children");

        verify(tx).save(parent);
        verify(tx).save(child);

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
        when(tx.loadObject(NoShareEntity.class, 1L)).thenReturn(noShare);

        RequestScope goodScope = new RequestScope(null, tx, goodUser, dictionary, null, MOCK_AUDIT_LOGGER);
        PersistentResource<example.User> userResource = new PersistentResource<>(userModel, null, goodScope);

        userResource.updateRelation("noShare", ids.toPersistentResources(goodScope));
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
        when(tx.loadObject(NoShareEntity.class, 1L)).thenReturn(noShare1);
        when(tx.loadObject(NoShareEntity.class, 2L)).thenReturn(noShare2);

        RequestScope goodScope = new RequestScope(null, tx, goodUser, dictionary, null, MOCK_AUDIT_LOGGER);
        PersistentResource<example.User> userResource = new PersistentResource<>(userModel, null, goodScope);

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
        when(tx.loadObject(NoShareEntity.class, 1L)).thenReturn(noShare1);

        RequestScope goodScope = new RequestScope(null, tx, goodUser, dictionary, null, MOCK_AUDIT_LOGGER);
        PersistentResource<example.User> userResource = new PersistentResource<>(userModel, null, goodScope);

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
        when(tx.loadObject(NoShareEntity.class, 1L)).thenReturn(noShare);

        RequestScope goodScope = new RequestScope(null, tx, goodUser, dictionary, null, MOCK_AUDIT_LOGGER);
        PersistentResource<example.User> userResource = new PersistentResource<>(userModel, null, goodScope);

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

        RequestScope goodScope = new RequestScope(null, tx, goodUser, dictionary, null, MOCK_AUDIT_LOGGER);
        PersistentResource<example.User> userResource = new PersistentResource<>(userModel, null, goodScope);

        boolean returnVal = userResource.updateRelation("noShare", ids.toPersistentResources(goodScope));

        Assert.assertTrue(returnVal);
        Assert.assertNull(userModel.getNoShare());
    }

    @Test
    public void testCollectionChangeSpecType() {
        Function<String, BiFunction<ChangeSpec, BiFunction<Collection, Collection, Boolean>, Boolean>> collectionCheck =
                (fieldName) ->
                        (spec, condFn) -> {
                            if (!fieldName.equals(spec.getFieldName())) {
                                throw new IllegalStateException("Wrong field name: '" + spec.getFieldName() + "'. Expected: '" + fieldName + "'");
                            }
                            return condFn.apply((Collection) spec.getOriginal(), (Collection) spec.getModified());
                        };
        // Ensure that change specs coming from collections work properly
        PersistentResource<ChangeSpecModel> model = bootstrapPersistentResource(new ChangeSpecModel((spec) -> collectionCheck.apply("testColl").apply(spec, (original, modified) -> original == null && modified.equals(Arrays.asList("a", "b", "c")))));

        /* Attributes */
        // Set new data from null
        Assert.assertTrue(model.updateAttribute("testColl", Arrays.asList("a", "b", "c")));

        // Set data to empty
        model.getObject().checkFunction = (spec) -> collectionCheck.apply("testColl").apply(spec,
                (original, modified) -> original.equals(Arrays.asList("a", "b", "c")) && modified.isEmpty());
        Assert.assertTrue(model.updateAttribute("testColl", Lists.newArrayList()));

        model.getObject().checkFunction = (spec) -> collectionCheck.apply("testColl").apply(spec, (original, modified) -> original.isEmpty() && modified.equals(Arrays.asList("final", "List")));
        // / Overwrite attribute data
        Assert.assertTrue(model.updateAttribute("testColl", Arrays.asList("final", "List")));

        /* ToMany relationships */
        // Learn about the other kids
        model.getObject().checkFunction = (spec) -> collectionCheck.apply("otherKids").apply(spec, (original, modified) -> (original == null || original.isEmpty()) && modified.size() == 1 && modified.contains(new ChangeSpecChild(1)));
        Assert.assertTrue(model.updateRelation("otherKids", Sets.newHashSet(bootstrapPersistentResource(new ChangeSpecChild(1)))));

        // Add individual
        model.getObject().checkFunction = (spec) -> collectionCheck.apply("otherKids").apply(spec, (original, modified) -> original.equals(Arrays.asList(new ChangeSpecChild(1))) && modified.size() == 2 && modified.contains(new ChangeSpecChild(1)) && modified.contains(new ChangeSpecChild(2)));
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

    private <T> PersistentResource<T> bootstrapPersistentResource(T obj) {
        User goodUser = new User(1);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        RequestScope requestScope = new RequestScope(null, tx, goodUser, dictionary, null, MOCK_AUDIT_LOGGER);
        return new PersistentResource<>(obj, requestScope);
    }

    private RequestScope getUserScope(User user, AuditLogger auditLogger) {
        return new RequestScope(new JsonApiDocument(), null, user, dictionary, null, auditLogger);
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
    @CreatePermission(any = {Role.ALL.class})
    @ReadPermission(any = {Role.ALL.class})
    @UpdatePermission(any = {Role.NONE.class})
    @DeletePermission(any = {Role.ALL.class})
    public static final class ChangeSpecModel {
        @Id
        public long id;

        @ReadPermission(all = {Role.NONE.class})
        @UpdatePermission(all = {Role.NONE.class})
        public Function<ChangeSpec, Boolean> checkFunction;

        @UpdatePermission(all = {ChangeSpecNonCollection.class})
        public String testAttr;

        @UpdatePermission(all = {ChangeSpecCollection.class})
        public List<String> testColl;

        @OneToOne
        @UpdatePermission(all = {ChangeSpecNonCollection.class})
        public ChangeSpecChild child;

        @ManyToMany
        @UpdatePermission(all = {ChangeSpecCollection.class})
        public List<Child> otherKids;

        public ChangeSpecModel(final Function<ChangeSpec, Boolean> checkFunction) {
            this.checkFunction = checkFunction;
        }
    }

    @Entity
    @Include
    @EqualsAndHashCode
    @AllArgsConstructor
    @CreatePermission(any = {Role.ALL.class})
    @ReadPermission(any = {Role.ALL.class})
    @UpdatePermission(any = {Role.ALL.class})
    @DeletePermission(any = {Role.ALL.class})
    @SharePermission(any = {Role.ALL.class})
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
}
