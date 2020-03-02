/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.datastore.inmemory.HashMapDataStore;
import com.yahoo.elide.core.exceptions.DuplicateMappingException;
import com.yahoo.elide.datastores.aggregation.AggregationDataStore;
import com.yahoo.elide.datastores.aggregation.annotation.Join;
import com.yahoo.elide.datastores.aggregation.annotation.MetricAggregation;
import com.yahoo.elide.datastores.aggregation.annotation.MetricFormula;
import com.yahoo.elide.datastores.aggregation.core.JoinPath;
import com.yahoo.elide.datastores.aggregation.metadata.models.Column;
import com.yahoo.elide.datastores.aggregation.metadata.models.FunctionArgument;
import com.yahoo.elide.datastores.aggregation.metadata.models.LabelResolver;
import com.yahoo.elide.datastores.aggregation.metadata.models.LabelResolver.LabelGenerator;
import com.yahoo.elide.datastores.aggregation.metadata.models.Metric;
import com.yahoo.elide.datastores.aggregation.metadata.models.MetricFunction;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimension;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimensionGrain;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromSubquery;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromTable;
import com.yahoo.elide.utils.ClassScanner;

import org.hibernate.annotations.Subselect;

import lombok.Getter;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * MetaDataStore is a in-memory data store that manage data models for an {@link AggregationDataStore}.
 */
public class MetaDataStore extends HashMapDataStore {
    private static final Package META_DATA_PACKAGE = Table.class.getPackage();
    private static final Pattern REFERENCE_PARENTHESES = Pattern.compile("\\{\\{(.+?)}}");

    public static final List<Class<? extends Annotation>> METADATA_STORE_ANNOTATIONS =
            Arrays.asList(FromTable.class, FromSubquery.class, Subselect.class, javax.persistence.Table.class);

    @Getter
    private final Set<Class<?>> modelsToBind;

    public MetaDataStore(Set<Class<?>> modelsToBind) {
        super(META_DATA_PACKAGE);

        this.dictionary = new EntityDictionary(new HashMap<>());

        // bind meta data models to dictionary
        ClassScanner.getAllClasses(Table.class.getPackage().getName()).forEach(dictionary::bindEntity);

        // bind external data models in the package
        this.modelsToBind = modelsToBind;
        this.modelsToBind.forEach(cls -> dictionary.bindEntity(cls, Collections.singleton(Join.class)));
    }

    @Override
    public void populateEntityDictionary(EntityDictionary dictionary) {
        ClassScanner.getAllClasses(META_DATA_PACKAGE.getName())
                .forEach(cls -> dictionary.bindEntity(cls, Collections.singleton(Join.class)));
    }

    /**
     * Add a table metadata object.
     *
     * @param table table metadata
     */
    public void addTable(Table table) {
        addMetaData(table);
        table.getColumns().forEach(this::addColumn);
    }

    /**
     * Add a column metadata object.
     *
     * @param column column metadata
     */
    private void addColumn(Column column) {
        addMetaData(column);

        if (column instanceof TimeDimension) {
            ((TimeDimension) column).getSupportedGrains().forEach(this::addTimeDimensionGrain);
        } else if (column instanceof Metric) {
            addMetricFunction(((Metric) column).getMetricFunction());
        }
    }

    /**
     * Add a metric function metadata object.
     *
     * @param metricFunction metric function metadata
     */
    private void addMetricFunction(MetricFunction metricFunction) {
        addMetaData(metricFunction);
        metricFunction.getArguments().forEach(this::addFunctionArgument);
    }

    /**
     * Add a function argument metadata object.
     *
     * @param functionArgument function argument metadata
     */
    private void addFunctionArgument(FunctionArgument functionArgument) {
        addMetaData(functionArgument);
    }

    /**
     * Add a time dimension grain metadata object.
     *
     * @param timeDimensionGrain time dimension grain metadata
     */
    private void addTimeDimensionGrain(TimeDimensionGrain timeDimensionGrain) {
        addMetaData(timeDimensionGrain);
    }

    /**
     * Add a meta data object into this data store, check for duplication.
     *
     * @param object a meta data object
     */
    private void addMetaData(Object object) {
        Class<?> cls = dictionary.lookupBoundClass(object.getClass());
        String id = dictionary.getId(object);

        if (dataStore.get(cls).containsKey(id)) {
            if (!dataStore.get(cls).get(id).equals(object)) {
                throw new DuplicateMappingException("Duplicated " + cls.getSimpleName() + " metadata " + id);
            }
        } else {
            dataStore.get(cls).put(id, object);
        }
    }

    /**
     * Get all metadata of a specific metadata class
     *
     * @param cls metadata class
     * @param <T> metadata class
     * @return all metadata of given class
     */
    public <T> Set<T> getMetaData(Class<T> cls) {
        return dataStore.get(cls).values().stream().map(cls::cast).collect(Collectors.toSet());
    }

    /**
     * Returns whether or not an entity field is a metric field.
     * <p>
     * A field is a metric field iff that field is annotated by at least one of
     * <ol>
     *     <li> {@link MetricAggregation}
     * </ol>
     *
     * @param dictionary entity dictionary in current Elide instance
     * @param cls entity class
     * @param fieldName The entity field
     *
     * @return {@code true} if the field is a metric field
     */
    public static boolean isMetricField(EntityDictionary dictionary, Class<?> cls, String fieldName) {
        return dictionary.attributeOrRelationAnnotationExists(cls, fieldName, MetricAggregation.class)
                || dictionary.attributeOrRelationAnnotationExists(cls, fieldName, MetricFormula.class);
    }

    /**
     * Returns whether a field in a table/entity is actually a JOIN to other table/entity.
     *
     * @param cls table/entity class
     * @param fieldName field name
     * @param dictionary metadata dictionary
     * @return True if this field is a table join
     */
    public static boolean isTableJoin(Class<?> cls, String fieldName, EntityDictionary dictionary) {
        return dictionary.getAttributeOrRelationAnnotation(cls, Join.class, fieldName) != null;
    }

    /**
     * Construct a column name as meta data
     *
     * @param tableClass table class
     * @param fieldName field name
     * @param dictionary entity dictionary to use
     * @return <code>tableAlias.fieldName</code>
     */
    public static String constructColumnName(Class<?> tableClass, String fieldName, EntityDictionary dictionary) {
        return dictionary.getJsonAliasFor(tableClass) + "." + fieldName;
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

    /**
     * Resolve source columns for all Columns in all Tables.
     */
    public void resolveSourceColumn() {
        getMetaData(Table.class).forEach(table ->
                table.getColumns().forEach(column -> {
                    Path sourcePath = column.getSourcePath(dictionary);
                    Path.PathElement source = sourcePath.lastElement().get();

                    Table sourceTable = (Table) dataStore.get(Table.class)
                            .get(dictionary.getJsonAliasFor(source.getType()));

                    Column sourceColumn = column instanceof Metric
                            ? sourceTable.getMetric(source.getFieldName())
                            : sourceTable.getDimension(source.getFieldName());
                    column.setSourceColumn(sourceColumn);
                })
        );
    }

    /**
     * Get the label resolver for a field
     *
     * @param tableClass table class
     * @param fieldName field name
     * @return a label resolver
     */
    private LabelResolver getLabelResolver(Class<?> tableClass, String fieldName) {
        return ((Table) dataStore.get(Table.class).get(dictionary.getJsonAliasFor(tableClass)))
                .getColumnMap()
                .get(fieldName)
                .getLabelResolver();
    }

    /**
     * Resolve the label for field navigated by the path.
     *
     * @param path path to the field
     * @param toResolve paths that are pending resolving
     * @param resolved resolved paths
     * @param generator generator to construct labels
     * @param <T> label value type
     * @return resolved label
     */
    public <T> T resolveLabel(JoinPath path,
                              Set<JoinPath> toResolve,
                              Map<JoinPath, T> resolved,
                              LabelGenerator<T> generator) {
        if (resolved.containsKey(path)) {
            return resolved.get(path);
        }

        Path.PathElement last = path.lastElement().get();

        return getLabelResolver(last.getType(), last.getFieldName())
                .resolveLabel(path, toResolve, resolved, generator, this);
    }

    /**
     * Resolve the label for field navigated by the path.
     *
     * @param path path to the field
     * @param generator generator to construct labels
     * @param <T> label value type
     * @return resolved label
     */
    public <T> T resolveLabel(JoinPath path, LabelGenerator<T> generator) {
        return resolveLabel(path, new LinkedHashSet<>(), new LinkedHashMap<>(), generator);
    }

    /**
     * Resolve all column references in all tables.
     */
    public void resolveReference() {
        getMetaData(Table.class)
                .forEach(table -> table.getColumns()
                        .forEach(column -> column.resolveReference(this)));
    }
}
