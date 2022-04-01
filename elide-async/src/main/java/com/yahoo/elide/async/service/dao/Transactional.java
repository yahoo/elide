/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service.dao;

import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.DataStoreTransaction;

/**
 * Function which will be invoked for executing elide async transactions.
 */
@FunctionalInterface
public interface Transactional {
    public Object execute(DataStoreTransaction tx, RequestScope scope);
}
