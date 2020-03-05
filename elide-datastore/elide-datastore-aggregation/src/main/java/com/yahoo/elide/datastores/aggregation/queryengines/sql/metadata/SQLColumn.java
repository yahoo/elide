/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata;

import static com.yahoo.elide.datastores.aggregation.core.JoinPath.extendJoinPath;
import static com.yahoo.elide.utils.TypeHelper.extendTypeAlias;
import static com.yahoo.elide.utils.TypeHelper.getFieldAlias;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.datastores.aggregation.core.JoinPath;
import com.yahoo.elide.datastores.aggregation.metadata.LabelResolver;
import com.yahoo.elide.datastores.aggregation.metadata.LabelStore;
import com.yahoo.elide.datastores.aggregation.metadata.models.Column;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.SQLQueryEngine;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.DimensionFormula;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.JoinTo;

import java.util.Collections;
import java.util.List;
import java.util.Set;

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
     * Get a logical column reference resolver
     *
     * @param tableClass table class
     * @param fieldName field name
     * @return a resolver
     */
    default LabelResolver getLogicalColumnResolver(Class<?> tableClass, String fieldName) {
        return new LabelResolver(getColumn()) {
            @Override
            public String resolveLabel(LabelStore labelStore, String tableAlias) {
                return getFieldAlias(
                        tableAlias,
                        labelStore.getDictionary().getAnnotatedColumnName(tableClass, fieldName));
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
    default LabelResolver getJoinToResolver(Class<?> tableClass, JoinTo joinTo) {
        return new LabelResolver(getColumn()) {
            @Override
            public Set<JoinPath> resolveJoinPaths(LabelStore labelStore, JoinPath from) {
                JoinPath to = getJoinToPath(labelStore);

                return labelStore.getLabelResolver(to).resolveJoinPaths(labelStore, extendJoinPath(from, to));
            }

            @Override
            public Set<LabelResolver> getDependencyResolvers(LabelStore labelStore) {
                return Collections.singleton(labelStore.getLabelResolver(getJoinToPath(labelStore)));
            }

            @Override
            public String resolveLabel(LabelStore labelStore, String tableAlias) {
                JoinPath joinToPath = getJoinToPath(labelStore);

                return labelStore.resolveLabel(joinToPath, extendTypeAlias(tableAlias, joinToPath));
            }

            private JoinPath getJoinToPath(LabelStore labelStore) {
                return new JoinPath(tableClass, labelStore.getDictionary(), joinTo.path());
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
    default LabelResolver getFormulaResolver(Class<?> tableClass, DimensionFormula formula) {
        return LabelResolver.getFormulaResolver(getColumn(), tableClass, formula.value());
    }
}
