/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.initialization;

import com.yahoo.elide.audit.TestAuditLogger;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.server.ResourceConfig;

import javax.inject.Inject;

/**
 * Resource configuration for integration tests.
 */
public class IntegrationTestApplicationResourceConfig extends ResourceConfig {

    @Inject
    public IntegrationTestApplicationResourceConfig(ServiceLocator injector) {
        register(new StandardTestBinder(new TestAuditLogger(), injector));
    }
}
