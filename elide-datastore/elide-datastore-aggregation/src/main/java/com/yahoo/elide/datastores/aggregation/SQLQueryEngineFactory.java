/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.datastores.aggregation.engine.SQLQueryEngine;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class SQLQueryEngineFactory implements QueryEngineFactory {

    @Override
    public QueryEngine buildQueryEngine(EntityDictionary dictionary) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("aggregationStore");
        return new SQLQueryEngine(emf, dictionary);
    }
}
