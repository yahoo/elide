/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.security;

import com.yahoo.elide.core.datastore.DataStoreTransaction;

import java.util.List;
import java.util.Map;

/**
 * The request scope interface passed to checks.
 */
public interface RequestScope {
    User getUser();
    String getApiVersion();
    String getRequestHeaderByName(String headerName);
    String getBaseUrlEndPoint();
    Map<String, List<String>> getQueryParams();
    DataStoreTransaction getTransaction();
}
