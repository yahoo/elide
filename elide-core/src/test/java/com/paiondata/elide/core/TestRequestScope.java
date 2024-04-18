/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.core;

import static com.paiondata.elide.core.dictionary.EntityDictionary.NO_VERSION;

import com.paiondata.elide.ElideSettings;
import com.paiondata.elide.core.datastore.DataStoreTransaction;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.request.route.Route;
import com.paiondata.elide.core.security.User;
import com.paiondata.elide.jsonapi.JsonApiRequestScope;
import com.paiondata.elide.jsonapi.JsonApiSettings;
import com.paiondata.elide.jsonapi.links.DefaultJsonApiLinks;
import com.paiondata.elide.jsonapi.models.JsonApiDocument;


import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Utility subclass that helps construct RequestScope objects for testing.
 */
public class TestRequestScope extends JsonApiRequestScope {

    private Map<String, List<String>> queryParamOverrides = null;

    public TestRequestScope(String baseURL, DataStoreTransaction transaction, User user, EntityDictionary dictionary) {
        super(Route.builder().baseUrl(baseURL).apiVersion(NO_VERSION).build(), transaction, user, UUID.randomUUID(),
                ElideSettings.builder().dataStore(null).entityDictionary(dictionary)
                        .settings(JsonApiSettings.builder().path("/json")
                                .links(links -> links.enabled(true).jsonApiLinks(new DefaultJsonApiLinks())))
                        .build(),
                null, new JsonApiDocument());
    }

    public TestRequestScope(DataStoreTransaction transaction, User user, EntityDictionary dictionary) {
        super(Route.builder().apiVersion(NO_VERSION).build(), transaction, user, UUID.randomUUID(), ElideSettings
                .builder().dataStore(null).entityDictionary(dictionary).settings(JsonApiSettings.builder()).build(),
                null, new JsonApiDocument());
    }

    public TestRequestScope(EntityDictionary dictionary, String path, Map<String, List<String>> queryParams) {
        super(Route.builder().path(path).apiVersion(NO_VERSION).parameters(queryParams).build(), null, null,
                UUID.randomUUID(), ElideSettings.builder().dataStore(null).entityDictionary(dictionary)
                        .settings(JsonApiSettings.builder()).build(),
                null, new JsonApiDocument());
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
