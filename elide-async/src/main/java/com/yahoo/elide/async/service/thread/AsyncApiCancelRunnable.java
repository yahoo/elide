/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service.thread;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;

import com.yahoo.elide.Elide;
import com.yahoo.elide.async.models.AsyncApi;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.QueryStatus;
import com.yahoo.elide.async.service.dao.AsyncApiDao;
import com.yahoo.elide.core.Path.PathElement;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.TransactionRegistry;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.predicates.InPredicate;
import com.yahoo.elide.core.request.route.Route;
import com.yahoo.elide.jsonapi.JsonApiRequestScope;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.google.common.collect.Sets;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Runnable for cancelling AsyncAPI transactions
 * beyond the max run time or if it has status CANCELLED.
 */
@Slf4j
@Data
public class AsyncApiCancelRunnable implements Runnable {

    private long queryMaxRunTimeSeconds;
    private Elide elide;
    private AsyncApiDao asyncAPIDao;

    public AsyncApiCancelRunnable(Duration queryMaxRunTime, Elide elide, AsyncApiDao asyncAPIDao) {
        this.queryMaxRunTimeSeconds = queryMaxRunTime.toSeconds();
        this.elide = elide;
        this.asyncAPIDao = asyncAPIDao;
    }

    @Override
    public void run() {
        cancelAsyncAPI(AsyncQuery.class);
    }

    /**
     * This method cancels queries based on threshold.
     * @param type AsyncAPI Type Implementation.
     */
    protected <T extends AsyncApi> void cancelAsyncAPI(Class<T> type) {

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

            Iterable<T> asyncAPIIterable =
                    asyncAPIDao.loadAsyncApiByFilter(fltStatusExpression, type);

            //Active AsyncAPI UUIDs
            Set<UUID> asyncTransactionUUIDs = StreamSupport.stream(asyncAPIIterable.spliterator(), false)
                    .filter(query -> query.getStatus() == QueryStatus.CANCELLED
                    || TimeUnit.SECONDS.convert(Math.abs(new Date(System.currentTimeMillis()).getTime()
                            - query.getCreatedOn().getTime()), TimeUnit.MILLISECONDS) > queryMaxRunTimeSeconds)
                    .map(query -> UUID.fromString(query.getRequestId()))
            .collect(Collectors.toSet());

            //AsyncAPI UUIDs that have active transactions
            Set<UUID> queryUUIDsToCancel = Sets.intersection(runningTransactionUUIDs, asyncTransactionUUIDs);

            //AsyncAPI IDs that need to be cancelled
            Set<String> queryIDsToCancel = queryUUIDsToCancel.stream()
            .map(uuid -> StreamSupport
                .stream(asyncAPIIterable.spliterator(), false)
                .filter(query -> query.getRequestId().equals(uuid.toString()))
                .map(T::getId)
                .findFirst().orElseThrow(IllegalStateException::new))
            .collect(Collectors.toSet());

            //Cancel Transactions
            queryUUIDsToCancel.stream().forEach(uuid -> {
                DataStoreTransaction runningTransaction = transactionRegistry.getRunningTransaction(uuid);
                if (runningTransaction != null) {
                    JsonApiDocument jsonApiDoc = new JsonApiDocument();
                    MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
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
                asyncAPIDao.updateStatusAsyncApiByFilter(fltIdExpression, QueryStatus.CANCEL_COMPLETE,
                        type);
            }
        } catch (Exception e) {
            log.error("Exception in scheduled cancellation: {}", e.toString());
        }
    }
}
