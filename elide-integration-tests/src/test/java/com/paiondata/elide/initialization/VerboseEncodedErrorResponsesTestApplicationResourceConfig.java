/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.initialization;

import com.paiondata.elide.core.audit.TestAuditLogger;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.server.ResourceConfig;

import jakarta.inject.Inject;

public class VerboseEncodedErrorResponsesTestApplicationResourceConfig extends ResourceConfig {

    @Inject
    public VerboseEncodedErrorResponsesTestApplicationResourceConfig(ServiceLocator injector) {
        register(new VerboseErrorResponsesTestBinder(new TestAuditLogger(), injector));
    }
}
