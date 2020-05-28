/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jdk.nashorn.internal.objects.annotations.Getter;
/**
* Transaction Registry class.
*/
@Getter
public class TransactionRegistry {
    private Map<UUID, DataStoreTransaction> transactionMap = new HashMap<>();
    public Set<DataStoreTransaction> getRunningTransactions() {
        Set<DataStoreTransaction> transactions = transactionMap.values().stream().collect(Collectors.toSet());
        return transactions;
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
