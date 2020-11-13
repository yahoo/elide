/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfighelpers.model;

import java.util.Collection;

public interface Named {
    public String getName();

    /**
     * Checks if the collection has an object with given name.
     * @param collection 
     * @param name
     * @return true if the collection has an object with given name.
     */
    default boolean hasName(Collection<? extends Named> collection, String name) {
        return collection
                        .stream()
                        .map(obj -> obj.getName())
                        .anyMatch(name::equals);
    }
}
