/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql.containers;

import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.pagination.Pagination;
import com.yahoo.elide.graphql.Environment;
import com.yahoo.elide.graphql.PersistentResourceFetcher;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.BadRequestException;

/**
 * Container representing a GraphQL "connection" object.
 */
@AllArgsConstructor
public class ConnectionContainer implements GraphQLContainer {
    @Getter private final Set<PersistentResource> persistentResources;
    @Getter private final Optional<Pagination> pagination;
    // Refers to the type of persistentResources
    @Getter private final String typeName;

    public static final String EDGES_KEYWORD = "edges";
    public static final String PAGE_INFO_KEYWORD = "pageInfo";

    @Override
    public Object processFetch(Environment context, PersistentResourceFetcher fetcher) {
        String fieldName = context.field.getName();

        switch (fieldName) {
            case EDGES_KEYWORD:
                return getPersistentResources().stream()
                        .map(EdgesContainer::new)
                        .collect(Collectors.toList());
            case PAGE_INFO_KEYWORD:
                return new PageInfoContainer(this);
            default:
                break;
        }

        throw new BadRequestException("Invalid request. Looking for field: " + fieldName + " in a connection object.");
    }
}
