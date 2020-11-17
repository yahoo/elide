/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.multiplex;

import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.datastore.DataStoreTransaction;

/**
 * Multiplex transaction handler.
 */
public class MultiplexReadTransaction extends MultiplexTransaction {
    public MultiplexReadTransaction(MultiplexManager multiplexManager) {
        super(multiplexManager);
    }

    @Override
    protected DataStoreTransaction beginTransaction(DataStore dataStore) {
        // begin read transaction
        return dataStore.beginReadTransaction();
    }

    @Override
    public void save(Object entity, RequestScope scope) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(Object entity, RequestScope scope) {
        throw new UnsupportedOperationException();
    }
}
