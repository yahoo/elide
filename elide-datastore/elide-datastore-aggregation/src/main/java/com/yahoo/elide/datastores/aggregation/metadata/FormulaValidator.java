/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata;

import com.yahoo.elide.datastores.aggregation.annotation.DimensionFormula;
import com.yahoo.elide.datastores.aggregation.annotation.MetricFormula;
import com.yahoo.elide.datastores.aggregation.core.JoinPath;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.MetricProjection;
import com.yahoo.elide.datastores.aggregation.query.Queryable;

import java.util.LinkedHashSet;
import java.util.stream.Collectors;

/**
 * FormulaValidator check whether a column defined with {@link MetricFormula} or
 * {@link DimensionFormula} has reference loop. If so, throw out exception.
 */
public class FormulaValidator extends ColumnVisitor<Void> {
    private final LinkedHashSet<String> visited = new LinkedHashSet<>();

    public FormulaValidator(MetaDataStore metaDataStore) {
        super(metaDataStore);
    }

    @Override
    protected Void visitFormulaMetric(MetricProjection metric) {
        return visitFormulaColumn(metric);
    }

    @Override
    protected Void visitFormulaDimension(ColumnProjection dimension) {
        return visitFormulaColumn(dimension);
    }

    @Override
    protected Void visitFieldDimension(ColumnProjection dimension) {
        return null;
    }

    /**
     * For a FORMULA column, we just need to check all source columns in the formula expression.
     *
     * @param column a column defined with {@link MetricFormula} or {@link DimensionFormula}
     * @return null
     */
    private Void visitFormulaColumn(ColumnProjection column) {
        if (visited.contains(column.getId())) {
            throw new IllegalArgumentException(referenceLoopMessage(visited, column));
        }

        Queryable source = column.getSource();
        Class<?> tableClass = dictionary.getEntityClass(source.getName(), source.getVersion());

        visited.add(column.getId());
        for (String reference : resolveFormulaReferences(column.getExpression())) {

            //Column is from a query instead of a table.  Nothing to validate.
            if (column.getSource() != column.getSource().getSource()) {
                continue;
            } else if (reference.contains(".")) {
                JoinPath joinToPath = new JoinPath(tableClass, dictionary, reference);

                visitColumn(getColumn(joinToPath));
            } else {
                ColumnProjection referenceColumn = source.getColumnProjection(reference);

                // if the reference is to a logical column, check it
                if (referenceColumn != null && !reference.equals(column.getName())) {
                    visitColumn(referenceColumn);
                }
            }
        }
        visited.remove(column.getId());

        return null;
    }

    /**
     * Construct reference loop message.
     */
    private static String referenceLoopMessage(LinkedHashSet<String> visited, ColumnProjection conflict) {
        return "Formula reference loop found: "
                + visited.stream()
                    .collect(Collectors.joining("->"))
                + "->" + conflict.getId();
    }
}
