/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.datastore.test;

import com.yahoo.elide.core.DataStore;

/**
 * Stuff.
 */
public interface DataStoreHarness {
    public DataStore getDataStore();
    public void cleanseTestData();
}
