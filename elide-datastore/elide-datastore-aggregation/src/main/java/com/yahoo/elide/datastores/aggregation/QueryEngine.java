/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.exceptions.BadRequestException;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.metadata.models.Dimension;
import com.yahoo.elide.datastores.aggregation.metadata.models.Metric;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimension;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.MetricProjection;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.query.QueryResult;
import com.yahoo.elide.datastores.aggregation.query.TimeDimensionProjection;
import com.yahoo.elide.request.Argument;
import lombok.Getter;

import java.util.List;
import java.util.Map;
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
 */
public abstract class QueryEngine {
    @Getter
    protected MetaDataStore metaDataStore;

    protected EntityDictionary metadataDictionary;

    protected QueryEngine() {
    }
    /**
     * QueryEngine is constructed with a metadata store and is responsible for constructing all Tables and Entities
     * metadata in this metadata store.
     *  @param metaDataStore a metadata store
     *
     */
    public QueryEngine(MetaDataStore metaDataStore) {
        this.metaDataStore = metaDataStore;
        this.metadataDictionary = metaDataStore.getMetadataDictionary();
        populateMetaData(metaDataStore);
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
     * Construct a parameterized instance of a Column.
     * @param dimension The dimension column.
     * @param alias The client provide alias.
     * @param arguments The client provided parameterized arguments.
     * @return
     */
    public abstract ColumnProjection constructDimensionProjection(Dimension dimension,
                                                                  String alias,
                                                                  Map<String, Argument> arguments);

    /**
     * Construct a parameterized instance of a Column.
     * @param dimension The dimension column.
     * @param alias The client provide alias.
     * @param arguments The client provided parameterized arguments.
     * @return
     */
    public abstract TimeDimensionProjection constructTimeDimensionProjection(TimeDimension dimension,
                                                                             String alias,
                                                                             Map<String, Argument> arguments);
    /**
     * Construct a parameterized instance of a Column.
     * @param metric The metric column.
     * @param alias The client provide alias.
     * @param arguments The client provided parameterized arguments.
     * @return
     */
    public abstract MetricProjection constructMetricProjection(Metric metric,
                                                               String alias,
                                                               Map<String, Argument> arguments);

    /**
     * Query engine is responsible for constructing all Tables and Entities metadata in this metadata store.
     *
     * @param metaDataStore metadata store to populate
     */
    protected void populateMetaData(MetaDataStore metaDataStore) {
        metaDataStore.getModelsToBind()
                .forEach(model -> {
                    if (!metadataDictionary.isJPAEntity(model)
                            && !metadataDictionary.getRelationships(model).isEmpty()) {
                        throw new BadRequestException(
                                "Non-JPA entities " + model.getSimpleName() + " is not allowed to have relationship.");
                    }
                });


        metaDataStore.getModelsToBind().stream()
                .map(model -> constructTable(model, metadataDictionary))
                .forEach(metaDataStore::addTable);
    }

    /**
     * Contains state necessary for query execution.
     */
    public interface Transaction extends AutoCloseable {
        @Override
        void close();

        /**
         * Cancels running transaction
         */
        void cancel();
    }

    public abstract Transaction beginTransaction();

    /**
     * Executes the specified {@link Query} against a specific persistent storage, which understand the provided
     * {@link Query}. Results may be taken from a cache, if configured.
     *
     * @param query The query customized for a particular persistent storage or storage client
     * @param transaction
     * @return query results
     */
    public abstract QueryResult executeQuery(Query query, Transaction transaction);

    /**
     * Get a serial number or other token indicating the version of the data in the table.
     * No particular semantics are required, though it must change if the data changes.
     * If one is not available, returns null, which will prevent caching this table.
     * @param table The table to get version of
     * @param transaction The transaction to use for the lookup
     * @return a version token, or null if not available.
     */
    public abstract String getTableVersion(Table table, Transaction transaction);

    /**
     * Returns the actual query string(s) that would be executed for the input {@link Query}.
     *
     * @param query The query customized for a particular persistent storage or storage client.
     * @return List of SQL string(s) corresponding to the given query.
     */
    public abstract List<String> explain(Query query);

}
