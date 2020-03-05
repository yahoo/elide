package com.yahoo.elide.async.service;

import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.RequestScope;

@FunctionalInterface
public interface Transactional {
    public Object execute(DataStoreTransaction tx, RequestScope scope);
}
