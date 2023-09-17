/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;

import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.request.route.Route;
import com.yahoo.elide.core.security.User;
import com.yahoo.elide.jsonapi.JsonApiRequestScope;
import com.yahoo.elide.jsonapi.links.DefaultJsonApiLinks;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Utility subclass that helps construct RequestScope objects for testing.
 */
public class TestRequestScope extends JsonApiRequestScope {

    private Map<String, List<String>> queryParamOverrides = null;

    public TestRequestScope(String baseURL,
                            DataStoreTransaction transaction,
                            User user,
                            EntityDictionary dictionary) {
        super(Route.builder().baseUrl(baseURL).apiVersion(NO_VERSION).build(), transaction, user, UUID.randomUUID(),
                 new ElideSettingsBuilder(null)
                         .withEntityDictionary(dictionary)
                         .withJsonApiLinks(new DefaultJsonApiLinks())
                         .withJsonApiPath("/json")
                         .build(),
                         new JsonApiDocument());
    }

    public TestRequestScope(DataStoreTransaction transaction,
                        User user,
                        EntityDictionary dictionary) {
        super(Route.builder().apiVersion(NO_VERSION).build(), transaction, user, UUID.randomUUID(),
                new ElideSettingsBuilder(null).withEntityDictionary(dictionary).build(), new JsonApiDocument());
    }

    public TestRequestScope(EntityDictionary dictionary,
                            String path,
                            Map<String, List<String>> queryParams) {
        super(Route.builder().path(path).apiVersion(NO_VERSION).parameters(queryParams).build(), null, null,
                UUID.randomUUID(), new ElideSettingsBuilder(null).withEntityDictionary(dictionary).build(),
                new JsonApiDocument());
    }

    public void setQueryParams(Map<String, List<String>> queryParams) {
        this.queryParamOverrides = queryParams;
    }

    @Override
    public Route getRoute() {
        if (queryParamOverrides != null) {
            Route copy = super.getRoute();
            return Route.builder().baseUrl(copy.getBaseUrl()).path(copy.getPath()).parameters(queryParamOverrides)
                    .headers(copy.getHeaders()).apiVersion(copy.getApiVersion()).build();
        }
        return super.getRoute();
    }
}
