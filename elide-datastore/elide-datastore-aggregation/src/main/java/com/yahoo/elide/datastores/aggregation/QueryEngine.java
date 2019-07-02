/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.DataStoreTransaction;

/**
 * A {@link QueryEngine} is an abstraction layer that {@link DataStoreTransaction} leverages to optimize query and send
 * query, such as loading objects, to a persistent storage.
 * <p>
 * The purpose of {@link QueryEngine} is to allow a single {@link DataStore} to utilize multiple query frameworks, such
 * as Hibernate on SQL and Drill on NoSQL shown below. There will be 3 implementations of {@link QueryEngine}:
 * DrillQueryEngine, JpaQueryEngine, and HibernateQueryEngine
 * <pre>
 *                              +-----------+
 *                              |           |
 *                              | DataStore |
 *                              |           |
 *                              +-----+-----+
 *                                    |
 *                                    |
 *                         +----------v-----------+
 *                         |                      |
 *                         | DataStoreTransaction |
 *                         |                      |
 *                         +----------+-----------+
 *                                    |
 *                                    |
 *                             +------v------+
 *                             |             |
 *                             | QueryEngine |
 *                             |             |
 *                             +------+------+
 *                                    |
 *                                    |
 *             +-----------------------------------------------+
 *             |                      |                        |
 *             |                      |                        |
 * +-----------v----------+      +----v----+          +--------v-------+
 * |     Apache Drill     |      |   JPA   |          |    Hibernate   |
 * +-----------+----------+      +----+----+          +--------+-------+
 *             |                      |                        |
 * +-----------v----------------------v------------------------v----------+
 * |                             SQL Driver                               |
 * +-------------------+----------------------------+---------------------+
 *                     |                            |
 *               +-----v-----+                  +---v----+
 *               |           |                  |        |
 *               |  NoSQL DB |                  | SQL DB |
 *               |           |                  |        |
 *               +-----------+                  +--------+
 * </pre>
 * Implementor must assume that {@link DataStoreTransaction} will never keep reference to any internal state of a
 * {@link QueryEngine} object. This ensures the plugability of various {@link QueryEngine} implementations.
 * <p>
 * This is a {@link java.util.function functional interface} whose functional method is {@link #executeQuery(Query)}.
 */
@FunctionalInterface
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
}
