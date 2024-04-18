/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.jpa.transaction;

import com.paiondata.elide.core.datastore.DataStoreTransaction;

/**
 * Extended for JPA DataStoreTransaction.
 */
public interface JpaTransaction extends DataStoreTransaction {
    void begin();

    void rollback();

    boolean isOpen();
}
