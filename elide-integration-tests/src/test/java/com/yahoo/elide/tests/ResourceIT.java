/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.tests;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.audit.TestAuditLogger;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.initialization.AbstractIntegrationTestInitializer;
import com.yahoo.elide.jsonapi.models.Data;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.yahoo.elide.jsonapi.models.Resource;
import com.yahoo.elide.jsonapi.models.ResourceIdentifier;
import com.yahoo.elide.security.executors.BypassPermissionExecutor;
import com.yahoo.elide.utils.JsonParser;
import example.Child;
import example.FunWithPermissions;
import example.Parent;
import example.User;
import org.apache.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.startsWith;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * The type Config resource test.
 */
public class ResourceIT extends AbstractIntegrationTestInitializer {
    private static final String JSONAPI_CONTENT_TYPE = "application/vnd.api+json";
    private static final String JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION =
            "application/vnd.api+json; ext=jsonpatch";
    private final JsonParser jsonParser = new JsonParser();

    @BeforeClass
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

        Set<Child> friendSet = new HashSet<>();
        friendSet.add(c2);
        c1.setFriends(friendSet);

        Set<Child> childrenSet1 = new HashSet<>();
        childrenSet1.add(c1);
        childrenSet1.add(c2);

        p1.setChildren(childrenSet1);

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
        Set<Child> childrenSet2 = new HashSet<>();
        childrenSet2.add(c3);
        childrenSet2.add(c4);

        p2.setChildren(childrenSet2);
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

        tx.commit();
    }

    @Test(priority = -1)
    public void testRootCollection() throws Exception {
        String actual = given().when().get("/parent").then().statusCode(HttpStatus.SC_OK)
                .extract().body().asString();

        JsonApiDocument doc = jsonApiMapper.readJsonApiDocument(actual);
        assertEquals(doc.getData().get().size(), 4);
    }

    @Test
    public void testRootCollectionId() {
        String expected = jsonParser.getJson("/ResourceIT/testRootCollectionId.json");

        String actual = given().when().get("/parent/1").then().statusCode(HttpStatus.SC_OK)
                .extract().body().asString();

        assertEquals(actual, expected);
    }

    @Test
    public void testRootCollectionRelationships() {
        String expected = jsonParser.getJson("/ResourceIT/testRootCollectionRelationships.json");

        given()
            .when().get("/parent/1/relationships/children").then().statusCode(HttpStatus.SC_OK)
            .body(equalTo(expected));
    }

    @Test
    public void testChild() throws Exception {
        String expected = jsonParser.getJson("/ResourceIT/testChild.json");

        given().when().get("/parent/1/children/1").then().statusCode(HttpStatus.SC_OK)
        .body(equalTo(expected));
    }

    @Test
    public void testSubCollectionRelationships() throws Exception {
        String expected = jsonParser.getJson("/ResourceIT/testSubCollectionRelationships.json");

        given().when().get("/parent/1/children/1/relationships/parents").then().statusCode(HttpStatus.SC_OK)
            .body(equalTo(expected));
    }

    @Test
    public void failRootCollectionRelationships() {
        given().when().get("/parent/1/relationships").then().statusCode(HttpStatus.SC_NOT_FOUND);
    }

    @Test
    public void failRootCollection() throws Exception {
        String expected = jsonParser.getJson("/ResourceIT/failRootCollection.json");

        given().when().get("/unknown").then().statusCode(HttpStatus.SC_NOT_FOUND)
        .body(equalTo(expected));
    }

    @Test
    public void failRootCollectionId() {
        String expected = jsonParser.getJson("/ResourceIT/failRootCollectionId.json");

        given().when().get("/parent/6789").then().statusCode(HttpStatus.SC_NOT_FOUND)
        .body(equalTo(expected));
    }

    @Test
    public void failChild() throws Exception {
        String expected = jsonParser.getJson("/ResourceIT/failChild.json");

        given().when().get("/parent/1/unknown").then().statusCode(HttpStatus.SC_NOT_FOUND)
        .body(equalTo(expected));
    }

    @Test
    public void failFieldRequest() throws Exception {
        String expected = jsonParser.getJson("/ResourceIT/failFieldRequest.json");

        given().when().get("/parent/1/id").then().statusCode(HttpStatus.SC_NOT_FOUND)
        .body(equalTo(expected));
    }

    @Test
    public void parseFailure() {
        String expected = jsonParser.getJson("/ResourceIT/parseFailure.json");

        given().when().get("company/1|apps/2/links/foo").then().statusCode(HttpStatus.SC_NOT_FOUND)
        .body(equalTo(expected));
    }

    @Test(priority = 1)
    public void testPatchAttrSingle() throws Exception {
        String request = jsonParser.getJson("/ResourceIT/testPatchAttrSingle.json");

        given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .accept(JSONAPI_CONTENT_TYPE)
            .body(request)
            .patch("/parent/2")
            .then()
            .statusCode(HttpStatus.SC_NO_CONTENT)
            .header("content-length", (String) null);

        String actual = given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .accept(JSONAPI_CONTENT_TYPE)
            .get("/parent/2")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .contentType(JSONAPI_CONTENT_TYPE)
            .extract().response().asString();

        JsonApiDocument doc = jsonApiMapper.readJsonApiDocument(actual);
        Data<Resource> data = doc.getData();
        Resource resource = data.getSingleValue();

        assertEquals(resource.getAttributes().get("firstName"), "syzygy");
        assertEquals(resource.getRelationships().size(), 2);
        assertEquals(resource.getRelationships().get("children").getData().get().size(), 2);
    }

    @Test(priority = 2)
    public void testPatchAttrNoUpdateSingle() {
        String request = jsonParser.getJson("/ResourceIT/testPatchAttrNoUpdateSingle.json");

        given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .accept(JSONAPI_CONTENT_TYPE)
            .body(request)
            .patch("/parent/2")
            .then()
            .statusCode(HttpStatus.SC_NO_CONTENT)
            .body(isEmptyOrNullString());
    }

    @Test(priority = 3)
    public void testPatchAttrList() throws Exception {
        String request = jsonParser.getJson("/ResourceIT/testPatchAttrList.json");

        given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .accept(JSONAPI_CONTENT_TYPE)
            .body(request)
            .patch("/parent/3")
            .then()
            .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    @Test(priority = 4)
    public void testPatchSetRel() throws Exception {
        String request = jsonParser.getJson("/ResourceIT/testPatchSetRel.json");

        given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .accept(JSONAPI_CONTENT_TYPE)
            .body(request)
            .patch("/parent/4")
            .then()
            .statusCode(HttpStatus.SC_NO_CONTENT)
            .header("content-length", (String) null);

        String actual = given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .accept(JSONAPI_CONTENT_TYPE)
            .get("/parent/4")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .extract().response().asString();

        JsonApiDocument doc = jsonApiMapper.readJsonApiDocument(actual);
        Data<Resource> data = doc.getData();
        Resource resource = data.getSingleValue();
        Iterator<Resource> itr = resource.getRelationships().get("children").getData().get().iterator();
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

        assertEquals(resource.getAttributes().get("firstName"), "Unknown");
        assertEquals(id1, "4");
        assertEquals(id2, "5");
    }

    @Test(priority = 5)
    public void testPatchRemoveRelSingle() {
        String request = jsonParser.getJson("/ResourceIT/testPatchRemoveRelSingle.req.json");
        String expected = jsonParser.getJson("/ResourceIT/testPatchRemoveRelSingle.json");

        given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .accept(JSONAPI_CONTENT_TYPE)
            .body(request)
            .patch("/parent/4")
            .then()
            .statusCode(HttpStatus.SC_NO_CONTENT)
            .header("content-length", (String) null);

        String actual = given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .accept(JSONAPI_CONTENT_TYPE)
            .get("/parent/4")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .extract().body().asString();

        assertEqualDocuments(actual, expected);
    }

    @Test(priority = 6)
    public void testPatchRelNoUpdateSingle() {
        String request = jsonParser.getJson("/ResourceIT/testPatchRelNoUpdateSingle.json");

        given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .accept(JSONAPI_CONTENT_TYPE)
            .body(request)
            .patch("/parent/4")
            .then()
            .statusCode(HttpStatus.SC_NO_CONTENT)
            .header("content-length", (String) null);
    }

    @Test(priority = 7)
    public void testPatchRelRemoveColl() {
        String request = jsonParser.getJson("/ResourceIT/testPatchRelRemoveColl.req.json");
        String expected = jsonParser.getJson("/ResourceIT/testPatchRelRemoveColl.json");

        given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .accept(JSONAPI_CONTENT_TYPE)
            .body(request)
            .patch("/parent/4")
            .then()
            .statusCode(HttpStatus.SC_NO_CONTENT)
            .header("content-length", (String) null);

        String actual = given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .accept(JSONAPI_CONTENT_TYPE)
            .get("/parent/4")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .extract().body().asString();

        assertEqualDocuments(actual, expected);
    }

    @Test(priority = 8)
    public void testGetNestedSingleInclude() throws IOException {
        String expected  = jsonParser.getJson("/ResourceIT/testGetNestedSingleInclude.json");

        String actual = given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/parent/2?include=children.friends")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract().body().asString();

        assertEqualDocuments(actual, expected);
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
        String expected  = jsonParser.getJson("/ResourceIT/testGetSingleIncludeOnCollection.json");

        String actual = given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/parent?include=children")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract().body().asString();

        assertEqualDocuments(actual, expected);
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
        String expected  = jsonParser.getJson("/ResourceIT/testGetMultipleIncludeOnCollection.json");

        String actual = given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/parent?include=children,spouses")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract().body().asString();

        assertEqualDocuments(actual, expected);
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
        String expected = jsonParser.getJson("/ResourceIT/testGetSingleIncludeOnRelationship.json");

        given()
                .when().get("/parent/1/relationships/children?include=children").then().statusCode(HttpStatus.SC_OK)
                .body(equalTo(expected));
    }

    @Test(priority = 8)
    public void testGetIncludeBadRelation() {

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
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
        String expected  = jsonParser.getJson("/ResourceIT/testGetSortCollection.json");

        String actual = given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/parent?sort=+firstName")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract().body().asString();

        assertEqualDocuments(actual, expected);
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
        String expected  = jsonParser.getJson("/ResourceIT/testGetReverseSortCollection.json");

        String actual = given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/parent?sort=-firstName")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract().body().asString();

        assertEquals(actual, expected);
    }

    @Test(priority = 8)
    public void testGetRelEmptyColl() {
        String expected = jsonParser.getJson("/ResourceIT/testGetRelEmptyColl.json");

        given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .accept(JSONAPI_CONTENT_TYPE)
            .get("/parent/4/relationships/children")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body(equalTo(expected));
    }

    @Test(priority = 8)
    public void testGetWithTrailingSlash() {
        String expected = jsonParser.getJson("/ResourceIT/testGetWithTrailingSlash.json");

        String actual = given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .accept(JSONAPI_CONTENT_TYPE)
            .get("/parent/")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .extract().body().asString();

        assertEqualDocuments(actual, expected);
    }

    @Test(priority = 9)
    public void testPatchRelSetDirect() throws Exception {
        String request = jsonParser.getJson("/ResourceIT/testPatchRelSetDirect.json");

        given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .accept(JSONAPI_CONTENT_TYPE)
            .body(request)
            .patch("/parent/4/relationships/children")
            .then()
            .statusCode(HttpStatus.SC_NO_CONTENT)
            .header("content-length", (String) null);

        String actual = given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .accept(JSONAPI_CONTENT_TYPE)
            .get("/parent/4/relationships/children")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .extract().response().asString();

        JsonApiDocument doc = jsonApiMapper.readJsonApiDocument(actual);
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
        String request = jsonParser.getJson("/ResourceIT/testPatchRelNoUpdateDirect.json");

        given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .accept(JSONAPI_CONTENT_TYPE)
            .body(request)
            .patch("/parent/4/relationships/children")
            .then()
            .statusCode(HttpStatus.SC_NO_CONTENT)
            .header("content-length", (String) null);
    }

    @Test(priority = 11)
    public void testNoDeleteExcludedRelationship() throws Exception {
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body("{\"data\":{\"type\":\"excludedRelationship\",\"id\":\"1\"}}")
                .delete("/parent/4/children/4/relationships/excludedRelationship")
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND);
    }

    @Test(priority = 11)
    public void testForbiddenDeleteEmptyCollectionRelationship() throws Exception {
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body("{\"data\":[]}")
                .delete("/parent/4/children/4/relationships/parents")
                .then()
                .statusCode(HttpStatus.SC_FORBIDDEN);
    }

    @Test(priority = 11)
    public void testDeleteParent() {
        given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .accept(JSONAPI_CONTENT_TYPE)
            .delete("/parent/1")
            .then()
            .statusCode(HttpStatus.SC_NO_CONTENT)
            .body(isEmptyOrNullString());
    }


    @Test(priority = 12)
    public void failDeleteParent() {
        given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .accept(JSONAPI_CONTENT_TYPE)
            .delete("/parent/678")
            .then()
            .statusCode(HttpStatus.SC_NOT_FOUND);
    }

    @Test(priority = 1)
    public void createParentNoRels() {
        String request = jsonParser.getJson("/ResourceIT/createParentNoRels.req.json");
        String expected = jsonParser.getJson("/ResourceIT/createParentNoRels.json");

        String actual = given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .accept(JSONAPI_CONTENT_TYPE)
            .body(request)
            .post("/parent")
            .then()
            .statusCode(HttpStatus.SC_CREATED)
            .extract().body().asString();

        assertEqualDocuments(actual, expected);
    }

    @Test(priority = 2)
    public void createParentWithRels() {
        String request = jsonParser.getJson("/ResourceIT/createParentWithRels.req.json");
        String expected = jsonParser.getJson("/ResourceIT/createParentWithRels.json");

        String actual = given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .accept(JSONAPI_CONTENT_TYPE)
            .body(request)
            .post("/parent")
            .then()
            .statusCode(HttpStatus.SC_CREATED)
            .extract().body().asString();

        assertEqualDocuments(actual, expected);
    }

    @Test
    public void createParentList() {
        String request = jsonParser.getJson("/ResourceIT/createParentList.req.json");
        String expected = "{\"errors\":[\"InvalidEntityBodyException: Bad Request Body";

        given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .accept(JSONAPI_CONTENT_TYPE)
            .body(request)
            .post("/parent")
            .then()
            .statusCode(HttpStatus.SC_BAD_REQUEST)
            .body(startsWith(expected));
    }

    @Test(priority = 3)
    public void createChild() throws Exception {
        String request = jsonParser.getJson("/ResourceIT/createChild.json");

        String childActual = given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .accept(JSONAPI_CONTENT_TYPE)
            .body(request)
            .post("/parent/5/children")
            .then()
            .statusCode(HttpStatus.SC_CREATED)
            .extract().body().asString();

        JsonApiDocument childJsonApiDocument = jsonApiMapper.readJsonApiDocument(childActual);
        Resource resource = childJsonApiDocument.getData().getSingleValue();
        Collection<ResourceIdentifier> resourceIdentifiers = resource.getRelationships().get("parents").getResourceIdentifierData().get();
        ResourceIdentifier rId1 = resourceIdentifiers.iterator().next();
        assertEquals(resource.getId(), "6");
        assertEquals(resource.getType(), "child");
        assertEquals(resource.getRelationships().size(), 2);
        assertEquals(resourceIdentifiers.size(), 2);
        assertEquals(rId1.getType(), "parent");

        String parentActual = given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .accept(JSONAPI_CONTENT_TYPE)
            .get("/parent/5")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .extract().body().asString();

        JsonApiDocument parentJsonApiDocument = jsonApiMapper.readJsonApiDocument(parentActual);
        boolean hasIdentifier = false;

        resource = parentJsonApiDocument.getData().getSingleValue();
        resourceIdentifiers = resource.getRelationships().get("children").getResourceIdentifierData().get();
        for (ResourceIdentifier resourceIdentifier : resourceIdentifiers) {
            hasIdentifier |= resourceIdentifier.getId().equals("6");
        }
        assertTrue(hasIdentifier);
    }

    @Test
    public void createParentBadUri() {
        String request = jsonParser.getJson("/ResourceIT/createParentBadUri.json");

        given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .accept(JSONAPI_CONTENT_TYPE)
            .body(request)
            .post("/parent/678")
            .then()
            .statusCode(HttpStatus.SC_NOT_FOUND);
    }

    @Test
    public void createChildNonRootable() {
        String request = jsonParser.getJson("/ResourceIT/createChildNonRootable.json");
        given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .accept(JSONAPI_CONTENT_TYPE)
            .body(request)
            .post("/child")
            .then()
            .statusCode(HttpStatus.SC_NOT_FOUND);
    }

    @Test
    public void testAddAndRemoveOneToOneRelationship() {
        // first set
        final String request1 = jsonParser.getJson("/ResourceIT/testAddAndRemoveOneToOneRelationship.req.json");
        given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .accept(JSONAPI_CONTENT_TYPE)
            .body(request1)
            .patch("/fun/1")
            .then()
            .statusCode(HttpStatus.SC_NO_CONTENT)
            .header("content-length", (String) null);

        final String expected1 = jsonParser.getJson("/ResourceIT/testAddAndRemoveOneToOneRelationship.json");
        final String actual1 = given().when().get("/fun/1").then().statusCode(HttpStatus.SC_OK).extract().body().asString();
        assertEqualDocuments(actual1, expected1);

        // second set
        final String request2 = jsonParser.getJson("/ResourceIT/testAddAndRemoveOneToOneRelationship.2.req.json");
        given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .accept(JSONAPI_CONTENT_TYPE)
            .body(request2)
            .patch("/fun/1")
            .then()
            .statusCode(HttpStatus.SC_NO_CONTENT)
            .header("content-length", (String) null);
        final String expected2 = jsonParser.getJson("/ResourceIT/testAddAndRemoveOneToOneRelationship.2.json");
        final String actual2 = given().when().get("/fun/1").then().statusCode(HttpStatus.SC_OK).extract().body().asString();
        assertEqualDocuments(actual2, expected2);
    }

    @Test(priority = 13)
    public void createDependentPatchExt() {
        String request = jsonParser.getJson("/ResourceIT/createDependentPatchExt.req.json");
        String expected = jsonParser.getJson("/ResourceIT/createDependentPatchExt.json");
        given()
            .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
            .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
            .body(request)
            .patch("/parent")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body(equalTo(expected));
    }

    @Test(priority = 14)
    public void createChildRelateExisting() {
        String request = jsonParser.getJson("/ResourceIT/createChildRelateExisting.req.json");
        String expected = jsonParser.getJson("/ResourceIT/createChildRelateExisting.json");
        given()
            .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
            .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
            .body(request)
            .patch("/")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body(equalTo(expected));
    }

    @Test(priority = 15)
    public void updateChildRelationToExisting() {
        String request = jsonParser.getJson("/ResourceIT/updateChildRelationToExisting.req.json");
        String expected1 = jsonParser.getJson("/ResourceIT/updateChildRelationToExisting.1.json");
        String expected2 = jsonParser.getJson("/ResourceIT/updateChildRelationToExisting.2.json");
        given()
            .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
            .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
            .body(request)
            .patch("/parent")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body(equalTo(expected1));
        given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .accept(JSONAPI_CONTENT_TYPE)
            .get("/parent/5/children/8")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body(equalTo(expected2));
    }

    @Test(priority = 16)
    public void replaceAttributesAndRelationship() {
        String request = jsonParser.getJson("/ResourceIT/replaceAttributesAndRelationship.req.json");
        String expected1 = jsonParser.getJson("/ResourceIT/replaceAttributesAndRelationship.json");
        String expected2 = jsonParser.getJson("/ResourceIT/replaceAttributesAndRelationship.2.json");
        given()
            .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
            .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
            .body(request)
            .patch("/parent")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body(equalTo(expected1));
        String response = given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .accept(JSONAPI_CONTENT_TYPE)
            .get("/parent/7")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .extract().body().asString();

        assertEqualDocuments(response, expected2);
    }

    @Test(priority = 17)
    public void removeObject() {
        String req1 = jsonParser.getJson("/ResourceIT/removeObject.1.req.json");
        String req2 = jsonParser.getJson("/ResourceIT/removeObject.2.req.json");
        String expectedDirect = jsonParser.getJson("/ResourceIT/removeObject.direct.json");
        String expected1 = jsonParser.getJson("/ResourceIT/removeObject.1.json");
        String expected2 = jsonParser.getJson("/ResourceIT/removeObject.2.json");
        given()
            .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
            .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
            .body(req1)
            .patch("/parent")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body(equalTo(expected1));
        given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .accept(JSONAPI_CONTENT_TYPE)
            .get("/parent/8")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body(equalTo(expectedDirect));
        given()
            .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
            .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
            .body(req2)
            .patch("/parent")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body(equalTo(expected2));
        given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .accept(JSONAPI_CONTENT_TYPE)
            .get("/parent/8")
            .then()
            .statusCode(HttpStatus.SC_NOT_FOUND);
    }

    @Test(priority = 18)
    public void createAndRemoveParent() {
        String request = jsonParser.getJson("/ResourceIT/createAndRemoveParent.req.json");
        String expected = jsonParser.getJson("/ResourceIT/createAndRemoveParent.json");
        given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .accept(JSONAPI_CONTENT_TYPE)
            .get("/parent/7")
            .then()
            .statusCode(HttpStatus.SC_OK);
        given()
            .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
            .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
            .body(request)
            .patch("/parent")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body(equalTo(expected));
        given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .accept(JSONAPI_CONTENT_TYPE)
            .get("/parent/7")
            .then()
            .statusCode(HttpStatus.SC_NOT_FOUND);
    }

    @Test(priority = 19)
    public void testAddRoot() {
        String request = jsonParser.getJson("/ResourceIT/testAddRoot.req.json");
        String expected1 = jsonParser.getJson("/ResourceIT/testAddRoot.1.json");
        String expected2 = jsonParser.getJson("/ResourceIT/testAddRoot.2.json");
        given()
            .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
            .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
            .body(request)
            .patch("/")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body(equalTo(expected1));
        given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .accept(JSONAPI_CONTENT_TYPE)
            .get("/parent/10")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body(equalTo(expected2));
    }

    @Test(priority = 20)
    public void updateRelationshipDirect() {
        String request = jsonParser.getJson("/ResourceIT/updateRelationshipDirect.req.json");
        String expected1 = jsonParser.getJson("/ResourceIT/updateRelationshipDirect.1.json");
        String expected2 = jsonParser.getJson("/ResourceIT/updateRelationshipDirect.2.json");
        given()
            .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
            .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
            .body(request)
            .patch("/parent/10/relationships/children")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body(equalTo(expected1));
        String response = given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .accept(JSONAPI_CONTENT_TYPE)
            .get("/parent/10")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .extract().body().asString();

        assertEqualDocuments(response, expected2);
    }

    @Test(priority = 21)
    public void removeSingleRelationship() {
        String request = jsonParser.getJson("/ResourceIT/removeSingleRelationship.req.json");
        String expected1 = jsonParser.getJson("/ResourceIT/removeSingleRelationship.1.json");
        String expected2 = jsonParser.getJson("/ResourceIT/removeSingleRelationship.2.json");
        given()
            .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
            .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
            .body(request)
            .patch("/parent/10/relationships/children")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body(equalTo(expected1));
        String response = given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .accept(JSONAPI_CONTENT_TYPE)
            .get("/parent/10")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .extract().body().asString();

        assertEqualDocuments(response, expected2);
    }

    @Test(priority = 22)
    public void addRelationshipChild() {
        String request = jsonParser.getJson("/ResourceIT/addRelationshipChild.req.json");
        String expected = jsonParser.getJson("/ResourceIT/addRelationshipChild.json");
        given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .accept(JSONAPI_CONTENT_TYPE)
            .body(request)
            .post("/parent/5/children/6/relationships/parents")
            .then()
            .statusCode(HttpStatus.SC_NO_CONTENT)
            .header("content-length", (String) null);
        String response = given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .accept(JSONAPI_CONTENT_TYPE)
            .get("/parent/5/children/6")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .extract().body().asString();

        assertEqualDocuments(response, expected);
    }

    @Test(priority = 23)
    public void removeRelationshipChild() {
        String request = jsonParser.getJson("/ResourceIT/removeRelationshipChild.req.json");
        String expected = jsonParser.getJson("/ResourceIT/removeRelationshipChild.json");
        given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .accept(JSONAPI_CONTENT_TYPE)
            .body(request)
            .delete("/parent/5/children/6/relationships/parents")
            .then()
            .statusCode(HttpStatus.SC_NO_CONTENT)
            .header("content-length", (String) null);
        given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .accept(JSONAPI_CONTENT_TYPE)
            .get("/parent/5/children/6")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body(equalTo(expected));
    }

    @Test(priority = 24)
    public void addRelationships() throws IOException {
        String request = jsonParser.getJson("/ResourceIT/addRelationships.req.json");
        String expected1 = jsonParser.getJson("/ResourceIT/addRelationships.json");
        String expected2 = jsonParser.getJson("/ResourceIT/addRelationships.2.json");
        given()
            .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
            .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
            .body(request)
            .patch("/parent/10/relationships/children")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body(equalTo(expected1));
        String response = given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .accept(JSONAPI_CONTENT_TYPE)
            .get("/parent/10")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .extract().body().asString();

        assertEqualDocuments(response, expected2);
    }

    @Test(priority = 25)
    public void checkJsonApiPatchWithError() {
        String request = jsonParser.getJson("/ResourceIT/checkJsonApiPatchWithError.req.json");
        String expected = jsonParser.getJson("/ResourceIT/checkJsonApiPatchWithError.json");
        given()
            .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
            .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
            .body(request)
            .patch("/parent")
            .then()
            .statusCode(HttpStatus.SC_BAD_REQUEST)
            .body(equalTo(expected));
    }

    @Test(priority = 26)
    public void patchExtBadId() {
        String request = jsonParser.getJson("/ResourceIT/patchExtBadId.req.json");
        String expected = jsonParser.getJson("/ResourceIT/patchExtBadId.json");
        given()
            .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
            .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
            .body(request)
            .patch("/")
            .then()
            .statusCode(HttpStatus.SC_BAD_REQUEST)
            .body(equalTo(expected));
    }

    @Test(priority = 27)
    public void patchExtAddUpdate() {
        String request = jsonParser.getJson("/ResourceIT/patchExtAddUpdate.req.json");
        String expected = jsonParser.getJson("/ResourceIT/patchExtAddUpdate.json");
        given()
            .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
            .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
            .body(request)
            .patch("/")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body(equalTo(expected));
    }

    @Test(priority = 28)
    public void patchExtBadValue() throws IOException {
        String request = jsonParser.getJson("/ResourceIT/patchExtBadValue.req.json");

        JsonNode result = jsonApiMapper.getObjectMapper().readTree(given()
            .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
            .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
            .body(request)
            .patch("/")
            .then()
            .statusCode(HttpStatus.SC_BAD_REQUEST)
            .extract()
            .body()
            .asString());

        JsonNode errors = result.get("errors");
        Assert.assertNotNull(errors);
        Assert.assertEquals(errors.size(), 3);

        String error = errors.get(2).get("detail").asText();
        String expected = "Unknown identifier '2d1b";
        Assert.assertTrue(error.startsWith(expected), "Error does not start with '" + expected + "'");
        Assert.assertEquals(errors.get(2).get("status").asInt(), 404);
    }

    @Test(priority = 29)
    public void patchExtBadDelete() {
        String request = jsonParser.getJson("/ResourceIT/patchExtBadDelete.req.json");
        String expected = jsonParser.getJson("/ResourceIT/patchExtBadDelete.json");
        given()
            .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
            .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
            .body(request)
            .patch("/")
            .then()
            .statusCode(HttpStatus.SC_BAD_REQUEST)
            .body(equalTo(expected));
    }

    @Test(priority = 30)
    public void createParentWithoutId() {
        String request = jsonParser.getJson("/ResourceIT/createParentWithoutId.req.json");
        String expected = jsonParser.getJson("/ResourceIT/createParentWithoutId.json");

        String actualResponse = given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(request)
                .post("/parent")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .extract().body().asString();

        assertEqualDocuments(actualResponse, expected);
    }

    @Test(priority = 31)
    public void testOneToOneRelationshipAdding() {
        // This is a regression test: we had an issue that disallowed creation of non-root one-to-one objects.
        String createRoot = jsonParser.getJson("/ResourceIT/createOneToOneRoot.json");

        given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .accept(JSONAPI_CONTENT_TYPE)
            .body(createRoot)
            .post("/oneToOneRoot")
            .then()
            .statusCode(HttpStatus.SC_CREATED);

        // Verify it was actually created
        String o = given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .accept(JSONAPI_CONTENT_TYPE)
            .body(createRoot)
            .get("/oneToOneRoot/1")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .extract().asString();

        // Create other object
        String createChild = jsonParser.getJson("/ResourceIT/createOneToOneNonRoot.json");
        given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .accept(JSONAPI_CONTENT_TYPE)
            .body(createChild)
            .post("/oneToOneRoot/1/otherObject")
            .then()
            .statusCode(HttpStatus.SC_CREATED);

        // Verify contents
        String actualFirst = given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .accept(JSONAPI_CONTENT_TYPE)
            .body(createChild)
            .get("/oneToOneRoot/1")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .extract().body().asString();

        String actualChild = given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .accept(JSONAPI_CONTENT_TYPE)
            .body(createChild)
            .get("/oneToOneRoot/1/otherObject")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .extract().body().asString();

        String expectedFirst = jsonParser.getJson("/ResourceIT/oneToOneRootCreatedRelationship.json");
        String expectedChild = jsonParser.getJson("/ResourceIT/oneToOneNonRootCreatedRelationship.json");

        assertEqualDocuments(actualFirst, expectedFirst);
        assertEqualDocuments(actualChild, expectedChild);
    }

    @Test(priority = 32)
    public void testReadPermissionDefaultOverride() {
        String create = jsonParser.getJson("/ResourceIT/createYetAnotherPermissionRead.req.json");

        given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .accept(JSONAPI_CONTENT_TYPE)
            .body(create)
            .post("/yetAnotherPermission")
            .then()
            .statusCode(HttpStatus.SC_CREATED);

        // Verify contents
        String actual = given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .accept(JSONAPI_CONTENT_TYPE)
            .get("/yetAnotherPermission")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .extract().body().asString();

        String expected = jsonParser.getJson("/ResourceIT/createYetAnotherPermissionRead.json");

        assertEqualDocuments(actual, expected);
    }

    @Test(priority = 33)
    public void testUpdateToOneCollection() {
        String createRoot = jsonParser.getJson("/ResourceIT/createOneToOneRoot.json");

        // Verify it was actually created
        String o = given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(createRoot)
                .get("/oneToOneRoot/1")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract().asString();

        // Create other object
        String createChild = jsonParser.getJson("/ResourceIT/updateOneToOneNonRoot.json");
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(createChild)
                .post("/oneToOneRoot/1/otherObject")
                .then()
                .statusCode(HttpStatus.SC_CREATED);

        // Verify contents
        String actualFirst = given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(createChild)
                .get("/oneToOneRoot/1")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract().body().asString();

        String actualChild = given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(createChild)
                .get("/oneToOneRoot/1/otherObject")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract().body().asString();

        String
                expectedFirst = jsonParser.getJson("/ResourceIT/oneToOneRootUpdatedRelationship.json");
        String expectedChild = jsonParser.getJson("/ResourceIT/oneToOneNonRootUpdatedRelationship.json");

        assertEqualDocuments(actualFirst, expectedFirst);
        assertEqualDocuments(actualChild, expectedChild);
    }

    @Test(priority = 34)
    public void testPostToRecord() {
        String createRoot = jsonParser.getJson("/ResourceIT/createOneToOneRoot.json");

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(createRoot)
                .post("/oneToOneRoot/1")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    @Test(priority = 34)
    public void testFilterIds() {
        String expectedRels = jsonParser.getJson("/ResourceIT/testFilterIdRels.json");
        String expectedIncl = jsonParser.getJson("/ResourceIT/testFilterIdIncluded.json");
        String expectedColl = jsonParser.getJson("/ResourceIT/testFilterIdCollection.json");
        given()
                .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .get("/parent/10/relationships/children?filter[child.id]=4")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(expectedRels));
        given()
                .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .get("/parent/10?include=children&filter[child.id]=4,5")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(expectedIncl));
        given()
                .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .get("/parent?include=children&filter[child.id]=4")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(expectedColl));
    }

    @Test(priority = 36)
    public void testNestedPatch() {
        String req = jsonParser.getJson("/ResourceIT/nestedPatchCreate.req.json");
        String expected = jsonParser.getJson("/ResourceIT/nestedPatchCreate.resp.json");
        given()
                .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .body(req)
                .patch("/parent")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(expected));
    }

    @Test(priority = 37)
    public void testCreatedRootNoReadPermRequired() {
        String req = jsonParser.getJson("/ResourceIT/testPatchExtNoReadPermForNew.req.json");
        String badReq = "[{\n"
                 + "    \"op\": \"add\",\n"
                 + "    \"path\": \"/1/child\",\n"
                 + "    \"value\": {\n"
                 + "      \"type\": \"child\",\n"
                 + "      \"id\": \"12345678-1234-1234-1234-123456789ab2\"\n"
                 + "    }\n"
                 + "  }]";
        String expected = jsonParser.getJson("/ResourceIT/testPatchExtNoReadPermForNew.resp.json");
        given()
                .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .body(req)
                .patch("/specialread")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(expected));
        given()
                .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .body(badReq)
                .patch("/specialread")
                .then()
                .statusCode(HttpStatus.SC_FORBIDDEN)
                .body(equalTo("{\"errors\":[\"ForbiddenAccessException\"]}"));
    }

    @Test(priority = 38)
    public void testUserNoShare() {
        String initialCreate = "{\n"
                + "  \"data\":{\n"
                + "    \"type\":\"noShareBid\",\n"
                + "    \"id\":\"1\"\n"
                + "  }\n"
                + "}\n";
        String req = jsonParser.getJson("/ResourceIT/noShareBiDirectional.req.json");
        String expected = "{\"data\":{\"type\":\"noShareBid\",\"id\":\"2\",\"relationships\":{\"other\":{\"data\":{\"type\":\"noShareBid\",\"id\":\"1\"}}}}}";
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(initialCreate)
                .post("/noShareBid")
                .then()
                .statusCode(HttpStatus.SC_CREATED);
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(req)
                .post("/noShareBid/1/other")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .body(equalTo(expected));
    }

    @Test(priority = 39)
    public void testPatchExtNoCommit() {
        String req = jsonParser.getJson("/ResourceIT/testPatchExtNoCommit.req.json");
        given()
                .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .body(req)
                .patch("/")
                .then()
                .statusCode(HttpStatus.SC_FORBIDDEN)
                .body(equalTo("{\"errors\":[\"ForbiddenAccessException\"]}"));
    }

    @Test(priority = 40)
    public void testInverseDeleteFromCollection() {
        // NOTE: This only tests this behavior is correct BECAUSE of the Child4Parent10 check.
        // It's a bit contrived, but we shouldn't lose the logic.
        // The problem: when deleting an inverse relation, it checks whether it can update its inverse field back
        // to the original. This is flawed logic since you're deleting the original in the first place (and that check
        // succeeded if we got there).
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .delete("/parent/10/children/4")
                .then()
                .statusCode(204);
    }

    @Test(priority = 41)
    public void testPostInvalidRelationship() {
        // Note: This tests the correct response when POSTing a resource with a not "include" relationship. The server
        // should returns UnknownEntityException rather than NPE.
        String createRoot = jsonParser.getJson("/ResourceIT/testPostWithInvalidRelationship.json");

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(createRoot)
                .post("resourceWithInvalidRelationship")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    public void assignedIdString() {
        String expected = jsonParser.getJson("/ResourceIT/assignedIdString.json");

        //Create user with assigned id
        String request = jsonParser.getJson("/ResourceIT/assignedIdString.req.json");
        String actual = given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(request)
                .post("/assignedIdString")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .extract().body().asString();
        assertEqualDocuments(actual, expected);

        //Fetch newly created user
        String getResponse = given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/assignedIdString/user1")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract().body().asString();
        assertEqualDocuments(getResponse, expected);

        //Try to reassign id
        String patchRequest = jsonParser.getJson("/ResourceIT/failPatchIdString.req.json");
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(patchRequest)
                .patch("/assignedIdString/user1")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    public void assignedIdLong() {
        String expected = jsonParser.getJson("/ResourceIT/assignedIdLong.json");

        //Create user with assigned id
        String postRequest = jsonParser.getJson("/ResourceIT/assignedIdLong.req.json");
        String postResponse = given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(postRequest)
                .post("/assignedIdLong")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .extract().body().asString();
        assertEqualDocuments(postResponse, expected);

        //Fetch newly created user
        String getResponse = given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/assignedIdLong/1")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract().body().asString();
        assertEqualDocuments(getResponse, expected);

        //Try to reassign id
        String patchRequest = jsonParser.getJson("/ResourceIT/failPatchIdLong.req.json");
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(patchRequest)
                .patch("/assignedIdLong/1")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    public void assignedIdWithoutProvidedId() {
        String request = jsonParser.getJson("/ResourceIT/assignedIdWithoutId.req.json");

        given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .accept(JSONAPI_CONTENT_TYPE)
            .body(request)
            .post("/assignedIdString")
            .then()
            .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    public void elideBypassSecurity() {
        String expected = jsonParser.getJson("/ResourceIT/elideBypassSecurity.json");

        Elide elide = new Elide.Builder(new TestAuditLogger(), AbstractIntegrationTestInitializer.getDatabaseManager())
                .permissionExecutor(BypassPermissionExecutor.class)
                .build();
        ElideResponse response =
                elide.get("parent/1/children/1", new MultivaluedHashMap<>(), -1);
        assertEquals(response.getResponseCode(), HttpStatus.SC_OK);
        assertEquals(response.getBody(), expected);
    }

    @Test
    public void elideSecurityEnabled() {
        Elide elide = new Elide.Builder(new TestAuditLogger(), AbstractIntegrationTestInitializer.getDatabaseManager()).build();
        ElideResponse response = elide.get("parent/1/children", new MultivaluedHashMap<>(), -1);
        assertEquals(response.getResponseCode(), HttpStatus.SC_OK);
        assertEquals(response.getBody(), "{\"data\":[]}");
    }

    @Test
    public void testComputedAttribute() throws Exception {
        String expected = jsonParser.getJson("/ResourceIT/testComputedAttribute.json");
        String request = jsonParser.getJson("/ResourceIT/testComputedAttribute.req.json");

        given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .accept(JSONAPI_CONTENT_TYPE)
            .body(request)
            .patch("/user/1")
            .then()
            .statusCode(HttpStatus.SC_NO_CONTENT)
            .header("content-lenth", (String) null);

        given().when().get("/user/1").then().statusCode(HttpStatus.SC_OK)
            .body(equalTo(expected));
    }

    // Update checks should be _deferred_ (neither ignored nor aggressively applied) on newly created objects.
    @Test
    public void testUpdateDeferredOnCreate() {
        String expected = jsonParser.getJson("/ResourceIT/createButNoUpdate.resp.json");
        String badRequest = jsonParser.getJson("/ResourceIT/createButNoUpdate.bad.req.json");
        String request = jsonParser.getJson("/ResourceIT/createButNoUpdate.req.json");
        String updateRequest = jsonParser.getJson("/ResourceIT/createButNoUpdate.update.req.json");

        // First ensure we cannot update fields that are explicitly disallowed
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(badRequest)
                .post("/createButNoUpdate")
                .then()
                .statusCode(HttpStatus.SC_FORBIDDEN);

        // Now check that updating allowed fields enables us to create the object
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(request)
                .post("/createButNoUpdate")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .body(equalTo(expected));

        // Ensure we cannot update that newly created object
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(updateRequest)
                .patch("/createButNoUpdate/1")
                .then()
                .statusCode(HttpStatus.SC_FORBIDDEN);
    }

    @Test
    public void testPatchDeferredOnCreate() {
        String request = jsonParser.getJson("/ResourceIT/testPatchDeferredOnCreate.req.json");
        String expected = jsonParser.getJson("/ResourceIT/testPatchDeferredOnCreate.json");
        given()
            .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
            .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
            .body(request)
            .patch("/")
            .then()
            .statusCode(HttpStatus.SC_FORBIDDEN)
            .body(equalTo(expected));
    }

    // TODO: Test that user checks still apply at commit time

    @Test
    public void badRoot() {
        given().when().get("/oops").then().statusCode(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void badRootId() {
        given().when().get("/oops/1").then().statusCode(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void badChildCollection() {
        given().when().get("/user/1/oops").then().statusCode(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void badChildCollectionId() {
        given().when().get("/user/1/oops/1").then().statusCode(Status.NOT_FOUND.getStatusCode());
    }
}
