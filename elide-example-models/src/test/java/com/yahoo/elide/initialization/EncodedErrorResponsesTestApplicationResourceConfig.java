/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.initialization;

import com.yahoo.elide.audit.TestAuditLogger;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.server.ResourceConfig;

import javax.inject.Inject;

/**
 * Resource configuration for encoded error response integration tests.
 */
public class EncodedErrorResponsesTestApplicationResourceConfig extends ResourceConfig {

    @Inject
    public EncodedErrorResponsesTestApplicationResourceConfig(ServiceLocator injector) {
        register(new EncodedErrorResponsesTestBinder(new TestAuditLogger(), injector, false, false));
    }
}
