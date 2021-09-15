/*
 * Copyright 2021, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql.subscriptions.containers;

import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.graphql.ElideDataFetcher;
import com.yahoo.elide.graphql.Environment;
import com.yahoo.elide.graphql.containers.GraphQLContainer;
import com.yahoo.elide.graphql.containers.NodeContainer;
import org.reactivestreams.Publisher;

import io.reactivex.Flowable;
import lombok.AllArgsConstructor;

/**
 * Container which wraps a stream of nodes - each which wrap a PersistentResource.
 */
@AllArgsConstructor
public class SubscriptionCollectionContainer implements GraphQLContainer<Publisher<NodeContainer>> {

    Flowable<PersistentResource> resourcePublisher;

    @Override
    public Publisher<NodeContainer> processFetch(Environment context, ElideDataFetcher fetcher) {
        return resourcePublisher.map(NodeContainer::new);
    }
}
