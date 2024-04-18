/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.graphql.subscriptions;

import com.paiondata.elide.core.PersistentResource;
import com.paiondata.elide.core.exceptions.InvalidEntityBodyException;
import com.paiondata.elide.core.request.EntityProjection;
import com.paiondata.elide.graphql.Environment;
import com.paiondata.elide.graphql.NonEntityDictionary;
import com.paiondata.elide.graphql.QueryLogger;
import com.paiondata.elide.graphql.RelationshipOp;
import com.paiondata.elide.graphql.subscriptions.containers.SubscriptionNodeContainer;

import graphql.language.OperationDefinition;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;

/**
 * Data Fetcher which fetches Elide subscription models.
 */
@Slf4j
public class SubscriptionDataFetcher implements DataFetcher<Object>, QueryLogger {

    private final NonEntityDictionary nonEntityDictionary;
    private final Integer bufferSize;

    /**
     * Constructor.
     * @param nonEntityDictionary Entity dictionary for types that are not Elide models.
     */
    public SubscriptionDataFetcher(NonEntityDictionary nonEntityDictionary) {
        this(nonEntityDictionary, 100);
    }

    /**
     * Constructor.
     * @param nonEntityDictionary Entity dictionary for types that are not Elide models.
     * @param bufferSize Internal buffer for reactive streams.
     */
    public SubscriptionDataFetcher(NonEntityDictionary nonEntityDictionary, int bufferSize) {
        this.nonEntityDictionary = nonEntityDictionary;
        this.bufferSize = bufferSize;
    }

    @Override
    public Object get(DataFetchingEnvironment environment) throws Exception {
        OperationDefinition.Operation op = environment.getOperationDefinition().getOperation();
        if (op != OperationDefinition.Operation.SUBSCRIPTION) {
            throw new InvalidEntityBodyException(String.format("%s not supported for subscription models.", op));
        }

        /* build environment object, extracts required fields */
        Environment context = new Environment(environment, nonEntityDictionary);


        /* safe enable debugging */
        if (log.isDebugEnabled()) {
            logContext(log, RelationshipOp.FETCH, context);
        }

        if (context.isRoot()) {
            String entityName = context.field.getName();
            String aliasName = context.field.getAlias();
            EntityProjection projection = context.requestScope
                    .getProjectionInfo()
                    .getProjection(aliasName, entityName);

            Flowable<PersistentResource> recordPublisher =
                    PersistentResource.loadRecords(projection, new ArrayList<>(), context.requestScope)
                            .toFlowable(BackpressureStrategy.BUFFER)
                            .onBackpressureBuffer(bufferSize, true, false);

            return recordPublisher.map(SubscriptionNodeContainer::new);
        }

        //If this is not the root, instead of returning a reactive publisher, we process same
        //as the PersistentResourceFetcher.
        return context.container.processFetch(context);
    }
}
