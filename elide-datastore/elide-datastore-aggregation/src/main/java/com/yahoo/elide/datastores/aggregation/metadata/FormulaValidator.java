/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata;

import static com.yahoo.elide.core.utils.TypeHelper.nullOrEmpty;
import static com.yahoo.elide.datastores.aggregation.metadata.ColumnVisitor.resolveFormulaReferences;
import static com.yahoo.elide.datastores.aggregation.metadata.ColumnVisitor.toFormulaReference;
import static com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable.PERIOD;
import static com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable.SQL_HELPER_PREFIX;

import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.Queryable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.ColumnDefinition;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.TableContext;

import com.github.jknack.handlebars.EscapingStrategy;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import com.github.jknack.handlebars.Template;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * FormulaValidator validates the expression for any {@link ColumnProjection}. Ensures logical column references exists
 * and there are no cycles between them.
 */
public class FormulaValidator {
    private static final String PERIOD_REGEX = "[.]";
    private final Handlebars handlebars;
    private final TableContext tblCtx;

    private static String getColumnId(Queryable parent, ColumnProjection column) {
        return parent.getName() + PERIOD + column.getName();
    }

    public FormulaValidator(TableContext tableCtx) {
        this.tblCtx = tableCtx;

        this.handlebars = new Handlebars()
                        .with(EscapingStrategy.NOOP)
                        .registerHelper("sql", new Helper<Object>() {

                            @Override
                            public Object apply(final Object context, final Options options) throws IOException {
                                String from = options.hash("from");
                                String column = options.hash("column");
                                int argsIndex = column.indexOf('[');

                                // Remove args from column
                                column = argsIndex == -1 ? column : column.substring(0, argsIndex);
                                // Prefix column with join table name
                                column = nullOrEmpty(from) ? column : from + PERIOD + column;

                                return column;
                            }
                        });
    }

    public void validateColumn(Queryable source, ColumnProjection column) {
        checkCycle(tblCtx, column.getName(), getColumnId(source, column), new HashSet<>());
    }

    private void checkCycle(TableContext tblCtx, String columnName, String columnId, Set<String> alreadyVisited) {

        Set<String> visited = new HashSet<>(alreadyVisited);

        // Physical References starts with $
        if (columnName.lastIndexOf('$') == 0) {
            return;
        }

        if (!visited.add(tblCtx.getAlias() + PERIOD + columnName)) {
            throw new IllegalArgumentException(
                            String.format("Formula validation failed for: %s. Reference Loop detected.", columnId));
        }

        ColumnDefinition columnDefinition = tblCtx.getColumnDefinition(columnName);
        if (columnDefinition == null) {
            throw new IllegalArgumentException(String.format(
                            "Formula validation failed for: %s. Couldn't find column: %s.", columnId, columnName));
        }

        String expr = columnDefinition.getExpression();

        for (String reference : resolveFormulaReferences(expr)) {

            // Physical References & Query Contexts starts with $, they can not cause loop.
            if (reference.indexOf('$') == 0) {
                continue;
            }

            // Change {{sql from='joinName' column='columnName[a1:v1][a2:v2]'}} to joinName.columnName
            if (reference.startsWith(SQL_HELPER_PREFIX)) {
                try {
                    Template template = handlebars.compileInline(toFormulaReference(reference));
                    reference = template.apply(tblCtx);
                } catch (IOException e) {
                    // Do Nothing
                }
            }

            int dotIndex = reference.lastIndexOf('.');
            if (dotIndex >= 0) {
                // eg: join1.col1 or join1.join2.col1
                String joinTables = reference.substring(0, dotIndex);
                String joinCol = reference.substring(dotIndex + 1);

                String[] joins = joinTables.split(PERIOD_REGEX);
                TableContext currentCtx = tblCtx;
                TableContext joinTblCtx = null;
                for (int i = 0; i < joins.length; i++) {
                    joinTblCtx = currentCtx.getJoins().get(joins[i]);
                    if (joinTblCtx == null) {
                        throw new IllegalArgumentException(
                                        String.format("Formula validation failed for: %s. Couldn't find join: %s.",
                                                        columnId, joins[i]));
                    }
                    currentCtx = joinTblCtx;
                }
                checkCycle(joinTblCtx, joinCol, columnId, visited);
            } else {
                checkCycle(tblCtx, reference, columnId, visited);
            }
        }
    }
}
