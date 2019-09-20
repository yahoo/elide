package com.yahoo.elide.datastores.aggregation;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.datastore.inmemory.HashMapDataStore;
import com.yahoo.elide.core.datastore.test.DataStoreTestHarness;
import com.yahoo.elide.datastores.aggregation.example.Country;
import com.yahoo.elide.datastores.aggregation.example.PlayerStats;
import com.yahoo.elide.datastores.aggregation.example.VideoGame;
import com.yahoo.elide.initialization.IntegrationTest;
import sun.awt.image.ImageWatched;

import java.util.LinkedHashSet;
import java.util.Set;

public class AggregationIntegrationTest extends IntegrationTest {

    @Override
    protected DataStoreTestHarness createHarness() {
        return new TestDataStoreTestHarness();
    }

}
