/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.search.constraints;

import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.exceptions.HttpStatusException;
import com.yahoo.elide.core.filter.FilterPredicate;

/**
 * Depending on how an entity is indexed, it is possible to override the constraints that determine
 * how a given filter predicate is handled by the store.  The behaviors include:
 * 1. Use the search data store fully for filtering.
 * 2. Use the search data store partially for filtering (some will be performed in memory)
 * 2. Delegate to the underlying wrapped store
 * 3. Throw a HTTPStatusException
 */
public interface SearchConstraint {

    /**
     * Determines whether or not the SearchDataStore can load objects given a particular filter request.
     * @param entityClass The class to search
     * @param predicate The filter expression to search on.
     * @return FULL if the entire filter predicate can be satisfied by the datastore.  PARTIAL if some in memory
     * filtering is also required.  NONE if the search should be delegated to the underlying wrapped store.
     * @throws HttpStatusException If the search request is a violation of the API contract.
     */
    DataStoreTransaction.FeatureSupport canSearch(Class<?> entityClass, FilterPredicate predicate)
            throws HttpStatusException;
}
