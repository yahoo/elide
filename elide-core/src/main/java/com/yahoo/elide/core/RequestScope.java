/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import com.google.common.base.Preconditions;
import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.audit.Logger;
import com.yahoo.elide.core.filter.Predicate;
import com.yahoo.elide.jsonapi.JsonApiMapper;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.yahoo.elide.security.Check;
import com.yahoo.elide.security.User;
import lombok.Getter;

import javax.ws.rs.core.MultivaluedMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Request scope object for relaying request-related data to various subsystems.
 */
public class RequestScope {
    @Getter private final JsonApiDocument jsonApiDocument;
    @Getter private final DataStoreTransaction transaction;
    @Getter private final User user;
    @Getter private final EntityDictionary dictionary;
    @Getter private final JsonApiMapper mapper;
    @Getter private final Logger logger;
    @Getter private final Optional<MultivaluedMap<String, String>> queryParams;
    @Getter private final Map<String, Set<String>> sparseFields;
    @Getter private final Map<String, Set<Predicate>> predicates;
    @Getter private final ObjectEntityCache objectEntityCache;
    @Getter private final SecurityMode securityMode;
    @Getter private final Set<PersistentResource> newResources;

    private transient LinkedHashSet<Runnable> deferredChecks = null;

    public RequestScope(JsonApiDocument jsonApiDocument,
                        DataStoreTransaction transaction,
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
        this.queryParams = (queryParams == null || (queryParams.size() == 0)
                ? Optional.empty() : Optional.of(queryParams));
        this.objectEntityCache = new ObjectEntityCache();
        this.securityMode = securityMode;

        if (this.queryParams.isPresent()) {
            sparseFields = parseSparseFields(this.queryParams.get());
            predicates = Predicate.parseQueryParams(this.dictionary, this.queryParams.get());
        } else {
            sparseFields = Collections.emptyMap();
            predicates = Collections.emptyMap();
        }

        newResources = new LinkedHashSet<>();
    }

    public RequestScope(JsonApiDocument jsonApiDocument,
                        DataStoreTransaction transaction,
                        User user,
                        EntityDictionary dictionary,
                        JsonApiMapper mapper,
                        Logger logger,
                        SecurityMode securityMode) {
        this(jsonApiDocument, transaction, user, dictionary, mapper, logger, null, securityMode);
    }

    public RequestScope(JsonApiDocument jsonApiDocument,
                        DataStoreTransaction transaction,
                        User user,
                        EntityDictionary dictionary,
                        JsonApiMapper mapper,
                        Logger logger,
                        MultivaluedMap<String, String> queryParams) {
        this(jsonApiDocument, transaction, user, dictionary, mapper, logger, queryParams, SecurityMode.ACTIVE);
    }

    public RequestScope(JsonApiDocument jsonApiDocument,
                        DataStoreTransaction transaction,
                        User user,
                        EntityDictionary dictionary,
                        JsonApiMapper mapper,
                        Logger logger) {
        this(jsonApiDocument, transaction, user, dictionary, mapper, logger, null, SecurityMode.ACTIVE);
    }

    /**
     * Outer RequestScope constructor for use by Patch Extension.
     *
     * @param transaction the transaction
     * @param user        the user
     * @param dictionary  the dictionary
     * @param mapper      the mapper
     * @param logger      the logger
     */
    protected RequestScope(
            DataStoreTransaction transaction,
            User user,
            EntityDictionary dictionary,
            JsonApiMapper mapper,
            Logger logger) {
        this(null, transaction, user, dictionary, mapper, logger);
        this.deferredChecks = new LinkedHashSet<>();
    }

    /**
     * Special copy constructor for use by PatchRequestScope.
     *
     * @param jsonApiDocument   the json api document
     * @param outerRequestScope the outer request scope
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
        this.predicates = Collections.emptyMap();
        this.objectEntityCache = outerRequestScope.objectEntityCache;
        this.securityMode = outerRequestScope.securityMode;
        this.deferredChecks = outerRequestScope.deferredChecks;
        this.newResources = outerRequestScope.newResources;
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
     * Get predicates for a specific collection type.
     * @param type The name of the type
     * @return The set of predicates for the given type
     */
    public Set<Predicate> getPredicatesOfType(String type) {
        return predicates.getOrDefault(type, Collections.emptySet());
    }

    /**
     * run any deferred permission checks due to create.
     *
     * @see com.yahoo.elide.annotation.CreatePermission
     */
    public void runDeferredPermissionChecks() {
        if (deferredChecks != null) {
            try {
                new ArrayList<>(deferredChecks).forEach(Runnable::run);
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

    private static class CheckPermissions implements Runnable {
        final Class<? extends Check>[] anyChecks;
        final boolean any;
        final PersistentResource resource;
        private final Class<?> annotationClass;

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
