/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.extension.test;

import static com.yahoo.elide.Elide.JSONAPI_CONTENT_TYPE;
import static com.yahoo.elide.test.graphql.GraphQLDSL.document;
import static com.yahoo.elide.test.graphql.GraphQLDSL.field;
import static com.yahoo.elide.test.graphql.GraphQLDSL.selection;
import static com.yahoo.elide.test.graphql.GraphQLDSL.selections;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.attr;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.attributes;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.data;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.id;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.resource;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.type;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import com.yahoo.elide.Elide;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.dictionary.Injector;
import com.yahoo.elide.extension.test.models.Book;
import org.apache.http.HttpStatus;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

public class ElideExtensionTest {

    // Start unit test with your extension loaded
    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
        .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                .addAsResource("application.properties")
                .addClass(Book.class));

    @Inject
    EntityDictionary dictionary;

    @Inject
    Elide elide;

    @Inject
    Injector injector;

    @Test
    public void testBookJsonApiEndpoint() {
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(
                        data(
                                resource(
                                        type("book"),
                                        id(1),
                                        attributes(
                                                attr("title", "foo")
                                        )
                                )
                        )
                )
                .post("/book")
                .then()
                .log().all()
                .statusCode(HttpStatus.SC_CREATED);
        RestAssured.when().get("/jsonapi/book").then().log().all().statusCode(200);
    }

    @Test
    public void testBookGraphqlEndpoint() {
        String query = document(
                selection(
                        field(
                                "book",
                                selections(
                                        field("id"),
                                        field("title")
                                )
                        )
                )
        ).toQuery();

        String wrapped = String.format("{ \"query\" : \"%s\" }", query);

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(wrapped)
                .post("/graphql")
                .then()
                .log().all()
                .statusCode(HttpStatus.SC_OK);
    }

    @Test
    public void testSwaggerCollectionEndpoint() {
        RestAssured.when().get("/doc").then().log().all().statusCode(200);
    }

    @Test
    public void testSwaggerApiEndpoint() {
        RestAssured.when().get("/doc/api").then().log().all().statusCode(200);
    }

    @Test
    public void testInjection() {
        EntityDictionary dictionary = injector.instantiate(EntityDictionary.class);
        assertNotNull(dictionary);

        Book test = injector.instantiate(Book.class);
        assertNotNull(test);
    }
}
