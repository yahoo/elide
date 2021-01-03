/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation;

import static com.yahoo.elide.core.utils.TypeHelper.getType;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.utils.ClassScanner;
import com.yahoo.elide.datastores.aggregation.annotation.Join;
import com.yahoo.elide.datastores.aggregation.cache.Cache;
import com.yahoo.elide.datastores.aggregation.core.QueryLogger;
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
    private final Set<Class<?>> dynamicCompiledClasses;
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
            dynamicCompiledClasses.forEach(dynamicLoadedClass -> dictionary.bindEntity(getType(dynamicLoadedClass),
                            Collections.singleton(Join.class)));
        }

        ClassScanner.getAnnotatedClasses(AGGREGATION_STORE_CLASSES).forEach(
                cls -> dictionary.bindEntity(getType(cls), Collections.singleton(Join.class))
        );
    }

    @Override
    public DataStoreTransaction beginTransaction() {
        return new AggregationDataStoreTransaction(queryEngine, cache, queryLogger);
    }
}
