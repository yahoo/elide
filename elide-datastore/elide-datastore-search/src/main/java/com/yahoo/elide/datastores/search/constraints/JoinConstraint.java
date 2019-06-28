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
 * Constraint on whether or not the filter predicate would require a join to another entity.
 */
public class JoinConstraint implements SearchConstraint {
    @Override
    public DataStoreTransaction.FeatureSupport canSearch(Class<?> entityClass, FilterPredicate predicate)
            throws HttpStatusException {

        /* We don't support joins to other relationships */
        if (predicate.getPath().getPathElements().size() != 1) {
            return DataStoreTransaction.FeatureSupport.NONE;
        }

        return DataStoreTransaction.FeatureSupport.FULL;
    }
}
