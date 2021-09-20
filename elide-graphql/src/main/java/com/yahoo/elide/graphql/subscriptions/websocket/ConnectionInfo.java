/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql.subscriptions.websocket;

import com.yahoo.elide.core.security.User;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

/**
 * Information about the sessions/connection passed to the request handler.
 */
@Value
@Builder
public class ConnectionInfo {
    /**
     * Return a Elide user object for the session.
     * @return Elide user.
     */
    private User user;


    /**
     * Return the URL path for this request.
     * @return URL path.
     */
    private String baseUrl;

    /**
     * Get a map of parameters for the session.
     * @return map of parameters.
     */
    private Map<String, List<String>> parameters;

    /**
     * Gets the API version associated with this websocket.
     */
    private String getApiVersion;
}
