/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.request.EntityProjection;

import java.security.Principal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Query Logger Interface for Elide
 */
public interface QueryLogger {

    /**
     * Accepts the incoming GraphQL query and notes the start time for the query
     * @param queryId The RequestScope requestId.
     * @param user The Principal user
     * @param headers Http Request Headers
     * @param apiVer API Version
     * @param apiQuery QueryString of the requested URL request
     */
    void acceptQuery(UUID queryId, Principal user, Map<String, String> headers,
                     String apiVer, String apiQuery);

    /**
     * Accepts the incoming JSON API query and notes the start time for the query
     * @param queryId The RequestScope requestId.
     * @param user The Principal user
     * @param headers Http Request Headers
     * @param apiVer API Version
     * @param queryParams QueryParams for the incoming JSON API query
     * @param path The apiQuery endpoint path for the incoming query
     */
    void acceptQuery(UUID queryId, Principal user, Map<String, String> headers, String apiVer,
                     Optional<MultivaluedMap<String, String>> queryParams, String path);

    /**
     * Processes and logs all the queries from QueryDetail to STDOUT and an external file (target/trace.log)
     * @param queryId The RequestScope requestId.
     * @param projection The Entity Projection of the current request
     * @param scope The RequestScope of the current request
     * @param tx DataStore Transaction for the underlying datastore
     */
    void processQuery(UUID queryId, EntityProjection projection, RequestScope scope, DataStoreTransaction tx);

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
