/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.expression;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.datastores.aggregation.core.JoinPath;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ColumnType;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.Queryable;

import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a column or join expression and returns the reference abstract syntax tree for each discovered
 * reference.
 */
public class ExpressionParser {

    private static final Pattern REFERENCE_PARENTHESES = Pattern.compile("\\{\\{(.+?)}}");

    private MetaDataStore metaDataStore;
    private EntityDictionary dictionary;

    public ExpressionParser(MetaDataStore store) {
        this.dictionary = store.getMetadataDictionary();
        this.metaDataStore = store;
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
            if (referenceName.startsWith("$$")) {
                continue;
            }
            if (referenceName.startsWith("$")) {
                results.add(PhysicalReference
                        .builder()
                        .name(referenceName.substring(1))
                        .build());
            } else if (referenceName.contains(".")) {
                results.add(buildJoin(source, referenceName));
            } else {
                Reference reference = buildReferenceFromField(source, referenceName);
                results.add(LogicalReference
                        .builder()
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
                    .name(column.getName())
                    .build();
        } else {
            return LogicalReference
                    .builder()
                    .column(column)
                    .references(parse(source, column.getExpression()))
                    .build();
        }
    }

    private JoinReference buildJoin(Queryable source, String referenceName) {
        Queryable root = source.getRoot();
        Type<?> tableClass = dictionary.getEntityClass(root.getName(), root.getVersion());

        JoinPath joinPath = new JoinPath(tableClass, metaDataStore, referenceName);

        Path.PathElement lastElement = joinPath.lastElement().get();
        String fieldName = lastElement.getFieldName();

        Reference reference;
        if (fieldName.startsWith("$")) {
            reference = PhysicalReference
                    .builder()
                    .name(fieldName.substring(1))
                    .build();
        } else {
            Queryable joinSource = metaDataStore.getTable(lastElement.getType());
            reference = buildReferenceFromField(joinSource, fieldName);
        }

        return JoinReference
                .builder()
                .path(joinPath)
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
}
