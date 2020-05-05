/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.integration.tests.framework;

import com.yahoo.elide.audit.InMemoryLogger;

import com.google.common.collect.Lists;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.server.ResourceConfig;

import java.util.List;

import javax.inject.Inject;

public class AsyncIntegrationTestApplicationResourceConfig extends ResourceConfig {
    public static final InMemoryLogger LOGGER = new InMemoryLogger();

    @Inject
    public AsyncIntegrationTestApplicationResourceConfig(ServiceLocator injector) {
        register(new AsyncTestBinder(LOGGER, injector));
        registerFilters(Lists.newArrayList(AsyncAuthFilter.class));
    }

    /**
     * Register provided JAX-RS filters.
     */
    private void registerFilters(List<Class<?>> filters) {
        filters.forEach(this::register);
    }
}
