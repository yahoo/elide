/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.datastores.presto;

import com.yahoo.elide.core.pagination.Pagination;
import com.yahoo.elide.datastores.aggregation.AggregationDataStoreTransaction;
import com.yahoo.elide.datastores.aggregation.QueryEngine;

/**
 * Presto Data Store Transaction
 */
public class PrestoDataStoreTransaction extends AggregationDataStoreTransaction {
    public PrestoDataStoreTransaction(QueryEngine queryEngine) {
        super(queryEngine);
    }

    @Override
    public boolean supportsPagination(Class<?> entityClass, Pagination pagination) {
        // Presto doesn't support pagination with offset
        return pagination.getOffset() == 0;
    }
}
