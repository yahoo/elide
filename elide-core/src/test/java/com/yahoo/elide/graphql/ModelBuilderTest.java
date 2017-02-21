package com.yahoo.elide.graphql;

import com.yahoo.elide.core.EntityDictionary;
import example.Author;
import example.Book;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import org.testng.Assert;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.mock;

public class ModelBuilderTest {
    EntityDictionary dictionary;

    @BeforeSuite
    public void init() {
        dictionary = new EntityDictionary(Collections.EMPTY_MAP);

        dictionary.bindEntity(Book.class);
        dictionary.bindEntity(Author.class);
    }


    @Test
    public void testBuild() {

        DataFetcher fetcher = mock(DataFetcher.class);
        ModelBuilder builder = new ModelBuilder(dictionary, fetcher);

        GraphQLSchema schema = builder.build();

        List<GraphQLType> types = schema.getAllTypesAsList();

        Assert.assertNotEquals(schema.getType("author"), null);
        Assert.assertNotEquals(schema.getType("book"), null);
        Assert.assertNotEquals(schema.getType("root"), null);
    }
}
