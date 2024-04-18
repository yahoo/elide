/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.async.service.thread;

import static com.paiondata.elide.core.dictionary.EntityDictionary.NO_VERSION;

import com.paiondata.elide.Elide;
import com.paiondata.elide.async.models.AsyncApi;
import com.paiondata.elide.async.models.AsyncQuery;
import com.paiondata.elide.async.models.QueryStatus;
import com.paiondata.elide.async.service.dao.AsyncApiDao;
import com.paiondata.elide.core.Path.PathElement;
import com.paiondata.elide.core.RequestScope;
import com.paiondata.elide.core.TransactionRegistry;
import com.paiondata.elide.core.datastore.DataStoreTransaction;
import com.paiondata.elide.core.filter.expression.FilterExpression;
import com.paiondata.elide.core.filter.predicates.InPredicate;
import com.paiondata.elide.core.request.route.Route;
import com.paiondata.elide.jsonapi.JsonApiRequestScope;
import com.paiondata.elide.jsonapi.models.JsonApiDocument;
import com.google.common.collect.Sets;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Runnable for cancelling AsyncApi transactions
 * beyond the max run time or if it has status CANCELLED.
 */
@Slf4j
@Data
public class AsyncApiCancelRunnable implements Runnable {

    private long queryMaxRunTimeSeconds;
    private Elide elide;
    private AsyncApiDao asyncApiDao;

    public AsyncApiCancelRunnable(Duration queryMaxRunTime, Elide elide, AsyncApiDao asyncApiDao) {
        this.queryMaxRunTimeSeconds = queryMaxRunTime.toSeconds();
        this.elide = elide;
        this.asyncApiDao = asyncApiDao;
    }

    @Override
    public void run() {
        cancelAsyncApi(AsyncQuery.class);
    }

    /**
     * This method cancels queries based on threshold.
     * @param type AsyncApi Type Implementation.
     */
    protected <T extends AsyncApi> void cancelAsyncApi(Class<T> type) {

        try {
            TransactionRegistry transactionRegistry = elide.getTransactionRegistry();

            Map<UUID, DataStoreTransaction> runningTransactionMap = transactionRegistry.getRunningTransactions();

            //Running transaction UUIDs
            Set<UUID> runningTransactionUUIDs = runningTransactionMap.keySet();

            //Construct filter expression
            PathElement statusPathElement = new PathElement(type, QueryStatus.class, "status");
            FilterExpression fltStatusExpression =
                    new InPredicate(statusPathElement, QueryStatus.CANCELLED, QueryStatus.PROCESSING,
                            QueryStatus.QUEUED);

            Iterable<T> asyncApiIterable =
                    asyncApiDao.loadAsyncApiByFilter(fltStatusExpression, type);

            //Active AsyncApi UUIDs
            Set<UUID> asyncTransactionUUIDs = StreamSupport.stream(asyncApiIterable.spliterator(), false)
                    .filter(query -> query.getStatus() == QueryStatus.CANCELLED
                    || TimeUnit.SECONDS.convert(Math.abs(new Date(System.currentTimeMillis()).getTime()
                            - query.getCreatedOn().getTime()), TimeUnit.MILLISECONDS) > queryMaxRunTimeSeconds)
                    .map(query -> UUID.fromString(query.getRequestId()))
            .collect(Collectors.toSet());

            //AsyncApi UUIDs that have active transactions
            Set<UUID> queryUUIDsToCancel = Sets.intersection(runningTransactionUUIDs, asyncTransactionUUIDs);

            //AsyncApi IDs that need to be cancelled
            Set<String> queryIDsToCancel = queryUUIDsToCancel.stream()
            .map(uuid -> StreamSupport
                .stream(asyncApiIterable.spliterator(), false)
                .filter(query -> query.getRequestId().equals(uuid.toString()))
                .map(T::getId)
                .findFirst().orElseThrow(IllegalStateException::new))
            .collect(Collectors.toSet());

            //Cancel Transactions
            queryUUIDsToCancel.stream().forEach(uuid -> {
                DataStoreTransaction runningTransaction = transactionRegistry.getRunningTransaction(uuid);
                if (runningTransaction != null) {
                    JsonApiDocument jsonApiDoc = new JsonApiDocument();
                    Map<String, List<String>> queryParams = new LinkedHashMap<>();
                    Route route = Route.builder().path("query").apiVersion(NO_VERSION).parameters(queryParams).build();
                    RequestScope scope = JsonApiRequestScope.builder().route(route)
                            .dataStoreTransaction(runningTransaction).requestId(uuid)
                            .elideSettings(elide.getElideSettings()).jsonApiDocument(jsonApiDoc).build();
                    runningTransaction.cancel(scope);
                }
            });

            //Change queryStatus for cancelled queries
            if (!queryIDsToCancel.isEmpty()) {
                PathElement idPathElement = new PathElement(type, String.class, "id");
                FilterExpression fltIdExpression =
                        new InPredicate(idPathElement, queryIDsToCancel);
                asyncApiDao.updateStatusAsyncApiByFilter(fltIdExpression, QueryStatus.CANCEL_COMPLETE,
                        type);
            }
        } catch (Exception e) {
            log.error("Exception in scheduled cancellation: {}", e.toString());
        }
    }
}
