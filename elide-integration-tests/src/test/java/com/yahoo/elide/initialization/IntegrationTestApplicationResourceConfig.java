/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.initialization;

import com.yahoo.elide.audit.TestAuditLogger;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * Resource configuration for integration tests.
 */
public class IntegrationTestApplicationResourceConfig extends ResourceConfig {
    public IntegrationTestApplicationResourceConfig() {
        register(new StandardTestBinder(new TestAuditLogger()));
    }
}
