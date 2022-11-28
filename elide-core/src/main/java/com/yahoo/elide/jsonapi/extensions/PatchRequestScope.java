/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.jsonapi.extensions;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.security.User;
import com.yahoo.elide.jsonapi.EntityProjectionMaker;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.MultivaluedMap;

/**
 * Special request scope for Patch Extension.
 */
public class PatchRequestScope extends RequestScope {

    /**
     * Outer RequestScope constructor for use by Patch Extension.
     *
     * @param baseUrlEndPoint base URL with prefix endpoint
     * @param path the URL path
     * @param apiVersion client requested API version
     * @param transaction current database transaction
     * @param user        request user
     * @param requestId   request ID
     * @param queryParams request query parameters
     * @param requestHeaders request headers
     * @param elideSettings Elide settings object
     */
    public PatchRequestScope(
            String baseUrlEndPoint,
            String path,
            String apiVersion,
            DataStoreTransaction transaction,
            User user,
            UUID requestId,
            MultivaluedMap<String, String> queryParams,
            Map<String, List<String>> requestHeaders,
            ElideSettings elideSettings) {
        super(
                baseUrlEndPoint,
                path,
                apiVersion,
                (JsonApiDocument) null,
                transaction,
                user,
                queryParams,
                requestHeaders,
                requestId,
                elideSettings
        );
    }

    /**
     * Inner RequestScope copy constructor for use by Patch Extension actions.
     *
     * @param path the URL path
     * @param jsonApiDocument document
     * @param scope           outer request scope
     */
    public PatchRequestScope(String path, JsonApiDocument jsonApiDocument, PatchRequestScope scope) {
        super(path, scope.getApiVersion(), jsonApiDocument, scope);
        this.setEntityProjection(new EntityProjectionMaker(dictionary, this).parsePath(path));
    }
}
