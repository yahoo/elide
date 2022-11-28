/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.security.User;
import com.yahoo.elide.graphql.parser.GraphQLProjectionInfo;

import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.MultivaluedHashMap;

/**
 * Persistent state across GraphQL requests. This object is stored in the environment context.
 */
public class GraphQLRequestScope extends RequestScope {
    @Getter private final Map<String, Long> totalRecordCounts = new HashMap<>();

    @Getter
    private final GraphQLProjectionInfo projectionInfo;

    public GraphQLRequestScope(
            String baseUrlEndpoint,
            DataStoreTransaction transaction,
            User user,
            String apiVersion,
            ElideSettings elideSettings,
            GraphQLProjectionInfo projectionInfo,
            UUID requestId,
            Map<String, List<String>> requestHeaders
    ) {
        // TODO: We're going to break out the two request scopes. `RequestScope` should become an interface and
        // we should have a GraphQLRequestScope and a JSONAPIRequestScope.
        // TODO: What should mutate multiple entity value be? There is a problem with this setting in practice.
        // Namely, we don't filter or paginate in the data store.
        super(baseUrlEndpoint, "/", apiVersion, null, transaction, user,
                new MultivaluedHashMap<>(), requestHeaders, requestId, elideSettings);
        this.projectionInfo = projectionInfo;

        // Entity Projection is retrieved from projectionInfo.
        this.setEntityProjection(null);
    }
}
