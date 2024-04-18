/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.graphql;

import com.paiondata.elide.ElideSettings;
import com.paiondata.elide.core.RequestScope;
import com.paiondata.elide.core.datastore.DataStoreTransaction;
import com.paiondata.elide.core.request.EntityProjection;
import com.paiondata.elide.core.request.route.Route;
import com.paiondata.elide.core.security.User;
import com.paiondata.elide.graphql.parser.GraphQLProjectionInfo;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Persistent state across GraphQL requests. This object is stored in the environment context.
 */
public class GraphQLRequestScope extends RequestScope {
    @Getter private final Map<String, Long> totalRecordCounts = new HashMap<>();

    @Getter
    private final GraphQLProjectionInfo projectionInfo;

    public GraphQLRequestScope(
            Route route,
            DataStoreTransaction transaction,
            User user,
            UUID requestId,
            ElideSettings elideSettings,
            Function<RequestScope, EntityProjection> entityProjection,
            GraphQLProjectionInfo projectionInfo
    ) {
        // TODO: We're going to break out the two request scopes. `RequestScope` should become an interface and
        // we should have a GraphQLRequestScope and a JSONAPIRequestScope.
        // TODO: What should mutate multiple entity value be? There is a problem with this setting in practice.
        // Namely, we don't filter or paginate in the data store.
        super(route, transaction, user,
                requestId, elideSettings, entityProjection);
        this.projectionInfo = projectionInfo;
    }

    public static GraphQLRequestScopeBuilder builder() {
        return new GraphQLRequestScopeBuilder();
    }

    public static class GraphQLRequestScopeBuilder extends RequestScopeBuilder {
        protected GraphQLProjectionInfo projectionInfo;

        public GraphQLRequestScopeBuilder projectionInfo(GraphQLProjectionInfo projectionInfo) {
            this.projectionInfo = projectionInfo;
            return this;
        }

        @Override
        public GraphQLRequestScope build() {
            applyDefaults();
            return new GraphQLRequestScope(this.route, this.dataStoreTransaction, this.user, this.requestId,
                    this.elideSettings, this.entityProjection, this.projectionInfo);
        }

        @Override
        public GraphQLRequestScopeBuilder route(Route route) {
            super.route(route);
            return this;
        }

        @Override
        public GraphQLRequestScopeBuilder dataStoreTransaction(DataStoreTransaction transaction) {
            super.dataStoreTransaction(transaction);
            return this;
        }

        @Override
        public GraphQLRequestScopeBuilder user(User user) {
            super.user(user);
            return this;
        }

        @Override
        public GraphQLRequestScopeBuilder requestId(UUID requestId) {
            super.requestId(requestId);
            return this;
        }

        @Override
        public GraphQLRequestScopeBuilder elideSettings(ElideSettings elideSettings) {
            super.elideSettings(elideSettings);
            return this;
        }

        @Override
        public GraphQLRequestScopeBuilder entityProjection(Function<RequestScope, EntityProjection> entityProjection) {
            super.entityProjection(entityProjection);
            return this;
        }

        @Override
        public GraphQLRequestScopeBuilder entityProjection(EntityProjection entityProjection) {
            super.entityProjection(entityProjection);
            return this;
        }
    }
}
