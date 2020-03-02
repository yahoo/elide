/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata;

import static com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore.resolveFormulaReferences;
import static com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore.toFormulaReference;
import static com.yahoo.elide.datastores.aggregation.metadata.models.LabelResolver.resolveReference;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.datastores.aggregation.core.JoinPath;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.metadata.models.LabelResolver;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.DimensionFormula;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.JoinTo;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface SQLColumn {
    Table getTable();

    String getName();

    String getReference();

    List<JoinPath> getJoinPaths();

    /**
     * SQL Column would resolve references in {@link DimensionFormula} when constructing physical reference.
     *
     * @return {@link LabelResolver} for sql columns
     */
    default LabelResolver sqlColumnLabelResolver() {
        return new LabelResolver() {
            @Override
            public <T> T resolveLabel(JoinPath fromPath,
                                      Set<JoinPath> toResolve,
                                      Map<JoinPath, T> resolved,
                                      LabelGenerator<T> generator,
                                      MetaDataStore metaDataStore) {
                EntityDictionary dictionary = metaDataStore.getDictionary();
                Class<?> tableClass = dictionary.getEntityClass(getTable().getId());
                String fieldName = getName();

                DimensionFormula formula = dictionary.getAttributeOrRelationAnnotation(
                        tableClass, DimensionFormula.class, fieldName);

                if (formula == null) {
                    JoinTo joinTo = dictionary.getAttributeOrRelationAnnotation(tableClass, JoinTo.class, fieldName);

                    if (joinTo == null || joinTo.path().equals("")) {
                        // the initial reference is the physical column reference
                        return generator.apply(fromPath, dictionary.getAnnotatedColumnName(tableClass, fieldName));
                    } else {
                        return resolveReference(
                                fromPath, tableClass, joinTo.path(), toResolve, resolved, generator, metaDataStore);
                    }
                } else {
                    String expression = formula.value();

                    // dimension references are deduplicated
                    List<String> references =
                            resolveFormulaReferences(expression).stream().distinct().collect(Collectors.toList());

                    // store resolved reference sql statements
                    Map<String, T> resolvedReferences = references.stream()
                            .collect(Collectors.toMap(
                                    Function.identity(),
                                    reference -> reference.indexOf('.') == -1
                                            && dictionary.getParameterizedType(tableClass, reference) == null
                                            ? generator.apply(fromPath, reference)
                                            : resolveReference(
                                                    fromPath,
                                                    tableClass,
                                                    reference,
                                                    toResolve,
                                                    resolved,
                                                    generator,
                                                    metaDataStore)));

                    for (String reference : references) {
                        expression = expression.replace(
                                toFormulaReference(reference), resolvedReferences.get(reference).toString());
                    }

                    return generator.apply(expression);
                }
            }
        };
    }
}
