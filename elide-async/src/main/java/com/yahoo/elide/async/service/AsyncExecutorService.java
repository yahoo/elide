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
import com.yahoo.elide.async.operation.AsyncAPIUpdateOperation;
import com.yahoo.elide.async.service.dao.AsyncAPIDAO;
import com.yahoo.elide.core.security.User;
import com.yahoo.elide.graphql.QueryRunner;

import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
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

    public static final int DEFAULT_THREAD_POOL_SIZE = 6;

    private Elide elide;
    private Map<String, QueryRunner> runners;
    private ExecutorService executor;
    private ExecutorService updater;
    private AsyncAPIDAO asyncAPIDao;
    private ThreadLocal<AsyncAPIResultFuture> asyncResultFutureThreadLocal = new ThreadLocal<>();

    /**
     * A Future with Synchronous Execution Complete Flag.
     */
    @Data
    private static class AsyncAPIResultFuture {
        private Future<AsyncAPIResult> asyncFuture;
        private boolean synchronousTimeout = false;
    }

    @Inject
    public AsyncExecutorService(Elide elide, ExecutorService executor, ExecutorService updater,
                    AsyncAPIDAO asyncAPIDao) {
        this.elide = elide;
        runners = new HashMap<>();

        for (String apiVersion : elide.getElideSettings().getDictionary().getApiVersions()) {
            runners.put(apiVersion, new QueryRunner(elide, apiVersion));
        }

        this.executor = executor;
        this.updater = updater;
        this.asyncAPIDao = asyncAPIDao;
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
            Thread.currentThread().interrupt();
            log.error("InterruptedException: {}", e.toString());
            queryObj.setStatus(QueryStatus.FAILURE);
        } catch (ExecutionException e) {
            log.error("ExecutionException: {}", e.toString());
            queryObj.setStatus(QueryStatus.FAILURE);
        } catch (TimeoutException e) {
            log.error("TimeoutException: {}", e.toString());
            resultFuture.setSynchronousTimeout(true);
        } catch (Exception e) {
            log.error("Exception: {}", e.toString());
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
            updater.execute(new AsyncAPIUpdateOperation(elide, asyncAPIResultFuture.getAsyncFuture(), query,
                    asyncAPIDao));
            asyncResultFutureThreadLocal.remove();
        } else {
            log.debug("Task has completed");
        }
    }
}
