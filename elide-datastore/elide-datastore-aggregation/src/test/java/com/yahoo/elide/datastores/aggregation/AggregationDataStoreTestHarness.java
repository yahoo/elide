/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.datastore.test.DataStoreTestHarness;
import com.yahoo.elide.datastores.aggregation.example.Country;
import com.yahoo.elide.datastores.aggregation.example.Player;
import com.yahoo.elide.datastores.aggregation.example.PlayerStats;
import com.yahoo.elide.datastores.aggregation.example.PlayerStatsView;
import com.yahoo.elide.datastores.aggregation.example.SubCountry;
import com.yahoo.elide.datastores.aggregation.example.VideoGame;

public class AggregationDataStoreTestHarness implements DataStoreTestHarness {
    private QueryEngineFactory queryEngineFactory;

    public AggregationDataStoreTestHarness(QueryEngineFactory queryEngineFactory) {
        this.queryEngineFactory = queryEngineFactory;
    }

    @Override
    public DataStore getDataStore() {
        return new AggregationDataStore(queryEngineFactory) {
            @Override
            public void populateEntityDictionary(EntityDictionary dictionary) {
                dictionary.bindEntity(PlayerStats.class);
                dictionary.bindEntity(Country.class);
                dictionary.bindEntity(SubCountry.class);
                dictionary.bindEntity(PlayerStatsView.class);
                dictionary.bindEntity(Player.class);
                dictionary.bindEntity(VideoGame.class);
                super.populateEntityDictionary(dictionary);
            }
        };
    }

    public void cleanseTestData() {

    }
}
