/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.async.service;

import com.paiondata.elide.Elide;
import com.paiondata.elide.async.service.dao.AsyncApiDao;
import com.paiondata.elide.async.service.thread.AsyncApiCancelRunnable;
import com.paiondata.elide.async.service.thread.AsyncApiCleanerRunnable;

import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.time.Duration;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
/**
 * Service to execute Async queries.
 * It will schedule task to track long running queries and kills them.
 * It will also schedule task to update orphan queries status
 * after host/app crash or restart.
 */
@Slf4j
public class AsyncCleanerService {

    private static final int DEFAULT_CLEANUP_DELAY_MINUTES = 120;
    private static final int MAX_INITIAL_DELAY_MINUTES = 100;
    private static AsyncCleanerService asyncCleanerService = null;

    @Inject
    private AsyncCleanerService(Elide elide, Duration queryMaxRunTime, Duration queryRetentionDuration,
            Duration queryCancellationCheckInterval, AsyncApiDao asyncQueryDao) {

        //If query is still running for twice than maxRunTime, then interrupt did not work due to host/app crash.
        Duration queryRunTimeThreshold = Duration.ofSeconds(queryMaxRunTime.getSeconds() * 2 + 30L);

        // Setting up query cleaner that marks long running query as TIMEDOUT.
        ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor();
        AsyncApiCleanerRunnable cleanUpTask = new AsyncApiCleanerRunnable(
                queryRunTimeThreshold, elide, queryRetentionDuration, asyncQueryDao,
                Clock.systemUTC());

        // Since there will be multiple hosts running the elide service,
        // setting up random delays to avoid all of them trying to cleanup at the same time.
        Random random = new Random();
        int initialDelayMinutes = random.ints(0, MAX_INITIAL_DELAY_MINUTES).limit(1).findFirst().getAsInt();
        log.debug("Initial Delay for cleaner service is {}", initialDelayMinutes);

        //Having a delay of at least DEFAULT_CLEANUP_DELAY between two cleanup attempts.
        //Or maxRunTimeMinutes * 2 so that this process does not coincides with query
        //interrupt process.

        cleaner.scheduleWithFixedDelay(cleanUpTask, initialDelayMinutes, Math.max(DEFAULT_CLEANUP_DELAY_MINUTES,
                queryRunTimeThreshold.toMinutes()), TimeUnit.MINUTES);

        //Setting up query cancel service that cancels long running queries based on status or runtime
        ScheduledExecutorService cancellation = Executors.newSingleThreadScheduledExecutor();

        AsyncApiCancelRunnable cancelTask = new AsyncApiCancelRunnable(queryMaxRunTime,
                elide, asyncQueryDao);

        cancellation.scheduleWithFixedDelay(cancelTask, 0, queryCancellationCheckInterval.toSeconds(),
                TimeUnit.SECONDS);
    }

    /**
     * Initialize the singleton AsyncCleanerService object.
     * If already initialized earlier, no new object is created.
     * @param elide Elide Instance
     * @param queryMaxRunTime max run times in seconds
     * @param queryRetentionDuration Async Query Clean up days
     * @param queryCancellationCheckInterval Async Query Transaction cancel delay
     * @param asyncQueryDao DAO Object
     */
    public static void init(Elide elide, Duration queryMaxRunTime, Duration queryRetentionDuration,
            Duration queryCancellationCheckInterval, AsyncApiDao asyncQueryDao) {
        if (asyncCleanerService == null) {
            asyncCleanerService = new AsyncCleanerService(elide, queryMaxRunTime, queryRetentionDuration,
                    queryCancellationCheckInterval, asyncQueryDao);
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
