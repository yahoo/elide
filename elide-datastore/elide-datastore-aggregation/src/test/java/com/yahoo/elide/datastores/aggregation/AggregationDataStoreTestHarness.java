package com.yahoo.elide.datastores.aggregation;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.datastore.test.DataStoreTestHarness;
import com.yahoo.elide.datastores.aggregation.example.Country;
import com.yahoo.elide.datastores.aggregation.example.Player;
import com.yahoo.elide.datastores.aggregation.example.PlayerStats;
import com.yahoo.elide.datastores.aggregation.example.PlayerStatsView;
import com.yahoo.elide.datastores.aggregation.example.VideoGame;

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
                dictionary.bindEntity(VideoGame.class);
            }
        };
    }

    public void cleanseTestData() {

    }
}
