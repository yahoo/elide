/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql.containers;

import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.pagination.Pagination;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Optional;
import java.util.Set;

/**
 * Container representing a GraphQL "connection" object.
 */
@AllArgsConstructor
public class ConnectionContainer {
    @Getter private final Set<PersistentResource> persistentResources;
    @Getter private final Optional<Pagination> pagination;
}
