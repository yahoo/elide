/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql.containers;

import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.graphql.Environment;
import com.yahoo.elide.graphql.PersistentResourceFetcher;

import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.ws.rs.BadRequestException;

/**
 * Container for edges.
 */
@AllArgsConstructor
public class EdgesContainer implements PersistentResourceContainer, GraphQLContainer {
    @Getter private final PersistentResource persistentResource;

    private static final String NODE_KEYWORD = "node";

    @Override
    public Object processFetch(Environment context, PersistentResourceFetcher fetcher) {
        String fieldName = context.field.getName();

        // TODO: Cursor
        if (NODE_KEYWORD.equals(fieldName)) {
            return new NodeContainer(context.parentResource);
        }

        throw new BadRequestException("Invalid request. Looking for field: " + fieldName + " in an edges object.");
    }
}
