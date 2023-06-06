/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.security;

import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.request.route.Route;
import com.yahoo.elide.jsonapi.document.processors.WithMetadata;

/**
 * The request scope interface passed to checks.
 */
public interface RequestScope extends WithMetadata {
    User getUser();
    Route getRoute();
    DataStoreTransaction getTransaction();
}
