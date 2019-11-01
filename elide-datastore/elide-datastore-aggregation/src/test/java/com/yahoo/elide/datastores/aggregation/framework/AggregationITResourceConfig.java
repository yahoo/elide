/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.framework;

import com.yahoo.elide.audit.TestAuditLogger;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.server.ResourceConfig;

import javax.inject.Inject;

public class AggregationITResourceConfig extends ResourceConfig {
    @Inject
    public AggregationITResourceConfig(ServiceLocator injector) {
        register(new AggregationTestBinder(new TestAuditLogger(), injector));
    }
}
