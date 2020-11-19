/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service.thread;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;
import com.yahoo.elide.Elide;
import com.yahoo.elide.async.models.AsyncAPI;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.QueryStatus;
import com.yahoo.elide.async.service.dao.AsyncAPIDAO;
import com.yahoo.elide.core.Path.PathElement;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.TransactionRegistry;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.predicates.InPredicate;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.google.common.collect.Sets;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Runnable thread for cancelling AsyncAPI transactions
 * beyond the max run time or if it has status CANCELLED.
 */
@Slf4j
@Data
@AllArgsConstructor
public class AsyncAPICancelThread implements Runnable {

    private int maxRunTimeSeconds;
    private Elide elide;
    private AsyncAPIDAO asyncAPIDao;

    @Override
    public void run() {
        cancelAsyncAPI(AsyncQuery.class);
    }

    /**
     * This method cancels queries based on threshold.
     * @param type AsyncAPI Type Implementation.
     */
    protected <T extends AsyncAPI> void cancelAsyncAPI(Class<T> type) {

        try {
            TransactionRegistry transactionRegistry = elide.getTransactionRegistry();

            Map<UUID, DataStoreTransaction> runningTransactionMap = transactionRegistry.getRunningTransactions();

            //Running transaction UUIDs
            Set<UUID> runningTransactionUUIDs = runningTransactionMap.keySet();

            //Construct filter expression
            PathElement statusPathElement = new PathElement(type, QueryStatus.class, "status");
            List<QueryStatus> statusList = new ArrayList<QueryStatus>();
            statusList.add(QueryStatus.CANCELLED);
            statusList.add(QueryStatus.PROCESSING);
            statusList.add(QueryStatus.QUEUED);
            FilterExpression fltStatusExpression =
                    new InPredicate(statusPathElement, statusList);

            Collection<T> asyncAPICollection =
                    asyncAPIDao.loadAsyncAPICollection(fltStatusExpression, type);

            //Active AsyncAPI UUIDs
            Set<UUID> asyncTransactionUUIDs = asyncAPICollection.stream()
                    .filter(query -> query.getStatus() == QueryStatus.CANCELLED
                    || TimeUnit.SECONDS.convert(Math.abs(new Date(System.currentTimeMillis()).getTime()
                            - query.getCreatedOn().getTime()), TimeUnit.MILLISECONDS) > maxRunTimeSeconds)
                    .map(query -> UUID.fromString(query.getRequestId()))
            .collect(Collectors.toSet());

            //AsyncAPI UUIDs that have active transactions
            Set<UUID> queryUUIDsToCancel = Sets.intersection(runningTransactionUUIDs, asyncTransactionUUIDs);

            //AsyncAPI IDs that need to be cancelled
            Set<String> queryIDsToCancel = queryUUIDsToCancel.stream()
            .map(uuid -> {
                return asyncAPICollection.stream()
                .filter(query -> query.getRequestId().equals(uuid.toString()))
                .map(T::getId)
                .findFirst().orElseThrow(IllegalStateException::new);
            })
            .collect(Collectors.toSet());

            //Cancel Transactions
            queryUUIDsToCancel.stream()
               .forEach((uuid) -> {
                   DataStoreTransaction runningTransaction = transactionRegistry.getRunningTransaction(uuid);
                   if (runningTransaction != null) {
                       JsonApiDocument jsonApiDoc = new JsonApiDocument();
                       MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<String, String>();
                       RequestScope scope = new RequestScope("", "query", NO_VERSION, jsonApiDoc,
                               runningTransaction, null, queryParams, null,
                               uuid, elide.getElideSettings());
                       runningTransaction.cancel(scope);
                   }
               });

            //Change queryStatus for cancelled queries
            if (!queryIDsToCancel.isEmpty()) {
                PathElement idPathElement = new PathElement(type, String.class, "id");
                FilterExpression fltIdExpression =
                        new InPredicate(idPathElement, queryIDsToCancel);
                asyncAPIDao.updateStatusAsyncAPICollection(fltIdExpression, QueryStatus.CANCEL_COMPLETE,
                        type);
            }
        } catch (Exception e) {
            log.error("Exception in scheduled cancellation: {}", e);
        }
    }
}
