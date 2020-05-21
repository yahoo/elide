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
    private TransactionRegistry registry = new TransactionRegistry();

    @Override
    public Set<TransactionEntry> getRunningTransactions() {
        registry.getRunningTransactions();
    }

    @Override
    public Set<TransactionEntry> getRunningTransaction(String requestId) {
        registry.getRunningTransaction(requestId);
    }

    @Override
    public void addRunningTransaction(TransactionEntry transactionEntry) {
        registry.addRunningTransaction(transactionEntry);
    }

    @Override
    public abstract void removeRunningTransaction(TransactionEntry transactionEntry) {
         registry.removeRunningTransaction(transactionEntry);
    }
}
