/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.security;

import com.paiondata.elide.ElideSettings;
import com.paiondata.elide.core.datastore.DataStoreTransaction;
import com.paiondata.elide.core.request.route.Route;
import com.paiondata.elide.jsonapi.document.processors.WithMetadata;

/**
 * The request scope interface passed to checks.
 */
public interface RequestScope extends WithMetadata {
    /**
     * Returns the {@link User} of the request.
     *
     * @return the user of the request
     */
    User getUser();

    /**
     * Returns the {@link Route} of the request which contains the base url, path,
     * headers, parameters and api version of the request.
     *
     * @return the route of the request
     */
    Route getRoute();

    /**
     * Returns the {@link DataStoreTransaction} of the request.
     *
     * @return the transaction of the request
     */
    DataStoreTransaction getTransaction();

    /**
     * Returns the {@link ElideSettings}.
     *
     * @return the settings
     */
    ElideSettings getElideSettings();
}
