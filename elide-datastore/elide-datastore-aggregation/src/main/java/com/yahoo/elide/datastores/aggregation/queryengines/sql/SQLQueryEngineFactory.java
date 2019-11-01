/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql;

import com.yahoo.elide.datastores.aggregation.AggregationDictionary;
import com.yahoo.elide.datastores.aggregation.QueryEngine;
import com.yahoo.elide.datastores.aggregation.QueryEngineFactory;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;

import javax.persistence.EntityManagerFactory;

/**
 * Object that constructs {@link QueryEngine} based on given entityDictionary and entityManagerFactory.
 */
public class SQLQueryEngineFactory implements QueryEngineFactory {
    private EntityManagerFactory emf;

    public SQLQueryEngineFactory(EntityManagerFactory emf) {
        this.emf = emf;
    }

    @Override
    public QueryEngine buildQueryEngine(AggregationDictionary dictionary, MetaDataStore metaDataStore) {
        return new SQLQueryEngine(emf, dictionary, metaDataStore);
    }
}
