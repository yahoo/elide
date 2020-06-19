/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service;

import com.yahoo.elide.Elide;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.core.exceptions.InvalidOperationException;
import com.yahoo.elide.graphql.QueryRunner;
import com.yahoo.elide.security.User;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;

/**
 * Service to execute Async queries.
 * It will schedule task to track long running queries and kills them.
 * It will also schedule task to update orphan query statuses after
 * host/app crash or restart.
 */
@Getter
@Slf4j
public class AsyncExecutorService {

    private final int defaultThreadpoolSize = 6;

    private Elide elide;
    private Map<String, QueryRunner> runners;
    private ExecutorService executor;
    private ExecutorService interruptor;
    private int maxRunTime;
    private AsyncQueryDAO asyncQueryDao;
    private static AsyncExecutorService asyncExecutorService = null;
    private ResultStorageEngine resultStorageEngine;

    @Inject
    private AsyncExecutorService(Elide elide, Integer threadPoolSize, Integer maxRunTime, AsyncQueryDAO asyncQueryDao) {
        this.elide = elide;
        runners = new HashMap();

        for (String apiVersion : elide.getElideSettings().getDictionary().getApiVersions()) {
            runners.put(apiVersion, new QueryRunner(elide, apiVersion));
        }

        this.maxRunTime = maxRunTime;
        executor = Executors.newFixedThreadPool(threadPoolSize == null ? defaultThreadpoolSize : threadPoolSize);
        interruptor = Executors.newFixedThreadPool(threadPoolSize == null ? defaultThreadpoolSize : threadPoolSize);
        this.asyncQueryDao = asyncQueryDao;
    }

    /**
     * Initialize the singleton AsyncExecutorService object.
     * If already initialized earlier, no new object is created.
     * @param elide Elide Instance
     * @param threadPoolSize thread pool size
     * @param maxRunTime max run times in minutes
     * @param asyncQueryDao DAO Object
     */
    public static void init(Elide elide, Integer threadPoolSize, Integer maxRunTime, AsyncQueryDAO asyncQueryDao) {
        if (asyncExecutorService == null) {
            asyncExecutorService = new AsyncExecutorService(elide, threadPoolSize, maxRunTime, asyncQueryDao);
        } else {
            log.debug("asyncExecutorService is already initialized.");
        }
    }

    /**
     * Get instance of AsyncExecutorService.
     * @return AsyncExecutorService Object
     */
    public synchronized static AsyncExecutorService getInstance() {
        return asyncExecutorService;
    }

    /**
     * Execute Query asynchronously.
     * @param queryObj Query Object
     * @param user User
     * @param apiVersion API Version
     */
    public void executeQuery(AsyncQuery queryObj, User user, String apiVersion) {

        QueryRunner runner = runners.get(apiVersion);
        if (runner == null) {
            throw new InvalidOperationException("Invalid API Version");
        }
        AsyncQueryThread queryWorker = new AsyncQueryThread(queryObj, user, elide, runner, asyncQueryDao,
                apiVersion, resultStorageEngine);
        Future<?> task = executor.submit(queryWorker);

        try {
            task.get(queryObj.getAsyncAfterSeconds(), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // In case the future.get is interrupted , the underlying query may still have succeeded
            log.error("InterruptedException: {}", e);
        } catch (ExecutionException e) {
            // Query Status set to failure will be handled by the processQuery method
            log.error("ExecutionException: {}", e);
        } catch (TimeoutException e) {
            log.error("TimeoutException: {}", e);
        }
    }
}
