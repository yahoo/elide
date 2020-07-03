/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import com.yahoo.elide.ElideResponse;
import lombok.extern.slf4j.Slf4j;

import java.security.Principal;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Slf4jQuery Logger Implementation for Elide. Overrides the default Noop QueryLogger
 * Implementation of Elide
 */
@Slf4j
public class Slf4jQueryLogger implements QueryLogger {

    @Override
    public void acceptQuery(UUID queryId, Principal user, Map<String, String> headers,
                                   String apiVer, String apiQuery) {
        long start = System.currentTimeMillis();

        log.debug("Accepted Query with RequestId: "
                + queryId
                + " User: "
                + user
                + " ApiVersion: "
                + apiVer
                + " ApiQuery: "
                + apiQuery
                + ". Start Time "
                + start);
    }

    @Override
    public void acceptQuery(UUID queryId, Principal user, Map<String, String> headers, String apiVer,
                            Optional<MultivaluedMap<String, String>> queryParams, String path) {
        long start = System.currentTimeMillis();
        String apiQuery =  constructAPIQuery(queryParams, path);

        log.debug("Accepted Query with RequestId: "
                + queryId
                + " User: "
                + user
                + " ApiVersion: "
                + apiVer
                + " ApiQuery: "
                + apiQuery
                + ". Start Time "
                + start);
    }

    @Override
    public void processQuery(UUID queryId, QueryDetail qd) {
        if (!qd.toString().equals("")) {
            log.debug("Processing Query with RequestId: " + queryId + ". QueryText: " + qd.toString());
        }
    }

    @Override
    public void cancelQuery(UUID queryId) {
        log.debug("Canceling Query with RequestId: " + queryId);
    }

    @Override
    public void completeQuery(UUID queryId, ElideResponse response) {
        long endTime = System.currentTimeMillis();
        int responseCode = response.getResponseCode();

        //log the result
        log.debug("Completed Query with RequestId: "
                + queryId
                + ". End Time: "
                + endTime
                + " Response Status: "
                + responseCode);
    }

    /**
     * Constructs the default apiQuery if it not formed earlier
     * @param queryParams KeyValue Pair of all parameters
     * @param path The apiQuery endpoint path
     * @return the apiQuery
     */
    private String constructAPIQuery(Optional<MultivaluedMap<String, String>> queryParams, String path) {
        String apiQuery;
        if (!queryParams.isPresent()) {
            apiQuery = path;
        } else {
            apiQuery = path + '?';
            MultivaluedMap<String, String> qParams = queryParams.get();
            Iterator<String> it = qParams.keySet().iterator();
            while (it.hasNext()) {
                String key = (String) it.next();
                List<String> value = qParams.get(key);
                for (String v : value) {
                    apiQuery += key + "=" + v;
                }
                if (it.hasNext()) {
                    apiQuery += '&';
                }
            }
        }
        return apiQuery;
    }
}
