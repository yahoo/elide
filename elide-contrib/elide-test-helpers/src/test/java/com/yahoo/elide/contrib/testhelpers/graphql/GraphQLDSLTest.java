package com.yahoo.elide.contrib.testhelpers.graphql;

import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.argument;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.arguments;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.document;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.entity;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.field;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.selection;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.selections;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.typedOperation;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.stringValue;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.variableDefinition;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.variableDefinitions;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.variableValue;

import com.yahoo.elide.contrib.testhelpers.graphql.elements.TypedOperation;

import org.testng.Assert;
import org.testng.annotations.Test;

public class GraphQLDSLTest {

    @Test
    public void verifyBasicRequest() {
        String expected = "{book {edges {node {id title}}}}";
        String actual = document(
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

        Assert.assertEquals(actual, expected);
    }

    @Test
    public void verifyMultipleTopLevelEntitiesSelection() {
        String expected = "{book {edges {node {user1SecretField}}} book {edges {node {id title}}}}";
        String actual = document(
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

        Assert.assertEquals(actual, expected);
    }

    @Test
    public void verifyRequestWithRelationship() {
        String expected = "{book {edges {node {id title authors {edges {node {name}}}}}}}";
        String actual = document(
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

        Assert.assertEquals(actual, expected);
    }

    @Test
    public void verifyRequestWithSingleStringArgument() {
        String expected = "{book(sort: \"-id\") {edges {node {id title}}}}";
        String actual = document(
                selections(
                        entity(
                                "book",
                                argument(
                                        argument(
                                                "sort",
                                                stringValue("-id")
                                        )
                                ),
                                selections(
                                        field("id"),
                                        field("title")
                                )
                        )
                )
        ).toQuery();

        Assert.assertEquals(actual, expected);
    }

    @Test
    public void verifyRequestWithMultipleStringArguments() {
        String expected = "{book(sort: \"-id\" id: \"5\") {edges {node {id title}}}}";
        String actual = document(
                selections(
                        entity(
                                "book",
                                arguments(
                                        argument(
                                                "sort",
                                                stringValue("-id")
                                        ),
                                        argument(
                                                "id",
                                                stringValue("5")
                                        )
                                ),
                                selections(
                                        field("id"),
                                        field("title")
                                )
                        )
                )
        ).toQuery();

        Assert.assertEquals(actual, expected);
    }

    @Test
    public void verifyRequestWithVariable(){
        String expected = "query myQuery($bookId: [String]) {book(ids: $bookId) {edges {node {id title authors {edges" +
                " {node {name}}}}}}}";

        String actual = document(
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

        Assert.assertEquals(actual, expected);
    }
}
