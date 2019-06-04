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
import com.yahoo.elide.datastores.aggregation.metadata.models.Dimension;
import com.yahoo.elide.datastores.aggregation.metadata.models.Metric;

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
        this.dictionary = metaDataStore.getDictionary();
    }

    public final Column getColumn(Class<?> tableClass, String fieldName) {
        return metaDataStore.getColumn(tableClass, fieldName);
    }

    public final Column getColumn(Path path) {
        return metaDataStore.getColumn(path);
    }

    /**
     * There are 2 types of {@link Metric} and 3 types of {@link Dimension}. Based on different column type, call the
     * corresponding method to parse it.
     *
     * @param column meta data column
     * @return output value
     */
    public final T visitColumn(Column column)  {
        switch (column.getColumnType()) {
            case FIELD:
                if (column instanceof Metric) {
                    return visitFieldMetric((Metric) column);
                } else {
                    return visitFieldDimension((Dimension) column);
                }
            case REFERENCE:
                return visitReferenceDimension((Dimension) column);
            case FORMULA:
                if (column instanceof Metric) {
                    return visitFormulaMetric((Metric) column);
                } else {
                    return visitFormulaDimension((Dimension) column);
                }
            default:
                return null;
        }
    }

    protected T visitPhysicalReference(String reference) {
        return null;
    }

    protected abstract T visitFieldMetric(Metric metric);

    protected abstract T visitFormulaMetric(Metric metric);

    protected abstract T visitFieldDimension(Dimension dimension);

    protected abstract T visitReferenceDimension(Dimension dimension);

    protected abstract T visitFormulaDimension(Dimension dimension);


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
