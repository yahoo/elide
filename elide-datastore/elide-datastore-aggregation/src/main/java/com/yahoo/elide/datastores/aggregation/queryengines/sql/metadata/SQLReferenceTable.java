/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata;

import static com.yahoo.elide.datastores.aggregation.core.JoinPath.extendJoinPath;
import static com.yahoo.elide.datastores.aggregation.queryengines.sql.SQLQueryEngine.getClassAlias;
import static com.yahoo.elide.utils.TypeHelper.extendTypeAlias;
import static com.yahoo.elide.utils.TypeHelper.getFieldAlias;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.datastores.aggregation.annotation.DimensionFormula;
import com.yahoo.elide.datastores.aggregation.annotation.JoinTo;
import com.yahoo.elide.datastores.aggregation.annotation.MetricAggregation;
import com.yahoo.elide.datastores.aggregation.annotation.MetricFormula;
import com.yahoo.elide.datastores.aggregation.core.JoinPath;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.metadata.models.Column;
import com.yahoo.elide.datastores.aggregation.metadata.models.Dimension;
import com.yahoo.elide.datastores.aggregation.metadata.models.Metric;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * LabelStore stores all label resolvers, resolved physical reference, resolved join paths for all columns.
 */
public class SQLReferenceTable {
    private static final Pattern REFERENCE_PARENTHESES = Pattern.compile("\\{\\{(.+?)}}");

    @Getter
    private final EntityDictionary dictionary;

    private final Map<Class<?>, Map<String, SQLReferenceResolver>> resolvers = new HashMap<>();
    private final Map<Class<?>, Map<String, String>> resolvedReferences = new HashMap<>();
    private final Map<Class<?>, Map<String, Set<JoinPath>>> resolvedJoinPaths = new HashMap<>();

    public SQLReferenceTable(MetaDataStore metaDataStore) {
        this.dictionary = metaDataStore.getDictionary();

        metaDataStore.getMetaData(Table.class).forEach(this::constructAndAddReferenceResolvers);
        validateResolvers();
        metaDataStore.getMetaData(Table.class).forEach(this::resolveAndStoreAllReferencesAndJoins);
    }

    /**
     * Get the resolver for a class and field name. If the field name doesn't have a corresponding resolver object, it
     * would be treated as a physical column and the resolver would simply append this field name to table alias.
     *
     * @param tableClass table class
     * @param fieldName field name
     * @return reference resolver for this field
     */
    private SQLReferenceResolver getReferenceResolver(Class<?> tableClass, String fieldName) {
        if (!resolvers.containsKey(tableClass) || !resolvers.get(tableClass).containsKey(fieldName)) {
            return constructPhysicalColumnResolver(tableClass, fieldName);
        }

        return resolvers.get(tableClass).get(fieldName);
    }

    /**
     * Get the label resolver for a path.
     *
     * @param path path to a logical field
     * @return reference resolver for this field
     */
    private SQLReferenceResolver getReferenceResolver(Path path) {
        Path.PathElement last = path.lastElement().get();

        return getReferenceResolver(last.getType(), last.getFieldName());
    }

    /**
     * Resolve physical reference for a field in a table.
     *
     * @param tableClass table class
     * @param fieldName field name
     * @param tableAlias alias of table
     * @return resolved physical reference as <code>tableAlias + fieldAlias</code>
     */
    private String resolveReference(Class<?> tableClass, String fieldName, String tableAlias) {
        return getReferenceResolver(tableClass, fieldName).resolveReference(this, tableAlias);
    }

    /**
     * Resolve physical reference for path.
     *
     * @param path path to a logical field
     * @param tableAlias alias of source table
     * @return resolved physical reference as <code>tableAlias + pathAlias</code>
     */
    public String resolveReference(Path path, String tableAlias) {
        return getReferenceResolver(path).resolveReference(this, tableAlias);
    }

    /**
     * Resolve all joins needed for a field in a table.
     *
     * @param tableClass table class
     * @param fieldName field name
     * @param from path from original class to this field
     * @return all needed joins
     */
    private Set<JoinPath> resolveJoinPaths(Class<?> tableClass, String fieldName, JoinPath from) {
        return getReferenceResolver(tableClass, fieldName).resolveJoinPaths(this, from);
    }

    /**
     * Resolve all joins needed for a path
     *
     * @param path path to a field
     * @param from path from original class to this field
     * @return all needed joins
     */
    private Set<JoinPath> resolveJoinPaths(Path path, JoinPath from) {
        return getReferenceResolver(path).resolveJoinPaths(this, from);
    }

    /**
     * Get the resolved physical SQL reference for a field from storage
     *
     * @param table table class
     * @param fieldName field name
     * @return resolved reference
     */
    public String getResolvedReference(Table table, String fieldName) {
        return resolvedReferences.get(dictionary.getEntityClass(table.getId())).get(fieldName);
    }

    /**
     * Get the resolved physical SQL reference for a field from storage
     *
     * @param table table class
     * @param fieldName field name
     * @return resolved reference
     */
    public Set<JoinPath> getResolvedJoinPaths(Table table, String fieldName) {
        return resolvedJoinPaths.get(dictionary.getEntityClass(table.getId())).get(fieldName);
    }

    /**
     * Construct reference resolvers for all columns in a {@link Table} and store them.
     *
     * @param table meta data table
     */
    private void constructAndAddReferenceResolvers(Table table) {
        Class<?> tableClass = dictionary.getEntityClass(table.getId());
        if (!resolvers.containsKey(tableClass)) {
            resolvers.put(tableClass, new HashMap<>());
        }

        resolvers.get(tableClass).putAll(
                table.getColumns().stream()
                        .collect(Collectors.toMap(
                                Column::getName,
                                column -> constructReferenceResolver(tableClass, column))));
    }

    /**
     * Construct reference resolver for a {@link Column}
     *
     * @param tableClass table class
     * @param column meta data column
     * @return constructed resolver
     */
    private SQLReferenceResolver constructReferenceResolver(Class<?> tableClass, Column column) {
        if (column instanceof Metric) {
            return constructMetricColumnResolver(tableClass, (Metric) column);
        } else {
            return constructDimensionColumnResolver(tableClass, (Dimension) column);
        }
    }

    /**
     * Construct reference resolver for a {@link Metric} column
     *
     * @param tableClass table class
     * @param metric meta data metric
     * @return constructed resolver
     */
    private SQLReferenceResolver constructMetricColumnResolver(Class<?> tableClass, Metric metric) {
        String fieldName = metric.getName();

        MetricAggregation aggregation = dictionary.getAttributeOrRelationAnnotation(
                tableClass,
                MetricAggregation.class,
                fieldName);

        if (aggregation != null) {
            return constructMetricAggregationResolver(tableClass, metric);
        } else {
            MetricFormula formula = dictionary.getAttributeOrRelationAnnotation(
                    tableClass,
                    MetricFormula.class,
                    fieldName);

            return constructFormulaResolver(metric, tableClass, formula.value());
        }
    }

    /**
     * Build a resolver for {@link MetricAggregation} metric field
     *
     * @param tableClass table class
     * @param metric a metric column
     * @return a resolver
     */
    private SQLReferenceResolver constructMetricAggregationResolver(Class<?> tableClass, Metric metric) {
        return new SQLReferenceResolver(metric) {
            @Override
            public String resolveReference(SQLReferenceTable referenceTable, String tableAlias) {
                return String.format(
                        metric.getMetricFunction().getExpression(),
                        getFieldAlias(
                                tableAlias,
                                referenceTable.getDictionary().getAnnotatedColumnName(
                                        tableClass,
                                        metric.getName())));
            }
        };
    }

    /**
     * Construct reference resolver for a {@link Dimension} column
     *
     * @param tableClass table class
     * @param dimension meta data dimension
     * @return constructed resolver
     */
    private SQLReferenceResolver constructDimensionColumnResolver(Class<?> tableClass, Dimension dimension) {
        String fieldName = dimension.getName();

        DimensionFormula formula = dictionary.getAttributeOrRelationAnnotation(
                tableClass, DimensionFormula.class, fieldName);

        if (formula == null) {
            JoinTo joinTo = dictionary.getAttributeOrRelationAnnotation(tableClass, JoinTo.class, fieldName);

            return joinTo == null || joinTo.path().equals("")
                    ? constructLogicalColumnResolver(tableClass, dimension)
                    : constructJoinToResolver(tableClass, dimension, joinTo);
        } else {
            return constructFormulaResolver(dimension, tableClass, formula.value());
        }
    }

    /**
     * Get a physical column reference resolver
     *
     * @param tableClass table class
     * @param reference physical column reference
     * @return a resolver
     */
    private static SQLReferenceResolver constructPhysicalColumnResolver(Class<?> tableClass, String reference) {
        return new SQLReferenceResolver(null) {
            @Override
            public String resolveReference(SQLReferenceTable referenceTable, String tableAlias) {
                return getFieldAlias(
                        tableAlias,
                        referenceTable.getDictionary().getAnnotatedColumnName(tableClass, reference));
            }
        };
    }

    /**
     * Get a logical column reference resolver
     *
     * @param tableClass table class
     * @param dimension a dimension column
     * @return a resolver
     */
    private static SQLReferenceResolver constructLogicalColumnResolver(Class<?> tableClass, Dimension dimension) {
        return new SQLReferenceResolver(dimension) {
            @Override
            public String resolveReference(SQLReferenceTable referenceTable, String tableAlias) {
                return getFieldAlias(
                        tableAlias,
                        referenceTable.getDictionary().getAnnotatedColumnName(tableClass, dimension.getName()));
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
    private static SQLReferenceResolver constructJoinToResolver(Class<?> tableClass,
                                                                Dimension dimension,
                                                                JoinTo joinTo) {
        return new SQLReferenceResolver(dimension) {
            @Override
            public Set<JoinPath> resolveJoinPaths(SQLReferenceTable referenceTable, JoinPath from) {
                JoinPath to = getJoinToPath(referenceTable);

                return referenceTable.resolveJoinPaths(to, extendJoinPath(from, to));
            }

            @Override
            public Set<SQLReferenceResolver> getDependencyResolvers(SQLReferenceTable referenceTable) {
                return Collections.singleton(
                        referenceTable.getReferenceResolver(getJoinToPath(referenceTable)));
            }

            @Override
            public String resolveReference(SQLReferenceTable referenceTable, String tableAlias) {
                JoinPath joinToPath = getJoinToPath(referenceTable);

                return referenceTable.resolveReference(joinToPath, extendTypeAlias(tableAlias, joinToPath));
            }

            private JoinPath getJoinToPath(SQLReferenceTable referenceTable) {
                return new JoinPath(tableClass, referenceTable.getDictionary(), joinTo.path());
            }
        };
    }

    /**
     * Get a {@link DimensionFormula} or {@link MetricFormula} reference resolver.
     *
     * @param column column that this resolver is built for
     * @param tableClass table class
     * @param expression formula expression contains physical column, logical column and {@link JoinTo} paths
     * @return a resolver
     */
    private static SQLReferenceResolver constructFormulaResolver(Column column,
                                                                 Class<?> tableClass,
                                                                 String expression) {
        // dimension references are deduplicated
        List<String> references =
                resolveFormulaReferences(expression).stream().distinct().collect(Collectors.toList());

        return new SQLReferenceResolver(column) {
            @Override
            public Set<JoinPath> resolveJoinPaths(SQLReferenceTable referenceTable, JoinPath from) {
                return references.stream()
                        .map(reference -> {
                            if (reference.contains(".")) {
                                JoinPath to = getJoinToPath(referenceTable, reference);

                                return referenceTable.resolveJoinPaths(to, extendJoinPath(from, to)).stream();
                            }

                            return referenceTable.resolveJoinPaths(tableClass, reference, from).stream();
                        })
                        .reduce(Stream.empty(), Stream::concat)
                        .collect(Collectors.toSet());
            }

            @Override
            public Set<SQLReferenceResolver> getDependencyResolvers(SQLReferenceTable referenceTable) {
                return references.stream()
                        .map(reference -> {
                            return reference.contains(".")
                                    ? referenceTable.getReferenceResolver(
                                            getJoinToPath(referenceTable, reference))
                                    : referenceTable.getReferenceResolver(tableClass, reference);

                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
            }

            @Override
            public String resolveReference(SQLReferenceTable referenceTable, String tableAlias) {
                String expr = expression;

                // replace references with resolved statements/expressions
                for (String reference : references) {
                    String resolvedReference;

                    if (reference.contains(".")) {
                        JoinPath joinPath = getJoinToPath(referenceTable, reference);

                        resolvedReference = referenceTable.resolveReference(
                                joinPath,
                                extendTypeAlias(tableAlias, joinPath));
                    } else {
                        resolvedReference = referenceTable.resolveReference(tableClass, reference, tableAlias);
                    }

                    expr = expr.replace(toFormulaReference(reference), resolvedReference);
                }

                return expr;
            }

            private JoinPath getJoinToPath(SQLReferenceTable referenceTable, String reference) {
                return new JoinPath(tableClass, referenceTable.getDictionary(), reference);
            }
        };
    }

    /**
     * Check and make sure there is no reference loop in resolvers.
     */
    private void validateResolvers() {
        resolvers.forEach(
                (tableClass, resolverMap) -> resolverMap.forEach(
                        (fieldName, resolver) -> resolver.checkResolverLoop(this)));
    }

    /**
     * Resolve all references and joins for a table and store them in this reference table.
     *
     * @param table meta data table
     */
    private void resolveAndStoreAllReferencesAndJoins(Table table) {
        Class<?> tableClass = dictionary.getEntityClass(table.getId());
        if (!resolvedReferences.containsKey(tableClass)) {
            resolvedReferences.put(tableClass, new HashMap<>());
        }
        if (!resolvedJoinPaths.containsKey(tableClass)) {
            resolvedJoinPaths.put(tableClass, new HashMap<>());
        }

        table.getColumns().forEach(column -> {
            String fieldName = column.getName();
            JoinPath rootPath = new JoinPath(tableClass, dictionary, fieldName);

            resolvedReferences.get(tableClass).put(
                    fieldName,
                    resolveReference(tableClass, fieldName, getClassAlias(tableClass)));
            resolvedJoinPaths.get(tableClass).put(
                    fieldName,
                    resolveJoinPaths(tableClass, fieldName, rootPath));

            if (column instanceof Metric) {
                ((Metric) column).getMetricFunction().setExpression(getResolvedReference(table, fieldName));
            }
        });
    }

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
    public static String toFormulaReference(String reference) {
        return "{{" + reference + "}}";
    }
}
