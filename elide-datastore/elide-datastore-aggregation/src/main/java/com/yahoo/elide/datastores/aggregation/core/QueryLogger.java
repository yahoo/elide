/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.core;

import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.security.User;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Query Logger Interface for Elide
 */
public interface QueryLogger {

    /**
     * Accepts the incoming JSON API query and notes the start time for the query
     * @param queryId The RequestScope requestId.
     * @param user The Principal user
     * @param headers Http Request Headers
     * @param apiVer API Version
     * @param queryParams QueryParams for the incoming JSON API query
     * @param path The apiQuery endpoint path for the incoming query
     */
    void acceptQuery(UUID queryId, User user, Map<String, String> headers, String apiVer,
                     Optional<MultivaluedMap<String, String>> queryParams, String path);

    /**
     * Processes and logs all the queries from QueryDetail
     * @param queryId The RequestScope requestId.
     * @param query The underlying Query
     * @param apiQuery The output querytext
     * @param isCached Whether the result came from a cache or not
     */
    void processQuery(UUID queryId, Query query, String apiQuery, boolean isCached);

    /**
     * Cancels all queries currently running for a particular requestId
     * Implementation must be thread-safe.
     * @param queryId The RequestScope requestId.
     */
    void cancelQuery(UUID queryId);

    /**
     * Registers the endtime for a query and logs it out
     * @param queryId The RequestScope requestId.
     * @param response The ElideResponse object
     */
    void completeQuery(UUID queryId, QueryResponse response);
}
