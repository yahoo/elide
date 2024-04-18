/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.datastores.aggregation.query;

/**
 * Optimizes query plans.
 */
public interface Optimizer {

    /**
     * Returns the hint that identifies this optimizations.
     * @return The hint which enables this optimization.
     */
    String hint();

    /**
     * Returns the hint that disables this optimization.
     * @return The hint which turns off this optimization.
     */
    default String negateHint() {
        return "No" + hint();
    }

    /**
     * Verifies if this optimizer can execute on the given query.
     * @return True if the query can be optimized by this optimizer.
     */
    boolean canOptimize(Query query);

    /**
     * Optimizes the query.
     * @param query The query to optimize.
     * @return A new optimized query.
     */
    Query optimize(Query query);
}
