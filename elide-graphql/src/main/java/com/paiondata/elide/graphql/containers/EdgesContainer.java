/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.graphql.containers;

import static com.paiondata.elide.graphql.KeyWord.NODE;

import com.paiondata.elide.core.PersistentResource;
import com.paiondata.elide.core.exceptions.BadRequestException;
import com.paiondata.elide.graphql.Environment;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Container for edges.
 */
@AllArgsConstructor
public class EdgesContainer implements PersistentResourceContainer, GraphQLContainer<NodeContainer> {
    @Getter private final PersistentResource persistentResource;

    @Override
    public NodeContainer processFetch(Environment context) {
        String fieldName = context.field.getName();

        // TODO: Cursor
        if (NODE.hasName(fieldName)) {
            return new NodeContainer(context.parentResource);
        }

        throw new BadRequestException("Invalid request. Looking for field: " + fieldName + " in an edges object.");
    }
}
