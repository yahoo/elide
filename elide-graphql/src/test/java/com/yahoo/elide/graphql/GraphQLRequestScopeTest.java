/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;


import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.request.route.Route;
import com.yahoo.elide.core.security.User;
import com.yahoo.elide.graphql.parser.GraphQLProjectionInfo;
import com.yahoo.elide.jsonapi.JsonApiSettings;

import org.junit.jupiter.api.Test;

import java.util.UUID;
/**
 * Test for GraphQLRequestScope.
 */
class GraphQLRequestScopeTest {

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

        GraphQLProjectionInfo projectionInfo = mock(GraphQLProjectionInfo.class);
        GraphQLRequestScope requestScope = GraphQLRequestScope.builder().route(route).requestId(requestId)
                .elideSettings(elideSettings)
                .dataStoreTransaction(dataStoreTransaction).user(user)
                .entityProjection(scope -> entityProjection)
                .projectionInfo(projectionInfo)
                .build();
        assertSame(user, requestScope.getUser());
        assertSame(elideSettings, requestScope.getElideSettings());
        assertSame(route, requestScope.getRoute());
        assertSame(requestId, requestScope.getRequestId());
        assertSame(entityProjection, requestScope.getEntityProjection());
        assertSame(projectionInfo, requestScope.getProjectionInfo());
    }
}
