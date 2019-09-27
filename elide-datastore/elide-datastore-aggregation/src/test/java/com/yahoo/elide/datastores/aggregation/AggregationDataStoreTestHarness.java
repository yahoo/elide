package com.yahoo.elide.datastores.aggregation;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.datastore.inmemory.HashMapDataStore;
import com.yahoo.elide.core.datastore.test.DataStoreTestHarness;
import com.yahoo.elide.datastores.aggregation.example.Country;
import com.yahoo.elide.datastores.aggregation.example.Player;
import com.yahoo.elide.datastores.aggregation.example.PlayerStats;
import com.yahoo.elide.datastores.aggregation.example.PlayerStatsView;
import com.yahoo.elide.datastores.aggregation.example.VideoGame;
import com.yahoo.elide.datastores.aggregation.example.Book;
import com.yahoo.elide.datastores.aggregation.example.Author;

import java.util.LinkedHashSet;
import java.util.Set;

public class AggregationDataStoreTestHarness implements DataStoreTestHarness {
    private QueryEngine qE;

    public AggregationDataStoreTestHarness(QueryEngine qE) {
        this.qE = qE;
    }
    @Override
    public DataStore getDataStore() {
        return new AggregationDataStore(qE) {
            @Override
            public void populateEntityDictionary(EntityDictionary dictionary) {
                dictionary.bindEntity(PlayerStats.class);
                dictionary.bindEntity(Country.class);
                dictionary.bindEntity(PlayerStatsView.class);
                dictionary.bindEntity(Player.class);
            }
        };
    }

    public void cleanseTestData() {

    }
}
