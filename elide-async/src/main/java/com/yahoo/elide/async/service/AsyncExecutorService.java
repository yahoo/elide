/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service;

import com.yahoo.elide.Elide;
import com.yahoo.elide.async.models.AsyncAPI;
import com.yahoo.elide.async.models.AsyncAPIResult;
import com.yahoo.elide.async.models.QueryStatus;
import com.yahoo.elide.core.exceptions.InvalidOperationException;
import com.yahoo.elide.graphql.QueryRunner;
import com.yahoo.elide.security.User;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
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
    private ExecutorService updater;
    private int maxRunTime;
    private AsyncQueryDAO asyncQueryDao;
    private static AsyncExecutorService asyncExecutorService = null;
    private ResultStorageEngine resultStorageEngine;

    @Inject
    private AsyncExecutorService(Elide elide, Integer threadPoolSize, Integer maxRunTime, AsyncQueryDAO asyncQueryDao,
            ResultStorageEngine resultStorageEngine) {
        this.elide = elide;
        runners = new HashMap();

        for (String apiVersion : elide.getElideSettings().getDictionary().getApiVersions()) {
            runners.put(apiVersion, new QueryRunner(elide, apiVersion));
        }

        this.maxRunTime = maxRunTime;
        executor = Executors.newFixedThreadPool(threadPoolSize == null ? defaultThreadpoolSize : threadPoolSize);
        updater = Executors.newFixedThreadPool(threadPoolSize == null ? defaultThreadpoolSize : threadPoolSize);
        this.asyncQueryDao = asyncQueryDao;
        this.resultStorageEngine = resultStorageEngine;
    }

    /**
     * Initialize the singleton AsyncExecutorService object.
     * If already initialized earlier, no new object is created.
     * @param elide Elide Instance
     * @param threadPoolSize thread pool size
     * @param maxRunTime max run times in minutes
     * @param asyncQueryDao DAO Object
     */
    public static void init(Elide elide, Integer threadPoolSize, Integer maxRunTime, AsyncQueryDAO asyncQueryDao,
            ResultStorageEngine resultStorageEngine) {
        if (asyncExecutorService == null) {
            asyncExecutorService = new AsyncExecutorService(elide, threadPoolSize, maxRunTime, asyncQueryDao,
                    resultStorageEngine);
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
     * @param apiVersion api version
     */
    public void executeQuery(AsyncAPI queryObj, User user, String apiVersion) {

        QueryRunner runner = runners.get(apiVersion);
        if (runner == null) {
            throw new InvalidOperationException("Invalid API Version");
        }
        AsyncAPIThread queryWorker = new AsyncAPIThread(queryObj, user, this, apiVersion);
        Future<AsyncAPIResult> task = executor.submit(queryWorker);
        try {
            queryObj.setStatus(QueryStatus.PROCESSING);
            AsyncAPIResult queryResultObj = task.get(queryObj.getAsyncAfterSeconds(), TimeUnit.SECONDS);
            queryObj.setResult(queryResultObj);
            queryObj.setStatus(QueryStatus.COMPLETE);
            queryObj.setUpdatedOn(new Date());
        } catch (InterruptedException e) {
            log.error("InterruptedException: {}", e);
            queryObj.setStatus(QueryStatus.FAILURE);
        } catch (ExecutionException e) {
            log.error("ExecutionException: {}", e);
            queryObj.setStatus(QueryStatus.FAILURE);
        } catch (TimeoutException e) {
            log.error("TimeoutException: {}", e);
            queryObj.setQueryUpdateWorker(new AsyncAPIUpdateThread(elide, task, queryObj, asyncQueryDao));
        } catch (Exception e) {
            log.error("Exception: {}", e);
            queryObj.setStatus(QueryStatus.FAILURE);
        }

    }
    /**
     * Complete Query asynchronously.
     * @param query AsyncQuery
     * @param user User
     * @param apiVersion API Version
     */
    public void completeQuery(AsyncAPI query, User user, String apiVersion) {
        if (query.getQueryUpdateWorker() != null) {
            log.debug("Task has not completed");
            updater.execute(query.getQueryUpdateWorker());
        } else {
            log.debug("Task has completed");
        }
    }
}
