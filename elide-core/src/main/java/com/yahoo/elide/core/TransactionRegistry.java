/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import lombok.Data;

import java.util.Set;
/**
 * Transaction Registry interface to surface transaction details to other parts of Elide.
 */
public interface TransactionRegistry {
    @Data
    /**
     * @see RequestScope
     * @see DataStoreTransaction
     * */
    public static class TransactionEntry {
        public RequestScope request;
        public DataStoreTransaction transaction;
    }

    /**
     * @return all running transactions
     */
    Set<TransactionEntry> getRunningTransactions();

    /**
     * @param requestId
     * @return matching running transaction
     */
    Set<TransactionEntry> getRunningTransaction(String requestId);

    /**
     * Adds running transaction
     * @param transactionEntry TransactionEntry transactionEntry
     */
    void addRunningTransaction(TransactionEntry transactionEntry);

    /**
     * Removes running transaction when we call cancel on it
     * @param transactionEntry TransactionEntry transactionEntry
     */
    void removeRunningTransaction(TransactionEntry transactionEntry);
}
