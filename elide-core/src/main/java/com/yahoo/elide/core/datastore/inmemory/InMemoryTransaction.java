/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.datastore.inmemory;
import com.yahoo.elide.core.TransactionRegistry;

import java.util.Set;

/**
 * InMemoryTransaction implementing TransactionRegistry
 */
public class InMemoryTransaction implements TransactionRegistry {
    private TransactionRegistry registry;
    @Override
    public Set<TransactionEntry> getRunningTransactions() {
        return registry.getRunningTransactions();
    }

    @Override
    public Set<TransactionEntry> getRunningTransaction(String requestId) {
        return registry.getRunningTransaction(requestId);
    }

    @Override
    public void addRunningTransaction(Class transactionEntry) {
        registry.addRunningTransaction(transactionEntry);
    }

    @Override
    public void removeRunningTransaction(Class transactionEntry) {
         registry.removeRunningTransaction(transactionEntry);
    }
}
