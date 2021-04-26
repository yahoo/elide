/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ColumnType;
import com.yahoo.elide.datastores.aggregation.metadata.models.Column;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.MetricProjection;
import com.yahoo.elide.datastores.aggregation.query.Queryable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ColumnVisitor is an abstract class that parses different types of column based on column's {@link ColumnType}.
 *
 * @param <T> output value type
 */
public abstract class ColumnVisitor<T> {
    private static final Pattern REFERENCE_PARENTHESES = Pattern.compile("\\{\\{(.+?)}}");

    protected final MetaDataStore metaDataStore;
    protected final EntityDictionary dictionary;

    public ColumnVisitor(MetaDataStore metaDataStore) {
        this.metaDataStore = metaDataStore;
        this.dictionary = metaDataStore.getMetadataDictionary();
    }

    public final Column getColumn(Path path) {
        return metaDataStore.getColumn(path);
    }

    /**
     * Visits a column.
     *
     * @param parent The queryable that owns the column
     * @param column meta data column
     * @return output value
     */
    public final T visitColumn(Queryable parent, ColumnProjection column)  {
        if (column instanceof MetricProjection) {
            return visitFormulaMetric(parent, (MetricProjection) column);
        } else {
            if (column.getColumnType() == ColumnType.FORMULA) {
                return visitFormulaDimension(parent, column);
            } else {
                return visitFieldDimension(parent, column);
            }
        }
    }


    protected T visitPhysicalReference(String reference) {
        return null;
    }

    protected abstract T visitFormulaMetric(Queryable parent, MetricProjection metric);

    protected abstract T visitFormulaDimension(Queryable parent, ColumnProjection dimension);

    protected abstract T visitFieldDimension(Queryable parent, ColumnProjection dimension);

    /**
     * Use regex to get all references from a formula expression.
     *
     * @param formula formula expression
     * @return references appear in the formula.
     */
    public static List<String> resolveFormulaReferences(String formula) {
        Matcher matcher = REFERENCE_PARENTHESES.matcher(formula);
        List<String> references = new ArrayList<>();

        while (matcher.find()) {
            String value = matcher.group(1);
            if (!value.startsWith("$$") && !value.startsWith("sql ")) {
                references.add(value);
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
    protected static String toFormulaReference(String reference) {
        return "{{" + reference + "}}";
    }
}
