/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql;

import com.fasterxml.jackson.databind.JsonNode;
import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import example.Child;
import example.Parent;
import graphql.ExecutionResult;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import javax.ws.rs.core.SecurityContext;

import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test the Fetch operation.
 */
@Slf4j
public class GraphQLTest extends AbstractPersistentResourceFetcherTest {

    private GraphQLEndpoint endpoint;
    private SecurityContext securityContext;

    @BeforeTest
    public void setup2() {
        final EntityDictionary dictionary = new EntityDictionary(new HashMap<>());
        dictionary.bindEntity(Child.class);
        dictionary.bindEntity(Parent.class);

        DataStore datastore = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        when(datastore.beginTransaction()).thenReturn(tx);
        Elide elide = new Elide(new ElideSettingsBuilder(datastore)
                .withEntityDictionary(dictionary)
                .build());
        endpoint = new GraphQLEndpoint(elide, (
                sc) -> {
            log.error("SC {}", sc);
            return sc.getUserPrincipal();
        });

        securityContext = mock(SecurityContext.class);
        Principal principal = mock(Principal.class);
        when(securityContext.getUserPrincipal()).thenReturn(principal);
    }

    protected String rqst(String query) {
        return rqst("fetch", query, Collections.emptyMap());
    }

    protected String rqst(String operationName, String query, Map<String, Object> variables) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>() {{
            put("operationName", operationName);
            put("query", query);
            put("variables", variables);
        }};
        String s = mapper.convertValue(map, JsonNode.class).toString();
        System.err.println(s);
        return s;
    }

    @Test
    public void testRootSingle() {
        String graphQLRequest = "{ book(id: \"1\") { id title } }";
        String expectedResponse = "{\"book\":[{\"id\":\"1\",\"title\":\"Libro Uno\"}]}";

        String body = endpoint.post(securityContext, rqst(graphQLRequest)).getEntity().toString();

        Assert.assertNotNull(body);
        Assert.assertEquals(body, expectedResponse, "." + body + ".");
    }
    @Test
    public void testRootCollection() {
        String graphQLRequest = "{ book { id title genre language } }";
        String expectedResponse =
                "{\"book\":[{\"id\":\"1\",\"title\":\"Libro Uno\",\"genre\":null,\"language\":null},{\"id\":\"2\",\"title\":\"Libro Dos\",\"genre\":null,\"language\":null}]}";

        SecurityContext securityContext = mock(SecurityContext.class);
        String body = endpoint.post(securityContext, rqst(graphQLRequest)).getEntity().toString();

        assertQueryEquals(body, expectedResponse);
    }

    @Test
    public void testRootCollectionFilter() {
        String graphQLRequest = "{ book(filter: \"title=\\\"Book*\\\"\") { id title } }";
        String expectedResponse = "{\"book\":[{\"id\":\"1\",\"title\":\"Libro Uno\"}]}";

        SecurityContext securityContext = mock(SecurityContext.class);
        String body = endpoint.post(securityContext, rqst(graphQLRequest)).getEntity().toString();

        assertQueryEquals(body, expectedResponse);
    }

    @Test
    public void testRootCollectionSort() {
        String graphQLRequest = "{ book(sort: \"-title\") { id title } }";
        String expectedResponse = "{\"book\":[{\"id\":\"1\",\"title\":\"Libro Uno\"}]}";

        SecurityContext securityContext = mock(SecurityContext.class);
        String body = endpoint.post(securityContext, rqst(graphQLRequest)).getEntity().toString();

        assertQueryEquals(body, expectedResponse);
    }

    @Test
    public void testRootCollectionPaginate() {
        String graphQLRequest = "{ book(first: 1) { id title } }";
        String expectedResponse = "{\"book\":[{\"id\":\"1\",\"title\":\"Libro Uno\"}]}";


        graphQLRequest = "{ book(first: 1, offset: 1) { id title } }";
        expectedResponse = "{\"book\":[{\"id\":\"1\",\"title\":\"Libro Dos\"}]}";

        SecurityContext securityContext = mock(SecurityContext.class);
        String body = endpoint.post(securityContext, rqst(graphQLRequest)).getEntity().toString();

        assertQueryEquals(body, expectedResponse);
    }

    @Test
    public void testNestedSingle() {
        String graphQLRequest = "{ author(id: \"1\") { books(id: \"1\") { id title } } }";
        String expectedResponse = "{\"author\":[{\"books\":[{\"id\":\"1\",\"title\":\"Libro Uno\"}]}]}";

        SecurityContext securityContext = mock(SecurityContext.class);
        String body = endpoint.post(securityContext, rqst(graphQLRequest)).getEntity().toString();

        assertQueryEquals(body, expectedResponse);
    }

    @Test
    public void testNestedCollection() {
        String graphQLRequest = "{ author(id: \"1\") { books { id title } } }";
        String expectedResponse =
                "{\"author\":[{\"books\":[{\"id\":\"1\",\"title\":\"Libro Uno\"},{\"id\":\"2\",\"title\":\"Libro Dos\"}]}]}";

        SecurityContext securityContext = mock(SecurityContext.class);
        String body = endpoint.post(securityContext, rqst(graphQLRequest)).getEntity().toString();

        assertQueryEquals(body, expectedResponse);
    }

    @Test
    public void testNestedCollectionFilter() {
        String graphQLRequest = "{ author(id: \"1\") { books { id title } } }";
        String expectedResponse = "{\"author\":[{\"books\":[{\"id\":\"1\",\"title\":\"Libro Uno\"}]}]}";

        Assert.fail("Not Implemented");
    }

    @Test
    public void testNestedCollectionSort() {
        String graphQLRequest = "{ author(id: \"1\") { books { id title } } }";
        String expectedResponse = "{\"author\":[{\"books\":[{\"id\":\"1\",\"title\":\"Libro Uno\"}]}]}";

        Assert.fail("Not Implemented");
    }

    @Test
    public void testNestedCollectionPaginate() {
        String graphQLRequest = "{ author(id: \"1\") { books { id title } } }";
        String expectedResponse = "{\"author\":[{\"books\":[{\"id\":\"1\",\"title\":\"Libro Uno\"}]}]}";

        Assert.fail("Not Implemented");
    }

    @Test
    public void testFailuresWithBody() {
        String graphQLRequest = "{ book(id: \"1\", data: [{\"id\": \"1\"}]) { id title } }";
        ExecutionResult result = api.execute(rqst(graphQLRequest), requestScope);
        System.err.println(result.getErrors());
        Assert.assertTrue(!result.getErrors().isEmpty());
    }
}
