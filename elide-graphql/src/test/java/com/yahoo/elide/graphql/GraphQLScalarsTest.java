/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yahoo.elide.core.utils.coerce.CoerceUtil;
import com.yahoo.elide.core.utils.coerce.converters.ISO8601DateSerde;
import com.yahoo.elide.core.utils.coerce.converters.OffsetDateTimeSerde;
import com.yahoo.elide.core.utils.coerce.converters.Serde;
import com.yahoo.elide.core.utils.coerce.converters.TimeZoneSerde;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import graphql.language.IntValue;
import graphql.language.StringValue;

import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.TimeZone;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GraphQLScalarsTest {

    private Serde oldSerde;

    @BeforeAll
    public void init() {

        oldSerde = CoerceUtil.lookup(Date.class);

        CoerceUtil.register(Date.class, new ISO8601DateSerde(
                "yyyy-MM-dd'T'HH:mm'Z'",
                TimeZone.getTimeZone("UTC"),
                java.sql.Date.class));

        CoerceUtil.register(OffsetDateTime.class, new OffsetDateTimeSerde());
    }

    @AfterAll
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
        assertEquals(expectedDate, actualDate);
    }

    @Test
    public void testGraphQLDateSerialize() {
        Date date = new Date(0L);
        Object actual = GraphQLScalars.GRAPHQL_DATE_TYPE.getCoercing().serialize(date);
        String expected = "1970-01-01T00:00Z";
        assertEquals(expected, actual);
    }

    @Test
    public void testGraphQLOffsetDateTimeDeserialize() {
        OffsetDateTime expectedDate =
                OffsetDateTime.of(1995, 11, 2,
                        16, 45, 4, 56,
                        ZoneOffset.ofHoursMinutes(5, 30));
        String input = "1995-11-02T16:45:04.000000056+05:30";
        OffsetDateTimeSerde offsetDateTimeSerde = new OffsetDateTimeSerde();
        SerdeCoercing serdeCoercing =
                new SerdeCoercing("", offsetDateTimeSerde);
        Object actualDate = serdeCoercing.parseLiteral(new StringValue(input));
        assertEquals(expectedDate, actualDate);
    }

    @Test
    public void testGraphQLTimeZoneDeserialize() {
        TimeZone expectedTz = TimeZone.getTimeZone("EST");
        String input = "EST";
        TimeZoneSerde timeZoneSerde = new TimeZoneSerde();
        SerdeCoercing serdeCoercing =
                new SerdeCoercing("", timeZoneSerde);
        Object actualTz = serdeCoercing.parseLiteral(new StringValue(input));
        assertEquals(expectedTz, actualTz);
    }
    @Test
    public void testGraphQLDeferredIdSerialize() {
        assertEquals(123L, GraphQLScalars.GRAPHQL_DEFERRED_ID.getCoercing().serialize(123L));
        assertEquals("123", GraphQLScalars.GRAPHQL_DEFERRED_ID.getCoercing().serialize("123"));
    }

    @Test
    public void testGraphQLDeferredIdParseLiteral() {
        assertEquals("123", GraphQLScalars.GRAPHQL_DEFERRED_ID.getCoercing().parseLiteral(new IntValue(new BigInteger("123"))));
        assertEquals("123", GraphQLScalars.GRAPHQL_DEFERRED_ID.getCoercing().parseLiteral(new StringValue("123")));
    }

    @Test
    public void testGraphQLDeferredIdParseValue() {
        assertEquals("123", GraphQLScalars.GRAPHQL_DEFERRED_ID.getCoercing().parseValue("123"));
        assertEquals("123", GraphQLScalars.GRAPHQL_DEFERRED_ID.getCoercing().parseValue(123L));
    }
}
