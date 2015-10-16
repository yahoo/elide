/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import static com.yahoo.elide.security.UserCheck.ALLOW;
import static com.yahoo.elide.security.UserCheck.DENY;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yahoo.elide.annotation.Audit;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.audit.LogMessage;
import com.yahoo.elide.audit.Logger;
import com.yahoo.elide.audit.LoggerSingleton;
import com.yahoo.elide.audit.TestLogger;
import com.yahoo.elide.core.exceptions.ForbiddenAccessException;
import com.yahoo.elide.core.exceptions.InvalidAttributeException;
import com.yahoo.elide.core.exceptions.InvalidObjectIdentifierException;
import com.yahoo.elide.jsonapi.models.Data;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.yahoo.elide.jsonapi.models.Relationship;
import com.yahoo.elide.jsonapi.models.Resource;
import com.yahoo.elide.jsonapi.models.ResourceIdentifier;
import com.yahoo.elide.security.User;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import example.Child;
import example.FunWithPermissions;
import example.Left;
import example.NegativeIntegerUserCheck;
import example.NoCreateEntity;
import example.NoDeleteEntity;
import example.NoReadEntity;
import example.NoUpdateEntity;
import example.Parent;
import example.Right;
import example.Role;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class PersistentResourceTest extends PersistentResource {
    private final RequestScope goodUserScope;
    private final RequestScope badUserScope;
    private DatabaseManager databaseManager;

    public PersistentResourceTest() {
        super(new Child(), null, new RequestScope(null, null, null, new EntityDictionary(), null));
        goodUserScope = new RequestScope(null, null, new User(1), dictionary, null);
        badUserScope = new RequestScope(null, null, new User(-1), dictionary, null);
    }

    @BeforeTest
    public void init() {
        databaseManager = Mockito.mock(DatabaseManager.class);
        dictionary.bindEntity(Child.class, databaseManager);
        dictionary.bindEntity(Parent.class, databaseManager);
        dictionary.bindEntity(FunWithPermissions.class, databaseManager);
        dictionary.bindEntity(Left.class, databaseManager);
        dictionary.bindEntity(Right.class, databaseManager);
        dictionary.bindEntity(NoReadEntity.class, databaseManager);
        dictionary.bindEntity(NoDeleteEntity.class, databaseManager);
        dictionary.bindEntity(NoUpdateEntity.class, databaseManager);
        dictionary.bindEntity(NoCreateEntity.class, databaseManager);

        Logger mockLogger = mock(Logger.class);
        LoggerSingleton.setLogger(mockLogger);
    }

    @Test
    public void testGetRelationships() {
        FunWithPermissions fun = new FunWithPermissions();
        fun.setRelation1(Sets.newHashSet());
        fun.relation2 = Sets.newHashSet();
        fun.setRelation3(null);

        User goodUser = new User(1);
        User badUser = new User(-1);
        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "3", goodUserScope);

        Map<String, Relationship> relationships = funResource.getRelationships();

        Assert.assertEquals(relationships.size(), 3, "All relationships should be returned.");
        Assert.assertTrue(relationships.containsKey("relation1"), "relation1 should be present");
        Assert.assertTrue(relationships.containsKey("relation2"), "relation2 should be present");
        Assert.assertTrue(relationships.containsKey("relation3"), "relation3 should be present");

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
        User goodUser = new User(1);
        User badUser = new User(-1);

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

            User goodUser = goodUserScope.getUser();

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

            User badUser = badUserScope.getUser();

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
    public void testDeleteBidirectionalRelation() {
        Left left = new Left();
        Right right = new Right();
        left.setOne2one(right);
        right.setOne2one(left);
        User goodUser = new User(1);

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
        User goodUser = new User(-1);

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
        DatabaseTransaction tx = mock(DatabaseTransaction.class);
        Left left = new Left();
        Right right = new Right();
        left.setId(2);
        right.setId(3);

        RequestScope goodScope = new RequestScope(null, tx, goodUser, dictionary, null);

        PersistentResource<Left> leftResource = new PersistentResource<>(left, null, "2", goodScope);

        Relationship ids = new Relationship(null, new Data<>(new ResourceIdentifier("right", "3").castToResource()));

        when(tx.loadObject(Right.class, "3")).thenReturn(right);
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
        DatabaseTransaction tx = mock(DatabaseTransaction.class);
        Parent parent = new Parent();
        RequestScope goodScope = new RequestScope(null, tx, goodUser, dictionary, null);

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


        when(tx.loadObject(Child.class, "2")).thenReturn(child2);
        when(tx.loadObject(Child.class, "3")).thenReturn(child3);
        when(tx.loadObject(Child.class, "-4")).thenReturn(child4);
        when(tx.loadObject(Child.class, "-5")).thenReturn(child5);
        when(tx.loadObject(Child.class, "6")).thenReturn(child6);

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
    public void testCheckType() {
        User goodUser = new User(1);
        Assert.assertEquals(goodUser.checkUserPermission(new Role.ALL()), ALLOW);
        Assert.assertEquals(goodUser.checkUserPermission(new Role.NONE()), DENY);
        Assert.assertEquals(goodUser.checkUserPermission(new NegativeIntegerUserCheck()), ALLOW);

        User badUser = new User(-1);
        Assert.assertEquals(badUser.checkUserPermission(new NegativeIntegerUserCheck()), DENY);
    }

    @Test
    public void testFilterScopeChecks() {
        User goodUser = new User(1);
        DatabaseTransaction tx = mock(DatabaseTransaction.class);
        RequestScope goodScope = new RequestScope(null, tx, goodUser, dictionary, null);
        FilterScope filterScope;

        filterScope = new FilterScope(goodScope, ANY, new Class[] { Role.NONE.class, Role.NONE.class });
        Assert.assertEquals(filterScope.getUserPermission(), DENY);

        filterScope = new FilterScope(goodScope, ALL, new Class[] { Role.ALL.class, Role.ALL.class });
        Assert.assertEquals(filterScope.getUserPermission(), ALLOW);
    }

    @Test
    public void testGetAttributeSuccess() {
        FunWithPermissions fun = new FunWithPermissions();
        fun.setField2("blah");
        fun.field3 = null;

        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "1", goodUserScope);

        User goodUser = new User(1);
        String result = (String) funResource.getAttribute("field2");
        Assert.assertEquals(result, "blah", "The correct attribute should be returned.");
        result = (String) funResource.getAttribute("field3");
        Assert.assertEquals(result, null, "The correct attribute should be returned.");
    }

    @Test(expectedExceptions = InvalidAttributeException.class)
    public void testGetAttributeInvalidField() {
        FunWithPermissions fun = new FunWithPermissions();

        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "1", goodUserScope);

        User goodUser = new User(1);
        funResource.getAttribute("invalid");
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testGetAttributeInvalidFieldPermissions() {
        FunWithPermissions fun = new FunWithPermissions();
        fun.setField1("foo");

        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "1", goodUserScope);

        User goodUser = new User(1);
        funResource.getAttribute("field1");
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testGetAttributeInvalidEntityPermissions() {
        NoReadEntity noread = new NoReadEntity();

        PersistentResource<NoReadEntity> noreadResource = new PersistentResource<>(noread, null, "1", goodUserScope);

        User goodUser = new User(1);
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
        User goodUser = new User(1);

        Set<PersistentResource> results = funResource.getRelation("relation2");

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
        User goodUser = new User(1);

        Set<PersistentResource> results = funResource.getRelation("relation2");

        Assert.assertEquals(results.size(), 2, "Only filtered relation elements should be returned.");
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testGetRelationForbiddenByEntity() {
        NoReadEntity noread = new NoReadEntity();

        PersistentResource<NoReadEntity> noreadResource = new PersistentResource<>(noread, null, "3", goodUserScope);
        User goodUser = new User(1);

        Set<PersistentResource> results = noreadResource.getRelation("child");
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testGetRelationForbiddenByField() {
        FunWithPermissions fun = new FunWithPermissions();

        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "3", badUserScope);
        User badUser = new User(-1);

        Set<PersistentResource> results = funResource.getRelation("relation1");
    }

    @Test(expectedExceptions = InvalidAttributeException.class)
    public void testGetRelationInvalidRelation() {
        FunWithPermissions fun = new FunWithPermissions();

        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "3", goodUserScope);
        User goodUser = new User(1);

        Set<PersistentResource> results = funResource.getRelation("invalid");
    }

    @Test
    public void testGetRelationByIdSuccess() {
        FunWithPermissions fun = new FunWithPermissions();
        Child child1 = newChild(1);
        Child child2 = newChild(2);
        Child child3 = newChild(3);
        fun.relation2 = Sets.newHashSet(child1, child2, child3);

        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "3", goodUserScope);
        User goodUser = new User(1);

        RequestScope goodScope = new RequestScope(null, null, goodUser, dictionary, null);

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

        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "3", goodUserScope);
        User goodUser = new User(1);

        RequestScope goodScope = new RequestScope(null, null, goodUser, dictionary, null);

        PersistentResource<?> result = funResource.getRelation("relation2", "-1000");
    }

    @Test
    void testDeleteResourceSuccess() {
        Parent parent = newParent(1);

        User goodUser = new User(1);
        DatabaseTransaction tx = mock(DatabaseTransaction.class);
        RequestScope goodScope = new RequestScope(null, tx, goodUser, dictionary, null);

        PersistentResource<Parent> parentResource = new PersistentResource<>(parent, null, "1", goodScope);

        parentResource.deleteResource(null);

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
        DatabaseTransaction tx = mock(DatabaseTransaction.class);
        RequestScope goodScope = new RequestScope(null, tx, goodUser, dictionary, null);

        PersistentResource<Parent> parentResource = new PersistentResource<>(parent, null, "1", goodScope);
        PersistentResource<Child> childResource = new PersistentResource<>(child, parentResource, "1", goodScope);

        childResource.deleteResource("children");

        verify(tx).save(child);
        verify(tx).save(parent);
        verify(tx).delete(child);
        verify(tx, never()).delete(parent);
        Assert.assertTrue(parent.getChildren().isEmpty());
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    void testDeleteResourceForbidden() {
        NoDeleteEntity nodelete = new NoDeleteEntity();
        nodelete.setId(1);

        DatabaseTransaction tx = mock(DatabaseTransaction.class);
        User goodUser = new User(1);

        RequestScope goodScope = new RequestScope(null, tx, goodUser, dictionary, null);

        PersistentResource<NoDeleteEntity> nodeleteResource = new PersistentResource<>(nodelete, null, "1", goodScope);

        nodeleteResource.deleteResource(null);

        verify(tx, never()).delete(nodelete);
    }

    @Test
    void testAddRelationSuccess() {
        FunWithPermissions fun = new FunWithPermissions();
        fun.setRelation1(Sets.newHashSet());

        Child child = newChild(1);

        User goodUser = new User(1);

        DatabaseTransaction tx = mock(DatabaseTransaction.class);

        RequestScope goodScope = new RequestScope(null, tx, goodUser, dictionary, null);
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

        DatabaseTransaction tx = mock(DatabaseTransaction.class);
        RequestScope badScope = new RequestScope(null, tx, badUser, dictionary, null);
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

        DatabaseTransaction tx = mock(DatabaseTransaction.class);
        User goodUser = new User(1);

        RequestScope goodScope = new RequestScope(null, tx, goodUser, dictionary, null);
        PersistentResource<NoUpdateEntity> noUpdateResource = new PersistentResource<>(noUpdate, null, "1", goodScope);
        PersistentResource<Child> childResource = new PersistentResource<>(child, null, "2", goodScope);
        noUpdateResource.addRelation("children", childResource);
    }

    @Test(expectedExceptions = InvalidAttributeException.class)
    public void testAddRelationInvalidRelation() {
        FunWithPermissions fun = new FunWithPermissions();

        Child child = newChild(1);

        User goodUser = new User(1);

        DatabaseTransaction tx = mock(DatabaseTransaction.class);
        RequestScope goodScope = new RequestScope(null, tx, goodUser, dictionary, null);
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

        DatabaseTransaction tx = mock(DatabaseTransaction.class);
        User goodUser = new User(1);
        RequestScope goodScope = new RequestScope(null, tx, goodUser, dictionary, null);
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

        DatabaseTransaction tx = mock(DatabaseTransaction.class);
        User goodUser = new User(1);
        RequestScope goodScope = new RequestScope(null, tx, goodUser, dictionary, null);

        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "1", goodScope);
        PersistentResource<Object> removeResource = new PersistentResource<>(child, null, "1", goodScope);

        funResource.removeRelation("relation3", removeResource);

        Assert.assertEquals(fun.getRelation3(), null, "The one-2-one relationship should be cleared");

        verify(tx).save(fun);
        verify(tx).save(child);
    }

    @Test()
    public void testClearToManyRelationSuccess() {
        Child child = newChild(1);
        Parent parent1 = newParent(1, child);
        Parent parent2 = newParent(2, child);
        Parent parent3 = newParent(3, child);
        child.setParents(Sets.newHashSet(parent1, parent2, parent3));

        DatabaseTransaction tx = mock(DatabaseTransaction.class);
        User goodUser = new User(1);
        RequestScope goodScope = new RequestScope(null, tx, goodUser, dictionary, null);

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

        DatabaseTransaction tx = mock(DatabaseTransaction.class);
        User goodUser = new User(1);
        RequestScope goodScope = new RequestScope(null, tx, goodUser, dictionary, null);
        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "1", goodScope);
        funResource.clearRelation("relation3");

        Assert.assertEquals(fun.getRelation3(), null, "The one-2-one relationship should be cleared");

        verify(tx).save(fun);
        verify(tx).save(child);
    }

    @Test()
    public void testClearRelationFilteredByReadAccess() {
        User goodUser = new User(1);
        DatabaseTransaction tx = mock(DatabaseTransaction.class);
        Parent parent = new Parent();
        RequestScope goodScope = new RequestScope(null, tx, goodUser, dictionary, null);

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

        DatabaseTransaction tx = mock(DatabaseTransaction.class);
        User goodUser = new User(1);
        RequestScope goodScope = new RequestScope(null, tx, goodUser, dictionary, null);
        PersistentResource<Left> leftResource = new PersistentResource<>(left, null, "1", goodScope);
        leftResource.clearRelation("noUpdateOne2One");
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testNoChangeRelationInvalidToOneUpdatePermission() {
        Left left = new Left();
        left.setId(1);
        Right right = new Right();
        right.setId(1);
        left.noUpdateOne2One = right;
        right.noUpdateOne2One = left;

        DatabaseTransaction tx = mock(DatabaseTransaction.class);
        User goodUser = new User(1);
        RequestScope goodScope = new RequestScope(null, tx, goodUser, dictionary, null);
        PersistentResource<Left> leftResource = new PersistentResource<>(left, null, "1", goodScope);
        leftResource.updateRelation("noUpdateOne2One", leftResource.getRelation("noUpdateOne2One"));
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


        DatabaseTransaction tx = mock(DatabaseTransaction.class);
        User goodUser = new User(1);
        RequestScope goodScope = new RequestScope(null, tx, goodUser, dictionary, null);
        PersistentResource<Left> leftResource = new PersistentResource<>(left, null, "1", goodScope);
        leftResource.clearRelation("noInverseUpdate");
    }


    @Test
    public void testClearRelationInvalidToOneDeletePermission() {
        Left left = new Left();
        left.setId(1);
        NoDeleteEntity noDelete = new NoDeleteEntity();
        noDelete.setId(1);
        left.noDeleteOne2One = noDelete;

        DatabaseTransaction tx = mock(DatabaseTransaction.class);
        User goodUser = new User(1);
        RequestScope goodScope = new RequestScope(null, tx, goodUser, dictionary, null);
        PersistentResource<Left> leftResource = new PersistentResource<>(left, null, "1", goodScope);
        Assert.assertTrue(leftResource.clearRelation("noDeleteOne2One"));
        Assert.assertEquals(leftResource.getObject().noDeleteOne2One, null);

    }

    @Test(expectedExceptions = InvalidAttributeException.class)
    public void testClearRelationInvalidRelation() {
        FunWithPermissions fun = new FunWithPermissions();
        Child child = newChild(1);
        fun.setRelation3(child);

        DatabaseTransaction tx = mock(DatabaseTransaction.class);
        User goodUser = new User(1);
        RequestScope goodScope = new RequestScope(null, tx, goodUser, dictionary, null);
        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "1", goodScope);
        funResource.clearRelation("invalid");
    }

    @Test
    public void testUpdateAttributeSuccess() {
        Parent parent = newParent(1);

        DatabaseTransaction tx = mock(DatabaseTransaction.class);
        User goodUser = new User(1);

        RequestScope goodScope = new RequestScope(null, tx, goodUser, dictionary, null);
        PersistentResource<Parent> parentResource = new PersistentResource<>(parent, null, "1", goodScope);
        parentResource.updateAttribute("firstName", "foobar");

        Assert.assertEquals(parent.getFirstName(), "foobar", "The attribute was updated successfully");
        verify(tx).save(parent);
    }

    @Test(expectedExceptions = InvalidAttributeException.class)
    public void testUpdateAttributeInvalidAttribute() {
        Parent parent = newParent(1);

        DatabaseTransaction tx = mock(DatabaseTransaction.class);
        User goodUser = new User(1);

        RequestScope goodScope = new RequestScope(null, tx, goodUser, dictionary, null);
        PersistentResource<Parent> parentResource = new PersistentResource<>(parent, null, "1", goodScope);
        parentResource.updateAttribute("invalid", "foobar");
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testUpdateAttributeInvalidUpdatePermission() {
        FunWithPermissions fun = new FunWithPermissions();
        fun.setId(1);


        DatabaseTransaction tx = mock(DatabaseTransaction.class);
        User badUser = new User(-1);

        RequestScope badScope = new RequestScope(null, tx, badUser, dictionary, null);

        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "1", badScope);
        funResource.updateAttribute("field4", "foobar");
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testUpdateAttributeInvalidUpdatePermissionNoChange() {
        FunWithPermissions fun = new FunWithPermissions();
        fun.setId(1);

        DatabaseTransaction tx = mock(DatabaseTransaction.class);
        User badUser = new User(-1);

        RequestScope badScope = new RequestScope(null, tx, badUser, dictionary, null);

        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, null, "1", badScope);
        funResource.updateAttribute("field4", funResource.getAttribute("field4"));
    }

    @Test()
    public void testLoadRecords() {
        Child child1 = newChild(1);
        Child child2 = newChild(-2);
        Child child3 = newChild(-3);
        Child child4 = newChild(4);
        Child child5 = newChild(5);

        DatabaseTransaction tx = mock(DatabaseTransaction.class);
        User goodUser = new User(1);

        when(tx.loadObjects(eq(Child.class), anyObject()))
            .thenReturn(Lists.newArrayList(child1, child2, child3, child4, child5));

        RequestScope goodScope = new RequestScope(null, tx, goodUser, dictionary, null);
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

        DatabaseTransaction tx = mock(DatabaseTransaction.class);
        User goodUser = new User(1);

        when(tx.loadObject(Child.class, "1")).thenReturn(child1);

        RequestScope goodScope = new RequestScope(null, tx, goodUser, dictionary, null);
        PersistentResource<Child> loaded = PersistentResource.loadRecord(Child.class, "1", goodScope);

        Assert.assertEquals(loaded.getObject(), child1,
                "The load function should return the requested child object"
        );
    }

    @Test(expectedExceptions = InvalidObjectIdentifierException.class)
    public void testLoadRecordInvalidId() {
        DatabaseTransaction tx = mock(DatabaseTransaction.class);
        User goodUser = new User(1);

        when(tx.loadObject(Child.class, "1")).thenReturn(null);

        RequestScope goodScope = new RequestScope(null, tx, goodUser, dictionary, null);
        PersistentResource<Child> loaded = PersistentResource.loadRecord(Child.class, "1", goodScope);
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testLoadRecordForbidden() {
        NoReadEntity noRead = new NoReadEntity();
        noRead.setId(1);
        DatabaseTransaction tx = mock(DatabaseTransaction.class);
        User goodUser = new User(1);

        when(tx.loadObject(NoReadEntity.class, "1")).thenReturn(noRead);

        RequestScope goodScope = new RequestScope(null, tx, goodUser, dictionary, null);
        PersistentResource<NoReadEntity> loaded = PersistentResource.loadRecord(NoReadEntity.class, "1", goodScope);
    }

    @Test()
    public void testCreateObjectSuccess() {
        Parent parent = newParent(1);
        DatabaseTransaction tx = mock(DatabaseTransaction.class);
        User goodUser = new User(1);

        when(tx.createObject(Parent.class)).thenReturn(parent);

        RequestScope goodScope = new RequestScope(null, tx, goodUser, dictionary, null);
        PersistentResource<Parent> created = PersistentResource.createObject(Parent.class, goodScope, "uuid");
        parent.setChildren(new HashSet<>());
        created.getRequestScope().runDeferredPermissionChecks();

        Assert.assertEquals(created.getObject(), parent,
            "The create function should return the requested parent object"
        );
    }

    @Test(expectedExceptions = ForbiddenAccessException.class)
    public void testCreateObjectForbidden() {
        NoCreateEntity noCreate = new NoCreateEntity();
        noCreate.setId(1);
        DatabaseTransaction tx = mock(DatabaseTransaction.class);
        User goodUser = new User(1);

        when(tx.createObject(NoCreateEntity.class)).thenReturn(noCreate);

        RequestScope goodScope = new RequestScope(null, tx, goodUser, dictionary, null);
        PersistentResource<NoCreateEntity> created = PersistentResource.createObject(NoCreateEntity.class, goodScope, "uuid");
        created.getRequestScope().runDeferredPermissionChecks();
    }

    @Test
    public void testDeletePermissionCheckedOnInverseRelationship() {
        Left left = new Left();
        left.setId(1);
        Right right = new Right();

        left.noInverseDelete = Sets.newHashSet(right);
        right.noDelete = Sets.newHashSet(left);

        //Bad User triggers the delete permission failure
        User badUser = new User(-1);
        DatabaseTransaction tx = mock(DatabaseTransaction.class);
        RequestScope badScope = new RequestScope(null, tx, badUser, dictionary, null);
        PersistentResource<Left> leftResource = new PersistentResource<>(left, null, badScope);
        Assert.assertTrue(leftResource.clearRelation("noInverseDelete"));
        Assert.assertEquals(leftResource.getObject().noInverseDelete.size(), 0);
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
        DatabaseTransaction tx = mock(DatabaseTransaction.class);
        RequestScope goodScope = new RequestScope(null, tx, goodUser, dictionary, null);
        PersistentResource<Left> leftResource = new PersistentResource<>(left, null, goodScope);

        leftResource.updateRelation("noInverseUpdate", ids.toPersistentResources(goodScope));
    }

    @Test
    public void testFieldLevelAudit() throws Exception {
        Logger oldLogger = LoggerSingleton.getLogger();

        Child child = newChild(5);

        Parent parent = newParent(7);

        User goodUser = new User(1);
        PersistentResource<Parent> parentResource = new PersistentResource<>(parent, getUserScope(goodUser));
        PersistentResource<Child> childResource = new PersistentResource<>(parentResource, child, getUserScope(goodUser));

        TestLogger logger = new TestLogger();
        LoggerSingleton.setLogger(logger);

        try {
            childResource.audit("name");
        } finally {
            LoggerSingleton.setLogger(oldLogger);
        }

        Assert.assertEquals(logger.getMessages().size(), 1, "One message should be logged");

        LogMessage message = logger.getMessages().get(0);
        Assert.assertEquals(message.getMessage(), "UPDATE Child 5 Parent 7", "Logging template should match");

        Assert.assertEquals(message.getOperationCode(), 1, "Operation code should match");
    }
    @Test
    public void testClassLevelAudit() throws Exception {
        Logger oldLogger = LoggerSingleton.getLogger();

        Child child = newChild(5);

        Parent parent = newParent(7);

        User goodUser = new User(1);
        PersistentResource<Parent> parentResource = new PersistentResource<>(parent, getUserScope(goodUser));
        PersistentResource<Child> childResource = new PersistentResource<>(parentResource, child, getUserScope(goodUser));

        TestLogger logger = new TestLogger();
        LoggerSingleton.setLogger(logger);

        try {
            childResource.audit(Audit.Action.CREATE);
        } finally {
            LoggerSingleton.setLogger(oldLogger);
        }

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
        DatabaseTransaction tx = mock(DatabaseTransaction.class);
        RequestScope goodScope = new RequestScope(null, tx, goodUser, dictionary, null);

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

    private RequestScope getUserScope(User user) {
        return new RequestScope(new JsonApiDocument(), null, user, dictionary, null);
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
}
