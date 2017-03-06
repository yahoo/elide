/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.initialization;

import com.yahoo.elide.audit.TestAuditLogger;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * Resource config for AssignedIdLong IT tests.
 */
public class PatchIntegrationTestApplicationResourceConfig extends ResourceConfig {
    public PatchIntegrationTestApplicationResourceConfig() {
        register(new PatchTestBinder(new TestAuditLogger()));
    }
}
