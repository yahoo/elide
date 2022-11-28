/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.operation;

import com.yahoo.elide.Elide;
import com.yahoo.elide.async.export.formatter.TableExportFormatter;
import com.yahoo.elide.async.export.validator.NoRelationshipsProjectionValidator;
import com.yahoo.elide.async.models.AsyncAPI;
import com.yahoo.elide.async.models.TableExport;
import com.yahoo.elide.async.service.AsyncExecutorService;
import com.yahoo.elide.async.service.storageengine.ResultStorageEngine;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.security.User;
import com.yahoo.elide.graphql.GraphQLRequestScope;
import com.yahoo.elide.graphql.QueryRunner;
import com.yahoo.elide.graphql.parser.GraphQLEntityProjectionMaker;
import com.yahoo.elide.graphql.parser.GraphQLProjectionInfo;
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
            AsyncAPI export, RequestScope scope, ResultStorageEngine engine) {
        super(formatter, service, export, scope, engine,
                        Arrays.asList(new NoRelationshipsProjectionValidator()));
    }

    @Override
    public RequestScope getRequestScope(TableExport export, RequestScope scope, DataStoreTransaction tx,
            Map<String, List<String>> additionalRequestHeaders) {
        UUID requestId = UUID.fromString(export.getRequestId());
        User user = scope.getUser();
        String apiVersion = scope.getApiVersion();
        return new GraphQLRequestScope("", tx, user, apiVersion, getService().getElide().getElideSettings(),
                null, requestId, additionalRequestHeaders);
    }

    @Override
    public Collection<EntityProjection> getProjections(TableExport export, RequestScope scope) {
        GraphQLProjectionInfo projectionInfo;
        try {
            String graphQLDocument = export.getQuery();
            Elide elide = getService().getElide();
            ObjectMapper mapper = elide.getMapper().getObjectMapper();

            JsonNode node = QueryRunner.getTopLevelNode(mapper, graphQLDocument);
            Map<String, Object> variables = QueryRunner.extractVariables(mapper, node);
            String queryString = QueryRunner.extractQuery(node);

            projectionInfo = new GraphQLEntityProjectionMaker(elide.getElideSettings(), variables,
                            scope.getApiVersion()).make(queryString);

        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        return projectionInfo.getProjections().values();
    }
}
