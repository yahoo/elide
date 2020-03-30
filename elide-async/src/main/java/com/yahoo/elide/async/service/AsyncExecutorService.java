/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import com.yahoo.elide.Elide;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.QueryStatus;
import com.yahoo.elide.graphql.QueryRunner;
import com.yahoo.elide.security.User;

import lombok.Getter;

/**
 * Service to execute Async queries. It will schedule task to track long
 * running queries and kills them. It will also schedule task to update
 * orphan query statuses after host/app crash or restart.
 */
@Getter
public class AsyncExecutorService {

    private final int DEFAULT_THREADPOOL_SIZE = 6;

    private Elide elide;
    private QueryRunner runner;
    private ExecutorService executor;
    private ExecutorService interruptor;
    private int maxRunTime;
    private AsyncQueryDAO asyncQueryDao;
    

    @Inject
    public AsyncExecutorService(Elide elide, Integer threadPoolSize, Integer maxRunTime, AsyncQueryDAO asyncQueryDao) {
        this.elide = elide;
        this.runner = new QueryRunner(elide);
        this.maxRunTime = maxRunTime;
        executor = Executors.newFixedThreadPool(threadPoolSize == null ? DEFAULT_THREADPOOL_SIZE : threadPoolSize);
        interruptor = Executors.newFixedThreadPool(threadPoolSize == null ? DEFAULT_THREADPOOL_SIZE : threadPoolSize);
        this.asyncQueryDao = asyncQueryDao;
    }

    public void executeQuery(AsyncQuery queryObj, User user) {
        AsyncQueryThread queryWorker = new AsyncQueryThread(queryObj, user, elide, runner, asyncQueryDao);
        // Change async query in Datastore to queued
        asyncQueryDao.updateStatus(queryObj, QueryStatus.QUEUED);
        AsyncQueryInterruptThread queryInterruptWorker = new AsyncQueryInterruptThread(elide, executor.submit(queryWorker), queryObj, new Date(),
               maxRunTime, asyncQueryDao);
        interruptor.execute(queryInterruptWorker);
    }

}