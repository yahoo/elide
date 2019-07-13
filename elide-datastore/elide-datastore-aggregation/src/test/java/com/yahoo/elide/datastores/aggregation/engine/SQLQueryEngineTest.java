/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.engine;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.datastores.aggregation.Query;
import com.yahoo.elide.datastores.aggregation.QueryEngine;
import com.yahoo.elide.datastores.aggregation.Schema;
import com.yahoo.elide.datastores.aggregation.dimension.TimeDimension;
import com.yahoo.elide.datastores.aggregation.example.Country;
import com.yahoo.elide.datastores.aggregation.example.PlayerStats;
import org.testng.annotations.Test;

import java.util.HashMap;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class SQLQueryEngineTest {

    private EntityManagerFactory emf;
    private Schema playerStatsSchema;

    public SQLQueryEngineTest() {
        emf = Persistence.createEntityManagerFactory("aggregationStore");
        EntityDictionary dictionary = new EntityDictionary(new HashMap<>());
        dictionary.bindEntity(PlayerStats.class);
        dictionary.bindEntity(Country.class);

        playerStatsSchema = new Schema(PlayerStats.class, dictionary);
    }

    @Test
    public void testFullTableLoad() throws Exception {
        EntityManager em = emf.createEntityManager();
        QueryEngine engine = new SQLQueryEngine(em);


        Query query = Query.builder()
                .entityClass(PlayerStats.class)
                .metrics(playerStatsSchema.getMetrics())
                .groupDimension(playerStatsSchema.getDimension("overallRating"))
                .timeDimension((TimeDimension) playerStatsSchema.getDimension("recordedDate"))
                .build();

        engine.executeQuery(query);
    }
}
