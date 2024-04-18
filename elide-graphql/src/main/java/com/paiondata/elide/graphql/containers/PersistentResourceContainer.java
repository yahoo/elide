/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.graphql.containers;

import com.paiondata.elide.core.PersistentResource;

/**
 * Interface describing containers that hold PersistentResources.
 */
public interface PersistentResourceContainer {
    PersistentResource getPersistentResource();
}
