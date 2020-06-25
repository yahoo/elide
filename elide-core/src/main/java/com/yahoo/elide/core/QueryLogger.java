/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import com.yahoo.elide.ElideResponse;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

/**
 * Query Logger Interface for Elide
 */
public interface QueryLogger {

    /**
     * Accepts the incoming query and notes the start time for the query
     * @param queryId The RequestScope requestId.
     * @param user The Principal user
     * @param headers Http Request Headers
     * @param apiVer API Version
     * @param apiQuery QueryString of the requested URL request
     */
    void acceptQuery(UUID queryId, Principal user, Map<String, String> headers, String apiVer, String apiQuery);

    /**
     * Processes and logs all the queries from QueryDetail to STDOUT and an external file (target/trace.log)
     * @param queryId The RequestScope requestId.
     * @param qd The QueryDetail Object
     */
    void processQuery(UUID queryId, QueryDetail qd);

    /**
     * Cancels all queries currently running for a particular requestId
     * @param queryId The RequestScope requestId.
     */
    void cancelQuery(UUID queryId);

    /**
     * Registers the endtime for a query and logs it out
     * @param queryId The RequestScope requestId.
     * @param response The ElideResponse object
     */
    void completeQuery(UUID queryId, ElideResponse response);
}
