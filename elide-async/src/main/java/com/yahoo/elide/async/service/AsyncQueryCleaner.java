package com.yahoo.elide.async.service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.inject.Singleton;

/**
 * Class for initializing the Query Cleaner.
 */
@Singleton
class AsyncQueryCleaner {

    private static AsyncQueryCleaner cleaner;
    private ScheduledExecutorService cleanerService;

    protected static AsyncQueryCleaner getInstance() {
        if (cleaner == null) {
          synchronized (AsyncQueryCleaner.class) {
          cleaner = new AsyncQueryCleaner();
          }
        }
        return cleaner;
    }

    protected AsyncQueryCleaner() {
      cleanerService = Executors.newSingleThreadScheduledExecutor();
    }

    protected ScheduledExecutorService getExecutorService() {
      return cleanerService;
    }
}
