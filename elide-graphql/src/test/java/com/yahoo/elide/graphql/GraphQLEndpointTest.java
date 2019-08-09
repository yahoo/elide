/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.argument;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.arguments;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.document;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.entity;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.enumValue;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.field;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.objectValueWithVariable;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.selection;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.selections;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.typedOperation;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.variableDefinition;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.variableDefinitions;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.variableValue;
import static com.yahoo.elide.contrib.testhelpers.relayjsonapi.RelayJsonApiDSL.attribute;
import static com.yahoo.elide.contrib.testhelpers.relayjsonapi.RelayJsonApiDSL.datum;
import static com.yahoo.elide.contrib.testhelpers.relayjsonapi.RelayJsonApiDSL.edges;
import static com.yahoo.elide.contrib.testhelpers.relayjsonapi.RelayJsonApiDSL.node;
import static com.yahoo.elide.contrib.testhelpers.relayjsonapi.RelayJsonApiDSL.resource;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.audit.AuditLogger;

import com.yahoo.elide.contrib.testhelpers.graphql.elements.TypedOperation;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.datastore.inmemory.HashMapDataStore;
import com.yahoo.elide.resources.DefaultOpaqueUserFunction;
import com.yahoo.elide.security.checks.Check;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;

import org.json.JSONException;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import graphqlEndpointTestModels.Author;
import graphqlEndpointTestModels.Book;
import graphqlEndpointTestModels.DisallowShare;
import graphqlEndpointTestModels.security.CommitChecks;
import graphqlEndpointTestModels.security.UserChecks;

import java.io.IOException;
import java.security.Principal;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

/**
 * GraphQL endpoint tests tested against the in-memory store.
 */
public class GraphQLEndpointTest {

    private GraphQLEndpoint endpoint;
    private final SecurityContext user1 = Mockito.mock(SecurityContext.class);
    private final SecurityContext user2 = Mockito.mock(SecurityContext.class);
    private final SecurityContext user3 = Mockito.mock(SecurityContext.class);
    private final AuditLogger audit = Mockito.mock(AuditLogger.class);

    public static class User implements Principal {
        String log = "";
        String name;

        @Override
        public String getName() {
            return name;
        }

        public User withName(String name) {
            this.name = name;
            return this;
        }

        public void appendLog(String stmt) {
            log = log + stmt;
        }

        public String getLog() {
            return log;
        }

        @Override
        public String toString() {
            return getLog();
        }
    }

    @BeforeTest
    public void setup() {
        Mockito.when(user1.getUserPrincipal()).thenReturn(new User().withName("1"));
        Mockito.when(user2.getUserPrincipal()).thenReturn(new User().withName("2"));
        Mockito.when(user3.getUserPrincipal()).thenReturn(new User().withName("3"));
    }

    @BeforeMethod
    public void setupTest() throws Exception {
        HashMapDataStore inMemoryStore = new HashMapDataStore(Book.class.getPackage());
        Map<String, Class<? extends Check>> checkMappings = new HashMap<>();

        checkMappings.put(UserChecks.IS_USER_1, UserChecks.IsUserId.One.class);
        checkMappings.put(UserChecks.IS_USER_2, UserChecks.IsUserId.Two.class);
        checkMappings.put(CommitChecks.IS_NOT_USER_3, CommitChecks.IsNotUser3.class);

        Elide elide = new Elide(
                new ElideSettingsBuilder(inMemoryStore)
                        .withEntityDictionary(new EntityDictionary(checkMappings))
                        .withAuditLogger(audit)
                        .build());
        endpoint = new GraphQLEndpoint(elide, new DefaultOpaqueUserFunction() {
            @Override
            public Object apply(SecurityContext securityContext) {
                return securityContext.getUserPrincipal();
            }
        });

        DataStoreTransaction tx = inMemoryStore.beginTransaction();

        // Initial data
        Book book1 = new Book();
        Author author1 = new Author();
        Author author2 = new Author();
        DisallowShare noShare = new DisallowShare();

        book1.setId(1L);
        book1.setTitle("My first book");
        book1.setAuthors(Sets.newHashSet(author1));

        author1.setId(1L);
        author1.setName("Ricky Carmichael");
        author1.setBooks(Sets.newHashSet(book1));
        author1.setBookTitlesAndAwards(
                Stream.of(
                        new AbstractMap.SimpleImmutableEntry<>("Bookz", "Pulitzer Prize"),
                        new AbstractMap.SimpleImmutableEntry<>("Lost in the Data", "PEN/Faulkner Award")
                ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
        );

        author2.setId(2L);
        author2.setName("The Silent Author");
        author2.setBookTitlesAndAwards(
                Stream.of(
                        new AbstractMap.SimpleImmutableEntry<>("Working Hard or Hardly Working", "Booker Prize")
                ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
        );

        noShare.setId(1L);

        tx.createObject(book1, null);
        tx.createObject(author1, null);
        tx.createObject(author2, null);
        tx.createObject(noShare, null);

        tx.save(book1, null);
        tx.save(author1, null);
        tx.save(author2, null);
        tx.save(noShare, null);

        tx.commit(null);
    }

    @Test
    public void testValidFetch() throws JSONException {
        String graphQLRequest = document(
                selections(
                        entity(
                                "book",
                                selections(
                                        field("id"),
                                        field("title"),
                                        field(
                                                "authors",
                                                selection(
                                                        field("name")
                                                )
                                        )
                                )
                        )
                )
        ).toQuery();

        String graphQLResponse = datum(
                resource(
                        "book",
                        edges(
                                node(
                                        attribute("id", "1"),
                                        attribute("title", "My first book"),
                                        resource(
                                                "authors",
                                                edges(
                                                        node(
                                                                attribute("name", "Ricky Carmichael")

                                                        )
                                                )
                                        )
                                )
                        )
                )
        ).toJSON();

        Response response = endpoint.post(user1, graphQLRequestToJSON(graphQLRequest));
        assert200EqualBody(response, graphQLResponse);
    }

    @Test
    void testValidFetchWithVariables() throws JSONException {
        String graphQLRequest = document(
                typedOperation(
                        TypedOperation.OperationType.QUERY,
                        "myQuery",
                        variableDefinitions(
                                variableDefinition("bookId", "[String]")
                        ),
                        selections(
                                entity(
                                        "book",
                                        arguments(
                                                argument("ids", variableValue("bookId"))
                                        ),
                                        selections(
                                                field("id"),
                                                field("title"),
                                                field(
                                                        "authors",
                                                        selection(
                                                                field("name")
                                                        )
                                                )
                                        )
                                )
                        )
                )
        ).toQuery();
        String graphQLResponse = datum(
                resource(
                        "book",
                        edges(
                                node(
                                        attribute("id", "1"),
                                        attribute("title", "My first book"),
                                        resource(
                                                "authors",
                                                edges(
                                                        node(
                                                                attribute("name", "Ricky Carmichael")

                                                        )
                                                )
                                        )
                                )
                        )
                )
        ).toJSON();
        Map<String, String> variables = new HashMap<>();
        variables.put("bookId", "1");
        Response response = endpoint.post(user1, graphQLRequestToJSON(graphQLRequest, variables));
        assert200EqualBody(response, graphQLResponse);
    }

    @Test
    void testCanReadRestrictedFieldWithAppropriateAccess() throws JSONException {
        String graphQLRequest = document(
                selection(
                        entity(
                                "book",
                                selection(
                                        field("user1SecretField")
                                )
                        )
                )
        ).toQuery();

        String graphQLResponse = datum(
                resource(
                        "book",
                        edges(
                                node(
                                        attribute("user1SecretField", "this is a secret for user 1 only1")
                                )
                        )
                )
        ).toJSON();

        Response response = endpoint.post(user1, graphQLRequestToJSON(graphQLRequest));
        assert200EqualBody(response, graphQLResponse);
    }

    @Test
    void testCannotReadRestrictedField() throws IOException {
        String graphQLRequest = document(
                selection(
                        entity(
                                "book",
                                selection(
                                        field("user1SecretField")
                                )
                        )
                )
        ).toQuery();

        Response response = endpoint.post(user2, graphQLRequestToJSON(graphQLRequest));
        assertHasErrors(response);
    }


    @Test
    void testPartialResponse() throws IOException, JSONException {
        String graphQLRequest = document(
                selections(
                        entity(
                                "book",
                                selection(
                                        field("user1SecretField")
                                )
                        ),
                        entity(
                                "book",
                                selections(
                                        field("id"),
                                        field("title")
                                )
                        )
                )
        ).toQuery();

        String expectedData = datum(
                resource(
                        "book",
                        edges(
                                node(
                                        attribute("user1SecretField", null),
                                        attribute("id", "1"),
                                        attribute("title", "My first book")
                                )
                        )
                )
        ).toJSON();

        Response response = endpoint.post(user2, graphQLRequestToJSON(graphQLRequest));
        assertHasErrors(response);
        assert200DataEqual(response, expectedData);
    }

    @Test
    void testFailedMutationAndRead() throws IOException, JSONException {
        String graphQLRequest = document(
                selection(
                        entity(
                                "book",
                                arguments(
                                        argument("op", enumValue("UPSERT")),
                                        argument(
                                                "data",
                                                objectValueWithVariable("id: \"1\", title: \"my new book!\", " +
                                                        "authors:[{id:\"2\"}]")
                                        )
                                ),
                                selections(
                                        field("id"),
                                        field("title")
                                )
                        )
                )
        ).toQuery();

        Response response = endpoint.post(user2, graphQLRequestToJSON(graphQLRequest));
        assertHasErrors(response);

        graphQLRequest = document(
                selection(
                        entity(
                                "book",
                                selections(
                                        field("id"),
                                        field("title")
                                )
                        )
                )
        ).toQuery();

        String expected = datum(
                resource(
                        "book",
                        edges(
                                node(
                                        attribute("id", "1"),
                                        attribute("title", "My first book")
                                )
                        )
                )
        ).toJSON();

        response = endpoint.post(user2, graphQLRequestToJSON(graphQLRequest));
        assert200EqualBody(response, expected);
    }

    @Test
    void testNonShareable() throws IOException, JSONException {
        String graphQLRequest = document(
                typedOperation(
                        TypedOperation.OperationType.MUTATION,
                        selection(
                                entity(
                                        "book",
                                        selections(
                                                field("id"),
                                                field(
                                                        "authors",
                                                        arguments(
                                                                argument("op", enumValue("UPSERT")),
                                                                argument(
                                                                        "data",
                                                                        objectValueWithVariable("id: \"123\", " +
                                                                                "name: \"my new author\", \" +\n" +
                                                                                "                " +
                                                                                "\"noShare:{id:\"1\"}")
                                                                )
                                                        ),
                                                        selections(
                                                                field("id"),
                                                                field("name"),
                                                                field(
                                                                        "noShare",
                                                                        selection(
                                                                                field("id")
                                                                        )
                                                                )
                                                        )
                                                        )
                                        )
                                )
                        )
                )
        ).toQuery();

        Response response = endpoint.post(user1, graphQLRequestToJSON(graphQLRequest));

        assertHasErrors(response);

        graphQLRequest = document(
                selection(
                        entity(
                                "book",
                                selections(
                                        field("id"),
                                        field(
                                                "authors",
                                                selections(
                                                        field("id"),
                                                        field("name"),
                                                        field(
                                                                "noShare",
                                                                selection(
                                                                        field("id")
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        ).toQuery();

        String expected = datum(
                resource(
                        "book",
                        edges(
                                node(
                                        attribute("id", "1"),
                                        resource(
                                                "authors",
                                                edges(
                                                        node(
                                                                attribute("id", "1"),
                                                                attribute("name", "Ricky Carmichael"),
                                                                resource(
                                                                        "noShare",
                                                                        edges()
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        ).toJSON();

        response = endpoint.post(user1, graphQLRequestToJSON(graphQLRequest));
        assert200EqualBody(response, expected);
    }

    @Test
    void testLifeCycleHooks () throws Exception {
        /* Separate user 1 so it doesn't interfere */
        SecurityContext user = Mockito.mock(SecurityContext.class);
        User principal = new User().withName("1");
        Mockito.when(user.getUserPrincipal()).thenReturn(principal);

        String graphQLRequest = document(
                typedOperation(
                        TypedOperation.OperationType.MUTATION,
                        selection(
                                entity(
                                        "book",
                                        arguments(
                                                argument("op", enumValue("UPSERT")),
                                                argument(
                                                        "data",
                                                        objectValueWithVariable("id: \"1\", title: \"my new book!\""))
                                        ),
                                        selections(
                                                field("id"),
                                                field("title")
                                        )
                                )
                        )
                )
        ).toQuery();

        String expected = datum(
                resource(
                        "book",
                        edges(
                                node(
                                        attribute("id", "1"),
                                        attribute("title", "my new book!")
                                )
                        )
                )
        ).toJSON();

        Response response = endpoint.post(user, graphQLRequestToJSON(graphQLRequest));
        assert200EqualBody(response, expected);

        String expectedLog = "On Title Update Pre Security\nOn Title Update Pre Commit\nOn Title Update Post Commit\n";

        Assert.assertEquals(principal.getLog(), expectedLog);
    }

    @Test
    void testAuditLogging() throws IOException {
        Mockito.reset(audit);

        String graphQLRequest = document(
                typedOperation(
                        TypedOperation.OperationType.MUTATION,
                        selection(
                                entity(
                                        "book",
                                        arguments(
                                                argument("op", enumValue("UPSERT")),
                                                argument(
                                                        "data",
                                                        objectValueWithVariable("title: \"my new book!\"")
                                                )
                                        ),
                                        selections(
                                                field("id"),
                                                field("title")
                                        )
                                )
                        )
                )
        ).toQuery();

        endpoint.post(user1, graphQLRequestToJSON(graphQLRequest));

        Mockito.verify(audit, Mockito.times(1)).log(Mockito.any());
        Mockito.verify(audit, Mockito.times(1)).commit(Mockito.any());
        Mockito.verify(audit, Mockito.times(1)).clear();
    }

    @Test
    void testSuccessfulMutation() throws JSONException {
        String graphQLRequest = document(
                typedOperation(
                        TypedOperation.OperationType.MUTATION,
                        selection(
                                entity(
                                        "book",
                                        arguments(
                                                argument("op", enumValue("UPSERT")),
                                                argument(
                                                        "data",
                                                        objectValueWithVariable("id: \"123\", title: \"my new " +
                                                                "book!\", authors:[{id:\"2\"}]")
                                                )
                                        ),
                                        selections(
                                                field("id"),
                                                field("title"),
                                                field("user1SecretField")
                                        )
                                )
                        )
                )
        ).toQuery();

        String expected = datum(
                resource(
                        "book",
                        edges(
                                node(
                                        attribute("id", "2"),
                                        attribute("title", "my new book!"),
                                        attribute("user1SecretField", "this is a secret for user 1 only1")
                                )
                        )
                )
        ).toJSON();

        Response response = endpoint.post(user1, graphQLRequestToJSON(graphQLRequest));
        assert200EqualBody(response, expected);

        graphQLRequest = document(
                selection(
                        entity(
                                "book",
                                selections(
                                        field("id"),
                                        field("title"),
                                        field(
                                                "authors",
                                                selections(
                                                        field("id"),
                                                        field("name")
                                                )
                                        )
                                )
                        )
                )
        ).toQuery();

        expected = datum(
                resource(
                        "book",
                        edges(
                                node(
                                        attribute("id", "1"),
                                        attribute("title", "My first book"),
                                        resource(
                                                "authors",
                                                edges(
                                                        node(
                                                                attribute("id", "1"),
                                                                attribute("name", "Ricky Carmichael")
                                                        )
                                                )
                                        )
                                ),
                                node(
                                        attribute("id", "2"),
                                        attribute("title", "my new book!"),
                                        resource(
                                                "authors",
                                                edges(
                                                        node(
                                                                attribute("id", "2"),
                                                                attribute("name", "The Silent Author")
                                                        )
                                                )
                                        )
                                )
                        )
                )
        ).toJSON();

        response = endpoint.post(user1, graphQLRequestToJSON(graphQLRequest));
        assert200EqualBody(response, expected);
    }

    @Test
    void testFailedCommitCheck() throws IOException {
        // NOTE: User 3 cannot update books.
        String graphQLRequest = document(
                typedOperation(
                        TypedOperation.OperationType.MUTATION,
                        selection(
                                entity(
                                        "book",
                                        arguments(
                                                argument("op", enumValue("UPSERT")),
                                                argument(
                                                        "data",
                                                        objectValueWithVariable("id:\"1\", title:\"update title\""))
                                        ),
                                        selections(
                                                field("id"),
                                                field("title")
                                        )
                                )
                        )
                )
        ).toQuery();

        Response response = endpoint.post(user3, graphQLRequestToJSON(graphQLRequest));
        assertHasErrors(response);
    }

    @Test
    void testQueryAMap() throws JSONException {
        String graphQLRequest = document(
                typedOperation(
                        TypedOperation.OperationType.QUERY,
                        selection(
                                entity(
                                        "book",
                                        selections(
                                                field("id"),
                                                field(
                                                        "authors",
                                                        selection(
                                                                field("bookTitlesAndAwards {key value}")
                                                        )
                                                )
                                        )
                                )
                        )
                )
        ).toQuery();

        Map<String, String> first = new HashMap<>();
        first.put("key", "Lost in the Data");
        first.put("value", "PEN/Faulkner Award");

        Map<String, String> second = new HashMap<>();
        second.put("key", "Bookz");
        second.put("value", "Pulitzer Prize");

        String expected = datum(
                resource(
                        "book",
                        edges(
                                node(
                                        attribute("id", "1"),
                                        resource(
                                                "authors",
                                                edges(
                                                        node(
                                                                attribute(
                                                                        "bookTitlesAndAwards",
                                                                        Arrays.asList(first, second)
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        ).toJSON();

        Response response = endpoint.post(user1, graphQLRequestToJSON(graphQLRequest));
        assert200EqualBody(response, expected);
    }

    @Test
    void testQueryAMapWithBadFields() throws IOException {
        String graphQLRequest = document(
                typedOperation(
                        TypedOperation.OperationType.QUERY,
                        selection(
                                entity(
                                        "book",
                                        selections(
                                                field("id"),
                                                field(
                                                        "authors",
                                                        selection(
                                                                field("bookTitlesAndAwards {key value Bookz}")
                                                        )
                                                )
                                        )
                                )
                        )
                )
        ).toQuery();

        Response response = endpoint.post(user1, graphQLRequestToJSON(graphQLRequest));
        assertHasErrors(response);
    }

    private static String graphQLRequestToJSON(String request) {
        return graphQLRequestToJSON(request, new HashMap<>());
    }

    private static String graphQLRequestToJSON(String request, Map<String, String> variables) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = JsonNodeFactory.instance.objectNode();

        ((ObjectNode) node).put("query", request);
        ((ObjectNode) node).set("variables", variables == null ? null : mapper.valueToTree(variables));
        return node.toString();
    }

    private static JsonNode extract200Response(Response response) throws IOException {
        return new ObjectMapper().readTree(extract200ResponseString(response));
    }

    private static String extract200ResponseString(Response response) {
        Assert.assertEquals(response.getStatus(), 200);
        return (String) response.getEntity();
    }

    private static void assert200EqualBody(Response response, String expected) throws JSONException {
        String actual = extract200ResponseString(response);
        JSONAssert.assertEquals(expected, actual, true);
    }

    private static void assert200DataEqual(Response response, String expected) throws IOException, JSONException {
        JsonNode actualNode = extract200Response(response);

        Iterator<Map.Entry<String, JsonNode>> iterator = actualNode.fields();

        // get json node that has "data" key
        String actual = null;
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> next = iterator.next();
            if (next.getKey() == "data") {
                actual = new ObjectMapper().writeValueAsString(next);
            }
        }

        JSONAssert.assertEquals(expected, actual, true);
    }

    private static void assertHasErrors(Response response) throws IOException {
        JsonNode node = extract200Response(response);
        Assert.assertTrue(node.get("errors").elements().hasNext());
    }
}
