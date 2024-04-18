/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.graphql.subscriptions.websocket;

import com.paiondata.elide.core.request.route.Route;
import com.paiondata.elide.core.security.User;

import lombok.Builder;
import lombok.Value;

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
     * Returns the {@link Route} of the request which contains the base url, path,
     * headers, parameters and api version of the request.
     *
     * @return the route of the request
     */
    private Route route;
}
