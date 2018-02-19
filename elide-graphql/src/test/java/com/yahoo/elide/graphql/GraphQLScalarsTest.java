/*
 * Copyright 2018, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

import graphql.language.IntValue;
import graphql.language.StringValue;
import org.testng.annotations.Test;

import java.math.BigInteger;
import java.util.Date;

import static org.testng.Assert.assertEquals;

public class GraphQLScalarsTest {

    @Test
    public void testGraphQLDateParseValue() {
        Date date = new Date(123L);
        assertEquals(GraphQLScalars.GRAPHQL_DATE_TYPE.getCoercing().parseValue("123"), date);
        assertEquals(GraphQLScalars.GRAPHQL_DATE_TYPE.getCoercing().parseValue(123L), date);
        assertEquals(GraphQLScalars.GRAPHQL_DATE_TYPE.getCoercing().parseValue(123), date);
        assertEquals(GraphQLScalars.GRAPHQL_DATE_TYPE.getCoercing().parseValue(123.123), date);
    }

    @Test
    public void testGraphQLDateParseLiteral() {
        Date date = new Date(123L);
        assertEquals(GraphQLScalars.GRAPHQL_DATE_TYPE.getCoercing().parseLiteral(new StringValue("123")), date);
        assertEquals(GraphQLScalars.GRAPHQL_DATE_TYPE.getCoercing().parseLiteral(new IntValue(new BigInteger("123"))), date);
    }

    @Test
    public void testGraphQLDateSerialize() {
        Date date = new Date(123L);
        assertEquals(GraphQLScalars.GRAPHQL_DATE_TYPE.getCoercing().serialize(date), 123L);
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
