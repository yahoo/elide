/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
         assertEquals(projectionInfo.getProjections().size(), 1);
         assertEquals(projectionInfo.getProjections().entrySet().iterator().next().getValue().getPagination().isDefaultInstance(), true);
     }

     @Test
     public void testLimitPagination() throws IOException {
         String graphQLRequest = loadGraphQLRequest("fetch/rootCollectionPaginateWithOffset" + ".graphql");
         GraphQLEntityProjectionMaker projectionMaker = new GraphQLEntityProjectionMaker(settings);
         GraphQLProjectionInfo projectionInfo = projectionMaker.make(graphQLRequest);
         assertEquals(projectionInfo.getProjections().size(), 1);
         Pagination page = projectionInfo.getProjections().entrySet().iterator().next().getValue().getPagination();
         assertEquals(page.isDefaultInstance(), false);
         assertEquals(page.getLimit(), 1);
     }
}
