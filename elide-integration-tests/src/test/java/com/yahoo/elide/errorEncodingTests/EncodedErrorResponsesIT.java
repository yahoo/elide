/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.errorEncodingTests;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.initialization.AbstractIntegrationTestInitializer;
import com.yahoo.elide.initialization.EncodedErrorResponsesTestApplicationResourceConfig;
import com.yahoo.elide.utils.JsonParser;

import com.google.common.collect.Sets;

import example.Book;
import example.Child;
import example.ExceptionThrowingBean;
import example.FunWithPermissions;
import example.Invoice;
import example.LineItem;
import example.Parent;
import example.User;

import org.apache.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;

@Slf4j
public class EncodedErrorResponsesIT extends AbstractIntegrationTestInitializer {

    private static final String JSONAPI_CONTENT_TYPE = "application/vnd.api+json";
    private static final String JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION =
            "application/vnd.api+json; ext=jsonpatch";
    private final JsonParser jsonParser = new JsonParser();

    public EncodedErrorResponsesIT() {
        super(EncodedErrorResponsesTestApplicationResourceConfig.class);
    }

    @BeforeClass
    public void setup() throws Exception {
        DataStoreTransaction tx = dataStore.beginTransaction();

        Parent parent = new Parent(); // id 1
        Child child = new Child(); // id 1
        parent.setChildren(Sets.newHashSet(child));
        parent.setSpouses(Sets.newHashSet());
        child.setParents(Sets.newHashSet(parent));

        tx.createObject(parent, null);
        tx.createObject(child, null);

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

        tx.createObject(c1, null);
        tx.createObject(c2, null);
        tx.createObject(c3, null);
        tx.createObject(c4, null);

        tx.createObject(p1, null);
        tx.createObject(p2, null);
        tx.createObject(p3, null);

        Book bookWithPercentage = new Book();
        bookWithPercentage.setTitle("titlewith%percentage");
        Book bookWithoutPercentage = new Book();
        bookWithoutPercentage.setTitle("titlewithoutpercentage");

        tx.createObject(bookWithPercentage, null);
        tx.createObject(bookWithoutPercentage, null);

        FunWithPermissions fun = new FunWithPermissions();
        tx.createObject(fun, null);

        User user = new User(); //ID 1
        user.setPassword("god");
        tx.createObject(user, null);

        Invoice invoice = new Invoice();
        invoice.setId(1);
        LineItem item = new LineItem();
        invoice.setItems(Sets.newHashSet(item));
        item.setInvoice(invoice);
        tx.createObject(invoice, null);
        tx.createObject(item, null);

        ExceptionThrowingBean etb = new ExceptionThrowingBean();
        etb.setId(1L);
        tx.createObject(etb, null);

        tx.commit(null);
        tx.close();

        tearDownServer();
        setUpServer();
    }

    @Test
    public void invalidAttributeException() {
        String request = jsonParser.getJson("/EncodedErrorResponsesIT/InvalidAttributeException.req.json");
        String expected = jsonParser.getJson("/EncodedErrorResponsesIT/invalidAttributeException.json");
        given()
                .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .body(request)
                .patch("/parent")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(equalTo(expected));
    }

    @Test
    public void invalidCollectionException() {
        String expected = jsonParser.getJson("/EncodedErrorResponsesIT/invalidCollection.json");
        given().when().get("/unknown").then().statusCode(HttpStatus.SC_NOT_FOUND).body(equalTo(expected));
    }

    @Test
    public void invalidEntityBodyException() {
        String request = jsonParser.getJson("/EncodedErrorResponsesIT/invalidEntityBodyException.req.json");
        String expected = jsonParser.getJson("/EncodedErrorResponsesIT/invalidEntityBodyException.json");
        given()
                .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .body(request)
                .patch("/parent")
        .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(equalTo(expected));
    }

    @Test
    public void invalidObjectIdentifierException() {
        String expected = jsonParser.getJson("/EncodedErrorResponsesIT/invalidObjectIdentifierExecption.json");
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/parent/100")
        .then()
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .body(equalTo(expected));
    }


    @Test
    public void invalidValueException() {
        String request = jsonParser.getJson("/EncodedErrorResponsesIT/invalidValueException.req.json");
        String expected = jsonParser.getJson("/EncodedErrorResponsesIT/invalidValueException.json");
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(request)
                .post("/invoice")
        .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(equalTo(expected));
    }

    @Test
    public void jsonPatchExtensionException() {
        String request = jsonParser.getJson("/EncodedErrorResponsesIT/jsonPatchExtensionException.req.json");
        String expected = jsonParser.getJson("/EncodedErrorResponsesIT/jsonPatchExtensionException.json");
        given()
                .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .body(request).patch()
        .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(equalTo(expected));
    }

    @Test
    public void transactionException() {
        // intentionally forget the comma between type and id to force a transaction exception
        String request = "{\"data\": {\"type\": \"invoice\" \"id\": 100}}";
        String expected = jsonParser.getJson("/EncodedErrorResponsesIT/transactionException.json");
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(request).post("/invoice")
        .then()
                .statusCode(HttpStatus.SC_LOCKED)
                .body(equalTo(expected));
    }
}
