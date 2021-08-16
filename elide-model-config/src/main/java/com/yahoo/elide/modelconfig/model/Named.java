/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig.model;

import java.util.Collection;

public interface Named {
    /**
     * Get the name local to its parent.
     * @return the local name
     */
    String getName();

    /**
     * Get the globally unique name .
     * @return the global name
     */
    default String getGlobalName() {
        return getName();
    }

    /**
     * Checks if the collection has an object with given name.
     * @param collection of object with name property
     * @param name to search for in given collection
     * @return true if the collection has an object with given name.
     */
    default boolean hasName(Collection<? extends Named> collection, String name) {
        return collection
                        .stream()
                        .map(Named::getGlobalName)
                        .anyMatch(name::equals);
    }
}
