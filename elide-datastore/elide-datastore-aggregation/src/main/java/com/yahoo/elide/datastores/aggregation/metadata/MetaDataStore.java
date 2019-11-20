/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.datastore.inmemory.HashMapDataStore;
import com.yahoo.elide.core.exceptions.DuplicateMappingException;
import com.yahoo.elide.datastores.aggregation.AggregationDataStore;
import com.yahoo.elide.datastores.aggregation.annotation.MetricAggregation;
import com.yahoo.elide.datastores.aggregation.metadata.models.AnalyticView;
import com.yahoo.elide.datastores.aggregation.metadata.models.Column;
import com.yahoo.elide.datastores.aggregation.metadata.models.DataType;
import com.yahoo.elide.datastores.aggregation.metadata.models.FunctionArgument;
import com.yahoo.elide.datastores.aggregation.metadata.models.Metric;
import com.yahoo.elide.datastores.aggregation.metadata.models.MetricFunction;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimension;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimensionGrain;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromSubquery;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromTable;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * MetaDataStore is a in-memory data store that manage data models for an {@link AggregationDataStore}.
 */
public class MetaDataStore extends HashMapDataStore {
    public static final Package META_DATA_PACKAGE =
            Package.getPackage("com.yahoo.elide.datastores.aggregation.metadata.models");

    public MetaDataStore() {
        super(META_DATA_PACKAGE);
    }

    public void populateEntityDictionary(EntityDictionary dictionary) {
        super.populateEntityDictionary(dictionary);
    }

    /**
     * Load meta data of models from an external populated entity dictionary.
     *
     * @param dictionary entity dictionary used by an aggregation data store.
     */
    public void loadMetaData(EntityDictionary dictionary) {
        Set<Class<?>> classes = dictionary.getBindings();

        classes.stream()
                .filter(cls -> !META_DATA_PACKAGE.equals(cls.getPackage()))
                .forEach(cls -> addTable(
                        isAnalyticView(cls)
                                ? new AnalyticView(cls, dictionary)
                                : new Table(cls, dictionary)));
    }

    /**
     * Add a table metadata object.
     *
     * @param table table metadata
     */
    private void addTable(Table table) {
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
        addDataType(column.getDataType());

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
     * Add a datatype metadata object.
     *
     * @param dataType datatype metadata
     */
    private void addDataType(DataType dataType) {
        addMetaData(dataType);
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
        Class<?> cls = this.getDictionary().lookupEntityClass(object.getClass());
        String id = getDictionary().getId(object);

        if (dataStore.get(cls).containsKey(id)) {
            if (!dataStore.get(cls).get(id).equals(object)) {
                throw new DuplicateMappingException("Duplicated " + cls.getSimpleName() + " metadata " + id);
            }
        } else {
            dataStore.get(cls).put(id, object);
        }
    }

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
        return dictionary.attributeOrRelationAnnotationExists(cls, fieldName, MetricAggregation.class);
    }

    /**
     * Returns whether an entity class is analytic view
     *
     * @param cls entity class
     * @return True if {@link FromTable} or {@link FromSubquery} is presented.
     */
    private static boolean isAnalyticView(Class<?> cls) {
        return cls.isAnnotationPresent(FromTable.class) || cls.isAnnotationPresent(FromSubquery.class);
    }
}
