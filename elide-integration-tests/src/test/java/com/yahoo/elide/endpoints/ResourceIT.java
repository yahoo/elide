/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.endpoints;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.startsWith;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.audit.TestLogger;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.SecurityMode;
import com.yahoo.elide.hibernate.AHibernateTest;
import com.yahoo.elide.jsonapi.models.Data;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.yahoo.elide.jsonapi.models.Resource;
import com.yahoo.elide.jsonapi.models.ResourceIdentifier;

import com.google.common.collect.Sets;
import example.Child;
import example.Filtered;
import example.FunWithPermissions;
import example.Parent;
import example.User;
import org.apache.http.HttpStatus;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.ws.rs.core.MultivaluedHashMap;

/**
 * The type Config resource test.
 */
// TODO: These tests (i.e. the whole suite) are too tightly coupled. We need to refactor them.
public class ResourceIT extends AHibernateTest {
    public ResourceIT() {
        /* There is no good way to get the dictionary from Elide */
        EntityDictionary empty = new EntityDictionary();
    }

    @BeforeTest
    public static void setup() {
        DataStoreTransaction tx = dataStore.beginTransaction();
        Parent parent = new Parent(); // id 1
        Child child = new Child(); // id 1
        parent.setChildren(Sets.newHashSet(child));
        parent.setSpouses(Sets.newHashSet());
        child.setParents(Sets.newHashSet(parent));

        tx.save(parent);
        tx.save(child);

        // Single tests
        Parent p1 = new Parent(); // id 2
        p1.setFirstName("John");
        p1.setSpouses(Sets.newHashSet());

        Child c1 = new Child(); // id 2
        c1.setName("Child-ID2");
        c1.setParents(Sets.newHashSet(p1));

        Child c2 = new Child(); // id 3
        c2.setName("Child-ID3");
        c2.setParents(Sets.newHashSet(p1));

        Set<Child> friends = new HashSet<>();
        friends.add(c2);
        c1.setFriends(friends);

        Set<Child> children = new HashSet<>();
        children.add(c1);
        children.add(c2);

        p1.setChildren(children);

        tx.save(p1);
        tx.save(c1);
        tx.save(c2);

        // List tests
        Parent p2 = new Parent(); // id 3
        Parent p3 = new Parent();  // id 4
        Child c3 = new Child(); // id 4
        c3.setParents(Sets.newHashSet(p2));
        Child c4 = new Child(); // id 5
        c4.setParents(Sets.newHashSet(p2));
        Set<Child> children2 = new HashSet<>();
        children2.add(c3);
        children2.add(c4);

        p2.setChildren(children2);
        p2.setFirstName("Link");

        p3.setFirstName("Unknown");

        p2.setSpouses(Sets.newHashSet());
        p3.setSpouses(Sets.newHashSet());
        p3.setChildren(Sets.newHashSet());

        tx.save(p2);
        tx.save(p3);
        tx.save(c3);
        tx.save(c4);

        FunWithPermissions fun = new FunWithPermissions();
        tx.save(fun);

        User user = new User(); //ID 1
        user.setPassword("god");
        tx.save(user);

        tx.save(tx.createObject(Filtered.class));
        tx.save(tx.createObject(Filtered.class));
        tx.save(tx.createObject(Filtered.class));
        tx.commit();
    }

    @Test(priority = -1)
    public void testRootCollection() throws Exception {
        String resp = given().when().get("/parent").then().statusCode(HttpStatus.SC_OK)
        .extract().body().asString();

        JsonApiDocument doc = mapper.readJsonApiDocument(resp);
        assertEquals(doc.getData().get().size(), 4);
    }

    @Test
    public void testRootCollectionId() {
        String expected = getJson("/ResourceIT/testRootCollectionId.json");

        String response = given().when().get("/parent/1").then().statusCode(HttpStatus.SC_OK)
        .extract().body().asString();

        assertEquals(response, expected);
    }

    @Test
    public void testRootCollectionRelationships() {
        String expected = getJson("/ResourceIT/testRootCollectionRelationships.json");

        given()
            .when().get("/parent/1/relationships/children").then().statusCode(HttpStatus.SC_OK)
            .body(equalTo(expected));
    }

    @Test
    public void testChild() throws Exception {
        String expected = getJson("/ResourceIT/testChild.json");

        given().when().get("/parent/1/children/1").then().statusCode(HttpStatus.SC_OK)
        .body(equalTo(expected));
    }

    @Test
    public void testSubCollectionRelationships() throws Exception {
        String expected = getJson("/ResourceIT/testSubCollectionRelationships.json");

        given().when().get("/parent/1/children/1/relationships/parents").then().statusCode(HttpStatus.SC_OK)
            .body(equalTo(expected));
    }

    @Test
    public void failRootCollectionRelationships() {
        given().when().get("/parent/1/relationships").then().statusCode(HttpStatus.SC_NOT_FOUND);
    }

    @Test
    public void failRootCollection() throws Exception {
        String expected = getJson("/ResourceIT/failRootCollection.json");

        given().when().get("/unknown").then().statusCode(HttpStatus.SC_NOT_FOUND)
        .body(equalTo(expected));
    }

    @Test
    public void failRootCollectionId() {
        String expected = getJson("/ResourceIT/failRootCollectionId.json");

        given().when().get("/parent/6789").then().statusCode(HttpStatus.SC_NOT_FOUND)
        .body(equalTo(expected));
    }

    @Test
    public void failChild() throws Exception {
        String expected = getJson("/ResourceIT/failChild.json");

        given().when().get("/parent/1/unknown").then().statusCode(HttpStatus.SC_NOT_FOUND)
        .body(equalTo(expected));
    }

    @Test
    public void failFieldRequest() throws Exception {
        String expected = getJson("/ResourceIT/failFieldRequest.json");

        given().when().get("/parent/1/id").then().statusCode(HttpStatus.SC_NOT_FOUND)
        .body(equalTo(expected));
    }

    @Test
    public void parseFailure() {
        String expected = getJson("/ResourceIT/parseFailure.json");

        given().when().get("company/1|apps/2/links/foo").then().statusCode(HttpStatus.SC_NOT_FOUND)
        .body(equalTo(expected));
    }

    @Test(priority = 1)
    public void testPatchAttrSingle() throws Exception {
        String req = getJson("/ResourceIT/testPatchAttrSingle.json");

        given()
            .contentType("application/vnd.api+json")
            .accept("application/vnd.api+json")
            .body(req)
            .patch("/parent/2")
            .then()
            .statusCode(HttpStatus.SC_NO_CONTENT);

        String resp = given()
            .contentType("application/vnd.api+json")
            .accept("application/vnd.api+json")
            .get("/parent/2")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .contentType("application/vnd.api+json")
            .extract().response().asString();

        JsonApiDocument doc = mapper.readJsonApiDocument(resp);
        Data<Resource> data = doc.getData();
        Resource resource = data.get().iterator().next();

        System.out.println("Nachos: " + resp);

        assertEquals(resource.getAttributes().get("firstName"), "syzygy");
        assertEquals(resource.getRelationships().size(), 2);
        assertEquals(resource.getRelationships().get("children").getData().get().size(), 2);
    }

    @Test(priority = 2)
    public void testPatchAttrNoUpdateSingle() {
        String req = getJson("/ResourceIT/testPatchAttrNoUpdateSingle.json");

        given()
            .contentType("application/vnd.api+json")
            .accept("application/vnd.api+json")
            .body(req)
            .patch("/parent/2")
            .then()
            .statusCode(HttpStatus.SC_NO_CONTENT)
            .body(isEmptyOrNullString());
    }

    @Test(priority = 3)
    public void testPatchAttrList() throws Exception {
        String req = getJson("/ResourceIT/testPatchAttrList.json");

        given()
            .contentType("application/vnd.api+json")
            .accept("application/vnd.api+json")
            .body(req)
            .patch("/parent/3")
            .then()
            .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    @Test(priority = 4)
    public void testPatchSetRel() throws Exception {
        String req = getJson("/ResourceIT/testPatchSetRel.json");

        given()
            .contentType("application/vnd.api+json")
            .accept("application/vnd.api+json")
            .body(req)
            .patch("/parent/4")
            .then()
            .statusCode(HttpStatus.SC_NO_CONTENT);

        String resp = given()
            .contentType("application/vnd.api+json")
            .accept("application/vnd.api+json")
            .get("/parent/4")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .extract().response().asString();

        JsonApiDocument doc = mapper.readJsonApiDocument(resp);
        Data<Resource> data = doc.getData();
        Resource r = data.get().iterator().next();
        Iterator<Resource> itr = r.getRelationships().get("children").getData().get().iterator();
        String rel1 = itr.next().getId();
        String rel2 = itr.next().getId();

        // Sort is not enabled-- order agnostic.
        String id1;
        String id2;
        if (rel1.equals("4")) {
            id1 = rel1;
            id2 = rel2;
        } else {
            id1 = rel2;
            id2 = rel1;
        }

        assertEquals(r.getAttributes().get("firstName"), "Unknown");
        assertEquals(id1, "4");
        assertEquals(id2, "5");
    }

    @Test(priority = 5)
    public void testPatchRemoveRelSingle() {
        String req = getJson("/ResourceIT/testPatchRemoveRelSingle.req.json");
        String expected = getJson("/ResourceIT/testPatchRemoveRelSingle.json");

        given()
            .contentType("application/vnd.api+json")
            .accept("application/vnd.api+json")
            .body(req)
            .patch("/parent/4")
            .then()
            .statusCode(HttpStatus.SC_NO_CONTENT);

        String response = given()
            .contentType("application/vnd.api+json")
            .accept("application/vnd.api+json")
            .get("/parent/4")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .extract().body().asString();

        assertEqualDocuments(response, expected);
    }

    @Test(priority = 6)
    public void testPatchRelNoUpdateSingle() {
        String req = getJson("/ResourceIT/testPatchRelNoUpdateSingle.json");

        given()
            .contentType("application/vnd.api+json")
            .accept("application/vnd.api+json")
            .body(req)
            .patch("/parent/4")
            .then()
            .statusCode(HttpStatus.SC_NO_CONTENT);
    }

    @Test(priority = 7)
    public void testPatchRelRemoveColl() {
        String req = getJson("/ResourceIT/testPatchRelRemoveColl.req.json");
        String expected = getJson("/ResourceIT/testPatchRelRemoveColl.json");

        given()
            .contentType("application/vnd.api+json")
            .accept("application/vnd.api+json")
            .body(req)
            .patch("/parent/4")
            .then()
            .statusCode(HttpStatus.SC_NO_CONTENT);

        String response = given()
            .contentType("application/vnd.api+json")
            .accept("application/vnd.api+json")
            .get("/parent/4")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .extract().body().asString();

        assertEqualDocuments(response, expected);
    }

    @Test(priority = 8)
    public void testGetNestedSingleInclude() throws IOException {

        String expected  = getJson("/ResourceIT/testGetNestedSingleInclude.json");

        String response = given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .get("/parent/2?include=children.friends")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract().body().asString();

        assertEqualDocuments(response, expected);
    }

    @Test(priority = 8)
    public void testGetSingleIncludeOnCollection() throws Exception {

        /*
         * /parent?include=children
         *
         * {data: [
         *      all the parents
         * ], include: [
         *      all the children belonging to a parent
         * ]}
         */
        String expected  = getJson("/ResourceIT/testGetSingleIncludeOnCollection.json");

        String response = given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .get("/parent?include=children")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract().body().asString();

        assertEqualDocuments(response, expected);
    }

    @Test(priority = 8)
    public void testGetMultipleIncludeOnCollection() throws Exception {

        /*
         * /parent?include=children,spouses
         *
         * {data: [
         *      all the parents
         * ], include: [
         *      all the children and spouses belonging to a parent
         * ]}
         */
        String expected  = getJson("/ResourceIT/testGetMultipleIncludeOnCollection.json");

        String response = given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .get("/parent?include=children,spouses")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract().body().asString();

        assertEqualDocuments(response, expected);
    }

    @Test
    public void testGetSingleIncludeOnRelationship() {

        /*
         * /parent/1/relationships/children?include=children
         *
         * {data: [
         *      child 1 resource identification object
         * ], include: [
         *      child 1's data
         * ]}
         */
        String expected = getJson("/ResourceIT/testGetSingleIncludeOnRelationship.json");

        given()
                .when().get("/parent/1/relationships/children?include=children").then().statusCode(HttpStatus.SC_OK)
                .body(equalTo(expected));
    }

    @Test(priority = 8)
    public void testGetIncludeBadRelation() {

        given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .get("/parent/1?include=children.BadRelation")
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND);
    }

    @Test(priority = 8)
    public void testGetSortCollection() throws Exception {

        /*
         * /parent?sort=firstName
         *
         * {data: [
         *      parents sorted by name
         * ]}
         */
        String expected  = getJson("/ResourceIT/testGetSortCollection.json");

        String response = given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .get("/parent?sort=+firstName")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract().body().asString();

        assertEqualDocuments(response, expected);
    }

    @Test(priority = 8)
    public void testGetReverseSortCollection() throws Exception {

        /*
         * /parent?sort=firstName
         *
         * {data: [
         *      parents sorted by name
         * ]}
         */
        String expected  = getJson("/ResourceIT/testGetReverseSortCollection.json");

        String response = given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .get("/parent?sort=-firstName")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract().body().asString();

        assertEquals(response, expected);
    }

    @Test(priority = 8)
    public void testGetRelEmptyColl() {
        String expected = getJson("/ResourceIT/testGetRelEmptyColl.json");

        given()
            .contentType("application/vnd.api+json")
            .accept("application/vnd.api+json")
            .get("/parent/4/relationships/children")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body(equalTo(expected));
    }

    @Test(priority = 8)
    public void testGetWithTrailingSlash() {
        String expected = getJson("/ResourceIT/testGetWithTrailingSlash.json");

        String response = given()
            .contentType("application/vnd.api+json")
            .accept("application/vnd.api+json")
            .get("/parent/")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .extract().body().asString();

        assertEqualDocuments(response, expected);
    }

    @Test(priority = 9)
    public void testPatchRelSetDirect() throws Exception {
        String req = getJson("/ResourceIT/testPatchRelSetDirect.json");

        given()
            .contentType("application/vnd.api+json")
            .accept("application/vnd.api+json")
            .body(req)
            .patch("/parent/4/relationships/children")
            .then()
            .statusCode(HttpStatus.SC_NO_CONTENT);

        String resp = given()
            .contentType("application/vnd.api+json")
            .accept("application/vnd.api+json")
            .get("/parent/4/relationships/children")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .extract().response().asString();

        JsonApiDocument doc = mapper.readJsonApiDocument(resp);
        Data<Resource> list = doc.getData();
        Iterator<Resource> itr = list.get().iterator();
        String rel1 = itr.next().getId();
        String rel2 = itr.next().getId();

        // Sort is not enabled-- order agnostic.
        String id1;
        String id2;
        if (rel1.equals("4")) {
            id1 = rel1;
            id2 = rel2;
        } else {
            id1 = rel2;
            id2 = rel1;
        }

        assertEquals(id1, "4");
        assertEquals(id2, "5");
    }

    @Test(priority = 10)
    public void testPatchRelNoUpdateDirect() throws Exception {
        String req = getJson("/ResourceIT/testPatchRelNoUpdateDirect.json");

        given()
            .contentType("application/vnd.api+json")
            .accept("application/vnd.api+json")
            .body(req)
            .patch("/parent/4/relationships/children")
            .then()
            .statusCode(HttpStatus.SC_NO_CONTENT);
    }

    @Test(priority = 11)
    public void testDeleteParent() {
        given()
            .contentType("application/vnd.api+json")
            .accept("application/vnd.api+json")
            .delete("/parent/1")
            .then()
            .statusCode(HttpStatus.SC_NO_CONTENT)
            .body(isEmptyOrNullString());
    }


    @Test(priority = 12)
    public void failDeleteParent() {
        given()
            .contentType("application/vnd.api+json")
            .accept("application/vnd.api+json")
            .delete("/parent/678")
            .then()
            .statusCode(HttpStatus.SC_NOT_FOUND);
    }

    @Test(priority = 1)
    public void createParentNoRels() {
        String req = getJson("/ResourceIT/createParentNoRels.req.json");
        String resp = getJson("/ResourceIT/createParentNoRels.json");

        String response = given()
            .contentType("application/vnd.api+json")
            .accept("application/vnd.api+json")
            .body(req)
            .post("/parent")
            .then()
            .statusCode(HttpStatus.SC_CREATED)
            .extract().body().asString();

        assertEqualDocuments(response, resp);
    }

    @Test(priority = 2)
    public void createParentWithRels() {
        String req = getJson("/ResourceIT/createParentWithRels.req.json");
        String resp = getJson("/ResourceIT/createParentWithRels.json");

        String response = given()
            .contentType("application/vnd.api+json")
            .accept("application/vnd.api+json")
            .body(req)
            .post("/parent")
            .then()
            .statusCode(HttpStatus.SC_CREATED)
            .extract().body().asString();

        assertEqualDocuments(response, resp);
    }

    @Test
    public void createParentList() {
        String req = getJson("/ResourceIT/createParentList.req.json");
        String expected = "{\"errors\":[\"Bad Request Body";

        given()
            .contentType("application/vnd.api+json")
            .accept("application/vnd.api+json")
            .body(req)
            .post("/parent")
            .then()
            .statusCode(HttpStatus.SC_BAD_REQUEST)
            .body(startsWith(expected));
    }

    @Test(priority = 3)
    public void createChild() throws Exception {
        String req = getJson("/ResourceIT/createChild.json");

        String cResp = given()
            .contentType("application/vnd.api+json")
            .accept("application/vnd.api+json")
            .body(req)
            .post("/parent/5/children")
            .then()
            .statusCode(HttpStatus.SC_CREATED)
            .extract().body().asString();

        JsonApiDocument respDoc = mapper.readJsonApiDocument(cResp);
        Resource r = respDoc.getData().get().iterator().next();
        Collection<ResourceIdentifier> rIds = (r.getRelationships().get("parents").getResourceIdentifierData()).get();
        ResourceIdentifier rId1 = rIds.iterator().next();
        assertEquals(r.getId(), "6");
        assertEquals(r.getType(), "child");
        assertEquals(r.getRelationships().size(), 2);
        assertEquals(rIds.size(), 2);
        assertEquals(rId1.getType(), "parent");

        String resp = given()
            .contentType("application/vnd.api+json")
            .accept("application/vnd.api+json")
            .get("/parent/5")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .extract().body().asString();

        JsonApiDocument doc = mapper.readJsonApiDocument(resp);
        boolean hasIdentifier = false;

        r = doc.getData().get().iterator().next();
        rIds = r.getRelationships().get("children").getResourceIdentifierData().get();
        for (ResourceIdentifier id : rIds) {
            hasIdentifier |= id.getId().equals("6");
        }
        assertTrue(hasIdentifier);
    }

    @Test
    public void createParentBadUri() {
        String req = getJson("/ResourceIT/createParentBadUri.json");

        given()
            .contentType("application/vnd.api+json")
            .accept("application/vnd.api+json")
            .body(req)
            .post("/parent/678")
            .then()
            .statusCode(HttpStatus.SC_NOT_FOUND);
    }

    @Test
    public void createChildNonRootable() {
        String req = getJson("/ResourceIT/createChildNonRootable.json");
        given()
            .contentType("application/vnd.api+json")
            .accept("application/vnd.api+json")
            .body(req)
            .post("/child")
            .then()
            .statusCode(HttpStatus.SC_NOT_FOUND);
    }

    @Test
    public void testAddAndRemoveOneToOneRelationship() {
        String req = getJson("/ResourceIT/testAddAndRemoveOneToOneRelationship.req.json");

        given()
            .contentType("application/vnd.api+json")
            .accept("application/vnd.api+json")
            .body(req)
            .patch("/fun/1")
            .then()
            .statusCode(HttpStatus.SC_NO_CONTENT);

        String expected = getJson("/ResourceIT/testAddAndRemoveOneToOneRelationship.json");

        String response = given().when().get("/fun/1").then().statusCode(HttpStatus.SC_OK).extract().body().asString();

        assertEqualDocuments(response, expected);

        req = getJson("/ResourceIT/testAddAndRemoveOneToOneRelationship.2.req.json");

        given()
            .contentType("application/vnd.api+json")
            .accept("application/vnd.api+json")
            .body(req)
            .patch("/fun/1")
            .then()
            .statusCode(HttpStatus.SC_NO_CONTENT);

        expected = getJson("/ResourceIT/testAddAndRemoveOneToOneRelationship.2.json");

        response = given().when().get("/fun/1").then().statusCode(HttpStatus.SC_OK).extract().body().asString();

        assertEqualDocuments(response, expected);
    }

    @Test(priority = 13)
    public void createDependentPatchExt() {
        String req = getJson("/ResourceIT/createDependentPatchExt.req.json");
        String expected = getJson("/ResourceIT/createDependentPatchExt.json");
        given()
            .contentType("application/vnd.api+json; ext=jsonpatch")
            .accept("application/vnd.api+json; ext=jsonpatch")
            .body(req)
            .patch("/parent")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body(equalTo(expected));
    }

    @Test(priority = 14)
    public void createChildRelateExisting() {
        String req = getJson("/ResourceIT/createChildRelateExisting.req.json");
        String expected = getJson("/ResourceIT/createChildRelateExisting.json");
        given()
            .contentType("application/vnd.api+json; ext=jsonpatch")
            .accept("application/vnd.api+json; ext=jsonpatch")
            .body(req)
            .patch("/")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body(equalTo(expected));
    }

    @Test(priority = 15)
    public void updateChildRelationToExisting() {
        String req = getJson("/ResourceIT/updateChildRelationToExisting.req.json");
        String expected1 = getJson("/ResourceIT/updateChildRelationToExisting.1.json");
        String expected2 = getJson("/ResourceIT/updateChildRelationToExisting.2.json");
        given()
            .contentType("application/vnd.api+json; ext=jsonpatch")
            .accept("application/vnd.api+json; ext=jsonpatch")
            .body(req)
            .patch("/parent")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body(equalTo(expected1));
        given()
            .contentType("application/vnd.api+json")
            .accept("application/vnd.api+json")
            .get("/parent/5/children/8")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body(equalTo(expected2));
    }

    @Test(priority = 16)
    public void replaceAttributesAndRelationship() {
        String req = getJson("/ResourceIT/replaceAttributesAndRelationship.req.json");
        String expected1 = getJson("/ResourceIT/replaceAttributesAndRelationship.json");
        String expected2 = getJson("/ResourceIT/replaceAttributesAndRelationship.2.json");
        given()
            .contentType("application/vnd.api+json; ext=jsonpatch")
            .accept("application/vnd.api+json; ext=jsonpatch")
            .body(req)
            .patch("/parent")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body(equalTo(expected1));
        String response = given()
            .contentType("application/vnd.api+json")
            .accept("application/vnd.api+json")
            .get("/parent/7")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .extract().body().asString();

        assertEqualDocuments(response, expected2);
    }

    @Test(priority = 17)
    public void removeObject() {
        String req1 = getJson("/ResourceIT/removeObject.1.req.json");
        String req2 = getJson("/ResourceIT/removeObject.2.req.json");
        String expectedDirect = getJson("/ResourceIT/removeObject.direct.json");
        String expected1 = getJson("/ResourceIT/removeObject.1.json");
        String expected2 = getJson("/ResourceIT/removeObject.2.json");
        given()
            .contentType("application/vnd.api+json; ext=jsonpatch")
            .accept("application/vnd.api+json; ext=jsonpatch")
            .body(req1)
            .patch("/parent")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body(equalTo(expected1));
        given()
            .contentType("application/vnd.api+json")
            .accept("application/vnd.api+json")
            .get("/parent/8")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body(equalTo(expectedDirect));
        given()
            .contentType("application/vnd.api+json; ext=jsonpatch")
            .accept("application/vnd.api+json; ext=jsonpatch")
            .body(req2)
            .patch("/parent")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body(equalTo(expected2));
        given()
            .contentType("application/vnd.api+json")
            .accept("application/vnd.api+json")
            .get("/parent/8")
            .then()
            .statusCode(HttpStatus.SC_NOT_FOUND);
    }

    @Test(priority = 18)
    public void createAndRemoveParent() {
        String req = getJson("/ResourceIT/createAndRemoveParent.req.json");
        String expected = getJson("/ResourceIT/createAndRemoveParent.json");
        given()
            .contentType("application/vnd.api+json")
            .accept("application/vnd.api+json")
            .get("/parent/7")
            .then()
            .statusCode(HttpStatus.SC_OK);
        given()
            .contentType("application/vnd.api+json; ext=jsonpatch")
            .accept("application/vnd.api+json; ext=jsonpatch")
            .body(req)
            .patch("/parent")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body(equalTo(expected));
        given()
            .contentType("application/vnd.api+json")
            .accept("application/vnd.api+json")
            .get("/parent/7")
            .then()
            .statusCode(HttpStatus.SC_NOT_FOUND);
    }

    @Test(priority = 19)
    public void testAddRoot() {
        String req = getJson("/ResourceIT/testAddRoot.req.json");
        String expected1 = getJson("/ResourceIT/testAddRoot.1.json");
        String expected2 = getJson("/ResourceIT/testAddRoot.2.json");
        given()
            .contentType("application/vnd.api+json; ext=jsonpatch")
            .accept("application/vnd.api+json; ext=jsonpatch")
            .body(req)
            .patch("/")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body(equalTo(expected1));
        given()
            .contentType("application/vnd.api+json")
            .accept("application/vnd.api+json")
            .get("/parent/10")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body(equalTo(expected2));
    }

    @Test(priority = 20)
    public void updateRelationshipDirect() {
        String req = getJson("/ResourceIT/updateRelationshipDirect.req.json");
        String expected1 = getJson("/ResourceIT/updateRelationshipDirect.1.json");
        String expected2 = getJson("/ResourceIT/updateRelationshipDirect.2.json");
        given()
            .contentType("application/vnd.api+json; ext=jsonpatch")
            .accept("application/vnd.api+json; ext=jsonpatch")
            .body(req)
            .patch("/parent/10/relationships/children")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body(equalTo(expected1));
        String response = given()
            .contentType("application/vnd.api+json")
            .accept("application/vnd.api+json")
            .get("/parent/10")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .extract().body().asString();

        assertEqualDocuments(response, expected2);
    }

    @Test(priority = 21)
    public void removeSingleRelationship() {
        String req = getJson("/ResourceIT/removeSingleRelationship.req.json");
        String expected1 = getJson("/ResourceIT/removeSingleRelationship.1.json");
        String expected2 = getJson("/ResourceIT/removeSingleRelationship.2.json");
        given()
            .contentType("application/vnd.api+json; ext=jsonpatch")
            .accept("application/vnd.api+json; ext=jsonpatch")
            .body(req)
            .patch("/parent/10/relationships/children")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body(equalTo(expected1));
        String response = given()
            .contentType("application/vnd.api+json")
            .accept("application/vnd.api+json")
            .get("/parent/10")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .extract().body().asString();

        assertEqualDocuments(response, expected2);
    }

    @Test(priority = 22)
    public void addRelationshipChild() {
        String req = getJson("/ResourceIT/addRelationshipChild.req.json");
        String expected = getJson("/ResourceIT/addRelationshipChild.json");
        given()
            .contentType("application/vnd.api+json")
            .accept("application/vnd.api+json")
            .body(req)
            .post("/parent/5/children/6/relationships/parents")
            .then()
            .statusCode(HttpStatus.SC_NO_CONTENT);
        String response = given()
            .contentType("application/vnd.api+json")
            .accept("application/vnd.api+json")
            .get("/parent/5/children/6")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .extract().body().asString();

        assertEqualDocuments(response, expected);
    }

    @Test(priority = 23)
    public void removeRelationshipChild() {
        String req = getJson("/ResourceIT/removeRelationshipChild.req.json");
        String expected = getJson("/ResourceIT/removeRelationshipChild.json");
        given()
            .contentType("application/vnd.api+json")
            .accept("application/vnd.api+json")
            .body(req)
            .delete("/parent/5/children/6/relationships/parents")
            .then()
            .statusCode(HttpStatus.SC_NO_CONTENT);
        given()
            .contentType("application/vnd.api+json")
            .accept("application/vnd.api+json")
            .get("/parent/5/children/6")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body(equalTo(expected));
    }

    @Test(priority = 24)
    public void addRelationships() throws IOException {
        String req = getJson("/ResourceIT/addRelationships.req.json");
        String expected1 = getJson("/ResourceIT/addRelationships.json");
        String expected2 = getJson("/ResourceIT/addRelationships.2.json");
        given()
            .contentType("application/vnd.api+json; ext=jsonpatch")
            .accept("application/vnd.api+json; ext=jsonpatch")
            .body(req)
            .patch("/parent/10/relationships/children")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body(equalTo(expected1));
        String response = given()
            .contentType("application/vnd.api+json")
            .accept("application/vnd.api+json")
            .get("/parent/10")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .extract().body().asString();

        assertEqualDocuments(response, expected2);
    }

    @Test(priority = 25)
    public void checkJsonApiPatchWithError() {
        String req = getJson("/ResourceIT/checkJsonApiPatchWithError.req.json");
        String expected = getJson("/ResourceIT/checkJsonApiPatchWithError.json");
        given()
            .contentType("application/vnd.api+json; ext=jsonpatch")
            .accept("application/vnd.api+json; ext=jsonpatch")
            .body(req)
            .patch("/parent")
            .then()
            .statusCode(HttpStatus.SC_BAD_REQUEST)
            .body(equalTo(expected));
    }

    @Test(priority = 26)
    public void patchExtBadId() {
        String req = getJson("/ResourceIT/patchExtBadId.req.json");
        String expected = getJson("/ResourceIT/patchExtBadId.json");
        given()
            .contentType("application/vnd.api+json; ext=jsonpatch")
            .accept("application/vnd.api+json; ext=jsonpatch")
            .body(req)
            .patch("/")
            .then()
            .statusCode(HttpStatus.SC_BAD_REQUEST)
            .body(equalTo(expected));
    }

    @Test(priority = 27)
    public void patchExtAddUpdate() {
        String req = getJson("/ResourceIT/patchExtAddUpdate.req.json");
        String expected = getJson("/ResourceIT/patchExtAddUpdate.json");
        given()
            .contentType("application/vnd.api+json; ext=jsonpatch")
            .accept("application/vnd.api+json; ext=jsonpatch")
            .body(req)
            .patch("/")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body(equalTo(expected));
    }

    @Test(priority = 28)
    public void patchExtBadValue() {
        String req = getJson("/ResourceIT/patchExtBadValue.req.json");
        String expected = getJson("/ResourceIT/patchExtBadValue.json");
        given()
            .contentType("application/vnd.api+json; ext=jsonpatch")
            .accept("application/vnd.api+json; ext=jsonpatch")
            .body(req)
            .patch("/")
            .then()
            .statusCode(HttpStatus.SC_LOCKED)
            .body(equalTo(expected));
    }

    @Test(priority = 29)
    public void patchExtBadDelete() {
        String req = getJson("/ResourceIT/patchExtBadDelete.req.json");
        String expected = getJson("/ResourceIT/patchExtBadDelete.json");
        given()
            .contentType("application/vnd.api+json; ext=jsonpatch")
            .accept("application/vnd.api+json; ext=jsonpatch")
            .body(req)
            .patch("/")
            .then()
            .statusCode(HttpStatus.SC_BAD_REQUEST)
            .body(equalTo(expected));
    }

    @Test(priority = 30)
    public void createParentWithoutId() {
        String req = getJson("/ResourceIT/createParentWithoutId.req.json");
        String res = getJson("/ResourceIT/createParentWithoutId.json");

        String response = given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body(req)
                .post("/parent")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .extract().body().asString();

        assertEqualDocuments(response, res);
    }

    @Test
    public void assignedIdString() {
        String expectedResponse = getJson("/ResourceIT/assignedIdString.json");

        //Create user with assigned id
        String postRequest = getJson("/ResourceIT/assignedIdString.req.json");
        String postResponse = given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body(postRequest)
                .post("/assignedIdString")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .extract().body().asString();
        assertEqualDocuments(postResponse, expectedResponse);

        //Fetch newly created user
        String getResponse = given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .get("/assignedIdString/user1")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract().body().asString();
        assertEqualDocuments(getResponse, expectedResponse);

        //Try to reassign id
        String patchRequest = getJson("/ResourceIT/failPatchIdString.req.json");
        given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body(patchRequest)
                .patch("/assignedIdString/user1")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    public void assignedIdLong() {
        String expectedResponse = getJson("/ResourceIT/assignedIdLong.json");

        //Create user with assigned id
        String postRequest = getJson("/ResourceIT/assignedIdLong.req.json");
        String postResponse = given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body(postRequest)
                .post("/assignedIdLong")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .extract().body().asString();
        assertEqualDocuments(postResponse, expectedResponse);

        //Fetch newly created user
        String getResponse = given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .get("/assignedIdLong/1")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract().body().asString();
        assertEqualDocuments(getResponse, expectedResponse);

        //Try to reassign id
        String patchRequest = getJson("/ResourceIT/failPatchIdLong.req.json");
        given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body(patchRequest)
                .patch("/assignedIdLong/1")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    public void assignedIdWithoutProvidedId() {
        String req = getJson("/ResourceIT/assignedIdWithoutId.req.json");

        given()
            .contentType("application/vnd.api+json")
            .accept("application/vnd.api+json")
            .body(req)
            .post("/assignedIdString")
            .then()
            .statusCode(HttpStatus.SC_FORBIDDEN);
    }

    @Test
    public void elideBypassSecurity() {
        String expected = getJson("/ResourceIT/testChild.json");

        Elide elide = new Elide(new TestLogger(), AHibernateTest.getDatabaseManager(), new EntityDictionary());
        ElideResponse response =
                elide.get("parent/1/children/1", new MultivaluedHashMap<>(), -1, SecurityMode.BYPASS_SECURITY);
        assertEquals(response.getResponseCode(), HttpStatus.SC_OK);
        assertEquals(response.getBody(), expected);
    }

    @Test
    public void elideSecurityEnabled() {
        Elide elide = new Elide(new TestLogger(), AHibernateTest.getDatabaseManager(), new EntityDictionary());
        ElideResponse response = elide.get("parent/1/children", new MultivaluedHashMap<>(), -1, SecurityMode.ACTIVE);
        assertEquals(response.getResponseCode(), HttpStatus.SC_OK);
        assertEquals(response.getBody(), "{\"data\":[]}");
    }

    @Test
    public void testFiltered() throws Exception {
        String expected = getJson("/ResourceIT/testFiltered.json");

        given().when().get("/filtered").then().statusCode(HttpStatus.SC_OK)
        .body(equalTo(expected));
    }

    @Test
    public void testComputedAttribute() throws Exception {
        String expected = getJson("/ResourceIT/testComputedAttribute.json");

        String req = getJson("/ResourceIT/testComputedAttribute.req.json");

        given()
            .contentType("application/vnd.api+json")
            .accept("application/vnd.api+json")
            .body(req)
            .patch("/user/1")
            .then()
            .statusCode(HttpStatus.SC_NO_CONTENT);

        given().when().get("/user/1").then().statusCode(HttpStatus.SC_OK)
            .body(equalTo(expected));
    }
}
