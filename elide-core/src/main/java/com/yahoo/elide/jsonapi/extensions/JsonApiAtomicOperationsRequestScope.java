/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.jsonapi.extensions;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.request.route.Route;
import com.yahoo.elide.core.security.User;
import com.yahoo.elide.jsonapi.JsonApiRequestScope;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;

import java.util.UUID;

/**
 * The request scope for the JSON API Atomic Operations extension.
 */
public class JsonApiAtomicOperationsRequestScope extends JsonApiRequestScope {

    /**
     * Outer RequestScope constructor for use by Atomic Extension.
     *
     * @param route         the route
     * @param transaction   current transaction
     * @param user          request user
     * @param requestId     request ID
     * @param elideSettings Elide settings object
     */
    public JsonApiAtomicOperationsRequestScope(
            Route route,
            DataStoreTransaction transaction,
            User user,
            UUID requestId,
            ElideSettings elideSettings) {
        super(
                route,
                transaction,
                user,
                requestId,
                elideSettings,
                null
        );
    }

    /**
     * Inner RequestScope copy constructor for use by Atomic Extension actions.
     *
     * @param path            the URL path
     * @param jsonApiDocument document
     * @param scope           outer request scope
     */
    public JsonApiAtomicOperationsRequestScope(String path, JsonApiDocument jsonApiDocument,
            JsonApiAtomicOperationsRequestScope scope) {
        super(scope.getRoute().mutate().path(path).build(), jsonApiDocument, scope);
    }
}
