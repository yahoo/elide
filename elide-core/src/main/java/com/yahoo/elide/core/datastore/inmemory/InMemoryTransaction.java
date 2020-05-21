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
public abstract class InMemoryTransaction implements TransactionRegistry {
    public Set<TransactionEntry> getRunningTransactions();

    public Set<TransactionEntry> getRunningTransaction(String requestId);

    public void addRunningTransaction(TransactionEntry transactionEntry);

    public void removeRunningTransaction(TransactionEntry transactionEntry);
}
