/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.endpoints.bootstrap;

import com.jayway.restassured.RestAssured;
import com.yahoo.elide.hibernate.AHibernateTest;
import org.testng.annotations.BeforeClass;

/**
 * Bootstrap with book-author data
 */
public abstract class AbstractBookAuthorData extends AHibernateTest {
    @BeforeClass
    public void setupBookAuthorData() {
        // Create Author: Ernest Hemingway
        RestAssured
                .given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body(getJson("/bootstrap/bookauthor/ernest_hemingway.json"))
                .post("/author");

        // Create Book: The Old Man and the Sea
        RestAssured
                .given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body(getJson("/bootstrap/bookauthor/the_old_man_and_the_sea.json"))
                .post("/book");

        // Create Relationship: Ernest Hemingway -> The Old Man and the Sea
        RestAssured
                .given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body(getJson("/bootstrap/bookauthor/ernest_hemingway_relationship.json"))
                .patch("/book/1/relationships/authors");

        // Create Author: Orson Scott Card
        RestAssured
                .given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body(getJson("/bootstrap/bookauthor/orson_scott_card.json"))
                .post("/author");

        // Create Book: Ender's Game
        RestAssured
                .given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body(getJson("/bootstrap/bookauthor/enders_game.json"))
                .post("/book");

        // Create Relationship: Orson Scott Card -> Ender's Game
        RestAssured
                .given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body(getJson("/bootstrap/bookauthor/orson_scott_card_relationship.json"))
                .patch("/book/2/relationships/authors");

        // Create Book: For Whom the Bell Tolls
        RestAssured
                .given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body(getJson("/bootstrap/bookauthor/for_whom_the_bell_tolls.json"))
                .post("/book");

        // Create Relationship: Ernest Hemingway -> For Whom the Bell Tolls
        RestAssured
                .given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body(getJson("/bootstrap/bookauthor/ernest_hemingway_relationship.json"))
                .patch("/book/3/relationships/authors");
    }
}
