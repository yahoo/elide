/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.datastore.inmemory.HashMapDataStore;
import com.yahoo.elide.core.exceptions.DuplicateMappingException;
import com.yahoo.elide.datastores.aggregation.AggregationDataStore;
import com.yahoo.elide.datastores.aggregation.annotation.Join;
import com.yahoo.elide.datastores.aggregation.annotation.MetricAggregation;
import com.yahoo.elide.datastores.aggregation.annotation.MetricFormula;
import com.yahoo.elide.datastores.aggregation.metadata.models.Column;
import com.yahoo.elide.datastores.aggregation.metadata.models.FunctionArgument;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * MetaDataStore is a in-memory data store that manage data models for an {@link AggregationDataStore}.
 */
public class MetaDataStore extends HashMapDataStore {
    private static final Package META_DATA_PACKAGE = Table.class.getPackage();

    private static final List<Class<? extends Annotation>> METADATA_STORE_ANNOTATIONS =
            Arrays.asList(FromTable.class, FromSubquery.class, Subselect.class, javax.persistence.Table.class);

    @Getter
    private final Set<Class<?>> modelsToBind;

    public MetaDataStore() {
        this(ClassScanner.getAnnotatedClasses(METADATA_STORE_ANNOTATIONS));
    }

    /**
     * Construct MetaDataStore with data models.
     *
     * @param modelsToBind models to bind
     */
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
}
