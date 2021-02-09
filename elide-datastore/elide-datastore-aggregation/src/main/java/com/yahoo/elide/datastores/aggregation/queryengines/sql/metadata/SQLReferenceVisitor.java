/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata;

import static com.yahoo.elide.core.utils.TypeHelper.extendTypeAlias;
import static com.yahoo.elide.core.utils.TypeHelper.getFieldAlias;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.datastores.aggregation.core.JoinPath;
import com.yahoo.elide.datastores.aggregation.metadata.ColumnVisitor;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.MetricProjection;
import com.yahoo.elide.datastores.aggregation.query.Queryable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialect;

import java.util.Stack;

/**
 * SQLReferenceVisitor convert each column to its full expanded aliased SQL physical expression.
 */
public class SQLReferenceVisitor extends ColumnVisitor<String> {
    // this visitor is using DFS pattern when traversing columns, use Stack as state tracker
    private final Stack<String> tableAliases = new Stack<>();
    private final SQLDialect dialect;

    public SQLReferenceVisitor(MetaDataStore metaDataStore, String tableAlias, SQLDialect dialect) {
        super(metaDataStore);
        tableAliases.push(tableAlias);
        this.dialect = dialect;
    }

    /**
     * For physical reference, just append it to the table alias
     *
     * @param reference physical column name
     * @return <code>table.reference</code>
     */
    @Override
    protected String visitPhysicalReference(String reference) {
        return getFieldAlias(applyQuotes(tableAliases.peek()), applyQuotes(reference));
    }

    @Override
    protected String visitFormulaMetric(MetricProjection metric) {
        return visitFormulaColumn(metric);
    }

    /**
     * For a FIELD dimension, append its physical columnName to the table alias
     *
     * @param dimension a FIELD dimension
     * @return <code>table.columnName</code>
     */
    @Override
    protected String visitFieldDimension(ColumnProjection dimension) {
        Queryable source = dimension.getSource();

        //This is a table.  Check if there is a @Column annotation.
        if (source == source.getSource()) {
            return getFieldAlias(
                    applyQuotes(tableAliases.peek()),
                    applyQuotes(dictionary.getAnnotatedColumnName(
                            dictionary.getEntityClass(source.getName(), source.getVersion()),
                            dimension.getName())));

        //This is a nested query.  Don't do table lookups.
        } else {
            String expr = dimension.getExpression();
            // Remove leading '{{' & trailing '}}'
            expr = expr.substring(2, expr.length() - 2);
            return visitPhysicalReference(expr);
        }
    }

    @Override
    protected String visitFormulaDimension(ColumnProjection dimension) {
        return visitFormulaColumn(dimension);
    }

    /**
     * For a FORMULA column, resolve each reference individually and
     *
     * @param column
     * @return
     */
    private String visitFormulaColumn(ColumnProjection column) {
        Queryable source  = column.getSource();

        String expr = column.getExpression();

        // replace references with resolved statements/expressions
        for (String reference : resolveFormulaReferences(column.getExpression())) {
            String resolvedReference;

            //The column is sourced from a query rather than a table.
            if (column.getSource() != column.getSource().getSource()) {
                resolvedReference = visitPhysicalReference(reference);

            //The reference is a join to another logical column.
            } else if (reference.contains(".")) {
                Type<?> tableClass = dictionary.getEntityClass(source.getName(), source.getVersion());
                resolvedReference = visitTableJoinToReference(tableClass, reference);
            } else {
                ColumnProjection referenceColumn = source.getColumnProjection(reference);

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
    private String visitTableJoinToReference(Type<?> tableClass, String joinToPath) {
        JoinPath joinPath = new JoinPath(tableClass, dictionary, joinToPath);

        tableAliases.push(extendTypeAlias(tableAliases.peek(), joinPath));
        String result;
        ColumnProjection columnProjection = getColumn(joinPath);
        if (columnProjection == null) {
            result = visitPhysicalReference(getFieldName(joinPath));
        } else {
            result = visitColumn(columnProjection);
        }
        tableAliases.pop();

        return result;
    }

    /**
     * Get name for the last field in a {@link Path}
     *
     * @param path path to a field
     * @return field name
     */
    private static String getFieldName(Path path) {
        Path.PathElement last = path.lastElement().get();
        return last.getFieldName();
    }

    /**
     * Quote column / table aliases using dialect specific quote characters.
     *
     * @param str alias
     * @return quoted alias
     */
    private String applyQuotes(String str) {
        return SQLReferenceTable.applyQuotes(str, dialect);
    }
}
