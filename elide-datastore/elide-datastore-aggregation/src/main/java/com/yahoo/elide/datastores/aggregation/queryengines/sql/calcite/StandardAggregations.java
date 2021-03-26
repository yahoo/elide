/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.calcite;

import java.util.ArrayList;
import java.util.List;

/**
 * Standard aggregation functions common to all database dialects.
 */
public enum StandardAggregations {
    SUM("SUM", new String[]{"SUM(%s)"}, "SUM(%s)"),
    COUNT("COUNT", new String[]{"COUNT(%s)"}, "COUNT(%s)"),
    MIN("MIN", new String[]{"MIN(%s)"}, "MIN(%s)"),
    MAX("MAX", new String[]{"MAX(%s)"}, "MAX(%s)"),
    AVERAGE("AVG", new String[]{"SUM(%1$s)", "COUNT(%1$s)"}, "SUM(%1$s)/COUNT(%1$s)");
    //TODO - Add more aggregations functions.

    //The SQL name
    private String name;

    //Set of inner query templates to translate the aggregation function to alternative functions that can be nested.
    private String[] innerTemplates;

    //Outer query template to translate the aggregation function to alternative functions that can be nested.
    private String outerTemplate;

    StandardAggregations(String name, String[] innerTemplates, String outerTemplate) {
        this.name = name;
        this.innerTemplates = innerTemplates;
        this.outerTemplate = outerTemplate;
    }

    public List<String> getInnerAggregations(String ... operands) {
        List<String> innerAggregations = new ArrayList<>();

        for (String innerTemplate : innerTemplates) {
            innerAggregations.add(String.format(innerTemplate, operands));
        }

        return innerAggregations;
    }

    public String getOuterAggregation(String ... operands) {
        return String.format(outerTemplate, operands);
    }

    public static StandardAggregations find(String name) {
        for (StandardAggregations current : StandardAggregations.values()) {
            if (current.name.equals(name)) {
                return current;
            }
        }
        return null;
    }
}
