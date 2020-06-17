/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.utils.coerce.CoerceUtil;
import com.yahoo.elide.utils.coerce.converters.OffsetDateTimeSerde;
import com.yahoo.elide.utils.coerce.converters.TimeZoneSerde;

import org.junit.jupiter.api.Test;

import graphql.schema.GraphQLScalarType;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.TimeZone;

public class GraphQLConversionUtilsTest {

    @Test
    public void testGraphQLConversionUtilsClassToScalarType() {
        CoerceUtil.register(OffsetDateTime.class, new OffsetDateTimeSerde());
        CoerceUtil.register(TimeZone.class, new TimeZoneSerde());
        GraphQLConversionUtils graphQLConversionUtils =
                new GraphQLConversionUtils(new EntityDictionary(new HashMap<>()), new NonEntityDictionary());
        GraphQLScalarType offsetDateTimeType = graphQLConversionUtils.classToScalarType(OffsetDateTime.class);
        GraphQLScalarType timeZoneType = graphQLConversionUtils.classToScalarType(TimeZone.class);
        assertNotNull(offsetDateTimeType);
        assertNotNull(timeZoneType);
        String expected = "OffsetDateTime";
        assertEquals(expected, offsetDateTimeType.getName());
        String expectedTimezone = "TimeZone";
        assertEquals(expectedTimezone, timeZoneType.getName());
    }
}
