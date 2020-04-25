/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core;

import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.yahoo.elide.security.User;

import java.util.Optional;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Utility subclass that helps construct RequestScope objects for testing.
 */
public class TestRequestScope extends RequestScope {

    private MultivaluedMap queryParamOverrides = null;

    public TestRequestScope(DataStoreTransaction transaction,
                        User user,
                        EntityDictionary dictionary) {
        super(null, "", new JsonApiDocument(), transaction, user, null,
                new ElideSettingsBuilder(null)
                .withEntityDictionary(dictionary)
                .build());
    }

    public TestRequestScope(EntityDictionary dictionary,
                            String path,
                            MultivaluedMap<String, String> queryParams) {
        super(path, "", new JsonApiDocument(), null, null, queryParams,
                new ElideSettingsBuilder(null)
                        .withEntityDictionary(dictionary)
                        .build());
    }

    public void setQueryParams(MultivaluedMap<String, String> queryParams) {
        this.queryParamOverrides = queryParams;
    }

    @Override
    public Optional<MultivaluedMap<String, String>> getQueryParams() {
        if (queryParamOverrides != null) {
            return Optional.of(queryParamOverrides);
        } else {
            return super.getQueryParams();
        }
    }
}
