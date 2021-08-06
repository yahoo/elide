/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.jms;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.security.User;

import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.ws.rs.core.MultivaluedHashMap;

/**
 * Request Scope for GraphQL Subscription requests.
 */
public class SubscriptionRequestScope extends RequestScope {

    @Getter
    private long timeoutInMs;

    /**
     * Constructor.
     * @param baseUrlEndpoint base path URL
     * @param transaction Data store transaction
     * @param user The user
     * @param apiVersion The api version
     * @param elideSettings Elide settings
     * @param requestId Elide internal request ID
     * @param requestHeaders HTTP request headers.
     * @param timeoutInMs request timeout in milliseconds.  0 means immediate.  -1 means no timeout.
     */
    public SubscriptionRequestScope(
            String baseUrlEndpoint,
            DataStoreTransaction transaction,
            User user,
            String apiVersion,
            ElideSettings elideSettings,
            UUID requestId,
            Map<String, List<String>> requestHeaders,
            long timeoutInMs
    ) {
        super(baseUrlEndpoint, "/", apiVersion, null, transaction, user,
                new MultivaluedHashMap<>(), requestHeaders, requestId, elideSettings);

        this.timeoutInMs = timeoutInMs;
    }
}
