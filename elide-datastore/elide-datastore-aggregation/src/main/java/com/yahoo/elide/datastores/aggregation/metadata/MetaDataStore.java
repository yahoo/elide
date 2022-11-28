/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static com.yahoo.elide.core.utils.TypeHelper.getClassType;
import static com.yahoo.elide.datastores.aggregation.AggregationDataStore.IS_FIELD_HIDDEN;
import static com.yahoo.elide.datastores.aggregation.dynamic.NamespacePackage.DEFAULT;
import static com.yahoo.elide.datastores.aggregation.dynamic.NamespacePackage.DEFAULT_NAMESPACE;
import static com.yahoo.elide.datastores.aggregation.dynamic.NamespacePackage.EMPTY;

import com.yahoo.elide.annotation.ApiVersion;
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
import com.yahoo.elide.datastores.aggregation.annotation.MetricFormula;
import com.yahoo.elide.datastores.aggregation.dynamic.NamespacePackage;
import com.yahoo.elide.datastores.aggregation.dynamic.TableType;
import com.yahoo.elide.datastores.aggregation.metadata.models.ArgumentDefinition;
import com.yahoo.elide.datastores.aggregation.metadata.models.Column;
import com.yahoo.elide.datastores.aggregation.metadata.models.Dimension;
import com.yahoo.elide.datastores.aggregation.metadata.models.Metric;
import com.yahoo.elide.datastores.aggregation.metadata.models.Namespace;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import com.yahoo.elide.datastores.aggregation.metadata.models.TableSource;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimension;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimensionGrain;
import com.yahoo.elide.datastores.aggregation.metadata.models.Versioned;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromSubquery;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromTable;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.annotations.Subselect;

import jakarta.persistence.Entity;
import lombok.Getter;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
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
            Arrays.asList(FromTable.class, FromSubquery.class, Subselect.class, jakarta.persistence.Table.class,
                    jakarta.persistence.Entity.class);

    private static final Function<String, HashMapDataStore> SERVER_ERROR = key -> {
        throw new InternalServerErrorException("API version " + key + " not found");
    };

    @Getter
    private final Set<Type<?>> modelsToBind;

    private final Map<Pair<String, String>, NamespacePackage> namespacesToBind = new HashMap<>();

    @Getter
    private boolean enableMetaDataStore = false;

    private Map<Type<?>, Table> tables = new HashMap<>();
    private Set<Namespace> namespaces = new HashSet<>();

    @Getter
    private final EntityDictionary metadataDictionary;

    @Getter
    private Map<String, HashMapDataStore> hashMapDataStores = new HashMap<>();

    private final Set<Class<?>> metadataModelClasses;

    public MetaDataStore(
            ClassScanner scanner,
            Collection<com.yahoo.elide.modelconfig.model.Table> tables,
            boolean enableMetaDataStore) {
        this(scanner, tables, new HashSet<>(), enableMetaDataStore);
    }

    public MetaDataStore(ClassScanner scanner, Collection<com.yahoo.elide.modelconfig.model.Table> tables,
            Collection<com.yahoo.elide.modelconfig.model.NamespaceConfig> namespaceConfigs,
            boolean enableMetaDataStore) {
        this(scanner, getClassType(getAllAnnotatedClasses(scanner)), enableMetaDataStore);

        Map<String, Type<?>> typeMap = new HashMap<>();
        Set<String> joinNames = new HashSet<>();
        Set<Type<?>> dynamicTypes = new HashSet<>();

        Map<String, NamespacePackage> namespaceMap = new HashMap<>();
        //Convert namespaces into packages.

        namespaceConfigs.stream().forEach(namespace -> {
            NamespacePackage namespacePackage = new NamespacePackage(namespace);
            ApiVersion apiVersion = namespacePackage.getDeclaredAnnotation(ApiVersion.class);
            String apiVersionName = apiVersion != null ? apiVersion.version() : NO_VERSION;

            Pair<String, String> registration = Pair.of(namespacePackage.getName(), apiVersionName);
            namespacesToBind.put(registration, namespacePackage);
        });

        //Convert tables into types.
        tables.stream().forEach(table -> {

            //TODO - when table versions are added, use the table version.
            Pair<String, String> registration = Pair.of(table.getNamespace(), NO_VERSION);

            if (! namespacesToBind.containsKey(registration)) {
                if (table.getNamespace() != DEFAULT) {
                    throw new IllegalStateException("No matching namespace found: " + table.getNamespace());
                }

                registration = Pair.of(EMPTY, NO_VERSION);
                namespacesToBind.put(registration, DEFAULT_NAMESPACE);
            }

            TableType tableType = new TableType(table, namespacesToBind.get(registration));
            dynamicTypes.add(tableType);
            typeMap.put(table.getGlobalName(), tableType);
            table.getJoins().stream().forEach(join ->
                joinNames.add(join.getTo())
            );
        });

        //Built a list of static types referenced from joins in the dynamic types.
        metadataDictionary.getBindings().stream()
                .filter(binding -> joinNames.contains(binding.getJsonApiType()))
                .forEach(staticType ->
                    typeMap.put(staticType.getJsonApiType(), staticType.getEntityClass())
                );

        //Resolve the join fields & bind the dynamic types.
        dynamicTypes.stream().forEach(table -> {
            ((TableType) table).resolveJoins(typeMap);
            String version = EntityDictionary.getModelVersion(table);
            HashMapDataStore hashMapDataStore = hashMapDataStores.computeIfAbsent(version,
                    getHashMapDataStoreInitializer(scanner));
            hashMapDataStore.getDictionary().bindEntity(table, IS_FIELD_HIDDEN);
            this.metadataDictionary.bindEntity(table, IS_FIELD_HIDDEN);
            this.modelsToBind.add(table);
            this.hashMapDataStores.putIfAbsent(version, hashMapDataStore);
        });
    }

    public MetaDataStore(ClassScanner scanner, boolean enableMetaDataStore) {
        this(scanner, getClassType(getAllAnnotatedClasses(scanner)), enableMetaDataStore);
    }

    /**
     * get all MetaDataStore supported annotated classes.
     * @return Set of Class with specific annotations.
     */
    private static Set<Class<?>> getAllAnnotatedClasses(ClassScanner scanner) {
        return scanner.getAnnotatedClasses(METADATA_STORE_ANNOTATIONS,
                clazz -> clazz.getAnnotation(Entity.class) == null || clazz.getAnnotation(Include.class) != null);
    }

    public Set<Type<?>> getDynamicTypes() {
        return modelsToBind.stream()
                .filter(type -> type instanceof TableType)
                .collect(Collectors.toSet());
    }

    /**
     * Construct MetaDataStore with data models, namespaces.
     *
     * @param modelsToBind models to bind
     * @param enableMetaDataStore If Enable MetaDataStore
     */
    public MetaDataStore(ClassScanner scanner, Set<Type<?>> modelsToBind, boolean enableMetaDataStore) {

        metadataDictionary = EntityDictionary.builder()
                .scanner(scanner)
                .build();

        //Hardcoded to avoid ClassGraph scan.
        this.metadataModelClasses = new HashSet<>(Arrays.asList(
                Column.class,
                Metric.class,
                ArgumentDefinition.class,
                TableSource.class,
                Dimension.class,
                TimeDimension.class,
                TimeDimensionGrain.class,
                Table.class,
                Versioned.class,
                Namespace.class
        ));

        this.enableMetaDataStore = enableMetaDataStore;

        modelsToBind.forEach(cls -> {
            String version = EntityDictionary.getModelVersion(cls);
            HashMapDataStore hashMapDataStore = hashMapDataStores.computeIfAbsent(version,
                    getHashMapDataStoreInitializer(scanner));
            hashMapDataStore.getDictionary().bindEntity(cls, IS_FIELD_HIDDEN);
            this.metadataDictionary.bindEntity(cls, IS_FIELD_HIDDEN);
            this.hashMapDataStores.putIfAbsent(version, hashMapDataStore);

            Include include = (Include) EntityDictionary.getFirstPackageAnnotation(cls, Arrays.asList(Include.class));

            //Register all the default namespaces.
            if (include == null) {
                Pair<String, String> registration = Pair.of(EMPTY, version);
                namespacesToBind.put(registration,
                        new NamespacePackage(EMPTY, "Default Namespace", DEFAULT, version));
            } else {
                Pair<String, String> registration = Pair.of(include.name(), version);
                namespacesToBind.put(registration,
                        new NamespacePackage(
                                include.name(),
                                include.description(),
                                include.friendlyName(),
                                version));
            }
        });

        // bind external data models in the package.
        this.modelsToBind = modelsToBind;
    }

    @Override
    public void populateEntityDictionary(EntityDictionary dictionary) {
        if (enableMetaDataStore) {
            metadataModelClasses.forEach(
                cls -> dictionary.bindEntity(cls, IS_FIELD_HIDDEN)
            );
        }
    }

    private final Function<String, HashMapDataStore> getHashMapDataStoreInitializer(ClassScanner scanner) {
        return key -> {
            HashMapDataStore hashMapDataStore = new HashMapDataStore(metadataModelClasses);
            EntityDictionary dictionary = EntityDictionary.builder().scanner(scanner).build();
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
        if (! table.isHidden()) {
            addMetaData(table, version);
        }
        table.getAllColumns().stream()
                .forEach(this::addColumn);

        table.getArgumentDefinitions().forEach(arg -> addArgument(arg, version));
    }

    /**
     * Add a namespace metadata object.
     *
     * @param namespace Namespace metadata
     */
    public void addNamespace(Namespace namespace) {
        String version = namespace.getVersion();
        namespaces.add(namespace);
        addMetaData(namespace, version);
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
     * Get a namespace object.
     *
     * @param modelType the model type
     * @return the namespace
     */
    public Namespace getNamespace(Type<?> modelType) {
        String apiVersionName = EntityDictionary.getModelVersion(modelType);
        Include include = (Include) EntityDictionary.getFirstPackageAnnotation(modelType, Arrays.asList(Include.class));

        String namespaceName;
        if (include != null && ! include.name().isEmpty()) {
            namespaceName = include.name();
        } else {
            namespaceName = DEFAULT;
        }

        return namespaces
                 .stream()
                 .filter(namespace -> namespace.getName().equals(namespaceName))
                 .filter(namespace -> namespace.getVersion().equals(apiVersionName))
                 .findFirst()
                 .orElse(null);
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
     * Returns the complete set of tables.
     * @return a set of tables.
     */
    public Set<Table> getTables() {
        return new HashSet<>(tables.values());
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
     * Returns all the discovered namespaces.
     * @return all discovered namespaces.
     */
    public Set<NamespacePackage> getNamespacesToBind() {
        return new HashSet<>(namespacesToBind.values());
    }

    /**
     * Add a column metadata object.
     *
     * @param column column metadata
     */
    private void addColumn(Column column) {
        String version = column.getVersion();
        if (! column.isHidden()) {
            addMetaData(column, version);
        }

        if (column instanceof TimeDimension) {
            TimeDimension timeDimension = (TimeDimension) column;
            for (TimeDimensionGrain grain : timeDimension.getSupportedGrains()) {
                addTimeDimensionGrain(grain, version);
            }
        }

        column.getArgumentDefinitions().forEach(arg -> addArgument(arg, version));
    }

    /**
     * Add a argument metadata object.
     *
     * @param argument argument metadata
     */
    private void addArgument(ArgumentDefinition argument, String version) {
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
