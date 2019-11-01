/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata;

import static com.yahoo.elide.datastores.aggregation.AggregationDictionary.isAnalyticView;

import com.yahoo.elide.core.datastore.inmemory.HashMapDataStore;
import com.yahoo.elide.core.exceptions.DuplicateMappingException;
import com.yahoo.elide.datastores.aggregation.AggregationDataStore;
import com.yahoo.elide.datastores.aggregation.AggregationDictionary;
import com.yahoo.elide.datastores.aggregation.metadata.models.AnalyticView;
import com.yahoo.elide.datastores.aggregation.metadata.models.Dimension;
import com.yahoo.elide.datastores.aggregation.metadata.models.Metric;
import com.yahoo.elide.datastores.aggregation.metadata.models.MetricFunction;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;

import java.util.Set;

/**
 * MetaDataStore is a in-memory data store that manage data models for an {@link AggregationDataStore}.
 */
public class MetaDataStore extends HashMapDataStore {

    public MetaDataStore() {
        super(MetaDataStore.class.getPackage());
    }

    /**
     * Populate this meta data store with data models in an aggregation data store.
     *
     * @param dictionary entity dictionary used by an aggregation data store.
     */
    public void storeMetaData(AggregationDictionary dictionary) {
        Set<Class<?>> classes = dictionary.getBindings();

        classes.forEach(cls -> storeMetaData(
                isAnalyticView(cls)
                        ? new AnalyticView(cls, dictionary)
                        : new Table(cls, dictionary)));
    }

    /**
     * Add a meta data object into this data store.
     *
     * @param object a meta data object
     */
    private void storeMetaData(Object object) {
        Class<?> cls = object.getClass();
        String id = getDictionary().getId(object);

        if (dataStore.get(cls).containsKey(id)) {
            if (!dataStore.get(cls).get(id).equals(object)) {
                throw new DuplicateMappingException("Duplicated " + cls.getSimpleName() + " metadata " + id);
            }
        }

        dataStore.get(cls).put(id, object);

        if (object instanceof Table) {
            ((Table) object).getColumns().forEach(this::storeMetaData);
        } else if (object instanceof Dimension) {
            storeMetaData(((Dimension) object).getDataType());
        } else if (object instanceof Metric) {
            storeMetaData(((Metric) object).getDataType());
            ((Metric) object).getSupportedFunctions().forEach(this::storeMetaData);
        } else if (object instanceof MetricFunction) {
            ((MetricFunction) object).getArguments().forEach(this::storeMetaData);
        }
    }
}
