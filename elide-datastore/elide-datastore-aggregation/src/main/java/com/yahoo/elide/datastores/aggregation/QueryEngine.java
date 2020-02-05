/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.metadata.models.AnalyticView;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import com.yahoo.elide.datastores.aggregation.query.Query;

import com.google.common.base.Functions;

import lombok.Getter;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * A {@link QueryEngine} is an abstraction that an AggregationDataStore leverages to run analytic queries (OLAP style)
 * against an underlying persistence layer.
 * <p>
 * The purpose of {@link QueryEngine} is to allow a single {@link DataStore} to utilize multiple query frameworks, such
 * as JPA on SQL or NoSQL query engine on Druid shown below.
 * <pre>
 *        +-----------+
 *        |           |
 *        | DataStore |
 *        |           |
 *        +-----+-----+
 *              |
 *              |
 *   +----------v-----------+
 *   |                      |
 *   | DataStoreTransaction |
 *   |                      |
 *   +----------+-----------+
 *              |
 *              |
 *       +------v------+
 *       |             |
 *       | QueryEngine |
 *       |             |
 *       +------+------+
 *              |
 *              |
 *     +--------+---------+
 *     |                  |
 * +---v---+          +---v---+
 * |       |          |       |
 * | Druid |          | MySQL |
 * |       |          |       |
 * +-------+          +-------+
 * </pre>
 * Implementor must assume that {@link DataStoreTransaction} will never keep reference to any internal state of a
 * {@link QueryEngine} object. This ensures the plugability of various {@link QueryEngine} implementations.
 * <p>
 * This is a {@link java.util.function functional interface} whose functional method is {@link #executeQuery(Query)}.
 */
public abstract class QueryEngine {
    @Getter
    private final MetaDataStore metaDataStore;

    @Getter
    private final EntityDictionary metadataDictionary;

    @Getter
    private final Map<Class<?>, Table> tables;

    public QueryEngine(MetaDataStore metaDataStore) {
        this.metaDataStore = metaDataStore;
        this.metadataDictionary = metaDataStore.getDictionary();
        populateMetaData(metaDataStore);
        this.tables = metaDataStore.getMetaData(AnalyticView.class).stream()
                .collect(Collectors.toMap(Table::getCls, Functions.identity()));
    }

    protected abstract Table constructTable(Class<?> entityClass, EntityDictionary metaDataDictionary);

    protected abstract AnalyticView constructAnalyticView(Class<?> entityClass, EntityDictionary metaDataDictionary);

    private void populateMetaData(MetaDataStore metaDataStore) {
        metaDataStore.getModelsToBind().stream()
                .map(model -> MetaDataStore.isAnalyticView(model)
                        ? constructAnalyticView(model, metadataDictionary)
                        : constructTable(model, metadataDictionary))
                .forEach(metaDataStore::addTable);
    }

    /**
     * Executes the specified {@link Query} against a specific persistent storage, which understand the provided
     * {@link Query}.
     *
     * @param query  The query customized for a particular persistent storage or storage client
     *
     * @return query results
     */
    public abstract Iterable<Object> executeQuery(Query query);

    /**
     * Returns the schema for a given entity class.
     * @param entityClass The class to map to a schema.
     * @return The schema that represents the provided entity.
     */
    public Table getTable(Class<?> entityClass) {
        return tables.get(entityClass);
    }
}
