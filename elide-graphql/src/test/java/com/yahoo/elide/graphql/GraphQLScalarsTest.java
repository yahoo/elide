/*
 * Copyright 2018, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

import static org.testng.Assert.assertEquals;

import com.yahoo.elide.utils.coerce.CoerceUtil;
import com.yahoo.elide.utils.coerce.converters.ISO8601DateSerde;
import com.yahoo.elide.utils.coerce.converters.Serde;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import graphql.language.IntValue;
import graphql.language.StringValue;

import java.math.BigInteger;
import java.util.Date;
import java.util.TimeZone;

public class GraphQLScalarsTest {

    private Serde oldSerde;

    @BeforeClass
    public void init() {

        oldSerde = CoerceUtil.lookup(Date.class);

        CoerceUtil.register(Date.class, new ISO8601DateSerde(
                "yyyy-MM-dd'T'HH:mm'Z'",
                TimeZone.getTimeZone("UTC"),
                java.sql.Date.class));
    }

    @AfterClass
    public void cleanup() {
        CoerceUtil.register(Date.class, oldSerde);
    }

    @Test
    public void testGraphQLDateParseValue() {
        Date date = new Date(0L);
        assertEquals(GraphQLScalars.GRAPHQL_DATE_TYPE.getCoercing().parseValue("1970-01-01T00:00Z"), date);
    }

    @Test
    public void testGraphQLDateParseLiteral() {
        Date expectedDate = new Date(0L);
        Object actualDate = GraphQLScalars.GRAPHQL_DATE_TYPE.getCoercing().parseLiteral(new StringValue("1970-01-01T00:00Z"));
        assertEquals(actualDate, expectedDate);
    }

    @Test
    public void testGraphQLDateSerialize() {
        Date date = new Date(0L);
        Object actual = GraphQLScalars.GRAPHQL_DATE_TYPE.getCoercing().serialize(date);
        String expected = "1970-01-01T00:00Z";
        assertEquals(actual, expected);
    }

    @Test
    public void testGraphQLDeferredIdSerialize() {
        assertEquals(GraphQLScalars.GRAPHQL_DEFERRED_ID.getCoercing().serialize(123L), 123L);
        assertEquals(GraphQLScalars.GRAPHQL_DEFERRED_ID.getCoercing().serialize("123"), "123");
    }

    @Test
    public void testGraphQLDeferredIdParseLiteral() {
        assertEquals(GraphQLScalars.GRAPHQL_DEFERRED_ID.getCoercing().parseLiteral(new IntValue(new BigInteger("123"))), "123");
        assertEquals(GraphQLScalars.GRAPHQL_DEFERRED_ID.getCoercing().parseLiteral(new StringValue("123")), "123");
    }

    @Test
    public void testGraphQLDeferredIdParseValue() {
        assertEquals(GraphQLScalars.GRAPHQL_DEFERRED_ID.getCoercing().parseValue("123"), "123");
        assertEquals(GraphQLScalars.GRAPHQL_DEFERRED_ID.getCoercing().parseValue(123L), "123");
    }
}
