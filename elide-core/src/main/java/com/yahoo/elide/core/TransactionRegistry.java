/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
/**
* Transaction Registry class.
*/
@Getter
public class TransactionRegistry {
    private Map<UUID, DataStoreTransaction> transactionMap = new HashMap<>();

    public Map<UUID, DataStoreTransaction> getRunningTransactions() {
        return transactionMap;
    }

    public DataStoreTransaction getRunningTransaction(UUID requestId) {
        return transactionMap.get(requestId);
    }

    public void addRunningTransaction(UUID requestId, DataStoreTransaction tx) {
        transactionMap.put(requestId, tx);
    }

    public void removeRunningTransaction(UUID requestId) {
        transactionMap.remove(requestId);
    }
}
