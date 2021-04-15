/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.expression;

import static com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable.PERIOD;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.datastores.aggregation.core.JoinPath;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ColumnType;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.Queryable;
import com.github.jknack.handlebars.EscapingStrategy;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import com.github.jknack.handlebars.Template;
import com.google.common.base.Preconditions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a column or join expression and returns the reference abstract syntax tree for each discovered
 * reference.
 */
public class ExpressionParser {

    private static final Pattern REFERENCE_PARENTHESES = Pattern.compile("\\{\\{(.+?)}}");
    private static final String SQL_HELPER_PREFIX = "sql ";

    private MetaDataStore metaDataStore;
    private EntityDictionary dictionary;
    private final Handlebars handlebars = new Handlebars()
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
                            column = isEmpty(from) ? column : from + PERIOD + column;

                            return column;
                        }
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
        return parse(source, column.getExpression());
    }

    /**
     * Parses the column or join expression and returns the list of discovered references.
     * @param source The source table where the column or join expression lives.
     * @param expression The expression to parse.
     * @return A list of discovered references.
     */
    public List<Reference> parse(Queryable source, String expression) {
        List<String> referenceNames = resolveFormulaReferences(expression);

        List<Reference> results = new ArrayList<>();

        for (String referenceName : referenceNames) {

            // Change `sql from='joinName' column='columnName[a1:v1][a2:v2]'` to `joinName.columnName`
            if (referenceName.startsWith(SQL_HELPER_PREFIX)) {
                try {
                    Template template = handlebars.compileInline(toFormulaReference(referenceName));
                    referenceName = template.apply(Collections.emptyMap());
                } catch (IOException e) {
                    throw new IllegalStateException(e.getMessage());
                }
            }

            if (referenceName.startsWith("$$")) {
                continue;
            }
            if (referenceName.startsWith("$")) {
                results.add(PhysicalReference
                        .builder()
                        .source(source)
                        .name(referenceName.substring(1))
                        .build());
            } else if (referenceName.contains(".")) {
                results.add(buildJoin(source, referenceName));
            } else {
                Reference reference = buildReferenceFromField(source, referenceName);
                results.add(LogicalReference
                        .builder()
                        .source(source)
                        .column(source.getColumnProjection(referenceName))
                        .reference(reference)
                        .build());
            }
        }

        return results;
    }

    private Reference buildReferenceFromField(Queryable source, String fieldName) {
        ColumnProjection column = source.getColumnProjection(fieldName);

        Preconditions.checkNotNull(column);

        if (column.getColumnType() == ColumnType.FIELD) {
            return PhysicalReference
                    .builder()
                    .source(source)
                    .name(column.getName())
                    .build();
        } else {
            return LogicalReference
                    .builder()
                    .source(source)
                    .column(column)
                    .references(parse(source, column))
                    .build();
        }
    }

    private JoinReference buildJoin(Queryable source, String referenceName) {
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
            reference = buildReferenceFromField(joinSource, fieldName);
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
        Matcher matcher = REFERENCE_PARENTHESES.matcher(expression);
        List<String> references = new ArrayList<>();

        while (matcher.find()) {
            references.add(matcher.group(1));
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
