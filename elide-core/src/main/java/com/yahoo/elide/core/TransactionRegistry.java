/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;
import java.util.Set;
/**
 * Database interface library.
 */

public interface TransactionRegistry {

    public static class TransactionEntry {
        RequestScope request;
        DataStoreTransaction transaction;
    }

    Set<TransactionEntry> getRunningTransactions();

    Set<TransactionEntry> getRunningTransaction(String requestId);

    void addRunningTransaction(TransactionEntry transactionEntry);

    void removeRunningTransaction(TransactionEntry transactionEntry);
}
