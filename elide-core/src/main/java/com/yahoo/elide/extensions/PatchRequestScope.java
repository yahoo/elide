/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.extensions;

import com.yahoo.elide.Elide;
import com.yahoo.elide.audit.AuditLogger;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.filter.dialect.MultipleFilterDialect;
import com.yahoo.elide.jsonapi.JsonApiMapper;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.yahoo.elide.security.PermissionExecutor;
import com.yahoo.elide.security.User;

import javax.ws.rs.core.MultivaluedMap;
import java.util.function.Function;

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
     * @param dictionary  entity dictionary
     * @param mapper      Json API mapper
     * @param auditLogger      the logger
     * @param elideSettings Elide settings object
     * @param filterDialect filter dialect
     */
    public PatchRequestScope(
            String path,
            DataStoreTransaction transaction,
            User user,
            EntityDictionary dictionary,
            JsonApiMapper mapper,
            AuditLogger auditLogger,
            Function<RequestScope, PermissionExecutor> permissionExecutorGenerator,
            Elide.ElideSettings elideSettings,
            MultipleFilterDialect filterDialect) {
        super(
                path,
                (JsonApiDocument) null,
                transaction,
                user,
                dictionary,
                mapper,
                auditLogger,
                (MultivaluedMap<String, String>) null,
                permissionExecutorGenerator,
                elideSettings,
                filterDialect
        );
    }

    /**
     * Inner RequestScope copy constructor for use by Patch Extension actions.
     *
     * @param jsonApiDocument document
     * @param scope           outer request scope
     */
    public PatchRequestScope(String path, JsonApiDocument jsonApiDocument, PatchRequestScope scope) {
        super(path, jsonApiDocument, scope);
    }
}
