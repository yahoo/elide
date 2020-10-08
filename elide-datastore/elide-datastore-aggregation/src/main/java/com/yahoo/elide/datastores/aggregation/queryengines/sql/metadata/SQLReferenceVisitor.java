/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata;

import static com.yahoo.elide.utils.TypeHelper.extendTypeAlias;
import static com.yahoo.elide.utils.TypeHelper.getFieldAlias;

import com.yahoo.elide.datastores.aggregation.core.JoinPath;
import com.yahoo.elide.datastores.aggregation.metadata.ColumnVisitor;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.metadata.models.Column;
import com.yahoo.elide.datastores.aggregation.metadata.models.Dimension;
import com.yahoo.elide.datastores.aggregation.metadata.models.Metric;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;

import java.util.Stack;

/**
 * SQLReferenceVisitor convert each column to its full expanded aliased SQL physical expression.
 */
public class SQLReferenceVisitor extends ColumnVisitor<String> {
    // this visitor is using DFS pattern when traversing columns, use Stack as state tracker
    private final Stack<String> tableAliases = new Stack<>();

    public SQLReferenceVisitor(MetaDataStore metaDataStore, String tableAlias) {
        super(metaDataStore);
        tableAliases.push(tableAlias);
    }

    /**
     * For physical reference, just append it to the table alias
     *
     * @param reference physical column name
     * @return <code>table.reference</code>
     */
    @Override
    protected String visitPhysicalReference(String reference) {
        return getFieldAlias(tableAliases.peek(), reference);
    }

    /**
     * For a FIELD metric, resolve its expression
     *
     * @param metric a FIELD metric
     * @return <code>FUNCTION(table.columnName)</code>
     */
    @Override
    protected String visitFieldMetric(Metric metric) {
        Table table = metric.getTable();
        return String.format(
                metric.getMetricFunction().getExpression(),
                getFieldAlias(
                        tableAliases.peek(),
                        dictionary.getAnnotatedColumnName(
                                dictionary.getEntityClass(table.getName(), table.getVersion()),
                                metric.getName())));
    }

    @Override
    protected String visitFormulaMetric(Metric metric) {
        return visitFormulaColumn(metric);
    }

    /**
     * For a FIELD dimension, append its physical columnName to the table alias
     *
     * @param dimension a FIELD dimension
     * @return <code>table.columnName</code>
     */
    @Override
    protected String visitFieldDimension(Dimension dimension) {
        Table table = dimension.getTable();
        return getFieldAlias(
                tableAliases.peek(),
                dictionary.getAnnotatedColumnName(
                        dictionary.getEntityClass(table.getName(), table.getVersion()),
                        dimension.getName()));
    }

    @Override
    protected String visitFormulaDimension(Dimension dimension) {
        return visitFormulaColumn(dimension);
    }

    /**
     * For a FORMULA column, resolve each reference individually and
     *
     * @param column
     * @return
     */
    private String visitFormulaColumn(Column column) {
        Table table = column.getTable();
        Class<?> tableClass = dictionary.getEntityClass(table.getName(), table.getVersion());

        String expr = column.getExpression();

        // replace references with resolved statements/expressions
        for (String reference : resolveFormulaReferences(column.getExpression())) {
            String resolvedReference;

            //The reference is a join to another logical column.
            if (reference.contains(".")) {
                resolvedReference = visitTableJoinToReference(tableClass, reference);
            } else {
                Column referenceColumn = getColumn(tableClass, reference);

                //There is no logical column with this name, it must be a physical reference
                if (referenceColumn == null) {
                    resolvedReference = visitPhysicalReference(reference);

                //If the reference matches the column name - it means the logical and physical
                //columns have the same name.  Treat it like a physical column.
                } else if (reference.equals(column.getName())) {
                    resolvedReference = visitPhysicalReference(reference);
                //A reference to another logical column.
                } else {
                    resolvedReference = visitColumn(referenceColumn);
                }
            }

            expr = expr.replace(toFormulaReference(reference), resolvedReference);
        }

        return expr;
    }

    /**
     * Resolve path reference from a table. Append the join alias to table alias to get new table alias, then resolve
     * source column using new table alias.
     *
     * @param tableClass table class
     * @param joinToPath a dot separated path
     * @return resolved reference
     */
    private String visitTableJoinToReference(Class<?> tableClass, String joinToPath) {
        JoinPath joinPath = new JoinPath(tableClass, dictionary, joinToPath);

        tableAliases.push(extendTypeAlias(tableAliases.peek(), joinPath));
        String result = visitColumn(getColumn(joinPath));
        tableAliases.pop();

        return result;
    }
}
