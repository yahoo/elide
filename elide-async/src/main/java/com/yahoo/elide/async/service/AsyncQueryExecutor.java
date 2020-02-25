/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Singleton;

/**
 * Class for initializing the Query Executor.
 */
@Singleton
class AsyncQueryExecutor {

    private static AsyncQueryExecutor executor;
    private ExecutorService executorService;

    protected static AsyncQueryExecutor getInstance(int threadPoolSize) {
      if (executor == null) {
        synchronized (AsyncQueryExecutor.class) {
          executor = new AsyncQueryExecutor(threadPoolSize);
          }
        }
      return executor;
    }

    protected AsyncQueryExecutor(int threadPoolSize) {
      executorService = Executors.newFixedThreadPool(threadPoolSize);
    }

    protected ExecutorService getExecutorService() {
      return executorService;
    }
}
