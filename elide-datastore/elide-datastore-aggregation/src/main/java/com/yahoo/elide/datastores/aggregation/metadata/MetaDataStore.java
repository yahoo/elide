/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata;

import static com.yahoo.elide.datastores.aggregation.AggregationDictionary.isAnalyticView;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.datastore.inmemory.HashMapDataStore;
import com.yahoo.elide.core.exceptions.DuplicateMappingException;
import com.yahoo.elide.datastores.aggregation.AggregationDataStore;
import com.yahoo.elide.datastores.aggregation.AggregationDictionary;
import com.yahoo.elide.datastores.aggregation.metadata.models.AnalyticView;
import com.yahoo.elide.datastores.aggregation.metadata.models.Column;
import com.yahoo.elide.datastores.aggregation.metadata.models.DataType;
import com.yahoo.elide.datastores.aggregation.metadata.models.FunctionArgument;
import com.yahoo.elide.datastores.aggregation.metadata.models.Metric;
import com.yahoo.elide.datastores.aggregation.metadata.models.MetricFunction;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimension;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimensionGrain;

import java.util.Set;

/**
 * MetaDataStore is a in-memory data store that manage data models for an {@link AggregationDataStore}.
 */
public class MetaDataStore extends HashMapDataStore {
    private static final Package MODEL_PACKAGE =
            Package.getPackage("com.yahoo.elide.datastores.aggregation.metadata.models");

    public MetaDataStore() {
        super(MODEL_PACKAGE);
    }

    public void populateEntityDictionary(EntityDictionary dictionary) {
        super.populateEntityDictionary(dictionary);

        if (dictionary instanceof AggregationDictionary) {
            LoadMetaData((AggregationDictionary) dictionary);
        }
    }

    /**
     * Load meta data of models from an populated entity dictionary.
     *
     * @param dictionary entity dictionary used by an aggregation data store.
     */
    public void LoadMetaData(AggregationDictionary dictionary) {
        Set<Class<?>> classes = dictionary.getBindings();

        classes.stream()
                .filter(cls -> !MODEL_PACKAGE.equals(cls.getPackage()))
                .forEach(cls -> AddTable(
                        isAnalyticView(cls)
                                ? new AnalyticView(cls, dictionary)
                                : new Table(cls, dictionary)));
    }

    /**
     * Add a table metadata object
     *
     * @param table table metadata
     */
    private void AddTable(Table table) {
        AddMetaData(table);
        table.getColumns().forEach(this::AddColumn);
    }

    /**
     * Add a column metadata object
     *
     * @param column column metadata
     */
    private void AddColumn(Column column) {
        AddMetaData(column);
        AddDataType(column.getDataType());

        if (column instanceof TimeDimension) {
            ((TimeDimension) column).getSupportedGrains().forEach(this::AddTimeDimensionGrain);
        } else if (column instanceof Metric) {
            ((Metric) column).getSupportedFunctions().forEach(this::AddMetricFunction);
        }
    }

    /**
     * Add a metric function metadata object
     *
     * @param metricFunction metric function metadata
     */
    private void AddMetricFunction(MetricFunction metricFunction) {
        AddMetaData(metricFunction);
        metricFunction.getArguments().forEach(this::AddFunctionArgument);
    }

    /**
     * Add a datatype metadata object
     *
     * @param dataType datatype metadata
     */
    private void AddDataType(DataType dataType) {
        AddMetaData(dataType);
    }

    /**
     * Add a function argument metadata object
     *
     * @param functionArgument function argument metadata
     */
    private void AddFunctionArgument(FunctionArgument functionArgument) {
        AddMetaData(functionArgument);
    }

    /**
     * Add a time dimension grain metadata object
     *
     * @param timeDimensionGrain time dimension grain metadata
     */
    private void AddTimeDimensionGrain(TimeDimensionGrain timeDimensionGrain) {
        AddMetaData(timeDimensionGrain);
    }

    /**
     * Add a meta data object into this data store, check for duplication
     *
     * @param object a meta data object
     */
    private void AddMetaData(Object object) {
        Class<?> cls = object.getClass();
        String id = getDictionary().getId(object);

        if (dataStore.get(cls).containsKey(id)) {
            if (!dataStore.get(cls).get(id).equals(object)) {
                throw new DuplicateMappingException("Duplicated " + cls.getSimpleName() + " metadata " + id);
            }
        } else {
            dataStore.get(cls).put(id, object);
        }
    }
}
