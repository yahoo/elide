package com.yahoo.elide.datastores.aggregation;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;

public class TestAggregationDataStore implements DataStore {

    @Override
    public void populateEntityDictionary(EntityDictionary dictionary) {

    }

    @Override
    public DataStoreTransaction beginTransaction() {
        return null;
    }

    @Override
    public DataStoreTransaction beginReadTransaction() {
        return null;
    }
}
