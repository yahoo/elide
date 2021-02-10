/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.operation;

import com.yahoo.elide.Elide;
import com.yahoo.elide.async.export.formatter.TableExportFormatter;
import com.yahoo.elide.async.models.AsyncAPI;
import com.yahoo.elide.async.models.TableExport;
import com.yahoo.elide.async.service.AsyncExecutorService;
import com.yahoo.elide.async.service.storageengine.ResultStorageEngine;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.exceptions.BadRequestException;
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
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;

/**
 * TableExport Execute Operation Interface.
 */
@Slf4j
public class GraphQLTableExportOperation extends TableExportOperation {

    public GraphQLTableExportOperation(TableExportFormatter formatter, AsyncExecutorService service,
            AsyncAPI export, RequestScope scope, ResultStorageEngine engine) {
        super(formatter, service, export, scope, engine);
    }

    @Override
    public RequestScope getRequestScope(TableExport export, User user, String apiVersion,
            DataStoreTransaction tx) {
        UUID requestId = UUID.fromString(export.getRequestId());
        return new GraphQLRequestScope("", tx, user, apiVersion, getService().getElide().getElideSettings(),
                null, requestId, Collections.emptyMap());
    }

    @Override
    public EntityProjection getProjection(TableExport export, String apiVersion)
            throws BadRequestException {
        EntityProjection projection;
        try {
            String graphQLDocument = export.getQuery();
            Elide elide = getService().getElide();
            ObjectMapper mapper = elide.getMapper().getObjectMapper();

            JsonNode node = QueryRunner.getTopLevelNode(mapper, graphQLDocument);
            Map<String, Object> variables = QueryRunner.extractVariables(mapper, node);
            String queryString = QueryRunner.extractQuery(node);

            GraphQLProjectionInfo projectionInfo =
                    new GraphQLEntityProjectionMaker(elide.getElideSettings(), variables, apiVersion)
                        .make(queryString);

            //TODO Call Validators.
            Optional<Entry<String, EntityProjection>> optionalEntry =
                    projectionInfo.getProjections().entrySet().stream().findFirst();

            projection = optionalEntry.isPresent() ? optionalEntry.get().getValue() : null;

        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        return projection;
    }
}
