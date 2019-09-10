/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core;

import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.yahoo.elide.security.User;

/**
 * Utility subclass that helps construct RequestScope objects for testing.
 */
public class TestRequestScope extends RequestScope {
    public TestRequestScope(DataStoreTransaction transaction,
                        User user,
                        EntityDictionary dictionary,
                        Class<?> entityClass,
                        int nestLevel) {
        super(null, new JsonApiDocument(), transaction, user, null,
                new ElideSettingsBuilder(null)
                .withEntityDictionary(dictionary)
                .build());
    }
}
