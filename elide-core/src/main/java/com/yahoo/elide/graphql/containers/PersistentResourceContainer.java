/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql.containers;

import com.yahoo.elide.core.PersistentResource;

/**
 * Interface describing containers that hold PersistentResources.
 */
public interface PersistentResourceContainer {
    PersistentResource getPersistentResource();
}
