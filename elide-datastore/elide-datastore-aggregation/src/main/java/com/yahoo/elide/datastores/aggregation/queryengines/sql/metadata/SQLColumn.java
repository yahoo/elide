/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata;

import static com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore.resolveFormulaReferences;
import static com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore.toFormulaReference;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.datastores.aggregation.core.JoinPath;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.metadata.models.Column;
import com.yahoo.elide.datastores.aggregation.metadata.models.LabelResolver;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.SQLQueryEngine;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.DimensionFormula;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.JoinTo;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * SQLColumn is a wrapper for {@link Column} that contains physical information for {@link SQLQueryEngine}.
 */
public interface SQLColumn {
    /**
     * Get wrapped column
     *
     * @return column
     */
    Column getColumn();

    /**
     * Get full sql reference
     *
     * @return physical reference
     */
    String getReference();

    /**
     * Get join paths needed for this column
     *
     * @return all join paths to this column
     */
    List<JoinPath> getJoinPaths();

    /**
     * SQL Column would resolve references in {@link DimensionFormula} when constructing physical reference.
     *
     * @return {@link LabelResolver} for sql columns
     */
    default LabelResolver constructSQLColumnLabelResolver(EntityDictionary dictionary) {
        Class<?> tableClass = dictionary.getEntityClass(getColumn().getTable().getId());
        String fieldName = getColumn().getName();

        DimensionFormula formula = dictionary.getAttributeOrRelationAnnotation(
                tableClass, DimensionFormula.class, fieldName);

        if (formula == null) {
            JoinTo joinTo = dictionary.getAttributeOrRelationAnnotation(tableClass, JoinTo.class, fieldName);

            return joinTo == null || joinTo.path().equals("")
                    ? getLogicalColumnResolver(tableClass, fieldName)
                    : getJoinToResolver(tableClass, joinTo);
        } else {
            return getFormulaResolver(tableClass, formula);
        }
    }

    /**
     * A partial implemented {@link LabelResolver} for all sql columns
     */
    abstract class SqlColumnLabelResolver extends LabelResolver {
        private SqlColumnLabelResolver(SQLColumn column) {
            super(column.getColumn());
        }
    }

    /**
     * Get a logical column reference resolver
     *
     * @param tableClass table class
     * @param fieldName field name
     * @return a resolver
     */
    default SqlColumnLabelResolver getLogicalColumnResolver(Class<?> tableClass, String fieldName) {
        return new SqlColumnLabelResolver(this) {
            @Override
            public <T> T resolveLabel(JoinPath fromPath,
                                      Map<JoinPath, T> resolved,
                                      LabelGenerator<T> generator,
                                      MetaDataStore metaDataStore) {
                return generator.apply(
                        fromPath, metaDataStore.getDictionary().getAnnotatedColumnName(tableClass, fieldName));
            }
        };
    }

    /**
     * Get a {@link JoinTo} reference resolver.
     *
     * @param tableClass table class
     * @param joinTo join to path
     * @return a resolver
     */
    default SqlColumnLabelResolver getJoinToResolver(Class<?> tableClass, JoinTo joinTo) {
        return new SqlColumnLabelResolver(this) {
            @Override
            public Set<LabelResolver> getDependencyResolvers(MetaDataStore metaDataStore) {
                return Collections.singleton(
                        metaDataStore.getLabelResolver(
                                new JoinPath(tableClass, metaDataStore.getDictionary(), joinTo.path())));
            }

            @Override
            public <T> T resolveLabel(JoinPath fromPath,
                                      Map<JoinPath, T> resolved,
                                      LabelGenerator<T> generator,
                                      MetaDataStore metaDataStore) {
                return resolveReference(fromPath, tableClass, joinTo.path(), resolved, generator, metaDataStore);
            }
        };
    }

    /**
     * Get a {@link DimensionFormula} reference resolver.
     *
     * @param tableClass table class
     * @param formula formula contains physical column, logical column and {@link JoinTo} paths
     * @return a resolver
     */
    default SqlColumnLabelResolver getFormulaResolver(Class<?> tableClass, DimensionFormula formula) {
        final String expression = formula.value();

        // dimension references are deduplicated
        List<String> references =
                resolveFormulaReferences(expression).stream().distinct().collect(Collectors.toList());

        return new SqlColumnLabelResolver(this) {
            @Override
            public Set<LabelResolver> getDependencyResolvers(MetaDataStore metaDataStore) {
                EntityDictionary dictionary = metaDataStore.getDictionary();

                return references.stream()
                        .map(ref -> {
                            // physical columns don't have dependency resolvers
                            if (!ref.contains(".") && dictionary.getParameterizedType(tableClass, ref) == null) {
                                return null;
                            }

                            return metaDataStore.getLabelResolver(new JoinPath(tableClass, dictionary, ref));
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
            }

            @Override
            public <T> T resolveLabel(JoinPath fromPath,
                                      Map<JoinPath, T> resolved,
                                      LabelGenerator<T> generator,
                                      MetaDataStore metaDataStore) {
                EntityDictionary dictionary = metaDataStore.getDictionary();
                String expr = expression;

                // store resolved reference sql statements
                Map<String, T> resolvedReferences = references.stream()
                        .collect(Collectors.toMap(
                                Function.identity(),
                                reference -> reference.indexOf('.') == -1
                                        && dictionary.getParameterizedType(tableClass, reference) == null
                                        ? generator.apply(fromPath, reference) // if the column is physical
                                        : resolveReference(
                                                fromPath,
                                                tableClass,
                                                reference,
                                                resolved,
                                                generator,
                                                metaDataStore)));

                // replace references with resolved statements/expressions
                for (String reference : references) {
                    expr = expr.replace(toFormulaReference(reference), resolvedReferences.get(reference).toString());
                }

                return generator.apply(expr);
            }
        };
    }
}
