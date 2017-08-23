/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql;

import com.yahoo.elide.core.PersistentResource;

public class DeferredId {
    private PersistentResource resource;

    public DeferredId(PersistentResource resource) {
        this.resource = resource;
    }

    @Override
    public String toString() {
        return resource.getId();
    }
}