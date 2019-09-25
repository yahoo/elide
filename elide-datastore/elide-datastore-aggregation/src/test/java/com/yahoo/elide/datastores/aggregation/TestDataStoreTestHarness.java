package com.yahoo.elide.datastores.aggregation;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.datastore.inmemory.HashMapDataStore;
import com.yahoo.elide.core.datastore.test.DataStoreTestHarness;
import com.yahoo.elide.datastores.aggregation.example.Country;
import com.yahoo.elide.datastores.aggregation.example.PlayerStats;
import com.yahoo.elide.datastores.aggregation.example.VideoGame;
import com.yahoo.elide.datastores.aggregation.example.Book;
import com.yahoo.elide.datastores.aggregation.example.Author;

import java.util.LinkedHashSet;
import java.util.Set;

public class TestDataStoreTestHarness implements DataStoreTestHarness {
    @Override
    public DataStore getDataStore() {
        Set<Package> packageSet = new LinkedHashSet<>();
        packageSet.add(Country.class.getPackage());
        packageSet.add(VideoGame.class.getPackage());
        packageSet.add(PlayerStats.class.getPackage());
        packageSet.add(Book.class.getPackage());
        packageSet.add(Author.class.getPackage());
        return new TestAggregationDataStore(packageSet);
    }

    public void cleanseTestData() {

    }
}
