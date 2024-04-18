/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.jsonapi;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import com.paiondata.elide.ElideSettings;
import com.paiondata.elide.core.datastore.DataStoreTransaction;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.exceptions.BadRequestException;
import com.paiondata.elide.core.request.EntityProjection;
import com.paiondata.elide.core.request.route.Route;
import com.paiondata.elide.core.security.User;
import com.paiondata.elide.jsonapi.JsonApiRequestScope.JsonApiRequestScopeBuilder;
import com.paiondata.elide.jsonapi.JsonApiSettings.JsonApiSettingsBuilder;
import com.paiondata.elide.jsonapi.models.JsonApiDocument;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Test for JsonApiRequestScope.
 */
class JsonApiRequestScopeTest {

    @Test
    void builder() {
        DataStoreTransaction dataStoreTransaction = mock(DataStoreTransaction.class);
        User user = mock(User.class);
        EntityProjection entityProjection = mock(EntityProjection.class);

        ElideSettings elideSettings = ElideSettings.builder()
                .settings(JsonApiSettings.builder())
                .entityDictionary(EntityDictionary.builder().build())
                .build();
        Route route = Route.builder().build();
        UUID requestId = UUID.randomUUID();
        JsonApiDocument jsonApiDocument = new JsonApiDocument();

        JsonApiRequestScope requestScope = JsonApiRequestScope.builder().route(route).requestId(requestId)
                .elideSettings(elideSettings)
                .dataStoreTransaction(dataStoreTransaction).user(user)
                .entityProjection(scope -> entityProjection)
                .jsonApiDocument(jsonApiDocument)
                .build();
        assertSame(user, requestScope.getUser());
        assertSame(elideSettings, requestScope.getElideSettings());
        assertSame(route, requestScope.getRoute());
        assertSame(requestId, requestScope.getRequestId());
        assertSame(entityProjection, requestScope.getEntityProjection());
        assertSame(jsonApiDocument, requestScope.getJsonApiDocument());
    }

    @Test
    void invalidFilter() {
        DataStoreTransaction dataStoreTransaction = mock(DataStoreTransaction.class);
        User user = mock(User.class);
        EntityProjection entityProjection = mock(EntityProjection.class);
        EntityDictionary entityDictionary = EntityDictionary.builder().build();
        ElideSettings elideSettings = ElideSettings.builder()
                .settings(JsonApiSettingsBuilder.withDefaults(entityDictionary))
                .entityDictionary(entityDictionary)
                .build();
        Map<String, List<String>> parameters = Map.of("filter", List.of(""));
        Route route = Route.builder().parameters(parameters).build();
        UUID requestId = UUID.randomUUID();
        JsonApiDocument jsonApiDocument = new JsonApiDocument();

        JsonApiRequestScopeBuilder requestScopeBuilder = JsonApiRequestScope.builder().route(route).requestId(requestId)
                .elideSettings(elideSettings)
                .dataStoreTransaction(dataStoreTransaction).user(user)
                .entityProjection(scope -> entityProjection)
                .jsonApiDocument(jsonApiDocument);
        assertThrows(BadRequestException.class, () -> requestScopeBuilder.build());
    }
}
