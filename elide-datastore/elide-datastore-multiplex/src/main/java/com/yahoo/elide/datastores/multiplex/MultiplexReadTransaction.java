/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.multiplex;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.RequestScope;

import lombok.Getter;

import java.util.UUID;


/**
 * Multiplex transaction handler.
 */
public class MultiplexReadTransaction extends MultiplexTransaction {
    @Getter private final UUID Id = UUID.randomUUID();
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
