/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.RequestScope;
import example.Author;
import example.Book;
import graphql.ExecutionResult;
import graphql.GraphQL;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.LinkedHashSet;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Test our data fetcher.
 */
public class PersistentResourceFetcherTest extends AbstractGraphQLTest {
    protected GraphQL api;
    protected RequestScope requestScope;

    @BeforeMethod
    public void setup() {
        PersistentResourceFetcher fetcher = new PersistentResourceFetcher();
        ModelBuilder builder = new ModelBuilder(dictionary, fetcher);
        api = new GraphQL(builder.build());
        DataStoreTransaction tx = Mockito.mock(DataStoreTransaction.class);

        initTestData(tx);

        requestScope = new RequestScope("/", null, tx, null, null,
                new ElideSettingsBuilder(null)
                        .withEntityDictionary(dictionary)
                        .build());
    }

    public void initTestData(DataStoreTransaction tx) {
        Author author1 = new Author();
        author1.setId(1L);
        author1.setName("The People's Author");
        author1.setType(Author.AuthorType.EXCLUSIVE);

        Book book1 = new Book();
        book1.setId(1L);
        book1.setTitle("Book Numero Uno");
        book1.setAuthors(Arrays.asList(author1));

        when(tx.loadObject(eq(Book.class), eq(1L), any(), any())).thenReturn(book1);
        when(tx.loadObject(eq(Author.class), eq(1L), any(), any())).thenReturn(author1);
        when(tx.loadObjects(eq(Book.class), any(), any(), any(), any()))
                .thenReturn(new LinkedHashSet<>(Arrays.asList(book1)));
        when(tx.loadObjects(eq(Author.class), any(), any(), any(), any()))
                .thenReturn(new LinkedHashSet<>(Arrays.asList(author1)));
    }

    @Test
    public void testFetchRootObject() {
        String graphQLRequest = "{ book(id: \"1\") { id title } }";
        ExecutionResult result = api.execute(graphQLRequest, new GraphQLContext(requestScope));
        Assert.assertEquals(result.getErrors().size(), 0);
        Assert.assertEquals(result.getData(), "{book=[{id=1, title=[Book Numero Uno]}]}");
    }

    @Test
    public void testFetchRootCollection() {
        String graphQLRequest = "{ book { id title genre language } }";
        ExecutionResult result = api.execute(graphQLRequest, new GraphQLContext(requestScope));
        Assert.assertEquals(result.getErrors().size(), 0);
        Assert.assertEquals(result.getData(), "");
    }

    @Test
    public void testFetchNestedCollection() {
        String graphQLRequest = "{ author { id name books { id title genre language } } }";
        ExecutionResult result = api.execute(graphQLRequest, new GraphQLContext(requestScope));
        Assert.assertEquals(result.getErrors().size(), 0);
        Assert.assertEquals(result.getData(), "");
    }
}
