/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql.federation;

import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.exceptions.BadRequestException;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.graphql.GraphQLRequestScope;
import com.yahoo.elide.graphql.KeyWord;
import com.yahoo.elide.graphql.containers.NodeContainer;

import com.apollographql.federation.graphqljava._Entity;

import org.apache.commons.lang3.StringUtils;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Entities Data Fetcher for Apollo Federation.
 */
public class EntitiesDataFetcher implements DataFetcher<List<NodeContainer>> {

    @Override
    public List<NodeContainer> get(DataFetchingEnvironment environment) throws Exception {
        List<Map<String, Object>> representations = environment.getArgument(_Entity.argumentName);

        if (representations == null || representations.isEmpty()) {
            throw new BadRequestException("Empty list passed to representations");
        }

        GraphQLRequestScope requestScope = environment.getLocalContext();

        // A single _entities call can contain representations of multiple types. Group the
        // representation indices by their own __typename so each type is resolved with its own
        // projection, then return the results in the same order as the input representations.
        Map<String, List<Integer>> indicesByType = new LinkedHashMap<>();
        for (int index = 0; index < representations.size(); index++) {
            String typeName = representations.get(index).get(KeyWord.TYPENAME.getName()).toString();
            indicesByType.computeIfAbsent(typeName, key -> new ArrayList<>()).add(index);
        }

        NodeContainer[] nodes = new NodeContainer[representations.size()];

        for (Map.Entry<String, List<Integer>> entry : indicesByType.entrySet()) {
            String entityName = StringUtils.uncapitalize(entry.getKey());
            List<Integer> indices = entry.getValue();

            List<String> ids = indices.stream()
                    .map(index -> getId(representations.get(index)))
                    .toList();

            EntityProjection projection = requestScope.getProjectionInfo().getProjection(null, entityName);

            // Ignore errors as potentially an id on a subgraph no longer exists here
            Set<PersistentResource> results = PersistentResource.loadRecords(projection, ids, requestScope)
                    .onErrorResume(error -> Flux.empty())
                    .collect(Collectors.toCollection(LinkedHashSet::new))
                    .block();

            for (Integer index : indices) {
                String id = getId(representations.get(index));
                nodes[index] = results.stream()
                        .filter(resource -> id.equals(resource.getId()))
                        .findFirst()
                        .map(NodeContainer::new)
                        .orElse(null);
            }
        }

        // Return node containers in the order of the input representations.
        return Arrays.asList(nodes);
    }

    /**
     * Extracts the entity id from a representation: the single key field that is not __typename.
     * Only single (non-composite) id keys are supported, matching the original behavior.
     */
    private static String getId(Map<String, Object> representation) {
        String idKey = representation.keySet().stream()
                .filter(key -> !KeyWord.TYPENAME.getName().equals(key))
                .findFirst()
                .get();
        return (String) representation.get(idKey);
    }
}
