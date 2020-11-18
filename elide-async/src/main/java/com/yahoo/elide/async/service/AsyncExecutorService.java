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
import com.yahoo.elide.async.service.dao.AsyncAPIDAO;
import com.yahoo.elide.async.service.storageengine.ResultStorageEngine;
import com.yahoo.elide.async.service.thread.AsyncAPIUpdateThread;
import com.yahoo.elide.graphql.QueryRunner;
import com.yahoo.elide.security.User;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
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
    private AsyncAPIDAO asyncAPIDao;
    private static AsyncExecutorService asyncExecutorService = null;
    private ResultStorageEngine resultStorageEngine;
    private ThreadLocal<AsyncAPIResultFuture> asyncResultFutureThreadLocal = new ThreadLocal<>();

    /**
     * A Future with Synchronous Execution Complete Flag.
     */
    @Data
    private class AsyncAPIResultFuture {
        private Future<AsyncAPIResult> asyncFuture;
        private boolean synchronousTimeout = false;
    }

    @Inject
    private AsyncExecutorService(Elide elide, Integer threadPoolSize, AsyncAPIDAO asyncAPIDao,
            ResultStorageEngine resultStorageEngine) {
        this.elide = elide;
        runners = new HashMap();

        for (String apiVersion : elide.getElideSettings().getDictionary().getApiVersions()) {
            runners.put(apiVersion, new QueryRunner(elide, apiVersion));
        }

        executor = Executors.newFixedThreadPool(threadPoolSize == null ? defaultThreadpoolSize : threadPoolSize);
        updater = Executors.newFixedThreadPool(threadPoolSize == null ? defaultThreadpoolSize : threadPoolSize);
        this.asyncAPIDao = asyncAPIDao;
        this.resultStorageEngine = resultStorageEngine;
    }

    /**
     * Initialize the singleton AsyncExecutorService object.
     * If already initialized earlier, no new object is created.
     * @param elide Elide Instance
     * @param threadPoolSize thread pool size
     * @param asyncAPIDao DAO Object
     */
    public static void init(Elide elide, Integer threadPoolSize, AsyncAPIDAO asyncAPIDao,
            ResultStorageEngine resultStorageEngine) {
        if (asyncExecutorService == null) {
            asyncExecutorService = new AsyncExecutorService(elide, threadPoolSize, asyncAPIDao,
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
     * @param callable A Callabale implementation to execute in background.
     */
    public void executeQuery(AsyncAPI queryObj, Callable<AsyncAPIResult> callable) {
        AsyncAPIResultFuture resultFuture = new AsyncAPIResultFuture();
        try {
            Future<AsyncAPIResult> asyncExecuteFuture = executor.submit(callable);
            resultFuture.setAsyncFuture(asyncExecuteFuture);
            queryObj.setStatus(QueryStatus.PROCESSING);
            AsyncAPIResult queryResultObj = asyncExecuteFuture.get(queryObj.getAsyncAfterSeconds(), TimeUnit.SECONDS);
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
            resultFuture.setSynchronousTimeout(true);
        } catch (Exception e) {
            log.error("Exception: {}", e);
            queryObj.setStatus(QueryStatus.FAILURE);
        } finally {
            asyncResultFutureThreadLocal.set(resultFuture);
        }

    }
    /**
     * Complete Query asynchronously.
     * @param query AsyncQuery
     * @param user User
     * @param apiVersion API Version
     */
    public void completeQuery(AsyncAPI query, User user, String apiVersion) {
        AsyncAPIResultFuture asyncAPIResultFuture = asyncResultFutureThreadLocal.get();
        if (asyncAPIResultFuture.isSynchronousTimeout()) {
            log.debug("Task has not completed");
            updater.execute(new AsyncAPIUpdateThread(elide, asyncAPIResultFuture.getAsyncFuture(), query,
                    asyncAPIDao));
            asyncResultFutureThreadLocal.remove();
        } else {
            log.debug("Task has completed");
        }
    }
}
