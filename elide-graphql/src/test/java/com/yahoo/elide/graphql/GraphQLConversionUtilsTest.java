/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.core.utils.DefaultClassScanner;
import com.yahoo.elide.core.utils.coerce.CoerceUtil;
import com.yahoo.elide.core.utils.coerce.converters.OffsetDateTimeSerde;
import com.yahoo.elide.core.utils.coerce.converters.TimeZoneSerde;
import org.junit.jupiter.api.Test;

import graphql.schema.GraphQLScalarType;

import java.time.OffsetDateTime;
import java.util.TimeZone;

public class GraphQLConversionUtilsTest {

    @Test
    public void testGraphQLConversionUtilsOffsetDateTimeToScalarType() {
        CoerceUtil.register(OffsetDateTime.class, new OffsetDateTimeSerde());
        GraphQLConversionUtils graphQLConversionUtils =
                new GraphQLConversionUtils(EntityDictionary.builder().build(),
                        new NonEntityDictionary(DefaultClassScanner.getInstance(), CoerceUtil::lookup));
        GraphQLScalarType offsetDateTimeType = graphQLConversionUtils.classToScalarType(ClassType.of(OffsetDateTime.class));
        assertNotNull(offsetDateTimeType);
        String expected = "OffsetDateTime";
        assertEquals(expected, offsetDateTimeType.getName());
    }
    @Test
    public void testGraphQLConversionUtilsTimeZoneToScalarType() {
        CoerceUtil.register(TimeZone.class, new TimeZoneSerde());
        GraphQLConversionUtils graphQLConversionUtils =
                new GraphQLConversionUtils(EntityDictionary.builder().build(),
                        new NonEntityDictionary(DefaultClassScanner.getInstance(), CoerceUtil::lookup));
        GraphQLScalarType timeZoneType = graphQLConversionUtils.classToScalarType(ClassType.of(TimeZone.class));
        assertNotNull(timeZoneType);
        String expectedTimezone = "TimeZone";
        assertEquals(expectedTimezone, timeZoneType.getName());
    }
}
