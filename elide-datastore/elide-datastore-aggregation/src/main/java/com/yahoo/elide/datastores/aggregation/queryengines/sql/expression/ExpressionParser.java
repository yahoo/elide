/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.expression;

import static com.yahoo.elide.core.request.Argument.getArgumentMapFromString;
import static com.yahoo.elide.datastores.aggregation.metadata.ColumnContext.PERIOD;
import static com.yahoo.elide.datastores.aggregation.metadata.ColumnContext.mergedArgumentMap;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.request.Argument;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.datastores.aggregation.core.JoinPath;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ColumnType;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.Queryable;
import com.github.jknack.handlebars.EscapingStrategy;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.google.common.base.Preconditions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a column or join expression and returns the reference abstract syntax tree for each discovered
 * reference.
 */
public class ExpressionParser {

    private static final Pattern REFERENCE_PARENTHESES = Pattern.compile("\\{\\{\\[?(.+?)\\]?}}");
    private static final String SQL_HELPER_PREFIX = "sql ";
    private static final String COLUMN_ARGS_PREFIX = "$$column.args.";
    private static final String TABLE_ARGS_PREFIX = "$$table.args.";
    private static final String COLUMN_EXPR = "$$column.expr";

    private MetaDataStore metaDataStore;
    private EntityDictionary dictionary;
    private final Handlebars handlebars = new Handlebars()
            .with(EscapingStrategy.NOOP)
            .registerHelper("sql", (context, options) -> {
                String from = options.hash("from");
                String column = options.hash("column");

                // Prefix column with join table name
                return isEmpty(from) ? column : from + PERIOD + column;
            });

    public ExpressionParser(MetaDataStore store) {
        this.dictionary = store.getMetadataDictionary();
        this.metaDataStore = store;
    }

    /**
     * Parses the column or join expression and returns the list of discovered references.
     * @param source The source table where the column or join expression lives.
     * @param column {@link ColumnProjection}
     * @return A list of discovered references.
     */
    public List<Reference> parse(Queryable source, ColumnProjection column) {
        return parse(source, column.getExpression(), column.getArguments());
    }

    /**
     * Parses the column or join expression and returns the list of discovered references.
     * @param source The source table where the column or join expression lives.
     * @param expression The expression to parse.
     * @return A list of discovered references.
     */
    public List<Reference> parse(Queryable source, String expression) {
        return parse(source, expression, Collections.emptyMap());
    }

    /**
     * Parses the column or join expression and returns the list of discovered references.
     * @param source The source table where the column or join expression lives.
     * @param expression The expression to parse.
     * @param callingColumnArgs Arguments available with calling column.
     * @return A list of discovered references.
     */
    public List<Reference> parse(Queryable source, String expression, Map<String, Argument> callingColumnArgs) {
        List<String> referenceNames = resolveFormulaReferences(expression);

        List<Reference> results = new ArrayList<>();
        Map<String, Argument> fixedArguments = new HashMap<>();

        for (String referenceName : referenceNames) {

            // Change `sql from='joinName' column='columnName[a1:v1][a2:v2]'` to `joinName.columnName`
            if (referenceName.startsWith(SQL_HELPER_PREFIX)) {
                try {
                    Template template = handlebars.compileInline(toFormulaReference(referenceName));
                    referenceName = template.apply(Collections.emptyMap());
                    int argsIndex = referenceName.indexOf('[');
                    if (argsIndex >= 0) {
                        fixedArguments = getArgumentMapFromString(referenceName.substring(argsIndex));
                        referenceName = referenceName.substring(0, argsIndex);
                    }
                } catch (IOException e) {
                    throw new IllegalStateException(e.getMessage());
                }
            }

            // ignore $$column.expr
            if (referenceName.equals(COLUMN_EXPR)) {
                continue;
            }

            if (referenceName.startsWith(COLUMN_ARGS_PREFIX)) {
                results.add(ColumnArgReference.builder()
                                .argName(referenceName.substring(COLUMN_ARGS_PREFIX.length()))
                                .build());
            } else if (referenceName.startsWith(TABLE_ARGS_PREFIX)) {
                results.add(TableArgReference.builder()
                                .argName(referenceName.substring(TABLE_ARGS_PREFIX.length()))
                                .build());
            } else if (referenceName.startsWith("$")) {
                results.add(PhysicalReference
                        .builder()
                        .source(source)
                        .name(referenceName.substring(1))
                        .build());
            } else if (referenceName.contains(".")) {
                results.add(buildJoin(source, referenceName, callingColumnArgs, fixedArguments));
            } else {
                ColumnProjection referencedColumn = source.getColumnProjection(referenceName);
                Preconditions.checkNotNull(referencedColumn, String.format("Couldn't find column: '%s' for table: '%s'",
                                referenceName, source.getName()));

                ColumnProjection newColumn = referencedColumn.withArguments(
                                mergedArgumentMap(referencedColumn.getArguments(),
                                                  callingColumnArgs,
                                                  fixedArguments));

                List<Reference> references = buildReferenceForColumn(source, newColumn);

                results.add(LogicalReference
                        .builder()
                        .source(source)
                        .column(newColumn)
                        .references(references)
                        .build());
            }
        }

        return results;
    }

    private List<Reference> buildReferenceForColumn(Queryable source, ColumnProjection column) {
        if (column.getColumnType() == ColumnType.FIELD) {
            return Arrays.asList(PhysicalReference
                    .builder()
                    .source(source)
                    .name(column.getName())
                    .build());
        }
        return parse(source, column);
    }

    private JoinReference buildJoin(Queryable source, String referenceName, Map<String, Argument> callingColumnArgs,
                    Map<String, Argument> fixedArguments) {

        Queryable root = source.getRoot();
        Type<?> tableClass = dictionary.getEntityClass(root.getName(), root.getVersion());

        JoinPath joinPath = new JoinPath(tableClass, metaDataStore, referenceName);

        Path.PathElement lastElement = joinPath.lastElement().get();
        Queryable joinSource = metaDataStore.getTable(lastElement.getType());
        String fieldName = lastElement.getFieldName();

        Reference reference;
        if (fieldName.startsWith("$")) {
            reference = PhysicalReference
                    .builder()
                    .source(joinSource)
                    .name(fieldName.substring(1))
                    .build();
        } else {
            ColumnProjection referencedColumn = joinSource.getColumnProjection(fieldName);
            ColumnProjection newColumn = referencedColumn.withArguments(
                            mergedArgumentMap(referencedColumn.getArguments(),
                                              callingColumnArgs,
                                              fixedArguments));

            reference = LogicalReference
                            .builder()
                            .source(joinSource)
                            .column(newColumn)
                            .references(buildReferenceForColumn(joinSource, newColumn))
                            .build();
        }

        return JoinReference
                .builder()
                .path(joinPath)
                .source(source)
                .reference(reference)
                .build();
    }

    /**
     * Use regex to get all references from a formula expression.
     *
     * @param expression expression
     * @return references appear in the expression.
     */
    private static List<String> resolveFormulaReferences(String expression) {
        List<String> references = new ArrayList<>();
        if (! isEmpty(expression)) {
            Matcher matcher = REFERENCE_PARENTHESES.matcher(expression);
            while (matcher.find()) {
                references.add(matcher.group(1).trim());
            }
        }

        return references;
    }

    /**
     * Convert a resolved formula reference back to a reference presented in formula format.
     *
     * @param reference referenced field
     * @return formula reference, <code>{{reference}}</code>
     */
    private static String toFormulaReference(String reference) {
        return "{{" + reference + "}}";
    }
}
