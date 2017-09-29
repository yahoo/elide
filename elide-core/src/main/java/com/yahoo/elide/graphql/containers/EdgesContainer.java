/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql.containers;

import com.yahoo.elide.core.PersistentResource;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Container for edges.
 */
@AllArgsConstructor
public class EdgesContainer implements PersistentResourceContainer, NestedContainer {
    @Getter private final PersistentResource persistentResource;
}
