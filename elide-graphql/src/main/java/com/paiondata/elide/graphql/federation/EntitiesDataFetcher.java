/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.graphql.federation;

import com.paiondata.elide.core.PersistentResource;
import com.paiondata.elide.core.exceptions.BadRequestException;
import com.paiondata.elide.core.request.EntityProjection;
import com.paiondata.elide.graphql.GraphQLRequestScope;
import com.paiondata.elide.graphql.KeyWord;
import com.paiondata.elide.graphql.containers.NodeContainer;

import com.apollographql.federation.graphqljava._Entity;

import org.apache.commons.lang3.StringUtils;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import io.reactivex.Observable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
        Observable<PersistentResource> records = Optional.of(ids).map((idList) -> {
            /* handle empty list of ids */
            if (idList.isEmpty()) {
                throw new BadRequestException("Empty list passed to ids");
            }

            return PersistentResource.loadRecords(projection, idList, requestScope);
        }).orElseGet(() -> PersistentResource.loadRecords(projection, new ArrayList<>(), requestScope));


        // Ignore errors as potentially an id on a subgraph no longer exists here
        Set<PersistentResource> results = records.onExceptionResumeNext(Observable.empty())
                .toList(LinkedHashSet::new)
                .blockingGet();

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
