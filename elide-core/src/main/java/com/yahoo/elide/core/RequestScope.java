/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.OnCommit;
import com.yahoo.elide.audit.Logger;
import com.yahoo.elide.core.exceptions.ForbiddenAccessException;
import com.yahoo.elide.core.filter.Predicate;
import com.yahoo.elide.jsonapi.JsonApiMapper;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.yahoo.elide.security.Check;
import com.yahoo.elide.security.User;

import com.google.common.base.Preconditions;
import lombok.Getter;

import java.lang.annotation.Annotation;
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
    final private transient LinkedHashSet<Runnable> commitTriggers;
    private boolean doNotDefer = false;

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
        commitTriggers = new LinkedHashSet<>();
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
        this.commitTriggers = outerRequestScope.commitTriggers;
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
     * run any deferred post-commit triggers.
     *
     * @see com.yahoo.elide.annotation.CreatePermission
     */
    public void runCommitTriggers() {
        new ArrayList<>(commitTriggers).forEach(Runnable::run);
        commitTriggers.clear();
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
        checkPermissions(annotationClass, new CheckPermissions(annotationClass, checks, isAny, resource));
    }

    public <A extends Annotation> void checkFieldAwarePermissions(Class<A> annotationClass,
                                                                  PersistentResource resource) {
        checkPermissions(annotationClass, new FieldAwareCheck<>(annotationClass, resource));
    }

    public <A extends Annotation> void checkFieldAwarePermissions(Class<A> annotationClass,
                                                                  PersistentResource resource,
                                                                  String fieldName) {
        checkPermissions(annotationClass, new FieldAwareCheck<>(annotationClass, resource, fieldName));
    }

    public void queueCommitTrigger(PersistentResource resource) {
        queueCommitTrigger(resource, "");
    }

    public void queueCommitTrigger(PersistentResource resource, String fieldName) {
        commitTriggers.add(() -> {
            resource.runTriggers(OnCommit.class, fieldName);
        });
    }

    /**
     * Check permissions
     *
     * @param annotationClass
     * @param task
     */
    private void checkPermissions(Class<?> annotationClass, Runnable task) {
        // CreatePermission queues deferred permission checks
        if (deferredChecks == null && CreatePermission.class.equals(annotationClass)) {
            deferredChecks = new LinkedHashSet<>();
        }
        if (deferredChecks == null || doNotDefer) {
            task.run();
        } else {
            deferredChecks.add(task);
        }
    }

    private static class CheckPermissions implements Runnable {
        final Class<? extends Check>[] anyChecks;
        final boolean any;
        final PersistentResource resource;

        public CheckPermissions(
                Class<?> annotationClass,
                Class<? extends Check>[] checks,
                boolean isAny,
                PersistentResource resource) {
            Preconditions.checkArgument(checks.length > 0);
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

    /**
     * Field-Aware checks are for checking overrides on fields when appropriate. At time of writing, this should
     * really only include ReadPermission and UpdatePermission checks.
     *
     * @param <A> type annotation
     */
    private static class FieldAwareCheck<A extends Annotation> implements Runnable {
        final Class<A> annotationClass;
        final PersistentResource resource;
        final String fieldName;

        public FieldAwareCheck(Class<A> annotationClass, PersistentResource resource) {
            this.annotationClass = annotationClass;
            this.resource = resource;
            this.fieldName = null;
        }

        public FieldAwareCheck(Class<A> annotationClass, PersistentResource resource, String fieldName) {
            this.annotationClass = annotationClass;
            this.resource = resource;
            this.fieldName = fieldName;
        }

        public void run() {
            // Hack: doNotDefer is a special flag to temporarily disable deferred checking. Presumably, this check
            // should not be running if it needs to be deferred (in which case, deferred checks would also be executing)
            // We should probably find a cleaner way to do this.
            resource.getRequestScope().doNotDefer = true;
            if (fieldName != null && !fieldName.isEmpty()) {
                specificField(fieldName);
            } else {
                allFields();
            }
            resource.getRequestScope().doNotDefer = false;
        }

        /**
         * Determine whether or not a specific field has the specified permission. If this field has the permission
         * either by default or override, then this method will return successfully. Otherwise, a
         * ForbiddenAccessException is thrown.
         *
         * @param theField Field to check
         */
        private void specificField(String theField) {
            try {
                resource.checkPermission(annotationClass, resource);
            } catch (ForbiddenAccessException e) {
                resource.checkFieldPermissionIfExists(annotationClass, resource, theField);
            }
            resource.checkFieldPermission(annotationClass, resource, theField);
        }

        /**
         * Checks whether or not the object itself or ANY field on the object has the specified permission.
         * If either condition is true, then this method will return without error. Otherwise, a
         * ForbiddenAccessException is thrown.
         */
        private void allFields() {
            EntityDictionary dictionary = resource.getDictionary();

            try {
                resource.checkPermission(annotationClass, resource);
                return; // Object has permission
            } catch (ForbiddenAccessException e) {
                // Ignore
            }

            // Check attrs
            for (String attr : dictionary.getAttributes(resource.getObject().getClass())) {
                try {
                    specificField(attr);
                    return; // We have at least a single accessible field
                } catch (ForbiddenAccessException e) {
                    // Ignore this.
                }
            }

            // Check relationships
            for (String rel : dictionary.getRelationships(resource.getObject().getClass())) {
                try {
                    specificField(rel);
                    return; // We have at least a single accessible field
                } catch (ForbiddenAccessException e) {
                    // Ignore
                }
            }

            // No accessible fields and object is not accessible
            throw new ForbiddenAccessException();
        }
    }
}
