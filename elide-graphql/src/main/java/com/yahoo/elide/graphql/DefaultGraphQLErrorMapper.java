/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

import com.yahoo.elide.ElideError;

import graphql.ErrorClassification;
import graphql.GraphQLError;
import graphql.execution.ResultPath;
import graphql.language.SourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Default {@link GraphQLErrorMapper}.
 */
public class DefaultGraphQLErrorMapper implements GraphQLErrorMapper {

    @Override
    public GraphQLError toGraphQLError(ElideError error) {
        com.yahoo.elide.graphql.models.GraphQLError.GraphQLErrorBuilder graphqlError =
                com.yahoo.elide.graphql.models.GraphQLError.builder();
        if (error.getMessage() != null) {
            graphqlError.message(error.getMessage()); // The serializer will encode the message
        }
        if (error.getAttributes() != null && !error.getAttributes().isEmpty()) {
            Map<String, Object> extensions = new LinkedHashMap<>(error.getAttributes());
            if (attribute("classification", extensions) instanceof String classification) {
                graphqlError.errorType(ErrorClassification.errorClassification(classification));
            }
            Object path = attribute("path", extensions);
            if (path instanceof List<?> list) {
                List<Object> result = new ArrayList<>();
                list.forEach(result::add);
                graphqlError.path(result);
            } else if (path instanceof ResultPath resultPath) {
                graphqlError.path(resultPath);
            }
            Object locations = attribute("locations", extensions);
            if (locations instanceof List<?> list) {
                List<SourceLocation> result = new ArrayList<>();
                for (Object item : list) {
                    if (item instanceof SourceLocation sourceLocation) {
                        result.add(sourceLocation);
                    }
                }
                graphqlError.locations(result);
            }

            if (!extensions.isEmpty()) {
                graphqlError.extensions(extensions);
            }
        }
        return graphqlError.build();
    }

    private Object attribute(String key, Map<String, Object> map) {
        if (map.containsKey(key)) {
            Object result = map.get(key);
            map.remove(key);
            return result;
        }
        return null;
    }
}
