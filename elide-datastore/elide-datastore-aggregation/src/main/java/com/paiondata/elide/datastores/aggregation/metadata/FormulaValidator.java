/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.aggregation.metadata;

import com.paiondata.elide.datastores.aggregation.annotation.DimensionFormula;
import com.paiondata.elide.datastores.aggregation.annotation.MetricFormula;
import com.paiondata.elide.datastores.aggregation.query.ColumnProjection;
import com.paiondata.elide.datastores.aggregation.query.Queryable;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.expression.ExpressionParser;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.expression.Reference;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * FormulaValidator check whether a column defined with {@link MetricFormula} or
 * {@link DimensionFormula} has reference loop. If so, throw out exception.
 */
public class FormulaValidator extends ExpressionParser {
    private final LinkedHashSet<String> visited = new LinkedHashSet<>();

    private static String getColumnId(Queryable parent, ColumnProjection column) {
        return parent.getName() + "." + column.getName();
    }

    public FormulaValidator(MetaDataStore metaDataStore) {
        super(metaDataStore);
    }

    @Override
    public List<Reference> parse(Queryable source, ColumnProjection column) {
        String columnId = getColumnId(source, column);

        if (!visited.add(columnId)) {
            throw new IllegalArgumentException(referenceLoopMessage(visited, source, column));
        }
        List<Reference> references = parse(source, column.getExpression());
        visited.remove(columnId);

        return references;
    }

    /**
     * Construct reference loop message.
     */
    private static String referenceLoopMessage(LinkedHashSet<String> visited, Queryable source,
                                               ColumnProjection conflict) {
        return "Formula reference loop found: "
                + visited.stream()
                    .collect(Collectors.joining("->"))
                + "->" + getColumnId(source, conflict);
    }
}
