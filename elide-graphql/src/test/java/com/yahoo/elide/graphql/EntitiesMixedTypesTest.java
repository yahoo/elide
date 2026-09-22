/*
 * Copyright 2026, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.graphql.parser.GraphQLEntityProjectionMaker;
import com.yahoo.elide.graphql.parser.GraphQLProjectionInfo;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Regression tests for mixed entity representations in Apollo Federation.
 */
public class EntitiesMixedTypesTest extends PersistentResourceFetcherTest {

    @Test
    public void mixedRepresentationsResolveEachType() throws Exception {
        String query = """
                query {
                  _entities(representations: [
                    {__typename: "Book", id: "1"},
                    {__typename: "Author", id: "1"}
                  ]) {
                    __typename
                  }
                }
                """;
        ElideResponse<String> response = runGraphQLRequest(query, new HashMap<>());
        assertEquals("{" + "\"data\":{\"_entities\":[{\"__typename\":\"Book\"},"
                + "{\"__typename\":\"Author\"}]}}", response.getBody());
    }

    @Test
    public void mixedRepresentationsResolveTypeSpecificFields() throws Exception {
        String query = """
                query {
                  _entities(representations: [
                    {__typename: "Book", id: "1"},
                    {__typename: "Author", id: "1"}
                  ]) {
                    __typename
                    ... on Book { title }
                    ... on Author { name }
                  }
                }
                """;
        ElideResponse<String> response = runGraphQLRequest(query, new HashMap<>());
        assertEquals("{" + "\"data\":{\"_entities\":[{\"__typename\":\"Book\","
                + "\"title\":\"Libro Uno\"},{\"__typename\":\"Author\","
                + "\"name\":\"Mark Twain\"}]}}", response.getBody());
    }

    @Test
    public void singleTypeRepresentationsPreserveOrder() throws Exception {
        String query = """
                query {
                  _entities(representations: [
                    {__typename: "Book", id: "1"},
                    {__typename: "Book", id: "2"}
                  ]) {
                    ... on Book { title }
                  }
                }
                """;
        ElideResponse<String> response = runGraphQLRequest(query, new HashMap<>());
        assertEquals("{" + "\"data\":{\"_entities\":[{\"title\":\"Libro Uno\"},"
                + "{\"title\":\"Libro Dos\"}]}}", response.getBody());
    }

    @Test
    public void mixedRepresentationsDoNotReplayFirstType() throws Exception {
        String query = """
                query {
                  _entities(representations: [
                    {__typename: "Book", id: "1"},
                    {__typename: "Author", id: "1"}
                  ]) {
                    __typename
                    ... on Book { id }
                  }
                }
                """;
        ElideResponse<String> response = runGraphQLRequest(query, new HashMap<>());
        assertEquals("{" + "\"data\":{\"_entities\":[{\"__typename\":\"Book\","
                + "\"id\":\"1\"},{\"__typename\":\"Author\"}]}}", response.getBody());
    }

    @Test
    public void mixedRepresentationsHaveBothTypeProjections() {
        String query = """
                query {
                  _entities(representations: [
                    {__typename: "Book", id: "1"},
                    {__typename: "Author", id: "1"}
                  ]) {
                    ... on Book { title }
                    ... on Author { name }
                  }
                }
                """;
        GraphQLProjectionInfo projections = new GraphQLEntityProjectionMaker(settings).make(query);
        assertNotNull(projections.getProjection(null, "book"));
        assertNotNull(projections.getProjection(null, "author"));
    }

    @Test
    public void variableRepresentationsResolveEachType() throws Exception {
        String query = """
                query($representations: [_Any!]!) {
                  _entities(representations: $representations) {
                    __typename
                    ... on Book { title }
                    ... on Author { name }
                  }
                }
                """;
        Map<String, Object> variables = Map.of("representations", List.of(
                Map.of("__typename", "Book", "id", "1"),
                Map.of("__typename", "Author", "id", "1")));
        ElideResponse<String> response = runGraphQLRequest(query, variables);
        assertEquals("{" + "\"data\":{\"_entities\":[{\"__typename\":\"Book\","
                + "\"title\":\"Libro Uno\"},{\"__typename\":\"Author\","
                + "\"name\":\"Mark Twain\"}]}}", response.getBody());
    }
}
