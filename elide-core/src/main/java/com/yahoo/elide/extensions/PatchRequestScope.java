/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.extensions;

import com.yahoo.elide.audit.Logger;
import com.yahoo.elide.core.DatabaseTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.jsonapi.JsonApiMapper;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.yahoo.elide.security.User;

/**
 * Special request scope for Patch Extension.
 */
public class PatchRequestScope extends RequestScope {

    /**
     * Outer RequestScope constructor for use by Patch Extension.
     */
    public PatchRequestScope(
            DatabaseTransaction transaction,
            User user,
            EntityDictionary dictionary,
            JsonApiMapper mapper,
            Logger logger) {
        super(transaction, user, dictionary, mapper, logger);
    }

    /**
     * Inner RequestScope copy constructor for use by Patch Extension actions.
     *
     * @param jsonApiDocument document
     * @param scope outer request scope
     */
    public PatchRequestScope(JsonApiDocument jsonApiDocument, PatchRequestScope scope) {
        super(jsonApiDocument, scope);
    }
}
