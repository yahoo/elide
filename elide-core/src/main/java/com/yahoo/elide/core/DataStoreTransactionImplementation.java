/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import lombok.Getter;

import java.util.UUID;

/**
 * Data Store Transaction Implementation to return transaction id
 */
public abstract class DataStoreTransactionImplementation implements DataStoreTransaction {
    @Getter private final UUID requestId = UUID.randomUUID();

    @Override
    public UUID getRequestId() {
        return requestId;
    }
}
