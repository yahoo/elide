/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.core.sort.Sorting;
import com.yahoo.elide.request.Argument;
import com.yahoo.elide.request.Attribute;
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
        ElideSettings elideSettings = mock(ElideSettings.class);
        when(elideSettings.getDefaultMaxPageSize()).thenReturn(100);
        when(elideSettings.getDefaultPageSize()).thenReturn(100);

        projectionMaker = new GraphQLEntityProjectionMaker(dictionary, elideSettings);
    }

    @Test
    public void testMakeOnBasicQuery() {
        EntityProjection expectedProjection = bookProjection();

        // Fetch Single Book
        Collection<EntityProjection> entityProjections = projectionMaker.make(
                "{\n" +
                        //"  book(id: \"1\") {\n" +
                        "  book {\n" +
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

        EntityProjection actualProjection = entityProjections.stream().collect(Collectors.toList()).get(0);

        Assert.assertEquals(actualProjection, expectedProjection);
    }

    @Test
    public void testMakeOnBasicQueryWithArgument() {
        EntityProjection expectedProjection = EntityProjection.builder()
                .dictionary(dictionary)
                .type(Book.class)
                .attribute(
                        Attribute.builder()
                                .type(long.class)
                                .name("id")
                                .argument(Argument.builder().name("id").value("\"1\"").build())
                                .build()
                )
                .attribute(bookTitleAttribute())
                .relationship("Author", authorProjection())
                .build();

        // Fetch Single Book with single argument
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

        EntityProjection actualProjection = entityProjections.stream().collect(Collectors.toList()).get(0);

        Assert.assertEquals(actualProjection, expectedProjection);
    }

    private EntityProjection bookProjection() {
        return EntityProjection.builder()
                .dictionary(dictionary)
                .type(Book.class)
                .attribute(bookIdAttribute())
                .attribute(bookTitleAttribute())
                .relationship("Author", authorProjection())
                .build();
    }

    @Test
    public void testMakeOnQueryWithRelationship() {
        EntityProjection expectedProjection = bookProjection();

        // Fetch Single Book
        Collection<EntityProjection> entityProjections = projectionMaker.make(
                "{\n" +
                        "  book {\n" +
                        "    edges {\n" +
                        "      node {\n" +
                        "        id\n" +
                        "        title\n" +
                        "        authors {\n" +
                        "          edges {\n" +
                        "            node {\n" +
                        "              id\n" +
                        "              name\n" +
                        "            }\n" +
                        "          }\n" +
                        "        }\n" +
                        "      }\n" +
                        "    }\n" +
                        "  }\n" +
                        "}"
        );
        Assert.assertEquals(entityProjections.size(), 1);

        EntityProjection actualProjection = entityProjections.stream().collect(Collectors.toList()).get(0);

        Assert.assertEquals(actualProjection, expectedProjection);
    }

    @Test
    public void testMakeOnQueryWithRelationshipAndArgument() {
        EntityProjection expectedProjection = EntityProjection.builder()
                .dictionary(dictionary)
                .type(Book.class)
                .attribute(
                        Attribute.builder()
                                .type(long.class)
                                .name("id")
                                .argument(Argument.builder().name("id").value("\"1\"").build())
                                .build()
                )
                .attribute(bookTitleAttribute())
                .relationship("Author", authorProjection())
                .build();

        // Fetch Single Book
        Collection<EntityProjection> entityProjections = projectionMaker.make(
                "{\n" +
                        "  book(id: \"1\") {\n" +
                        "    edges {\n" +
                        "      node {\n" +
                        "        id\n" +
                        "        title\n" +
                        "        authors {\n" +
                        "          edges {\n" +
                        "            node {\n" +
                        "              id\n" +
                        "              name\n" +
                        "            }\n" +
                        "          }\n" +
                        "        }\n" +
                        "      }\n" +
                        "    }\n" +
                        "  }\n" +
                        "}"
        );
        Assert.assertEquals(entityProjections.size(), 1);

        EntityProjection actualProjection = entityProjections.stream().collect(Collectors.toList()).get(0);

        Assert.assertEquals(actualProjection, expectedProjection);
    }

    @Test
    public void testMakeOnQueryWithPagination() {

        // Fetch Single Book
        Collection<EntityProjection> entityProjections = projectionMaker.make(
                "{\n" +
                        "  book(first: 1, after: 1) {\n" +
                        "    edges {\n" +
                        "      node {\n" +
                        "        id\n" +
                        "        title\n" +
                        "        authors {\n" +
                        "          edges {\n" +
                        "            node {\n" +
                        "              id\n" +
                        "              name\n" +
                        "            }\n" +
                        "          }\n" +
                        "        }\n" +
                        "      }\n" +
                        "    }\n" +
                        "  }\n" +
                        "}"
        );
        Assert.assertEquals(entityProjections.size(), 1);

        EntityProjection actualProjection = entityProjections.stream().collect(Collectors.toList()).get(0);

        Assert.assertEquals(actualProjection.getPagination().getOffset(), 1);
    }

    @Test
    public void testMakeOnQueryWithSorting() {
        Collection<EntityProjection> entityProjections = projectionMaker.make(
                "{\n" +
                        "  book(sort: \"-author.id,id\") {\n" +
                        "    edges {\n" +
                        "      node {\n" +
                        "        id\n" +
                        "        title\n" +
                        "        authors {\n" +
                        "          edges {\n" +
                        "            node {\n" +
                        "              id\n" +
                        "              name\n" +
                        "            }\n" +
                        "          }\n" +
                        "        }\n" +
                        "      }\n" +
                        "    }\n" +
                        "  }\n" +
                        "}"
        );
        Assert.assertEquals(entityProjections.size(), 1);

        EntityProjection actualProjection = entityProjections.stream().collect(Collectors.toList()).get(0);

        Assert.assertEquals(actualProjection.getSorting(), Sorting.parseSortRule("-author.id,id"));
    }

    private Attribute bookIdAttribute() {
        return Attribute.builder()
                .type(long.class)
                .name("id")
                .build();
    }

    private Attribute bookTitleAttribute() {
        return Attribute.builder()
                .type(String.class)
                .name("title")
                .build();
    }

    private EntityProjection authorProjection() {
        return EntityProjection.builder()
                .dictionary(dictionary)
                .type(Author.class)
                .attribute(authorIdAttribute())
                .attribute(authorNameAttribute())
                .build();
    }

    private Attribute authorIdAttribute() {
        return Attribute.builder()
                .type(Long.class)
                .name("id")
                .build();
    }

    private Attribute authorNameAttribute() {
        return Attribute.builder()
                .type(String.class)
                .name("name")
                .build();
    }
}
