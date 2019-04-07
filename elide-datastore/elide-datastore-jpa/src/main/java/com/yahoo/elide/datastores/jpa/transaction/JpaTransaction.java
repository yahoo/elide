/*
 * Copyright 2018, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.jpa.transaction;

import com.yahoo.elide.core.DataStoreTransaction;

/**
 * Extended for JPA DataStoreTransaction.
 */
public interface JpaTransaction extends DataStoreTransaction {
    void begin();

    void rollback();

    boolean isOpen();
}
