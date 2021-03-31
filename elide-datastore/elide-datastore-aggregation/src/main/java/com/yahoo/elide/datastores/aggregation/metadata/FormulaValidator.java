/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata;

import static com.yahoo.elide.core.utils.TypeHelper.nullOrEmpty;
import static com.yahoo.elide.datastores.aggregation.metadata.ColumnVisitor.resolveFormulaReferences;
import static com.yahoo.elide.datastores.aggregation.metadata.ColumnVisitor.toFormulaReference;
import static com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable.DOUBLE_DOLLAR;
import static com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable.PERIOD;

import com.yahoo.elide.datastores.aggregation.annotation.DimensionFormula;
import com.yahoo.elide.datastores.aggregation.annotation.MetricFormula;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.Queryable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.TableContext;

import com.github.jknack.handlebars.EscapingStrategy;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.HandlebarsException;
import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import com.github.jknack.handlebars.TagType;
import com.github.jknack.handlebars.Template;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * FormulaValidator check whether a column defined with {@link MetricFormula} or
 * {@link DimensionFormula} has reference loop. If so, throw out exception.
 */
public class FormulaValidator {
    private final TableContext tableCtx;
    private final Handlebars handlebars;

    private static String getColumnId(Queryable parent, ColumnProjection column) {
        return parent.getName() + PERIOD + column.getName();
    }

    public FormulaValidator(TableContext tableCtx) {
        this.tableCtx = tableCtx;
        this.handlebars = new Handlebars()
                        .with(EscapingStrategy.NOOP)
                        .registerHelper("sql", new Helper<Object>() {

                            @Override
                            public Object apply(final Object context, final Options options) throws IOException {
                                String path = options.hash("path");
                                String from = options.hash("from");
                                String column = options.hash("column");
                                int argsIndex = column.indexOf('[');

                                // Remove args from column
                                column = argsIndex == -1 ? column : column.substring(0, argsIndex);
                                // Prefix column with join table name
                                column = nullOrEmpty(from) ? column : from + PERIOD + column;
                                // Prefix column with current path
                                column = nullOrEmpty(path) ? column : path + PERIOD + column;

                                return toFormulaReference(column);
                            }
                        });
    }

    public void validateColumn(Queryable source, ColumnProjection column) {
        String columnId = getColumnId(source, column);
        Set<String> visited = new HashSet<>();
        try {
            String current = getHandlebarVariables(column.getExpression());
            visited.add(current);
            Template template = handlebars.compileInline(current);

            while (!template.collect(TagType.VAR).isEmpty()) {
                String resolved = template.apply(tableCtx);
                current = getHandlebarVariables(resolved);
                if (!visited.add(current)) {
                    throw new IllegalArgumentException(
                                    "Formula validation failed. Reference Loop detected for: " + columnId);
                }
                template = handlebars.compileInline(current);
            }
        } catch (HandlebarsException e) {
            throw new IllegalArgumentException(
                            String.format("Formula validation failed. Possible Reference Loop for: %s. %s", columnId,
                                            e.getMessage()));
        } catch (IOException e) {
            // Do Nothing
        }
    }

    /**
     * Get the handlebar variables form input string.
     * eg: for "{{userName}} == {{$$user.identity}} AND {{userEnabled}}"
     * returns "{{userName}}{{userEnabled}}"
     */
    private String getHandlebarVariables(String expression) {

        return resolveFormulaReferences(expression).stream()
                        .filter(expr -> !expr.startsWith(DOUBLE_DOLLAR))
                        .map(ColumnVisitor::toFormulaReference)
                        .collect(Collectors.joining());
    }
}
