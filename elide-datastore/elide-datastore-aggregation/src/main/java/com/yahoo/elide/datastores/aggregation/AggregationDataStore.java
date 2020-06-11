/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation;

import com.yahoo.elide.core.ArgumentType;
import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.datastores.aggregation.annotation.Join;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimension;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromSubquery;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromTable;
import com.yahoo.elide.utils.ClassScanner;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * DataStore that supports Aggregation. Uses {@link QueryEngine} to return results.
 */
public class AggregationDataStore implements DataStore {
    private QueryEngine queryEngine;
    private Set<Class<?>> dynamicCompiledClasses;
    /**
     * These are the classes the Aggregation Store manages.
     */
    private static final List<Class<? extends Annotation>> AGGREGATION_STORE_CLASSES =
            Arrays.asList(FromTable.class, FromSubquery.class);

    public AggregationDataStore(QueryEngine queryEngine) {
        this.queryEngine = queryEngine;
    }

    public AggregationDataStore(QueryEngine qEngine, Set<Class<?>> dynamicCompiledClasses) {
        this.queryEngine = qEngine;
        this.dynamicCompiledClasses = dynamicCompiledClasses;
    }

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

        for (Class<? extends Annotation> annotation : AGGREGATION_STORE_CLASSES) {
            // bind non-jpa entity tables
            ClassScanner.getAnnotatedClasses(annotation)
                    .forEach(cls -> dictionary.bindEntity(cls, Collections.singleton(Join.class)));
        }

        /* Add 'grain' argument to each TimeDimensionColumn */
        for (Table table : queryEngine.getMetaDataStore().getMetaData(Table.class)) {
            for (TimeDimension timeDim : table.getColumns(TimeDimension.class)) {
                dictionary.addArgumentToAttribute(
                        dictionary.getEntityClass(table.getName(), table.getVersion()),
                        timeDim.getName(),
                        new ArgumentType("grain", String.class));
            }
        }
    }

    @Override
    public DataStoreTransaction beginTransaction() {
        return new AggregationDataStoreTransaction(queryEngine, cancelTransaction);
    }
}
