/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

import com.yahoo.elide.request.EntityProjection;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import example.Author;
import example.Book;

import java.util.Collection;
import java.util.stream.Collectors;

public class GraphQLEntityProjectionMakerTest extends GraphQLTest {

    private GraphQLEntityProjectionMaker projectionMaker;

    @BeforeMethod
    public void childInit() {
        projectionMaker = new GraphQLEntityProjectionMaker(dictionary);
    }

    @Test
    public void testMake() {
        Collection<EntityProjection> entityProjections = projectionMaker.make(
                "{\n" +
                        "  book(id: \"1\") {\n" +
                        "        id\n" +
                        "        title\n" +
                        "        author {\n" +
                        "              id\n" +
                        "              name\n" +
                        "            }\n" +
                        "  }\n" +
                        "}"
        );

        Assert.assertEquals(entityProjections.size(), 1);

        EntityProjection entityProjection = entityProjections.stream().collect(Collectors.toList()).get(0);

        Assert.assertEquals(entityProjection.getType(), Book.class);
        Assert.assertEquals(entityProjection.getRelationship("author").getType(), Author.class);
    }
}
