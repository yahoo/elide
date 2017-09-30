/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql.containers;

import com.yahoo.elide.core.PersistentResource;
import lombok.Getter;

/**
 * Container for nodes.
 */
public class PageInfoContainer implements PersistentResourceContainer {
    @Getter private final PersistentResource persistentResource;

    public PageInfoContainer(PersistentResource persistentResource) {
        this.persistentResource = persistentResource;
    }
}
