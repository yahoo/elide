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

import org.junit.jupiter.api.Test;

import graphql.schema.GraphQLScalarType;

import java.time.OffsetDateTime;
import java.util.HashMap;

public class GraphQLConversionUtilsTest {

    @Test
    public void testGraphQLConversionUtilsClassToScalarType() {
        CoerceUtil.register(OffsetDateTime.class, new OffsetDateTimeSerde());
        GraphQLConversionUtils graphQLConversionUtils =
                new GraphQLConversionUtils(new EntityDictionary(new HashMap<>()), new NonEntityDictionary());
        GraphQLScalarType type = graphQLConversionUtils.classToScalarType(OffsetDateTime.class);
        assertNotNull(type);
        String expected = "OffsetDateTime";
        assertEquals(expected, type.getName());
    }
}
