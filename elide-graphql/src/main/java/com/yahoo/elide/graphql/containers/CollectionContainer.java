/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql.containers;

import com.yahoo.elide.core.PersistentResource;

import java.util.Set;

public interface CollectionContainer {
    Set<PersistentResource> getPersistentResources();
}
