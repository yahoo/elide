/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.request.route.Route;
import com.yahoo.elide.core.security.User;

import java.util.UUID;

/**
 * Utility subclass that helps construct protocol-agnostic RequestScope objects for testing.
 */
public class TestRequestScope extends RequestScope {

    public TestRequestScope(DataStoreTransaction transaction, User user, EntityDictionary dictionary) {
        super(Route.builder().apiVersion(NO_VERSION).build(), transaction, user, UUID.randomUUID(),
                ElideSettings.builder().dataStore(null).entityDictionary(dictionary).build(), null);
    }
}
