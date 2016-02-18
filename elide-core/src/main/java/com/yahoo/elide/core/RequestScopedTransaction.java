/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

/**
 * Scope aware transaction
 */
public interface RequestScopedTransaction extends DataStoreTransaction {

    void setRequestScope(RequestScope requestScope);
}
