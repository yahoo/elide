/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.datastore.inmemory;

import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.TransactionRegistry;

import java.util.Set;

/**
 * InMemoryTransaction implementing TransactionRegistry
 */
public abstract class InMemoryTransaction implements TransactionRegistry {
    @Override
    public Set<TransactionEntry> getRunningTransactions();
    
    @Override
    public Set<TransactionEntry> getRunningTransaction(String requestId);

    @Override
    public void addRunningTransaction(TransactionEntry transactionEntry);
    
    @Override
    public void removeRunningTransaction(TransactionEntry transactionEntry);
}

