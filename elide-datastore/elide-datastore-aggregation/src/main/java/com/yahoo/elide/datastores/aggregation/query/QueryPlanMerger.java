/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.query;

/**
 * Merges multiple query plans into a smaller set (one if possible).
 */
public interface QueryPlanMerger {

    /**
     * Determines if two plans can be merged.
     * @param a plan A.
     * @param b plan B.
     * @return True if the plans can be merged.  False otherwise.
     */
    public boolean canMerge(QueryPlan a, QueryPlan b);

    /**
     * Merges two plans.
     * @param a plan A.
     * @param b plan B.
     * @return A new query plan resulting from the merge of A and B.
     */
    public QueryPlan merge(QueryPlan a, QueryPlan b);
}
