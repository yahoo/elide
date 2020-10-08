/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata;

import com.yahoo.elide.contrib.dynamicconfighelpers.compile.ElideDynamicEntityCompiler;
import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.datastore.inmemory.HashMapDataStore;
import com.yahoo.elide.core.exceptions.DuplicateMappingException;
import com.yahoo.elide.datastores.aggregation.AggregationDataStore;
import com.yahoo.elide.datastores.aggregation.annotation.Join;
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
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * MetaDataStore is a in-memory data store that manage data models for an {@link AggregationDataStore}.
 */
public class MetaDataStore implements DataStore {
    private static final Package META_DATA_PACKAGE = Table.class.getPackage();

    private static final List<Class<? extends Annotation>> METADATA_STORE_ANNOTATIONS =
            Arrays.asList(FromTable.class, FromSubquery.class, Subselect.class, javax.persistence.Table.class);

    public static final Function<String, HashMapDataStore> ERROR_OUT = new Function<String, HashMapDataStore>() {
        @Override
        public HashMapDataStore apply(String key) {
            throw new IllegalStateException("API version " + key + " not found");
        }
    };

    private static final Function<String, HashMapDataStore> SETUP_NEW = new Function<String, HashMapDataStore>() {
        @Override
        public HashMapDataStore apply(String key) {
            HashMapDataStore hashMapDataStore = new HashMapDataStore(META_DATA_PACKAGE);
            EntityDictionary dictionary = new EntityDictionary(new HashMap<>());
            ClassScanner.getAllClasses(META_DATA_PACKAGE.getName()).forEach(dictionary::bindEntity);
            hashMapDataStore.populateEntityDictionary(dictionary);
            return hashMapDataStore;
        }
    };

    @Getter
    private final Set<Class<?>> modelsToBind;

    private Map<Class<?>, Table> tables = new HashMap<>();

    @Getter
    private EntityDictionary dictionary = new EntityDictionary(new HashMap<>());

    @Getter
    private Map<String, HashMapDataStore> hashMapDataStores = new HashMap<>();

    public MetaDataStore() {
        this(ClassScanner.getAnnotatedClasses(METADATA_STORE_ANNOTATIONS));
    }

    public MetaDataStore(ElideDynamicEntityCompiler compiler) throws ClassNotFoundException {
        this();

        Set<Class<?>> dynamicCompiledClasses = compiler.findAnnotatedClasses(FromTable.class);
        dynamicCompiledClasses.addAll(compiler.findAnnotatedClasses(FromSubquery.class));

        if (dynamicCompiledClasses != null && dynamicCompiledClasses.size() != 0) {
            dynamicCompiledClasses.forEach(cls -> {
                String version = EntityDictionary.getModelVersion(cls);
                HashMapDataStore hashMapDataStore = hashMapDataStores.computeIfAbsent(version, SETUP_NEW);
                hashMapDataStore.getDictionary().bindEntity(cls, Collections.singleton(Join.class));
                this.dictionary.bindEntity(cls, Collections.singleton(Join.class));
                this.modelsToBind.add(cls);
                this.hashMapDataStores.putIfAbsent(version, hashMapDataStore);
            });
        }
    }

    /**
     * Construct MetaDataStore with data models.
     *
     * @param modelsToBind models to bind
     */
    public MetaDataStore(Set<Class<?>> modelsToBind) {

        modelsToBind.forEach(cls -> {
            String version = EntityDictionary.getModelVersion(cls);
            HashMapDataStore hashMapDataStore = hashMapDataStores.computeIfAbsent(version, SETUP_NEW);
            hashMapDataStore.getDictionary().bindEntity(cls, Collections.singleton(Join.class));
            this.dictionary.bindEntity(cls, Collections.singleton(Join.class));
            this.hashMapDataStores.putIfAbsent(version, hashMapDataStore);
        });

        // bind external data models in the package.
        this.modelsToBind = modelsToBind;
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
        String version = table.getVersion();
        EntityDictionary dictionary = hashMapDataStores.get(version).getDictionary();
        tables.put(dictionary.getEntityClass(table.getName(), version), table);
        addMetaData(table, version);
        table.getColumns().forEach(this::addColumn);
    }

    /**
     * Get a table metadata object
     *
     * @param tableClass table class
     * @return meta data table
     */
    public Table getTable(Class<?> tableClass) {
        return tables.get(tableClass);
    }

    /**
     * Get a {@link Column} from a table.
     *
     * @param tableClass table class
     * @param fieldName field name
     * @return meta data column
     */
    public final Column getColumn(Class<?> tableClass, String fieldName) {
        return getTable(tableClass).getColumnMap().get(fieldName);
    }

    /**
     * Get a {@link Column} for the last field in a {@link Path}
     *
     * @param path path to a field
     * @return meta data column
     */
    public final Column getColumn(Path path) {
        Path.PathElement last = path.lastElement().get();

        return getColumn(last.getType(), last.getFieldName());
    }

    /**
     * Add a column metadata object.
     *
     * @param column column metadata
     */
    private void addColumn(Column column) {
        String version = column.getVersion();
        addMetaData(column, version);

        if (column instanceof TimeDimension) {
            addTimeDimensionGrain(((TimeDimension) column).getSupportedGrain(), version);
        } else if (column instanceof Metric) {
            addMetricFunction(((Metric) column).getMetricFunction(), version);
        }
    }

    /**
     * Add a metric function metadata object.
     *
     * @param metricFunction metric function metadata
     */
    private void addMetricFunction(MetricFunction metricFunction, String version) {
        addMetaData(metricFunction, version);
        metricFunction.getArguments().forEach(arg -> addFunctionArgument(arg, version));
    }

    /**
     * Add a function argument metadata object.
     *
     * @param functionArgument function argument metadata
     */
    private void addFunctionArgument(FunctionArgument functionArgument, String version) {
        addMetaData(functionArgument, version);
    }

    /**
     * Add a time dimension grain metadata object.
     *
     * @param timeDimensionGrain time dimension grain metadata
     */
    private void addTimeDimensionGrain(TimeDimensionGrain timeDimensionGrain, String version) {
        addMetaData(timeDimensionGrain, version);
    }

    /**
     * Add a meta data object into this data store, check for duplication.
     *
     * @param object a meta data object
     */
    private void addMetaData(Object object, String version) {

        HashMapDataStore hashMapDataStore = hashMapDataStores.computeIfAbsent(version, ERROR_OUT);
        EntityDictionary dictionary = hashMapDataStore.getDictionary();
        Map<Class<?>, Map<String, Object>> dataStore = hashMapDataStore.getStorage();
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

        String version = EntityDictionary.getModelVersion(cls);
        HashMapDataStore hashMapDataStore = hashMapDataStores.computeIfAbsent(version, ERROR_OUT);
        Map<Class<?>, Map<String, Object>> dataStore = hashMapDataStore.getStorage();
        return dataStore.get(cls).values().stream().map(cls::cast).collect(Collectors.toSet());
    }

    /**
     * Returns whether or not an entity field is a metric field.
     * <p>
     * A field is a metric field if that field is annotated by
     * <ol>
     *     <li> {@link MetricFormula}
     * </ol>
     *
     * @param dictionary entity dictionary in current Elide instance
     * @param cls entity class
     * @param fieldName The entity field
     *
     * @return {@code true} if the field is a metric field
     */
    public static boolean isMetricField(EntityDictionary dictionary, Class<?> cls, String fieldName) {
        return dictionary.attributeOrRelationAnnotationExists(cls, fieldName, MetricFormula.class);
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

    @Override
    public DataStoreTransaction beginTransaction() {
        return new MetaDataStoreTransaction(hashMapDataStores);
    }
}
