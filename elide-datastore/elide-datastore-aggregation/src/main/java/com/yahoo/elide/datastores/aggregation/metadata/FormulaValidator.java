/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata;

import com.yahoo.elide.datastores.aggregation.annotation.DimensionFormula;
import com.yahoo.elide.datastores.aggregation.annotation.MetricFormula;
import com.yahoo.elide.datastores.aggregation.core.JoinPath;
import com.yahoo.elide.datastores.aggregation.metadata.models.Column;
import com.yahoo.elide.datastores.aggregation.metadata.models.Dimension;
import com.yahoo.elide.datastores.aggregation.metadata.models.Metric;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;

import java.util.LinkedHashSet;
import java.util.stream.Collectors;

/**
 * FormulaValidator check whether a column defined with {@link MetricFormula} or
 * {@link DimensionFormula} has reference loop. If so, throw out exception.
 */
public class FormulaValidator extends ColumnVisitor<Void> {
    private final LinkedHashSet<Column> visited = new LinkedHashSet<>();

    public FormulaValidator(MetaDataStore metaDataStore) {
        super(metaDataStore);
    }

    /**
     * For a FIELD column, don't need to check anything.
     *
     * @param metric a FIELD metric
     * @return null
     */
    @Override
    protected Void visitFieldMetric(Metric metric) {
        return null;
    }

    @Override
    protected Void visitFormulaMetric(Metric metric) {
        return visitFormulaColumn(metric);
    }

    /**
     * For a FIELD column, don't need to check anything.
     *
     * @param dimension a FIELD dimension
     * @return null
     */
    @Override
    protected Void visitFieldDimension(Dimension dimension) {
        return null;
    }

    @Override
    protected Void visitFormulaDimension(Dimension dimension) {
        return visitFormulaColumn(dimension);
    }

    /**
     * For a FORMULA column, we just need to check all source columns in the formula expression.
     *
     * @param column a column defined with {@link MetricFormula} or {@link DimensionFormula}
     * @return null
     */
    private Void visitFormulaColumn(Column column) {
        if (visited.contains(column)) {
            throw new IllegalArgumentException(referenceLoopMessage(visited, column));
        }

        Table table = column.getTable();
        Class<?> tableClass = dictionary.getEntityClass(table.getName(), table.getVersion());

        visited.add(column);
        for (String reference : resolveFormulaReferences(column.getExpression())) {
            if (reference.contains(".")) {
                JoinPath joinToPath = new JoinPath(tableClass, dictionary, reference);

                visitColumn(getColumn(joinToPath));
            } else {
                Column referenceColumn = getColumn(tableClass, reference);

                // if the reference is to a logical column, check it
                if (referenceColumn != null && ! reference.equals(column.getName())) {
                    visitColumn(referenceColumn);
                }
            }
        }
        visited.remove(column);

        return null;
    }

    /**
     * Construct reference loop message.
     */
    private static String referenceLoopMessage(LinkedHashSet<Column> visited, Column conflict) {
        return "Formula reference loop found: "
                + visited.stream()
                    .map(Column::getId)
                    .collect(Collectors.joining("->"))
                + "->" + conflict.getId();
    }
}
