/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.engine;

import com.yahoo.elide.datastores.aggregation.Query;
import com.yahoo.elide.datastores.aggregation.QueryEngine;
import com.yahoo.elide.datastores.aggregation.example.PlayerStats;
import org.testng.annotations.Test;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class SQLQueryEngineTest {

    private EntityManagerFactory emf;

    public SQLQueryEngineTest() {
        emf = Persistence.createEntityManagerFactory("aggregationStore");
    }

    @Test
    public void testFullTableLoad() throws Exception {
        EntityManager em = emf.createEntityManager();
        QueryEngine engine = new SQLQueryEngine(em);

        Query query = Query.builder()
                .entityClass(PlayerStats.class)
                .build();

        engine.executeQuery(query);
    }
}
