/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.initialization;

import com.yahoo.elide.audit.TestAuditLogger;

import org.glassfish.jersey.server.ResourceConfig;

/**
 * Resource configuration for error objects integration tests.
 */
public class ErrorObjectsIntegrationTestApplicationResourceConfig extends ResourceConfig {
    public ErrorObjectsIntegrationTestApplicationResourceConfig() {
        register(new ErrorObjectsTestBinder(new TestAuditLogger()));
    }
}
