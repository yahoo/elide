/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import com.yahoo.elide.annotation.OnCommit;
import com.yahoo.elide.audit.AuditLogger;
import com.yahoo.elide.core.filter.Predicate;
import com.yahoo.elide.jsonapi.JsonApiMapper;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.yahoo.elide.security.PermissionExecutor;
import com.yahoo.elide.security.User;

import lombok.Getter;

import javax.ws.rs.core.MultivaluedMap;
import java.util.ArrayList;
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
    @Getter private final AuditLogger auditLogger;
    @Getter private final Optional<MultivaluedMap<String, String>> queryParams;
    @Getter private final Map<String, Set<String>> sparseFields;
    @Getter private final Map<String, Set<Predicate>> predicates;
    @Getter private final ObjectEntityCache objectEntityCache;
    @Getter private final SecurityMode securityMode;
    @Getter private final Set<PersistentResource> newResources;
    @Getter private final PermissionExecutor permissionExecutor;

    final private transient LinkedHashSet<Runnable> commitTriggers;

    public RequestScope(JsonApiDocument jsonApiDocument,
                        DataStoreTransaction transaction,
                        User user,
                        EntityDictionary dictionary,
                        JsonApiMapper mapper,
                        AuditLogger auditLogger,
                        MultivaluedMap<String, String> queryParams,
                        SecurityMode securityMode) {
        this.jsonApiDocument = jsonApiDocument;
        this.transaction = transaction;
        this.user = user;
        this.dictionary = dictionary;
        this.mapper = mapper;
        this.auditLogger = auditLogger;
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
        permissionExecutor = new PermissionExecutor(this);
    }

    public RequestScope(JsonApiDocument jsonApiDocument,
                        DataStoreTransaction transaction,
                        User user,
                        EntityDictionary dictionary,
                        JsonApiMapper mapper,
                        AuditLogger auditLogger,
                        SecurityMode securityMode) {
        this(jsonApiDocument, transaction, user, dictionary, mapper, auditLogger, null, securityMode);
    }

    public RequestScope(JsonApiDocument jsonApiDocument,
                        DataStoreTransaction transaction,
                        User user,
                        EntityDictionary dictionary,
                        JsonApiMapper mapper,
                        AuditLogger auditLogger,
                        MultivaluedMap<String, String> queryParams) {
        this(jsonApiDocument, transaction, user, dictionary, mapper, auditLogger, queryParams,
                SecurityMode.SECURITY_ACTIVE);
    }

    public RequestScope(JsonApiDocument jsonApiDocument,
                        DataStoreTransaction transaction,
                        User user,
                        EntityDictionary dictionary,
                        JsonApiMapper mapper,
                        AuditLogger auditLogger) {
        this(jsonApiDocument, transaction, user, dictionary, mapper, auditLogger, null, SecurityMode.SECURITY_ACTIVE);
    }

    /**
     * Outer RequestScope constructor for use by Patch Extension.
     *
     * @param transaction the transaction
     * @param user        the user
     * @param dictionary  the dictionary
     * @param mapper      the mapper
     * @param auditLogger      the logger
     */
    protected RequestScope(
            DataStoreTransaction transaction,
            User user,
            EntityDictionary dictionary,
            JsonApiMapper mapper,
            AuditLogger auditLogger) {
        this(null, transaction, user, dictionary, mapper, auditLogger);
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
        this.auditLogger = outerRequestScope.auditLogger;
        this.queryParams = Optional.empty();
        this.sparseFields = Collections.emptyMap();
        this.predicates = Collections.emptyMap();
        this.objectEntityCache = outerRequestScope.objectEntityCache;
        this.securityMode = outerRequestScope.securityMode;
        this.newResources = outerRequestScope.newResources;
        this.commitTriggers = outerRequestScope.commitTriggers;
        this.permissionExecutor = new PermissionExecutor(this);
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
     * run any deferred post-commit triggers.
     *
     * @see com.yahoo.elide.annotation.CreatePermission
     */
    public void runCommitTriggers() {
        new ArrayList<>(commitTriggers).forEach(Runnable::run);
        commitTriggers.clear();
    }

    public void queueCommitTrigger(PersistentResource resource) {
        queueCommitTrigger(resource, "");
    }

    public void queueCommitTrigger(PersistentResource resource, String fieldName) {
        commitTriggers.add(() -> resource.runTriggers(OnCommit.class, fieldName));
    }
}
