/*
 *  * Copyright 2015, Yahoo Inc.
 *   * Licensed under the Apache License, Version 2.0
 *    * See LICENSE file in project root for terms.
 *     */
package com.yahoo.elide.core;

/**
 *  * Database interface library.
 *   */

public interface TransactionRegistry {
    @Data
    public static class TransactionEntry {
        RequestScope request;
        DataStoreTransaction transaction;
    }

    Set<TransactionEntry> getRunningTransactions();

    Set<TransactionEntry> getRunningTransaction(String requestId);

    void addRunningTransaction(TransactionEntry transactionEntry);

    void removeRunningTransaction(TransactionEntry transactionEntry);
}
