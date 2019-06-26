/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpressionVisitor;
import com.yahoo.elide.core.hibernate.Query;
import com.yahoo.elide.core.pagination.Pagination;
import com.yahoo.elide.core.sort.Sorting;
import com.yahoo.elide.datastores.aggregation.dimension.Dimension;
import com.yahoo.elide.datastores.aggregation.dimension.TimeDimension;
import com.yahoo.elide.datastores.aggregation.filter.expression.HavingFilterExpression;
import com.yahoo.elide.datastores.aggregation.filter.expression.WhereFilterExpression;
import com.yahoo.elide.datastores.aggregation.metric.Metric;

import java.util.Optional;
import java.util.Set;

/**
 * A {@link QueryEngine} is an abstraction layer that {@link DataStoreTransaction} leverages to optimize query and send
 * query, such as loading objects, to a persistent storage.
 * <p>
 * The purpose of {@link QueryEngine} is to allow a single {@link DataStore} to utilize multiple query frameworks, such
 * as Hibernate on SQL and Drill on NoSQL shown below:
 * <pre>
 *                           +-----------+                      +------------------+
 *                           |           |                      |                  |
 *                           | DataStore |              +-------+ DrillQueryEngine |
 *                           |           |              |       |                  |
 *                           +-----+-----+              |       +------------------+
 *                                 |                    |
 *                                 |                    |
 *                      +----------v-----------+        |       +------------------+
 *                      |                      |        |       |                  |
 *                      | DataStoreTransaction |        +-------+  JpaQueryEngine  |
 *                      |                      |        |       |                  |
 *                      +----------+-----------+        |       +------------------+
 *                                 |                    |
 *                                 |                    |
 *                          +------v------+             |       +----------------------+
 *                          |             |             |       |                      |
 *                          | QueryEngine +-------------+-------+ HibernateQueryEngine |
 *                          |             |                     |                      |
 *                          +------+------+                     +----------------------+
 *                                 |
 *                                 |
 *             +-------------------------------------+
 *             |                   |                 |
 * +-----------v----------+   +----v----+   +--------v-------+
 * |     Apache Drill     |   |   JPA   |   |    Hibernate   |
 * +-----------+----------+   +----+----+   +--------+-------+
 *             |                   |                 |
 * +-----------v-------------------v-----------------v--------+
 * |                         SQL Driver                       |
 * +---------------+----------------------------+-------------+
 *                 |                            |
 *                 |                            |
 *           +-----v-----+                  +---v----+
 *           |           |                  |        |
 *           |  NoSQL DB |                  | SQL DB |
 *           |           |                  |        |
 *           +-----------+                  +--------+
 * </pre>
 * Implementor must assume that {@link DataStoreTransaction} will never keep reference to any internal state of a
 * {@link QueryEngine} object. This ensures the plugability of various {@link QueryEngine} implementations.
 * <p>
 * A concrete and complete {@link QueryEngine} must also have the corresponding implementations of
 * <ol>
 *     <li> a {@link Query} that can be understood by a particular persistent storage or storage client
 *     <li> a {@link WhereFilterExpression} that maps an Elide WHERE filter clause to an equivalent query constraint,
 *          which can be interpreted by the particular persistent storage
 *     <li> a {@link HavingFilterExpression} that maps an Elide WHERE filter clause to an equivalent query constraint,
 *          which can be interpreted by the particular persistent storage
 *     <li> one or more {@link FilterExpressionVisitor}s that can split an arbitrary {@link FilterExpression} into
 *          aforementioned {@link WhereFilterExpression} and {@link HavingFilterExpression}
 * </ol>
 */
public interface QueryEngine {

    /**
     * Executes the specified {@link Query} against a specific persistent storage, which understand the provided
     * {@link Query}.
     *
     * @param query  The query customized for a particular persistent storage or storage client
     *
     * @return query results
     */
    Iterable<Object> executeQuery(Query query);

    /**
     * Parses request to generate a {@link Query} object to be executed by this {@link QueryEngine}.
     * <p>
     * This method should also incorporate all query optimizations if any
     *
     * @param entityClass  The type of the entity involved in this query
     * @param metrics  A set of metrics involved in this query
     * @param groupDimensions  A set of grouping dimensions involved in this query
     * @param timeDimensions  A special set of dimension for grouping temporal dimensions
     * @param whereFilter  A WHERE filter that can be evaluated in the data store. It is optional for the data store to
     * attempt evaluation
     * @param havingFilter  A HAVING filter that can be evaluated in the data store. It is optional for the data
     * store to attempt evaluation
     * @param sorting  Sorting which can be pushed down to the data store.
     * @param pagination  Pagination which can be pushed down to the data store.
     * @param scope  Contains request level metadata.
     *
     * @return a optimized query consumed by the data store
     */
    Query buildQuery(
            Class<?> entityClass,
            Set<Metric> metrics,
            Set<Dimension> groupDimensions,
            Set<TimeDimension> timeDimensions,
            Optional<WhereFilterExpression> whereFilter,
            Optional<HavingFilterExpression> havingFilter,
            Optional<Sorting> sorting,
            Optional<Pagination> pagination,
            RequestScope scope
    );
}
