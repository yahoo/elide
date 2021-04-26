/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata;

import static com.yahoo.elide.core.utils.TypeHelper.getClassType;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.datastore.inmemory.HashMapDataStore;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.exceptions.DuplicateMappingException;
import com.yahoo.elide.core.exceptions.InternalServerErrorException;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.core.utils.ClassScanner;
import com.yahoo.elide.datastores.aggregation.AggregationDataStore;
import com.yahoo.elide.datastores.aggregation.annotation.Join;
import com.yahoo.elide.datastores.aggregation.annotation.MetricFormula;
import com.yahoo.elide.datastores.aggregation.dynamic.TableType;
import com.yahoo.elide.datastores.aggregation.metadata.models.Argument;
import com.yahoo.elide.datastores.aggregation.metadata.models.Column;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimension;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimensionGrain;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromSubquery;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromTable;
import org.hibernate.annotations.Subselect;
import lombok.Getter;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.persistence.Entity;

/**
 * MetaDataStore is a in-memory data store that manage data models for an {@link AggregationDataStore}.
 */
public class MetaDataStore implements DataStore {
    private static final Package META_DATA_PACKAGE = Table.class.getPackage();

    private static final List<Class<? extends Annotation>> METADATA_STORE_ANNOTATIONS =
            Arrays.asList(FromTable.class, FromSubquery.class, Subselect.class, javax.persistence.Table.class,
                    javax.persistence.Entity.class);

    private static final Function<String, HashMapDataStore> SERVER_ERROR = key -> {
        throw new InternalServerErrorException("API version " + key + " not found");
    };

    @Getter
    private final Set<Type<?>> modelsToBind;

    @Getter
    private boolean enableMetaDataStore = false;

    private Map<Type<?>, Table> tables = new HashMap<>();

    @Getter
    private EntityDictionary metadataDictionary = new EntityDictionary(new HashMap<>());

    @Getter
    private Map<String, HashMapDataStore> hashMapDataStores = new HashMap<>();

    private final Set<Class<?>> metadataModelClasses;

    public MetaDataStore(Collection<com.yahoo.elide.modelconfig.model.Table> tables, boolean enableMetaDataStore) {
        this(getClassType(getAllAnnotatedClasses()), enableMetaDataStore);

        Map<String, Type<?>> typeMap = new HashMap<>();
        Set<String> joinNames = new HashSet<>();
        Set<Type<?>> dynamicTypes = new HashSet<>();

        //Convert tables into types.
        tables.stream().forEach(table -> {
            TableType tableType = new TableType(table);
            dynamicTypes.add(tableType);
            typeMap.put(table.getName(), tableType);
            table.getJoins().stream().forEach(join -> {
                joinNames.add(join.getTo());
            });
        });

        //Built a list of static types referenced from joins in the dynamic types.
        metadataDictionary.getBindings().stream()
                .filter(binding -> joinNames.contains(binding.getJsonApiType()))
                .forEach(staticType -> {
                    typeMap.put(staticType.getJsonApiType(), staticType.getEntityClass());
                });

        //Resolve the join fields & bind the dynamic types.
        dynamicTypes.stream().forEach(table -> {
            ((TableType) table).resolveJoins(typeMap);
            String version = EntityDictionary.getModelVersion(table);
            HashMapDataStore hashMapDataStore = hashMapDataStores.computeIfAbsent(version,
                    getHashMapDataStoreInitializer());
            hashMapDataStore.getDictionary().bindEntity(table, Collections.singleton(Join.class));
            this.metadataDictionary.bindEntity(table, Collections.singleton(Join.class));
            this.modelsToBind.add(table);
            this.hashMapDataStores.putIfAbsent(version, hashMapDataStore);
        });
    }

    public MetaDataStore(boolean enableMetaDataStore) {
        this(getClassType(getAllAnnotatedClasses()), enableMetaDataStore);
    }

    /**
     * get all MetaDataStore supported annotated classes.
     * @return Set of Class with specific annotations.
     */
    private static Set<Class<?>> getAllAnnotatedClasses() {
        return ClassScanner.getAnnotatedClasses(METADATA_STORE_ANNOTATIONS, (clazz) -> {
            return clazz.getAnnotation(Entity.class) == null
                    || clazz.getAnnotation(Include.class) != null;
        });
    }

    public Set<Type<?>> getDynamicTypes() {
        return modelsToBind.stream()
                .filter(type -> type instanceof TableType)
                .collect(Collectors.toSet());
    }

    /**
     * Construct MetaDataStore with data models.
     *
     * @param modelsToBind models to bind
     */
    public MetaDataStore(Set<Type<?>> modelsToBind, boolean enableMetaDataStore) {
        this.metadataModelClasses = ClassScanner.getAllClasses(META_DATA_PACKAGE.getName());
        this.enableMetaDataStore = enableMetaDataStore;

        modelsToBind.forEach(cls -> {
            String version = EntityDictionary.getModelVersion(cls);
            HashMapDataStore hashMapDataStore = hashMapDataStores.computeIfAbsent(version,
                    getHashMapDataStoreInitializer());
            hashMapDataStore.getDictionary().bindEntity(cls, Collections.singleton(Join.class));
            this.metadataDictionary.bindEntity(cls, Collections.singleton(Join.class));
            this.hashMapDataStores.putIfAbsent(version, hashMapDataStore);
        });

        // bind external data models in the package.
        this.modelsToBind = modelsToBind;
    }

    @Override
    public void populateEntityDictionary(EntityDictionary dictionary) {
        if (enableMetaDataStore) {
            metadataModelClasses.forEach(
                cls -> dictionary.bindEntity(cls, Collections.singleton(Join.class))
            );
        }
    }

    private final Function<String, HashMapDataStore> getHashMapDataStoreInitializer() {
        return key -> {
            HashMapDataStore hashMapDataStore = new HashMapDataStore(META_DATA_PACKAGE);
            EntityDictionary dictionary = new EntityDictionary(new HashMap<>());
            metadataModelClasses.forEach(dictionary::bindEntity);
            hashMapDataStore.populateEntityDictionary(dictionary);
            return hashMapDataStore;
        };
    }

    /**
     * Add a table metadata object.
     *
     * @param table table metadata
     */
    public void addTable(Table table) {
        String version = table.getVersion();
        EntityDictionary dictionary = hashMapDataStores.computeIfAbsent(version, SERVER_ERROR).getDictionary();
        tables.put(dictionary.getEntityClass(table.getName(), version), table);
        addMetaData(table, version);
        table.getColumns().forEach(this::addColumn);

        table.getArguments().forEach(arg -> addArgument(arg, version));
    }

    /**
     * Get a table metadata object.
     *
     * @param tableClass table class
     * @return meta data table
     */
    public <T extends Table> T getTable(Type<?> tableClass) {
        return (T) tables.get(tableClass);
    }

    /**
     * Returns the table for a given name and version.
     * @param name The name of the table
     * @param version The version of the table.
     * @return The table that matches or null.
     */
    public Table getTable(String name, String version) {
        return tables.values().stream()
                .filter(table -> table.getName().equals(name) && table.getVersion().equals(version))
                .findFirst()
                .orElse(null);
    }

    /**
     * Get a {@link Column} from a table.
     *
     * @param tableClass table class
     * @param fieldName field name
     * @return meta data column
     */
    public final Column getColumn(Type<?> tableClass, String fieldName) {
        return getTable(tableClass).getColumnMap().get(fieldName);
    }

    /**
     * Get a {@link Column} for the last field in a {@link Path}.
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
            TimeDimension timeDimension = (TimeDimension) column;
            for (TimeDimensionGrain grain : timeDimension.getSupportedGrains()) {
                addTimeDimensionGrain(grain, version);
            }
        }

        column.getArguments().forEach(arg -> addArgument(arg, version));
    }

    /**
     * Add a argument metadata object.
     *
     * @param argument argument metadata
     */
    private void addArgument(Argument argument, String version) {
        addMetaData(argument, version);
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

        HashMapDataStore hashMapDataStore = hashMapDataStores.computeIfAbsent(version, SERVER_ERROR);
        EntityDictionary dictionary = hashMapDataStore.getDictionary();
        Type<?> cls = dictionary.lookupBoundClass(EntityDictionary.getType(object));
        String id = dictionary.getId(object);

        if (hashMapDataStore.get(cls).containsKey(id)) {
            if (!hashMapDataStore.get(cls).get(id).equals(object)) {
                throw new DuplicateMappingException("Duplicated " + cls.getSimpleName() + " metadata " + id);
            }
        } else {
            hashMapDataStore.get(cls).put(id, object);
        }
    }

    /**
     * Get all metadata of a specific metadata class.
     *
     * @param cls metadata class
     * @param <T> metadata class
     * @return all metadata of given class
     */
    public <T> Set<T> getMetaData(Type<T> cls) {
        String version = EntityDictionary.getModelVersion(cls);
        HashMapDataStore hashMapDataStore = hashMapDataStores.computeIfAbsent(version, SERVER_ERROR);
        return hashMapDataStore.get(cls).values().stream().map(obj -> (T) obj).collect(Collectors.toSet());
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
    public static boolean isMetricField(EntityDictionary dictionary, Type<?> cls, String fieldName) {
        return dictionary.attributeOrRelationAnnotationExists(cls, fieldName, MetricFormula.class);
    }

    @Override
    public DataStoreTransaction beginTransaction() {
        return new MetaDataStoreTransaction(hashMapDataStores);
    }
}
