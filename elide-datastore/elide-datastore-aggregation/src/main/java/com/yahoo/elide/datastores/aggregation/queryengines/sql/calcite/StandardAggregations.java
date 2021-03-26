/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.calcite;

import java.util.ArrayList;
import java.util.List;

public enum StandardAggregations {
    SUM("SUM", new String[]{"SUM(%s)"}, "SUM(%s)"),
    COUNT("COUNT", new String[]{"COUNT(%s)"}, "COUNT(%s)"),
    MIN("MIN", new String[]{"MIN(%s)"}, "MIN(%s)"),
    MAX("MAX", new String[]{"MAX(%s)"}, "MAX(%s)"),
    AVERAGE("AVG", new String[]{"SUM(%s)", "COUNT(%s)"}, "SUM(%s)/COUNT(%s)");

    private String name;
    private String[] innerTemplates;
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
