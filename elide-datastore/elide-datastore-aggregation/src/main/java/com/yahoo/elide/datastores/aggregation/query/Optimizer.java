/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.query;

import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable;

/**
 * Optimizes query plans.
 */
public interface Optimizer {
    /**
     * Verifies if this optimizer can execute on the given query.
     * @return True if the query can be optimized by this optimizer.
     */
    boolean canOptimize(Query query, SQLReferenceTable referenceTable);

    /**
     * Optimizes the query.
     * @param query The query to optimize.
     * @return A new optimized query.
     */
    Query optimize(Query query, SQLReferenceTable referenceTable);
}
