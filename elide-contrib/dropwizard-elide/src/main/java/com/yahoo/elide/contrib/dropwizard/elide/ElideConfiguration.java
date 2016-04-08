/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.contrib.dropwizard.elide;

import com.yahoo.elide.audit.AuditLogger;
import com.yahoo.elide.audit.Slf4jLogger;
import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.resources.JsonApiEndpoint;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Environment;

/**
 * Elide Configuration
 *
 * @param <T> Dropwizard Configuration
 */
public interface ElideConfiguration<T extends Configuration> {
    /**
     * Get the AuditLogger for Elide
     *
     * Override this method to plug in your own AuditLogger
     *
     * @param configuration Dropwizard configuration
     * @param environment Dropwizard environment
     * @return auditLogger to be used in Elide
     */
    default AuditLogger getAuditLogger(T configuration, Environment environment) {
        return new Slf4jLogger();
    }

    /**
     * Get the DefaultOpaqueUserFunction for Elide
     *
     * Override this method to plug in your own DefaultOpaqueUserFunction
     *
     * @param configuration Dropwizard configuration
     * @param environment Dropwizard environment
     * @return defaultOpaqueUserFunction to be used in Elide
     */
    default JsonApiEndpoint.DefaultOpaqueUserFunction getUserFn(T configuration, Environment environment) {
        return v -> null;
    }

    /**
     * Get the DataStore for Elide
     *
     * Override this method to plug in your own DataStore
     *
     * @param configuration Dropwizard configuration
     * @param environment Dropwizard environment
     * @return dataStore to be used in Elide
     */
    DataStore getDataStore(T configuration, Environment environment);
}
