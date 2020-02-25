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
 * Class for initializing the Query Interruptor.
 */
@Singleton
class AsyncQueryInterruptor {

	  private static AsyncQueryInterruptor interruptor;
    private ExecutorService interruptorService;

    protected static AsyncQueryInterruptor getInstance(int threadPoolSize) {
      if (interruptor == null) {
        synchronized (AsyncQueryInterruptor.class) {
        interruptor = new AsyncQueryInterruptor(threadPoolSize);
        }
      }
      return interruptor;
    }

    protected AsyncQueryInterruptor(int threadPoolSize) {
      interruptorService = Executors.newFixedThreadPool(threadPoolSize);
    }

	  protected ExecutorService getExecutorService() {
      return interruptorService;
    }
}
