/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service;

import java.io.IOException;
import java.security.Principal;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.yahoo.elide.Elide;
import com.yahoo.elide.async.models.QueryStatus;
import com.yahoo.elide.async.models.QueryType;
import com.yahoo.elide.graphql.QueryRunner;

import lombok.extern.slf4j.Slf4j;

/**
 * Service to execute Async queries. It will schedule task to track long
 * running queries and kills them. It will also schedule task to update
 * orphan query statuses after host/app crash or restart.
 */
@Slf4j
@Singleton
public class AsyncExecutorService {

    private final int DEFAULT_THREADPOOL_SIZE = 6;

    private Elide elide;
    private QueryRunner runner;
    private ExecutorService executor;
    private ExecutorService interruptor;
    private int maxRunTime;

    @Inject
    public AsyncExecutorService(Elide elide, Integer threadPoolSize, Integer maxRunTime) {
        this.elide = elide;
        this.runner = new QueryRunner(elide);
        this.maxRunTime = maxRunTime;
        executor = AsyncQueryExecutor.getInstance(threadPoolSize == null ? DEFAULT_THREADPOOL_SIZE : threadPoolSize).getExecutorService();
        interruptor = AsyncQueryInterruptor.getInstance(threadPoolSize == null ? DEFAULT_THREADPOOL_SIZE : threadPoolSize).getExecutorService();
    }

    public void executeQuery(String query, QueryType queryType, Principal user, UUID id) {
        AsyncQueryThread queryWorker = new AsyncQueryThread(query, queryType, user, elide, runner, id);
        // Change async query in Datastore to queued
        AsyncDbUtil asyncDbUtil = AsyncDbUtil.getInstance(elide);
        try {
            asyncDbUtil.updateAsyncQuery(id, (asyncQueryObj) -> {
                asyncQueryObj.setStatus(QueryStatus.QUEUED);
                });
        } catch (IOException e) {
            log.error("IOException: {}", e.getMessage());
        }

        AsyncQueryInterruptThread queryInterruptWorker = new AsyncQueryInterruptThread(elide, executor.submit(queryWorker), id, new Date(), maxRunTime);
        interruptor.execute(queryInterruptWorker);
    }
}