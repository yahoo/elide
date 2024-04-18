/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.datastores.jms;

import com.paiondata.elide.core.audit.Slf4jLogger;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.server.ResourceConfig;

import jakarta.inject.Inject;

/**
 * Resource configuration for integration tests.
 */
public class TestResourceConfig extends ResourceConfig {

    @Inject
    public TestResourceConfig(ServiceLocator injector) {
        register(new TestBinder(new Slf4jLogger(), injector));
    }
}
