/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.extensions;

import com.yahoo.elide.audit.AuditLogger;
import com.yahoo.elide.core.DataStoreTransaction;
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
     *
     * @param transaction current database transaction
     * @param user        request user
     * @param dictionary  entity dictionary
     * @param mapper      Json API mapper
     * @param auditLogger      the logger
     */
    public PatchRequestScope(
            DataStoreTransaction transaction,
            User user,
            EntityDictionary dictionary,
            JsonApiMapper mapper,
            AuditLogger auditLogger) {
        super(transaction, user, dictionary, mapper, auditLogger);
    }

    /**
     * Inner RequestScope copy constructor for use by Patch Extension actions.
     *
     * @param jsonApiDocument document
     * @param scope           outer request scope
     */
    public PatchRequestScope(JsonApiDocument jsonApiDocument, PatchRequestScope scope) {
        super(jsonApiDocument, scope);
    }
}
