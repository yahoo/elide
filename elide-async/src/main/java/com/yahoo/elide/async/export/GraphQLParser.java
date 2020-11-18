/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.export;

import com.yahoo.elide.Elide;
import com.yahoo.elide.async.export.validator.Validator;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.core.exceptions.BadRequestException;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.graphql.QueryRunner;
import com.yahoo.elide.graphql.parser.GraphQLEntityProjectionMaker;
import com.yahoo.elide.graphql.parser.GraphQLProjectionInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

/**
 * GraphQL specific implementation of TableExportParser.
 */
public class GraphQLParser implements TableExportParser {

    private Elide elide;
    private String apiVersion;
    private Set<Validator> validators;

    public GraphQLParser(Elide elide, String apiVersion) {
        this.elide = elide;
        this.apiVersion = apiVersion;
        //TODO Initialize static list of validators for GraphqlParser
    }

    @Override
    public EntityProjection parse(AsyncQuery query) throws BadRequestException {
        EntityProjection projection;
        try {
            String graphQLDocument = query.getQuery();
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
