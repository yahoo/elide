/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.graphql.parser.GraphQLEntityProjectionMaker;
import com.yahoo.elide.graphql.parser.GraphQLProjectionInfo;
import com.yahoo.elide.request.Attribute;
import com.yahoo.elide.request.EntityProjection;
import example.models.inheritance.Character;
import example.models.inheritance.Droid;
import org.junit.jupiter.api.Test;

import java.util.TimeZone;

public class GraphQLEntityProjectionMakerTest extends GraphQLTest {
    ElideSettings settings;

    public GraphQLEntityProjectionMakerTest() {
        RSQLFilterDialect filterDialect = new RSQLFilterDialect(dictionary);

        settings = new ElideSettingsBuilder(null)
                .withEntityDictionary(dictionary)
                .withJoinFilterDialect(filterDialect)
                .withSubqueryFilterDialect(filterDialect)
                .withISO8601Dates("yyyy-MM-dd'T'HH:mm'Z'", TimeZone.getTimeZone("UTC"))
                .build();
    }

    @Test
    public void testInlineFragment() {
        GraphQLEntityProjectionMaker maker = new GraphQLEntityProjectionMaker(settings);

        String query = "{ character { edges { node { "
                + "__typename ... on Character { name } "
                + "__typename ... on Droid { primaryFunction }}}}}";

        GraphQLProjectionInfo info = maker.make(query);

        EntityProjection projection = info.getProjection("", "character");

        Attribute nameAttribute = projection.getAttributes().stream()
                .filter(attr -> attr.getName().equals("name"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException());

        assertEquals(Character.class, nameAttribute.getParentType());

        Attribute primaryFunctionAttribute = projection.getAttributes().stream()
                .filter(attr -> attr.getName().equals("primaryFunction"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException());

        assertEquals(Droid.class, primaryFunctionAttribute.getParentType());
    }
}
