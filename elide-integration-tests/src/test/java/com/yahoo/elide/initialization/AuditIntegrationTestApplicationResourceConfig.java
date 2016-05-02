/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.initialization;

import com.yahoo.elide.audit.InMemoryLogger;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * Resource config for Audit IT tests.
 */
public class AuditIntegrationTestApplicationResourceConfig extends ResourceConfig {
    public static final InMemoryLogger LOGGER = new InMemoryLogger();

    public AuditIntegrationTestApplicationResourceConfig() {
        register(new StandardTestBinder(LOGGER));
    }
}
