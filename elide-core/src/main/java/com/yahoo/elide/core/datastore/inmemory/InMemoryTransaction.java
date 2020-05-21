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
    public abstract Set<TransactionEntry> getRunningTransactions();

    public abstract Set<TransactionEntry> getRunningTransaction(String requestId);

    public abstract void addRunningTransaction(TransactionEntry transactionEntry);

    public abstract void removeRunningTransaction(TransactionEntry transactionEntry);
}
