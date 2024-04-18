/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.datastores.aggregation.query;

import java.util.LinkedList;
import java.util.List;

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

    /**
     * Collapses a list of query plans into a potentially smaller list of merged plans.
     * @param plans The list of plans to merge.
     * @return A list of merged plans - ideally containing a single merged plan.
     */
    default List<QueryPlan> merge(List<QueryPlan> plans) {
        QueryPlan mergedPlan = null;

        List<QueryPlan> result = new LinkedList<>();

        for (QueryPlan plan: plans) {
            if (mergedPlan == null) {
                mergedPlan = plan;
            } else {
                if (canMerge(mergedPlan, plan)) {
                    mergedPlan = merge(plan, mergedPlan);
                } else {
                    result.add(plan);
                }
            }
        }

        result.add(0, mergedPlan);
        return result;
    }
}
