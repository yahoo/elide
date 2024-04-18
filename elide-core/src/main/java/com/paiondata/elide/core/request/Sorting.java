/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.core.request;

import com.paiondata.elide.core.Path;
import com.paiondata.elide.core.type.Type;

import java.util.Map;

/**
 * Represents a client request to sort a collection.
 */
public interface Sorting {

   /**
    * Denotes the intended sort direction (ascending or descending).
    */
    public enum SortOrder { asc, desc }

    /**
     * Return an ordered map of paths and their sort order.
     * @param <T> The type to sort.
     * @return An ordered map of paths and their sort order.
     */
    public <T> Map<Path, SortOrder> getSortingPaths();

    /**
     * Get the type of the collection to sort.
     * @return the collection type.
     */
    public Type<?> getType();

    /**
     * Is this sorting the default instance (not present).
     * @return true if sorting wasn't requested.  False otherwise.
     */
    public boolean isDefaultInstance();
}
