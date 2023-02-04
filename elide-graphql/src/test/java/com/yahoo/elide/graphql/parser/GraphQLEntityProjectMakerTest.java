/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql.parser;

import static com.yahoo.elide.test.graphql.GraphQLDSL.argument;
import static com.yahoo.elide.test.graphql.GraphQLDSL.arguments;
import static com.yahoo.elide.test.graphql.GraphQLDSL.document;
import static com.yahoo.elide.test.graphql.GraphQLDSL.field;
import static com.yahoo.elide.test.graphql.GraphQLDSL.selection;
import static com.yahoo.elide.test.graphql.GraphQLDSL.selections;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yahoo.elide.core.request.Argument;
import com.yahoo.elide.core.request.Attribute;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.request.Pagination;
import com.yahoo.elide.graphql.PersistentResourceFetcherTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class GraphQLEntityProjectMakerTest extends PersistentResourceFetcherTest {

     @Test
     public void testDefaultPagination() throws IOException {
         String graphQLRequest = loadGraphQLRequest("fetch/rootSingle" + ".graphql");
         GraphQLEntityProjectionMaker projectionMaker = new GraphQLEntityProjectionMaker(settings);
         GraphQLProjectionInfo projectionInfo = projectionMaker.make(graphQLRequest);
         assertEquals(1, projectionInfo.getProjections().size());
         assertTrue(projectionInfo.getProjections().entrySet().iterator().next().getValue().getPagination().isDefaultInstance());
     }

     @Test
     public void testLimitPagination() throws IOException {
         String graphQLRequest = loadGraphQLRequest("fetch/rootCollectionPaginateWithOffset" + ".graphql");
         GraphQLEntityProjectionMaker projectionMaker = new GraphQLEntityProjectionMaker(settings);
         GraphQLProjectionInfo projectionInfo = projectionMaker.make(graphQLRequest);
         assertEquals(1, projectionInfo.getProjections().size());
         Pagination page = projectionInfo.getProjections().entrySet().iterator().next().getValue().getPagination();
         assertFalse(page.isDefaultInstance());
         assertEquals(1, page.getLimit());
     }

     @Test
     public void testParameterizedAttribute() {
         String graphQLRequest = document(
                 selection(
                         field(
                                 "parameterizedExample", arguments(
                                         argument("entityArgument", "xyz", true)
                                         ),
                                 selections(
                                         field("attribute", arguments(
                                                 argument("argument", "abc", true)
                                         ))
                                 )
                         )
                 )
         ).toQuery();

         GraphQLEntityProjectionMaker projectionMaker = new GraphQLEntityProjectionMaker(settings);
         GraphQLProjectionInfo projectionInfo = projectionMaker.make(graphQLRequest);
         assertEquals(1, projectionInfo.getProjections().size());

         EntityProjection projection = projectionInfo.getProjections().values().iterator().next();

         // Verify Entity Argument
         assertEquals(1, projection.getArguments().size());

         Argument entityArgument = projection.getArguments().iterator().next();

         assertEquals("entityArgument", entityArgument.getName());
         assertEquals(String.class, entityArgument.getType());
         assertEquals("xyz", entityArgument.getValue());

         // Verify Attribute Argument
         assertEquals(1, projection.getAttributes().size());

         Attribute attribute = projection.getAttributes().iterator().next();

         assertEquals(1, attribute.getArguments().size());

         Argument argument = attribute.getArguments().iterator().next();

         assertEquals("argument", argument.getName());
         assertEquals(String.class, argument.getType());
         assertEquals("abc", argument.getValue());
     }

    @Test
    public void testParameterizedAttributeDefaultValue() {
        String graphQLRequest = document(
                selection(
                        field(
                                "parameterizedExample",
                                selections(
                                        field("attribute")
                                )
                        )
                )
        ).toQuery();

        GraphQLEntityProjectionMaker projectionMaker = new GraphQLEntityProjectionMaker(settings);
        GraphQLProjectionInfo projectionInfo = projectionMaker.make(graphQLRequest);
        assertEquals(1, projectionInfo.getProjections().size());

        EntityProjection projection = projectionInfo.getProjections().values().iterator().next();

        // Verify Entity Argument
        assertEquals(1, projection.getArguments().size());

        Argument entityArgument = projection.getArguments().iterator().next();

        assertEquals("entityArgument", entityArgument.getName());
        assertEquals(String.class, entityArgument.getType());
        assertEquals("defaultArgValue", entityArgument.getValue());

        // Verify Attribute Argument
        assertEquals(1, projection.getAttributes().size());

        Attribute attribute = projection.getAttributes().iterator().next();

        assertEquals(1, attribute.getArguments().size());

        Argument argument = attribute.getArguments().iterator().next();

        assertEquals("argument", argument.getName());
        assertEquals(String.class, argument.getType());
        assertEquals("defaultValue", argument.getValue());
    }
}
