/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation;

import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.dictionary.ArgumentType;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.core.utils.ClassScanner;
import com.yahoo.elide.datastores.aggregation.annotation.Join;
import com.yahoo.elide.datastores.aggregation.cache.Cache;
import com.yahoo.elide.datastores.aggregation.core.QueryLogger;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ValueType;
import com.yahoo.elide.datastores.aggregation.metadata.models.Argument;
import com.yahoo.elide.datastores.aggregation.metadata.models.Column;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimension;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromSubquery;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromTable;
import lombok.Builder;
import lombok.NonNull;
import lombok.ToString;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * DataStore that supports Aggregation. Uses {@link QueryEngine} to return results.
 */
@Builder
@ToString
public class AggregationDataStore implements DataStore {
    @NonNull private final QueryEngine queryEngine;
    private final Cache cache;
    private final Set<Type<?>> dynamicCompiledClasses;
    private final QueryLogger queryLogger;

    /**
     * These are the classes the Aggregation Store manages.
     */
    private static final List<Class<? extends Annotation>> AGGREGATION_STORE_CLASSES =
            Arrays.asList(FromTable.class, FromSubquery.class);

    /**
     * Populate an {@link EntityDictionary} and use this dictionary to construct a {@link QueryEngine}.
     * @param dictionary the dictionary
     */
    @Override
    public void populateEntityDictionary(EntityDictionary dictionary) {

        if (dynamicCompiledClasses != null && dynamicCompiledClasses.size() != 0) {
            dynamicCompiledClasses.forEach(dynamicLoadedClass -> dictionary.bindEntity(dynamicLoadedClass,
                    Collections.singleton(Join.class)));
        }

        ClassScanner.getAnnotatedClasses(AGGREGATION_STORE_CLASSES).forEach(
                cls -> dictionary.bindEntity(cls, Collections.singleton(Join.class))
        );

        for (Table table : queryEngine.getMetaDataStore().getMetaData(ClassType.of(Table.class))) {
            /* Add 'grain' argument to each TimeDimensionColumn */
            for (TimeDimension timeDim : table.getTimeDimensions()) {
                dictionary.addArgumentToAttribute(
                        dictionary.getEntityClass(table.getName(), table.getVersion()),
                        timeDim.getName(),
                        new ArgumentType("grain", ClassType.STRING_TYPE, timeDim.getDefaultGrain().getGrain()));
            }

            /* Add argument to each Column */
            for (Column col : table.getColumns()) {
                for (Argument arg : col.getArguments()) {
                    dictionary.addArgumentToAttribute(
                            dictionary.getEntityClass(table.getName(), table.getVersion()),
                            col.getName(),
                            new ArgumentType(arg.getName(), ValueType.getType(arg.getType()), arg.getDefaultValue()));
                }
            }

            /* Add argument to each Table */
            for (Argument arg : table.getArguments()) {
                dictionary.addArgumentToEntity(
                        dictionary.getEntityClass(table.getName(), table.getVersion()),
                        new ArgumentType(arg.getName(), ValueType.getType(arg.getType()), arg.getDefaultValue()));
            }
        }
    }

    @Override
    public DataStoreTransaction beginTransaction() {
        return new AggregationDataStoreTransaction(queryEngine, cache, queryLogger);
    }
}
