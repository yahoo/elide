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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Entities Data Fetcher for Apollo Federation.
 */
public class EntitiesDataFetcher implements DataFetcher<List<NodeContainer>> {

    @Override
    public List<NodeContainer> get(DataFetchingEnvironment environment) throws Exception {
        List<Map<String, Object>> representations = environment.getArgument(_Entity.argumentName);
        List<String> ids = representations.stream() // only supports the single id key field
            .map(representation -> {
                    String idKey = representation.keySet()
                            .stream()
                            .filter(key -> !KeyWord.TYPENAME.getName().equals(key))
                            .findFirst()
                            .get();
                    return (String) representation.get(idKey);
                })
            .toList();

        String entityName = StringUtils.uncapitalize(representations.get(0).get(KeyWord.TYPENAME.getName()).toString());

        GraphQLRequestScope requestScope = environment.getLocalContext();
        EntityProjection projection = requestScope
                .getProjectionInfo()
                .getProjection(null, entityName);

        /* fetching a collection */
        Flux<PersistentResource> records = Optional.of(ids).map((idList) -> {
            /* handle empty list of ids */
            if (idList.isEmpty()) {
                throw new BadRequestException("Empty list passed to ids");
            }

            return PersistentResource.loadRecords(projection, idList, requestScope);
        }).orElseGet(() -> PersistentResource.loadRecords(projection, Collections.emptyList(), requestScope));

        // Ignore errors as potentially an id on a subgraph no longer exists here
        Set<PersistentResource> results = records.onErrorResume(error -> Flux.empty())
                .collect(Collectors.toCollection(LinkedHashSet::new)).block();

        // Return node containers in order of the ids from the representations
        return ids.stream().map(id -> {
            Optional<PersistentResource> result = results.stream().filter(r -> id.equals(r.getId())).findFirst();
            if (result.isPresent()) {
                return new NodeContainer(result.get());
            } else {
                return null;
            }
        }).toList();
    }
}
