/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.core.datastore.test;

import com.paiondata.elide.core.datastore.DataStore;

/**
 * Any data store that wants IT tests to run against it needs to provide an implementation of this harness.
 */
public interface DataStoreTestHarness {
    public DataStore getDataStore();
    public void cleanseTestData();
}
