/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.datastores.aggregation.queryengines.sql.calcite;

import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;

/**
 * SQL Aggregation function along with templates to support nesting.
 */
@Value
@Builder
public class SupportedAggregation {
    //The SQL name
    @NonNull
    private String name;

    //Set of inner query templates to translate the aggregation function to alternative functions that can be nested.
    @Singular
    @NonNull
    private List<String> innerTemplates;

    //Outer query template to translate the aggregation function to alternative functions that can be nested.
    @NonNull
    private String outerTemplate;

    public List<String> getInnerAggregations(String ... operands) {
        List<String> innerAggregations = new ArrayList<>();

        for (String innerTemplate : innerTemplates) {
            innerAggregations.add(String.format(innerTemplate, (Object[]) operands));
        }

        return innerAggregations;
    }

    public String getOuterAggregation(String ... operands) {
        return String.format(outerTemplate, (Object[]) operands);
    }
}
