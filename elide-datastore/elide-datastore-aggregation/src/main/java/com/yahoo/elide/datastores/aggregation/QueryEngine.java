/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.exceptions.InvalidPredicateException;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
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

    private final Map<String, Table> tables;

    /**
     * QueryEngine is constructed with a metadata store and is responsible for constructing all Tables and Entities
     * metadata in this metadata store.
     *
     * @param metaDataStore a metadata store
     */
    public QueryEngine(MetaDataStore metaDataStore) {
        this.metaDataStore = metaDataStore;
        this.metadataDictionary = metaDataStore.getDictionary();
        populateMetaData(metaDataStore);
        this.tables = metaDataStore.getMetaData(Table.class).stream()
                .collect(Collectors.toMap(Table::getId, Functions.identity()));
    }

    /**
     * Construct Table metadata for an entity.
     *
     * @param entityClass entity class
     * @param metaDataDictionary metadata dictionary
     * @return constructed Table
     */
    protected abstract Table constructTable(Class<?> entityClass, EntityDictionary metaDataDictionary);

    /**
     * Query engine is responsible for constructing all Tables and Entities metadata in this metadata store.
     *
     * @param metaDataStore metadata store to populate
     */
    private void populateMetaData(MetaDataStore metaDataStore) {
        metaDataStore.getModelsToBind()
                .forEach(model -> {
                    if (!metadataDictionary.isJPAEntity(model)
                            && !metadataDictionary.getRelationships(model).isEmpty()) {
                        throw new InvalidPredicateException(
                                "Non-JPA entities " + model.getSimpleName() + " is not allowed to have relationship.");
                    }
        });

        metaDataStore.getModelsToBind().stream()
                .map(model -> constructTable(model, metadataDictionary))
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
     * @param classAlias json type alias for that class
     * @return The schema that represents the provided entity.
     */
    public Table getTable(String classAlias) {
        return tables.get(classAlias);
    }
}
