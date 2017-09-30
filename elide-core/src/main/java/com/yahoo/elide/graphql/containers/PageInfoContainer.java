/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql.containers;

import lombok.Getter;

/**
 * Container for nodes.
 */
public class PageInfoContainer implements NestedContainer {
    @Getter private final ConnectionContainer connectionContainer;

    public PageInfoContainer(ConnectionContainer connectionContainer) {
        this.connectionContainer = connectionContainer;
    }
}
