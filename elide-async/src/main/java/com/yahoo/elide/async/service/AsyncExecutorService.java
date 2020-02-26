/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service;

import java.io.IOException;
import java.security.Principal;
import java.util.Date;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.yahoo.elide.Elide;
import com.yahoo.elide.async.models.QueryStatus;
import com.yahoo.elide.async.models.QueryType;
import com.yahoo.elide.core.RequestScope;
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
	private final int DEFAULT_CLEANUP_DELAY = 360;

	private Elide elide;
	private QueryRunner runner;
	private ExecutorService executor;
	private ExecutorService interruptor;
	private ScheduledExecutorService cleaner;
	private int maxRunTime;

	@Inject
    public AsyncExecutorService(Elide elide, Integer threadPoolSize, Integer maxRunTime, Integer numberOfNodes) {
		this.elide = elide;
		this.runner = new QueryRunner(elide);
		this.maxRunTime = maxRunTime;
		executor = AsyncQueryExecutor.getInstance(threadPoolSize == null ? DEFAULT_THREADPOOL_SIZE : threadPoolSize).getExecutorService();
		interruptor = AsyncQueryInterruptor.getInstance(threadPoolSize == null ? DEFAULT_THREADPOOL_SIZE : threadPoolSize).getExecutorService();

		// Setting up query cleaner that marks long running query as TIMEDOUT.
		cleaner = AsyncQueryCleaner.getInstance().getExecutorService();
		AsyncQueryCleanerThread cleanUpTask = new AsyncQueryCleanerThread(maxRunTime, elide);

		// Since there will be multiple hosts running the elide service,
		// setting up random delays to avoid all of them trying to cleanup at the same time.
		Random random = new Random();
		int initialDelay = random.ints(0, numberOfNodes * 2).limit(1).findFirst().getAsInt();

		//Having a delay of at least DEFAULT_CLEANUP_DELAY between two cleanup attempts.
		cleaner.scheduleWithFixedDelay(cleanUpTask, initialDelay, Math.max(DEFAULT_CLEANUP_DELAY, maxRunTime * 2), TimeUnit.MINUTES);
	}

	public void executeQuery(String query, QueryType queryType, Principal user, UUID id) {
		AsyncQueryThread queryWorker = new AsyncQueryThread(query, queryType, user, elide, runner, id);
		// Change async query in Datastore to queued
		try {
			queryWorker.updateAsyncQueryStatus(QueryStatus.QUEUED, id);
		} catch (IOException e) {
			log.error("IOException: {}", e.getMessage());
		}

		AsyncQueryInterruptThread queryInterruptWorker = new AsyncQueryInterruptThread(elide, executor.submit(queryWorker), id, new Date(), maxRunTime);
		interruptor.execute(queryInterruptWorker);
	}
}