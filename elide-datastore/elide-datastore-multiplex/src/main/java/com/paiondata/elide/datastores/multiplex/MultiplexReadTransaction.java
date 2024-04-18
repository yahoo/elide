/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.multiplex;

import com.paiondata.elide.core.RequestScope;
import com.paiondata.elide.core.datastore.DataStore;
import com.paiondata.elide.core.datastore.DataStoreTransaction;

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
    public <T> void save(T entity, RequestScope scope) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> void delete(T entity, RequestScope scope) {
        throw new UnsupportedOperationException();
    }
}
