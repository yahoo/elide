/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.contrib.dropwizard.elide;

import com.yahoo.elide.audit.AuditLogger;
import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.jsonapi.JsonApiMapper;
import com.yahoo.elide.resources.DefaultOpaqueUserFunction;
import com.yahoo.elide.security.PermissionExecutor;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Environment;

import java.util.function.Function;

/**
 * Elide Configuration.
 *
 * @param <T> Dropwizard Configuration
 */
public interface ElideConfiguration<T extends Configuration> {
    /**
     * Get the {@link AuditLogger} for Elide.
     *
     * Override this method to plug in your own AuditLogger
     *
     * @param configuration Dropwizard configuration
     * @param environment Dropwizard environment
     * @return auditLogger to be used in Elide
     */
    default AuditLogger getAuditLogger(T configuration, Environment environment) {
        return null;
    }

    /**
     * Get the {@link DefaultOpaqueUserFunction} for Elide.
     *
     * Override this method to plug in your own DefaultOpaqueUserFunction
     *
     * @param configuration Dropwizard configuration
     * @param environment Dropwizard environment
     * @return defaultOpaqueUserFunction to be used in Elide
     */
    default DefaultOpaqueUserFunction getUserFn(T configuration, Environment environment) {
        return v -> null;
    }

    /**
     * Get the DataStore for Elide.
     *
     * Override this method to plug in your own DataStore
     *
     * @param configuration Dropwizard configuration
     * @param environment Dropwizard environment
     * @return dataStore to be used in Elide
     */
    DataStore getDataStore(T configuration, Environment environment);

    /**
     * Get the {@link EntityDictionary} for Elide.
     *
     * Override this method to plug in your own EntityDictionary
     *
     * @param configuration Dropwizard Configuration
     * @param environment Dropwizard Environment
     * @return entityDictionary to be used in Elide
     */
    default EntityDictionary getEntityDictionary(T configuration, Environment environment) {
        return null;
    }

    /**
     * Get the {@link JsonApiMapper}.
     *
     * @param configuration Dropwizard Configuration
     * @param environment Dropwizard Environment
     * @return jsonApiMapper to be used in Elide
     */
    default JsonApiMapper getJsonApiMapper(T configuration, Environment environment) {
        return null;
    }

    /**
     * Get the {@link PermissionExecutor}.
     *
     * @param configuration Dropwizard Configuration
     * @param environment Dropwizard Environment
     * @return permissionExecutor function to be used in Elide
     */
    default Function<RequestScope, PermissionExecutor> getPermissionExecutor(T configuration, Environment environment) {
        return null;
    }
}
