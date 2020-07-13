/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.security;

import java.util.Optional;

/**
 * The persistent resource interface passed to change specs.
 * @param <T> resource type
 */
public interface PersistentResource<T> {

    boolean matchesId(String id);

    Optional<String> getUUID();
    String getId();
    String getType();

    T getObject();
    Class<T> getResourceClass();
    RequestScope getRequestScope();

    /**
     * Returns whether or not this resource was created in this transaction.
     * @return True if this resource is newly created.
     */
    default boolean isNewlyCreated() {
        return getRequestScope().getNewResources().contains(this);
    }
}
