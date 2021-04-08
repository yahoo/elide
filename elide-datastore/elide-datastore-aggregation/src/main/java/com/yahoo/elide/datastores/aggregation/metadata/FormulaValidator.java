/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata;

import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.datastores.aggregation.annotation.DimensionFormula;
import com.yahoo.elide.datastores.aggregation.annotation.MetricFormula;
import com.yahoo.elide.datastores.aggregation.core.JoinPath;
import com.yahoo.elide.datastores.aggregation.metadata.models.Column;
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

    private static String getColumnId(Queryable parent, ColumnProjection column) {
        return parent.getName() + "." + column.getName();
    }

    public FormulaValidator(MetaDataStore metaDataStore) {
        super(metaDataStore);
    }

    @Override
    protected Void visitFormulaMetric(Queryable parent, MetricProjection metric) {
        return visitFormulaColumn(parent, metric);
    }

    @Override
    protected Void visitFormulaDimension(Queryable parent, ColumnProjection dimension) {
        return visitFormulaColumn(parent, dimension);
    }

    @Override
    protected Void visitFieldDimension(Queryable parent, ColumnProjection dimension) {
        return null;
    }

    /**
     * For a FORMULA column, we just need to check all source columns in the formula expression.
     *
     * @param column a column defined with {@link MetricFormula} or {@link DimensionFormula}
     * @return null
     */
    private Void visitFormulaColumn(Queryable source, ColumnProjection column) {
        if (visited.contains(getColumnId(source, column))) {
            throw new IllegalArgumentException(referenceLoopMessage(visited, source, column));
        }

        Type<?> tableClass = dictionary.getEntityClass(source.getName(), source.getVersion());

        visited.add(getColumnId(source, column));
        for (String reference : resolveFormulaReferences(column.getExpression())) {

            //Column is from a query instead of a table.  Nothing to validate.
            if (source != source.getSource()) {
                continue;
            } else if (reference.contains(".")) {
                JoinPath joinToPath = new JoinPath(tableClass, metaDataStore, reference);
                Column joinToColumn = getColumn(joinToPath);
                if (joinToColumn != null) {
                    visitColumn(joinToColumn.getTable().toQueryable(), joinToColumn.toProjection());
                }
            } else {
                ColumnProjection referenceColumn = source.getColumnProjection(reference);

                // if the reference is to a logical column, check it
                if (referenceColumn != null && !reference.equals(column.getName())) {
                    visitColumn(source, referenceColumn);
                }
            }
        }
        visited.remove(getColumnId(source, column));

        return null;
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
