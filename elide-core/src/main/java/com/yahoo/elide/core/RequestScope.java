/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.audit.Logger;
import com.yahoo.elide.jsonapi.JsonApiMapper;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.yahoo.elide.security.Check;
import com.yahoo.elide.security.User;

import com.google.common.base.Preconditions;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.ws.rs.core.MultivaluedMap;

/**
 * Request scope object for relaying request-related data to various subsystems.
 */
public class RequestScope {
    private final @Getter JsonApiDocument jsonApiDocument;
    private final @Getter DatabaseTransaction transaction;
    private final @Getter User user;
    private final @Getter EntityDictionary dictionary;
    private final @Getter JsonApiMapper mapper;
    private final @Getter Logger logger;
    private final @Getter Optional<MultivaluedMap<String, String>> queryParams;
    private final @Getter Map<String, Set<String>> sparseFields;
    private final @Getter ObjectEntityCache objectEntityCache;
    private final @Getter SecurityMode securityMode;

    private transient LinkedHashSet<Runnable> deferredChecks = null;

    public RequestScope(JsonApiDocument jsonApiDocument,
                        DatabaseTransaction transaction,
                        User user,
                        EntityDictionary dictionary,
                        JsonApiMapper mapper,
                        Logger logger,
                        MultivaluedMap<String, String> queryParams,
                        SecurityMode securityMode) {
        this.jsonApiDocument = jsonApiDocument;
        this.transaction = transaction;
        this.user = user;
        this.dictionary = dictionary;
        this.mapper = mapper;
        this.logger = logger;
        this.queryParams = ((queryParams == null) || (queryParams.size() == 0)
                ? Optional.empty() : Optional.ofNullable(queryParams));
        this.objectEntityCache = new ObjectEntityCache();
        this.securityMode = securityMode;

        if (this.queryParams.isPresent()) {
            sparseFields = parseSparseFields(this.queryParams.get());
        } else {
            sparseFields = Collections.emptyMap();
        }
    }

    public RequestScope(JsonApiDocument jsonApiDocument,
                        DatabaseTransaction transaction,
                        User user,
                        EntityDictionary dictionary,
                        JsonApiMapper mapper,
                        Logger logger,
                        SecurityMode securityMode) {
        this(jsonApiDocument, transaction, user, dictionary, mapper, logger, null, securityMode);
    }

    public RequestScope(JsonApiDocument jsonApiDocument,
                        DatabaseTransaction transaction,
                        User user,
                        EntityDictionary dictionary,
                        JsonApiMapper mapper,
                        Logger logger,
                        MultivaluedMap<String, String> queryParams) {
        this(jsonApiDocument, transaction, user, dictionary, mapper, logger, queryParams, SecurityMode.ACTIVE);
    }

    public RequestScope(JsonApiDocument jsonApiDocument,
                        DatabaseTransaction transaction,
                        User user,
                        EntityDictionary dictionary,
                        JsonApiMapper mapper,
                        Logger logger) {
        this(jsonApiDocument, transaction, user, dictionary, mapper, logger, null, SecurityMode.ACTIVE);
    }

    /**
     * Outer RequestScope constructor for use by Patch Extension.
     */
    protected RequestScope(
            DatabaseTransaction transaction,
            User user,
            EntityDictionary dictionary,
            JsonApiMapper mapper,
            Logger logger) {
        this(null, transaction, user, dictionary, mapper, logger);
        this.deferredChecks = new LinkedHashSet<>();
    }

    /**
     * Special copy constructor for use by PatchRequestScope.
     */
    protected RequestScope(JsonApiDocument jsonApiDocument, RequestScope outerRequestScope) {
        this.jsonApiDocument = jsonApiDocument;
        this.transaction = outerRequestScope.transaction;
        this.user = outerRequestScope.user;
        this.dictionary = outerRequestScope.dictionary;
        this.mapper = outerRequestScope.mapper;
        this.logger = outerRequestScope.logger;
        this.queryParams = Optional.empty();
        this.sparseFields = Collections.emptyMap();
        this.objectEntityCache = outerRequestScope.objectEntityCache;
        this.securityMode = outerRequestScope.securityMode;
        this.deferredChecks = outerRequestScope.deferredChecks;
    }

    /**
     * Parses queryParams and produces sparseFields map.
     * @param queryParams The request query parameters
     * @return Parsed sparseFields map
     */
    private static Map<String, Set<String>> parseSparseFields(MultivaluedMap<String, String> queryParams) {
        Map<String, Set<String>> result = new HashMap<>();

        for (Map.Entry<String, List<String>> kv : queryParams.entrySet()) {
            String key = kv.getKey();
            if (key.startsWith("fields[") && key.endsWith("]")) {
                String type = key.substring(7, key.length() - 1);

                LinkedHashSet<String> filters = new LinkedHashSet<>();
                for (String filterParams : kv.getValue()) {
                    Collections.addAll(filters, filterParams.split(","));
                }

                if (!filters.isEmpty()) {
                    result.put(type, filters);
                }
            }
        }

        return result;
    }

    /**
     * run any deferred permission checks due to create.
     *
     * @see com.yahoo.elide.annotation.CreatePermission
     */
    public void runDeferredPermissionChecks() {
        if (deferredChecks != null) {
            try {
                for (Runnable task : new ArrayList<>(deferredChecks)) {
                    task.run();
                }
            } catch (RuntimeException e) {
                throw e;
            } finally {
                deferredChecks = null;
            }
        }
    }

    /**
     * Check provided access permission.
     *
     * @param annotationClass one of Create, Read, Update or Delete permission annotations
     * @param checks Check classes
     * @param isAny true if ANY, else ALL
     * @param resource given resource
     * @see com.yahoo.elide.annotation.CreatePermission
     * @see com.yahoo.elide.annotation.ReadPermission
     * @see com.yahoo.elide.annotation.UpdatePermission
     * @see com.yahoo.elide.annotation.DeletePermission
     */
    public void checkPermissions(Class<?> annotationClass, Class<? extends Check>[] checks, boolean isAny,
            PersistentResource resource) {
        CheckPermissions task = new CheckPermissions(annotationClass, checks, isAny, resource);
        // CreatePermission queues deferred permission checks
        if (deferredChecks == null && CreatePermission.class.equals(annotationClass)) {
            deferredChecks = new LinkedHashSet<>();
        }
        if (deferredChecks == null) {
            task.run();
        } else {
            deferredChecks.add(task);
        }
    }

    static private class CheckPermissions implements Runnable {
        Class<? extends Check>[] anyChecks;
        boolean any;
        PersistentResource resource;
        private Class<?> annotationClass;

        public CheckPermissions(
                Class<?> annotationClass,
                Class<? extends Check>[] checks,
                boolean isAny,
                PersistentResource resource) {
            Preconditions.checkArgument(checks.length > 0);
            this.annotationClass = annotationClass;
            this.anyChecks = Arrays.copyOf(checks, checks.length);
            this.any = isAny;
            this.resource = resource;
        }

        @Override
        public void run() {
            PersistentResource.checkPermissions(anyChecks, any, resource);
        }

        @Override
        public String toString() {
            return "CheckPermissions [anyChecks=" + Arrays.toString(anyChecks) + ", any=" + any + ", resource="
                    + resource.getId() + ", user=" + resource.getRequestScope().getUser() + "]";
        }
    }
}
