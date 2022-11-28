/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql.parser;

import com.yahoo.elide.core.request.Attribute;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.request.Relationship;

import graphql.language.SourceLocation;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

/**
 * A helper class that contains a collection of root-level entity projections and relationship map constructed from
 * a {@link graphql.language.Document}.
 */
@AllArgsConstructor
public class GraphQLProjectionInfo {
    @Getter private final Map<String, EntityProjection> projections;

    @Getter private final Map<SourceLocation, Relationship> relationshipMap;

    @Getter private final Map<SourceLocation, Attribute> attributeMap;

    public EntityProjection getProjection(String aliasName, String entityName) {
        return projections.get(computeProjectionKey(aliasName, entityName));
    }

    public static String computeProjectionKey(String aliasName, String entityName) {
        return (aliasName == null ? "" : aliasName) + ":" + entityName;
    }
}
