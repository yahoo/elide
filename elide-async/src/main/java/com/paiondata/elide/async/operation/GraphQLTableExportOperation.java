/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.async.operation;

import com.paiondata.elide.Elide;
import com.paiondata.elide.async.ResultTypeFileExtensionMapper;
import com.paiondata.elide.async.export.formatter.TableExportFormatter;
import com.paiondata.elide.async.export.validator.NoRelationshipsProjectionValidator;
import com.paiondata.elide.async.models.AsyncApi;
import com.paiondata.elide.async.models.TableExport;
import com.paiondata.elide.async.service.AsyncExecutorService;
import com.paiondata.elide.async.service.storageengine.ResultStorageEngine;
import com.paiondata.elide.core.RequestScope;
import com.paiondata.elide.core.datastore.DataStoreTransaction;
import com.paiondata.elide.core.request.EntityProjection;
import com.paiondata.elide.core.request.route.Route;
import com.paiondata.elide.core.security.User;
import com.paiondata.elide.graphql.GraphQLRequestScope;
import com.paiondata.elide.graphql.QueryRunner;
import com.paiondata.elide.graphql.parser.GraphQLEntityProjectionMaker;
import com.paiondata.elide.graphql.parser.GraphQLProjectionInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * TableExport Execute Operation Interface.
 */
@Slf4j
public class GraphQLTableExportOperation extends TableExportOperation {

    public GraphQLTableExportOperation(TableExportFormatter formatter, AsyncExecutorService service,
            AsyncApi export, RequestScope scope, ResultStorageEngine engine,
            ResultTypeFileExtensionMapper resultTypeFileExtensionMapper) {
        super(formatter, service, export, scope, engine, Arrays.asList(new NoRelationshipsProjectionValidator()),
                resultTypeFileExtensionMapper);
    }

    @Override
    public RequestScope getRequestScope(TableExport export, RequestScope scope, DataStoreTransaction tx,
            Map<String, List<String>> additionalRequestHeaders) {
        UUID requestId = UUID.fromString(export.getRequestId());
        User user = scope.getUser();
        String apiVersion = scope.getRoute().getApiVersion();
        Route route = Route.builder().baseUrl("").apiVersion(apiVersion).headers(additionalRequestHeaders).build();
        return GraphQLRequestScope.builder().route(route).dataStoreTransaction(tx).user(user).requestId(requestId)
                .elideSettings(getService().getElide().getElideSettings()).build();
    }

    @Override
    public Collection<EntityProjection> getProjections(TableExport export, RequestScope scope) {
        GraphQLProjectionInfo projectionInfo;
        try {
            String graphQLDocument = export.getQuery();
            Elide elide = getService().getElide();
            ObjectMapper mapper = elide.getObjectMapper();

            JsonNode node = QueryRunner.getTopLevelNode(mapper, graphQLDocument);
            Map<String, Object> variables = QueryRunner.extractVariables(mapper, node);
            String queryString = QueryRunner.extractQuery(node);

            projectionInfo = new GraphQLEntityProjectionMaker(elide.getElideSettings(), variables,
                            scope.getRoute().getApiVersion()).make(queryString);

        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        return projectionInfo.getProjections().values();
    }
}
