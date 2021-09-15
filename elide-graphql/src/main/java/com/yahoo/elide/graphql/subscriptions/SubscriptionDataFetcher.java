/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql.subscriptions;

import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.request.Relationship;
import com.yahoo.elide.graphql.ElideDataFetcher;
import com.yahoo.elide.graphql.Environment;
import com.yahoo.elide.graphql.NonEntityDictionary;
import com.yahoo.elide.graphql.subscriptions.containers.SubscriptionCollectionContainer;
import org.reactivestreams.Publisher;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Fetcher which fetches Elide subscription models.
 */
@Slf4j
public class SubscriptionDataFetcher extends ElideDataFetcher implements DataFetcher<Publisher<Object>> {

    private final Integer bufferSize;

    /**
     * Constructor
     * @param nonEntityDictionary Entity dictionary for types that are not Elide models.
     */
    public SubscriptionDataFetcher(NonEntityDictionary nonEntityDictionary) {
        this(nonEntityDictionary, 100);
    }

    /**
     * Constructor
     * @param nonEntityDictionary Entity dictionary for types that are not Elide models.
     * @param bufferSize Internal buffer for reactive streams.
     */
    public SubscriptionDataFetcher(NonEntityDictionary nonEntityDictionary, int bufferSize) {
        super(nonEntityDictionary);
        this.bufferSize = bufferSize;
    }

    @Override
    public Publisher<Object> get(DataFetchingEnvironment environment) throws Exception {
        /* build environment object, extracts required fields */
        Environment context = new Environment(environment);

        /* safe enable debugging */
        if (log.isDebugEnabled()) {
            //TODO refactor logging for common logging.
        }

        // Process fetch object for this container
        return (Publisher<Object>) context.container.processFetch(context, this);
    }

    @Override
    public SubscriptionCollectionContainer fetchRelationship(
            PersistentResource<?> parentResource,
            Relationship relationship,
            Optional<List<String>> unused) {

        Flowable<PersistentResource> recordPublisher = parentResource.getRelationCheckedFiltered(relationship)
                .toFlowable(BackpressureStrategy.BUFFER)
                .onBackpressureBuffer(bufferSize, true, false);

        return new SubscriptionCollectionContainer(recordPublisher);
    }

    @Override
    public SubscriptionCollectionContainer fetchObject(
            RequestScope requestScope,
            EntityProjection projection,
            Optional<List<String>> unused) {

        Flowable<PersistentResource> recordPublisher =
                PersistentResource.loadRecords(projection, new ArrayList<>(), requestScope)
                        .toFlowable(BackpressureStrategy.BUFFER)
                        .onBackpressureBuffer(bufferSize, true, false);

        return new SubscriptionCollectionContainer(recordPublisher);
    }
}
