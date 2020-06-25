/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import com.yahoo.elide.ElideResponse;
import lombok.extern.slf4j.Slf4j;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Slf4jQuery Logger Implementation for Elide. Overrides the default Noop QueryLogger
 * Implementation of Elide
 */
@Slf4j
public class Slf4jQueryLogger implements QueryLogger {
    private volatile Map<UUID, Long> startTime;

    public Slf4jQueryLogger() {
        startTime = new ConcurrentHashMap<>();
    }

    @Override
    public void acceptQuery(UUID queryId, Principal user, Map<String, String> headers, String apiVer, String apiQuery) {
        synchronized (this) {
            long start = System.currentTimeMillis();
            startTime.put(queryId, start);
        }

        String headersQuery = "";
        for (String key : headers.keySet()) {
            String value = headers.get(key);
            headersQuery = key + ": " + value + "\n";
        }

        log.info("Accepted Query with RequestId: "
                + queryId
                + " User: "
                + user
                + " ApiVersion: "
                + apiVer
                + " ApiQuery: "
                + apiQuery);
    }

    @Override
    public void processQuery(UUID queryId, QueryDetail qd) {
        if (qd.toString() != "") {
            log.info("Processing Query with RequestId: " + queryId + ". QueryText: " + qd.toString());
        }
    }

    @Override
    public void cancelQuery(UUID queryId) {
        synchronized (this) {
            startTime.remove(queryId);
        }
        log.info("Canceling Query with RequestId: " + queryId);
    }

    @Override
    public void completeQuery(UUID queryId, ElideResponse response) {
        if (!startTime.containsKey(queryId)) {
            //key does not exist (post, delete, patch etc)
            return;
        }

        long endTime, start;
        endTime = System.currentTimeMillis();
        synchronized (this) {
            start = startTime.get(queryId);
            startTime.remove(queryId);
        }

        long timeElapsed = endTime - start; //returns elapsed time in milliseconds
        int responseCode = response.getResponseCode();

        //log the result
        log.info("Completed Query with RequestId: "
                + queryId
                + ". Query Execution Time: "
                + timeElapsed
                + " Response Status: "
                + responseCode);
    }
}
