/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.extensions;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.yahoo.elide.security.User;

import javax.ws.rs.core.MultivaluedMap;

/**
 * Special request scope for Patch Extension.
 */
public class PatchRequestScope extends RequestScope {

    /**
     * Outer RequestScope constructor for use by Patch Extension.
     *
     * @param path the URL path
     * @param transaction current database transaction
     * @param user        request user
     * @param elideSettings Elide settings object
     */
    public PatchRequestScope(
            String path,
            DataStoreTransaction transaction,
            User user,
            ElideSettings elideSettings) {
        super(
                path,
                (JsonApiDocument) null,
                transaction,
                user,
                (MultivaluedMap<String, String>) null,
                elideSettings,
                true
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
        super(path, jsonApiDocument, scope);
    }
}
