package com.yahoo.elide.datastores.aggregation;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.datastore.test.DataStoreTestHarness;

public class TestDataStoreTestHarness implements DataStoreTestHarness {
    @Override
    public DataStore getDataStore() {
        return new TestAggregationDataStore();
    }
    public void cleanseTestData() {

    }
}
