/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata;

import static com.yahoo.elide.datastores.aggregation.metadata.ColumnVisitor.resolveFormulaReferences;
import static com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable.DOUBLE_DOLLAR;
import static com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable.PERIOD;
import static com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable.SQL_HELPER_PREFIX;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

/**
 * Column Context for Handlebars Resolution.
 */
@Data
@AllArgsConstructor
public class ColumnContext {
    private final String definition;
    private final String path;
    private final Map<String, Object> defaultColumnArgs;

    @Override
    public String toString() {

        String expr = definition;
        for (String reference : resolveFormulaReferences(expr)) {
            String referenceWithPath = reference;
            if (!path.isEmpty()) {
                if (reference.startsWith(SQL_HELPER_PREFIX)) {
                    referenceWithPath = reference + " path='" + path + "'";
                } else if (!reference.startsWith(DOUBLE_DOLLAR)) {
                    referenceWithPath = path + PERIOD + reference;
                }
            }
            expr = expr.replace(reference, referenceWithPath);
        }

        return expr;
    }
}
