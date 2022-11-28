/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

import static com.yahoo.elide.test.graphql.GraphQLDSL.UNQUOTED_VALUE;
import static com.yahoo.elide.test.graphql.GraphQLDSL.argument;
import static com.yahoo.elide.test.graphql.GraphQLDSL.arguments;
import static com.yahoo.elide.test.graphql.GraphQLDSL.document;
import static com.yahoo.elide.test.graphql.GraphQLDSL.field;
import static com.yahoo.elide.test.graphql.GraphQLDSL.mutation;
import static com.yahoo.elide.test.graphql.GraphQLDSL.query;
import static com.yahoo.elide.test.graphql.GraphQLDSL.selection;
import static com.yahoo.elide.test.graphql.GraphQLDSL.selections;
import static com.yahoo.elide.test.graphql.GraphQLDSL.toJson;
import static com.yahoo.elide.test.graphql.GraphQLDSL.variableDefinition;
import static com.yahoo.elide.test.graphql.GraphQLDSL.variableDefinitions;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.core.audit.AuditLogger;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.datastore.inmemory.HashMapDataStore;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.security.checks.Check;
import com.yahoo.elide.core.utils.DefaultClassScanner;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;

import graphqlEndpointTestModels.Author;
import graphqlEndpointTestModels.Book;
import graphqlEndpointTestModels.DisallowTransfer;
import graphqlEndpointTestModels.Incident;
import graphqlEndpointTestModels.security.CommitChecks;
import graphqlEndpointTestModels.security.UserChecks;

import java.io.IOException;
import java.net.URI;
import java.security.Principal;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

/**
 * GraphQL endpoint tests tested against the in-memory store.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GraphQLEndpointTest {

    private GraphQLEndpoint endpoint;
    private final SecurityContext user1 = Mockito.mock(SecurityContext.class);
    private final SecurityContext user2 = Mockito.mock(SecurityContext.class);
    private final SecurityContext user3 = Mockito.mock(SecurityContext.class);
    private final AuditLogger audit = Mockito.mock(AuditLogger.class);
    private final UriInfo uriInfo = Mockito.mock(UriInfo.class);
    private final HttpHeaders requestHeaders = Mockito.mock(HttpHeaders.class);

    private Elide elide;

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

    @BeforeAll
    public void setup() {
        Mockito.when(user1.getUserPrincipal()).thenReturn(new User().withName("1"));
        Mockito.when(user2.getUserPrincipal()).thenReturn(new User().withName("2"));
        Mockito.when(user3.getUserPrincipal()).thenReturn(new User().withName("3"));
        Mockito.when(uriInfo.getBaseUri()).thenReturn(URI.create("http://localhost:8080/graphql"));
        Mockito.when(requestHeaders.getRequestHeaders()).thenReturn(new MultivaluedHashMap<>());
    }

    @BeforeEach
    public void setupTest() throws Exception {
        HashMapDataStore inMemoryStore = new HashMapDataStore(DefaultClassScanner.getInstance(),
                Book.class.getPackage());
        Map<String, Class<? extends Check>> checkMappings = new HashMap<>();

        checkMappings.put(UserChecks.IS_USER_1, UserChecks.IsUserId.One.class);
        checkMappings.put(UserChecks.IS_USER_2, UserChecks.IsUserId.Two.class);
        checkMappings.put(CommitChecks.IS_NOT_USER_3, CommitChecks.IsNotUser3.class);

        elide = spy(
                new Elide(
                    new ElideSettingsBuilder(inMemoryStore)
                            .withEntityDictionary(EntityDictionary.builder().checks(checkMappings).build())
                            .withAuditLogger(audit)
                            .build())

                );

        elide.doScans();
        endpoint = new GraphQLEndpoint(elide);

        DataStoreTransaction tx = inMemoryStore.beginTransaction();

        // Initial data
        Book book1 = new Book();
        Author author1 = new Author();
        Author author2 = new Author();
        DisallowTransfer noShare = new DisallowTransfer();

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
                        field(
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

        String graphQLResponse = document(
                selection(
                        field(
                                "book",
                                selections(
                                        field("id", "1"),
                                        field("title", "My first book"),
                                        field(
                                                "authors",
                                                selection(
                                                        field("name", "Ricky Carmichael")
                                                )
                                        )
                                )
                        )
                )
        ).toResponse();

        Response response = endpoint.post(uriInfo, requestHeaders, user1, graphQLRequestToJSON(graphQLRequest));
        assert200EqualBody(response, graphQLResponse);
    }

    @Test
    void testValidFetchWithVariables() throws JSONException {
        String graphQLRequest = document(
                query(
                        "myQuery",
                        variableDefinitions(
                                variableDefinition("bookId", "[String]")
                        ),
                        selections(
                                field(
                                        "book",
                                        arguments(
                                                argument("ids", "$bookId")
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

        String graphQLResponse = document(
                selection(
                        field(
                                "book",
                                selections(
                                        field("id", "1"),
                                        field("title", "My first book"),
                                        field(
                                                "authors",
                                                selection(
                                                        field("name", "Ricky Carmichael")
                                                )
                                        )
                                )
                        )
                )
        ).toResponse();

        Map<String, String> variables = new HashMap<>();
        variables.put("bookId", "1");
        Response response = endpoint.post(uriInfo, requestHeaders, user1, graphQLRequestToJSON(graphQLRequest, variables));
        assert200EqualBody(response, graphQLResponse);
    }

    @Test
    void testCanReadRestrictedFieldWithAppropriateAccess() throws JSONException {
        String graphQLRequest = document(
                selection(
                        field(
                                "book",
                                selection(
                                        field("user1SecretField")
                                )
                        )
                )
        ).toQuery();

        String graphQLResponse = document(
                selection(
                        field(
                                "book",
                                selection(
                                        field("user1SecretField", "this is a secret for user 1 only1")
                                )
                        )
                )
        ).toResponse();

        Response response = endpoint.post(uriInfo, requestHeaders, user1, graphQLRequestToJSON(graphQLRequest));
        assert200EqualBody(response, graphQLResponse);
    }

    @Test
    void testCannotReadRestrictedField() throws JSONException {
        String graphQLRequest = document(
                selection(
                        field(
                                "book",
                                selection(
                                        field("user1SecretField")
                                )
                        )
                )
        ).toQuery();

        //Empty response because the collection is skipped because of failed user check.
        String expected = "{\"data\":{\"book\":{\"edges\":[]}}}";

        Response response = endpoint.post(uriInfo, requestHeaders, user2, graphQLRequestToJSON(graphQLRequest));
        assert200EqualBody(response, expected);
    }


    @Test
    void testPartialResponse() throws IOException, JSONException {
        String graphQLRequest = document(
                selections(
                        field(
                                "book",
                                selection(
                                        field("user1SecretField")
                                )
                        ),
                        field(
                                "book",
                                selections(
                                        field("id"),
                                        field("title")
                                )
                        )
                )
        ).toQuery();

        Response response = endpoint.post(uriInfo, requestHeaders, user2, graphQLRequestToJSON(graphQLRequest));
        assertHasErrors(response);
        verify(elide).mapError(any());
    }

    @Test
    void testCrypticErrorOnUpsert() throws IOException, JSONException {
        Incident incident = new Incident();

        String graphQLRequest = document(
           mutation(
               selection(
                   field(
                       "incidents",
                       arguments(
                           argument("op", "UPSERT"),
                           argument("data", incident)
                       ),
                       selections(
                           field("id"),
                           field("name")
                       )
                   )
               )
           )
        ).toQuery();

        Response response = endpoint.post(uriInfo, requestHeaders, user2, graphQLRequestToJSON(graphQLRequest));
        JsonNode node = extract200Response(response);
        Iterator<JsonNode> errors = node.get("errors").elements();
        assertTrue(errors.hasNext());
        assertTrue(errors.next().get("message").asText().contains("No id provided, cannot persist incidents"));
        verify(elide).mapError(any());
    }

    @Test
    void testFailedMutationAndRead() throws IOException, JSONException {
        Author author = new Author();
        author.setId(2L);

        Book book = new Book();
        book.setId(1);
        book.setTitle("my new book!");
        book.setAuthors(Sets.newHashSet(author));

        String graphQLRequest = document(
                selection(
                        field(
                                "book",
                                arguments(
                                        argument("op", "UPSERT"),
                                        argument("data", book)
                                ),
                                selections(
                                        field("id"),
                                        field("title")
                                )
                        )
                )
        ).toQuery();

        Response response = endpoint.post(uriInfo, requestHeaders, user2, graphQLRequestToJSON(graphQLRequest));
        assertHasErrors(response);

        graphQLRequest = document(
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

        String expected = document(
                selection(
                        field(
                                "book",
                               selections(
                                       field("id", "1"),
                                       field("title", "My first book")
                               )
                        )
                )
        ).toResponse();

        response = endpoint.post(uriInfo, requestHeaders, user2, graphQLRequestToJSON(graphQLRequest));
        assert200EqualBody(response, expected);
    }

    @Test
    void testNonShareable() throws IOException, JSONException {
        DisallowTransfer noShare = new DisallowTransfer();
        noShare.setId(1L);

        Author author = new Author();
        author.setId(123L);
        author.setName("my new author");
        author.setNoShare(noShare);

        String graphQLRequest = document(
                mutation(
                        selection(
                                field(
                                        "book",
                                        selections(
                                                field("id"),
                                                field(
                                                        "authors",
                                                        arguments(
                                                                argument("op", "UPSERT"),
                                                                argument("data", author)
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

        Response response = endpoint.post(uriInfo, requestHeaders, user1, graphQLRequestToJSON(graphQLRequest));

        assertHasErrors(response);

        graphQLRequest = document(
                selection(
                        field(
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

        String expected = document(
                selection(
                        field(
                                "book",
                                selections(
                                        field("id", "1"),
                                        field(
                                                "authors",
                                                selections(
                                                        field("id", "1"),
                                                        field("name", "Ricky Carmichael"),
                                                        field("noShare", "", false)
                                                )
                                        )
                                )
                        )
                )
        ).toResponse();

        response = endpoint.post(uriInfo, requestHeaders, user1, graphQLRequestToJSON(graphQLRequest));
        assert200EqualBody(response, expected);
    }

    @Test
    void testLifeCycleHooks () throws Exception {
        /* Separate user 1 so it doesn't interfere */
        SecurityContext user = Mockito.mock(SecurityContext.class);
        User principal = new User().withName("1");
        Mockito.when(user.getUserPrincipal()).thenReturn(principal);

        Book book = new Book();
        book.setId(1);
        book.setTitle("my new book!");

        String graphQLRequest = document(
                mutation(
                        selection(
                                field(
                                        "book",
                                        arguments(
                                                argument("op", "UPSERT"),
                                                argument("data", book)
                                        ),
                                        selections(
                                                field("id"),
                                                field("title")
                                        )
                                )
                        )
                )
        ).toQuery();

        String expected = document(
                selection(
                        field(
                                "book",
                                selections(
                                        field("id", "1"),
                                        field("title", "my new book!")
                                )
                        )
                )
        ).toResponse();

        Response response = endpoint.post(uriInfo, requestHeaders, user, graphQLRequestToJSON(graphQLRequest));
        assert200EqualBody(response, expected);

        String expectedLog = "On Title Update Pre Security\nOn Title Update Pre Commit\nOn Title Update Post Commit\n";

        assertEquals(principal.getLog(), expectedLog);
    }

    @Test
    void testAuditLogging() throws IOException {
        Mockito.reset(audit);

        Book book = new Book();
        book.setTitle("my new book!");

        String graphQLRequest = document(
                mutation(
                        selection(
                                field(
                                        "book",
                                        arguments(
                                                argument("op", "UPSERT"),
                                                argument("data", book)
                                        ),
                                        selections(
                                                field("id"),
                                                field("title")
                                        )
                                )
                        )
                )
        ).toQuery();

        endpoint.post(uriInfo, requestHeaders, user1, graphQLRequestToJSON(graphQLRequest));

        verify(audit, Mockito.times(1)).log(Mockito.any());
        verify(audit, Mockito.times(1)).commit();
        verify(audit, Mockito.times(1)).clear();
    }

    @Test
    void testSuccessfulMutation() throws JSONException {
        Author author = new Author();
        author.setId(2L);

        Book book = new Book();
        book.setId(123);
        book.setTitle("my new book!");
        book.setAuthors(Sets.newHashSet(author));

        String graphQLRequest = document(
                mutation(
                        selection(
                                field(
                                        "book",
                                        arguments(
                                                argument("op", "UPSERT"),
                                                argument("data", book)
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

        String expected = document(
                selection(
                        field(
                                "book",
                                selections(
                                        field("id", "2"),
                                        field("title", "my new book!"),
                                        field("user1SecretField", "this is a secret for user 1 only1")
                                )
                        )
                )
        ).toResponse();

        Response response = endpoint.post(uriInfo, requestHeaders, user1, graphQLRequestToJSON(graphQLRequest));
        assert200EqualBody(response, expected);

        graphQLRequest = document(
                selection(
                        field(
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

        expected = document(
                selection(
                        field(
                                "book",
                                selections(
                                        field("id", "1"),
                                        field("title", "My first book"),
                                        field(
                                                "authors",
                                                selections(
                                                        field("id", "1"),
                                                        field("name", "Ricky Carmichael")
                                                )
                                        )
                                ),
                                selections(
                                        field("id", "2"),
                                        field("title", "my new book!"),
                                        field(
                                                "authors",
                                                selections(
                                                        field("id", "2"),
                                                        field("name", "The Silent Author")
                                                )
                                        )
                                )

                        )
                )
        ).toResponse();

        response = endpoint.post(uriInfo, requestHeaders, user1, graphQLRequestToJSON(graphQLRequest));
        assert200EqualBody(response, expected);
    }

    @Test
    void testFailedCommitCheck() throws IOException {
        Book book = new Book();
        book.setId(1);
        book.setTitle("update title");

        // NOTE: User 3 cannot update books.
        String graphQLRequest = document(
                mutation(
                        selection(
                                field(
                                        "book",
                                        arguments(
                                                argument("op", "UPSERT"),
                                                argument("data", book)
                                        ),
                                        selections(
                                                field("id"),
                                                field("title")
                                        )
                                )
                        )
                )
        ).toQuery();

        Response response = endpoint.post(uriInfo, requestHeaders, user3, graphQLRequestToJSON(graphQLRequest));
        assertHasErrors(response);
        verify(elide).mapError(any());
    }

    @Test
    void testQueryAMap() throws JSONException {
        String graphQLRequest = document(
                mutation(
                        selection(
                                field(
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

        String expected = document(
                selection(
                        field(
                                "book",
                                selections(
                                        field("id", "1"),
                                        field(
                                                "authors",
                                                selection(
                                                        field(
                                                                "bookTitlesAndAwards",
                                                                toJson(Arrays.asList(first, second)),
                                                                UNQUOTED_VALUE
                                                        )
                                                )
                                        )
                                )
                        )
                )
        ).toResponse();

        Response response = endpoint.post(uriInfo, requestHeaders, user1, graphQLRequestToJSON(graphQLRequest));
        assert200EqualBody(response, expected);
    }

    @Test
    void testQueryAMapWithBadFields() throws IOException {
        String graphQLRequest = document(
                mutation(
                        selection(
                                field(
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

        Response response = endpoint.post(uriInfo, requestHeaders, user1, graphQLRequestToJSON(graphQLRequest));
        assertHasErrors(response);
        verify(elide).mapError(any());
    }


    @Test
    public void testMultipleRoot() throws JSONException {
        String graphQLRequest = document(
                selections(
                        field(
                                "author",
                                selections(
                                        field("id"),
                                        field("name"),
                                        field(
                                                "books",
                                                selection(
                                                        field("title")
                                                )
                                        )
                                )
                        ),
                        field(
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

        String graphQLResponse = document(
                selections(
                        field(
                                "author",
                                selections(
                                        field("id", "1"),
                                        field("name", "Ricky Carmichael"),
                                        field(
                                                "books",
                                                selections(
                                                        field("title", "My first book")
                                                )
                                        )
                                ),
                                selections(
                                        field("id", "2"),
                                        field("name", "The Silent Author"),
                                        field(
                                                "books", "", false
                                        )
                                )
                        ),
                        field(
                                "book",
                                selections(
                                        field("id", "1"),
                                        field("title", "My first book"),
                                        field(
                                                "authors",
                                                selection(
                                                        field("name", "Ricky Carmichael")
                                                )
                                        )
                                )
                        )
                )
        ).toResponse();


        Response response = endpoint.post(uriInfo, requestHeaders, user1, graphQLRequestToJSON(graphQLRequest));
        assert200EqualBody(response, graphQLResponse);
    }

    @Test
    public void testMultipleQueryWithAlias() throws JSONException {
        String graphQLRequest = document(
                selections(
                        field(
                                "AuthorBook",
                                "author",
                                selections(
                                        field("id"),
                                        field(
                                                "books",
                                                selection(
                                                        field("title")
                                                )
                                        )
                                )
                        ),
                        field(
                                "AuthorName",
                                "author",
                                selections(
                                        field("id"),
                                        field("name")
                                )
                        )
                )
        ).toQuery();
        String graphQLResponse = document(
                selections(
                        field(
                                "AuthorBook",
                                selections(
                                        field("id", "1"),
                                        field(
                                                "books",
                                                selections(
                                                        field("title", "My first book")
                                                )
                                        )
                                ),
                                selections(
                                        field("id", "2"),
                                        field(
                                                "books", "", false
                                        )
                                )
                        ),
                        field(
                                "AuthorName",
                                selections(
                                        field("id", "1"),
                                        field("name", "Ricky Carmichael")
                                ),
                                selections(
                                        field("id", "2"),
                                        field("name", "The Silent Author")
                                )
                        )
                )
        ).toResponse();


        Response response = endpoint.post(uriInfo, requestHeaders, user1, graphQLRequestToJSON(graphQLRequest));
        assert200EqualBody(response, graphQLResponse);
    }

    @Test
    public void testMultipleQueryWithAliasAndArguments() throws JSONException {
        String graphQLRequest = document(
                query(
                        "myQuery",
                        variableDefinitions(
                                variableDefinition("author1", "[String]"),
                                variableDefinition("author2", "[String]")
                        ),
                        selections(
                                field(
                                        "Author_1",
                                        "author",
                                        arguments(
                                                argument("ids", "$author1")
                                        ),
                                        selections(
                                                field("id"),
                                                field("name"),
                                                field(
                                                        "books",
                                                        selection(
                                                                field("title")
                                                        )
                                                )
                                        )
                                ),
                                field(
                                        "Author_2",
                                        "author",
                                        arguments(
                                                argument("ids", "$author2")
                                        ),
                                        selections(
                                                field("id"),
                                                field("name"),
                                                field(
                                                        "books",
                                                        selection(
                                                                field("title")
                                                        )
                                                )
                                        )
                                )
                        )
                )
        ).toQuery();
        String graphQLResponse = document(
                selections(
                        field(
                                "Author_1",
                                selections(
                                        field("id", "1"),
                                        field("name", "Ricky Carmichael"),
                                        field(
                                                "books",
                                                selections(
                                                        field("title", "My first book")
                                                )
                                        )
                                )
                        ),
                        field(
                                "Author_2",
                                selections(
                                        field("id", "2"),
                                        field("name", "The Silent Author"),
                                        field(
                                                "books", "", false
                                        )
                                )
                        )
                )
        ).toResponse();


        Map<String, String> variables = new HashMap<>();
        variables.put("author1", "1");
        variables.put("author2", "2");

        Response response = endpoint.post(uriInfo, requestHeaders, user1, graphQLRequestToJSON(graphQLRequest, variables));
        assert200EqualBody(response, graphQLResponse);
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
        assertEquals(200, response.getStatus());
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
        assertTrue(node.get("errors").elements().hasNext());
    }
}
