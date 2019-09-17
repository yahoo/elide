package com.yahoo.elide.datastores.aggregation;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.datastore.test.DataStoreTestHarness;
import com.yahoo.elide.initialization.IntegrationTest;

public class AggregationIntegrationTest extends IntegrationTest {

    @Override
    protected DataStoreTestHarness createHarness() {
        return new TestDataStoreTestHarness();
    }

}
