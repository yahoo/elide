/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service;

import com.yahoo.elide.Elide;
import com.yahoo.elide.async.service.dao.AsyncAPIDAO;
import com.yahoo.elide.async.service.thread.AsyncAPICancelRunnable;
import com.yahoo.elide.async.service.thread.AsyncAPICleanerRunnable;

import lombok.extern.slf4j.Slf4j;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
/**
 * Service to execute Async queries.
 * It will schedule task to track long running queries and kills them.
 * It will also schedule task to update orphan queries status
 * after host/app crash or restart.
 */
@Slf4j
public class AsyncCleanerService {

    private final int defaultCleanupDelayMinutes = 120;
    private final int maxInitialDelayMinutes = 100;
    private static AsyncCleanerService asyncCleanerService = null;

    @Inject
    private AsyncCleanerService(Elide elide, Integer maxRunTimeSeconds, Integer queryCleanupDays,
            Integer cancelDelaySeconds, AsyncAPIDAO asyncQueryDao) {

        //If query is still running for twice than maxRunTime, then interrupt did not work due to host/app crash.
        int queryRunTimeThresholdMinutes = (int) TimeUnit.SECONDS.toMinutes(maxRunTimeSeconds * 2 + 30L);

        // Setting up query cleaner that marks long running query as TIMEDOUT.
        ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor();
        AsyncAPICleanerRunnable cleanUpTask = new AsyncAPICleanerRunnable(
                queryRunTimeThresholdMinutes, elide, queryCleanupDays, asyncQueryDao, new DateUtil());

        // Since there will be multiple hosts running the elide service,
        // setting up random delays to avoid all of them trying to cleanup at the same time.
        Random random = new Random();
        int initialDelayMinutes = random.ints(0, maxInitialDelayMinutes).limit(1).findFirst().getAsInt();
        log.debug("Initial Delay for cleaner service is {}", initialDelayMinutes);

        //Having a delay of at least DEFAULT_CLEANUP_DELAY between two cleanup attempts.
        //Or maxRunTimeMinutes * 2 so that this process does not coincides with query
        //interrupt process.

        cleaner.scheduleWithFixedDelay(cleanUpTask, initialDelayMinutes, Math.max(defaultCleanupDelayMinutes,
                queryRunTimeThresholdMinutes), TimeUnit.MINUTES);

        //Setting up query cancel service that cancels long running queries based on status or runtime
        ScheduledExecutorService cancellation = Executors.newSingleThreadScheduledExecutor();

        AsyncAPICancelRunnable cancelTask = new AsyncAPICancelRunnable(maxRunTimeSeconds,
                elide, asyncQueryDao);

        cancellation.scheduleWithFixedDelay(cancelTask, 0, cancelDelaySeconds, TimeUnit.SECONDS);
    }

    /**
     * Initialize the singleton AsyncCleanerService object.
     * If already initialized earlier, no new object is created.
     * @param elide Elide Instance
     * @param maxRunTimeSeconds max run times in seconds
     * @param queryCleanupDays Async Query Clean up days
     * @param cancelDelaySeconds Async Query Transaction cancel delay
     * @param asyncQueryDao DAO Object
     */
    public static void init(Elide elide, Integer maxRunTimeSeconds, Integer queryCleanupDays,
            Integer cancelDelaySeconds, AsyncAPIDAO asyncQueryDao) {
        if (asyncCleanerService == null) {
            asyncCleanerService = new AsyncCleanerService(elide, maxRunTimeSeconds, queryCleanupDays,
                    cancelDelaySeconds, asyncQueryDao);
        } else {
            log.debug("asyncCleanerService is already initialized.");
        }
    }

    /**
     * Get instance of AsyncCleanerService.
     * @return AsyncCleanerService Object
     */
    public synchronized static AsyncCleanerService getInstance() {
        return asyncCleanerService;
    }
}
