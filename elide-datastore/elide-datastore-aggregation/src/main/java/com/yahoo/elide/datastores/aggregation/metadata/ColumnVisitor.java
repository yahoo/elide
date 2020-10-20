/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ColumnType;
import com.yahoo.elide.datastores.aggregation.metadata.models.Column;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.MetricProjection;

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

    private final MetaDataStore metaDataStore;
    protected final EntityDictionary dictionary;

    public ColumnVisitor(MetaDataStore metaDataStore) {
        this.metaDataStore = metaDataStore;
        this.dictionary = metaDataStore.getMetadataDictionary();
    }

    public final ColumnProjection getColumn(Path path) {
        Column column = metaDataStore.getColumn(path);
        if (column != null) {
            return column.getTable().toProjection(column);
        } else {
            // Assumption: Must be a Physical Column
            return null;
        }
    }

    public final Column getColumn(Class<?> fromClass, String fieldName) {
        return metaDataStore.getColumn(fromClass, fieldName);
    }

    public final String getFieldName(Path path) {
        return metaDataStore.getFieldName(path);
    }

    /**
     * Visits a column.
     *
     * @param column meta data column
     * @return output value
     */
    public final T visitColumn(ColumnProjection column)  {
        if (column instanceof MetricProjection) {
            return visitFormulaMetric((MetricProjection) column);
        } else {
            if (column.getColumnType() == ColumnType.FORMULA) {
                return visitFormulaDimension(column);
            } else {
                return visitFieldDimension(column);
            }
        }
    }


    protected T visitPhysicalReference(String reference) {
        return null;
    }

    protected abstract T visitFormulaMetric(MetricProjection metric);

    protected abstract T visitFormulaDimension(ColumnProjection dimension);

    protected abstract T visitFieldDimension(ColumnProjection dimension);

    /**
     * Use regex to get all references from a formula expression.
     *
     * @param formula formula expression
     * @return references appear in the formula.
     */
    protected static List<String> resolveFormulaReferences(String formula) {
        Matcher matcher = REFERENCE_PARENTHESES.matcher(formula);
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
    protected static String toFormulaReference(String reference) {
        return "{{" + reference + "}}";
    }
}
