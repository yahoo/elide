/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.endpoints;

import com.yahoo.elide.Elide;
import com.yahoo.elide.audit.TestLogger;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.datastore.AHibernateTest;
import com.yahoo.elide.resources.JsonApiEndpoint;

import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * Jersey ApplicationResourceConfig for test framework.
 */
public class TestApplicationResourceConfig extends ResourceConfig {
    public TestApplicationResourceConfig() {
        register(new AbstractBinder() {
            @Override
            protected void configure() {
                // Elide instance
                bindFactory(new Factory<Elide>() {
                    @Override
                    public Elide provide() {
                        return new Elide(new TestLogger(), AHibernateTest.getDatabaseManager(), new EntityDictionary());
                    }

                    @Override
                    public void dispose(Elide elide) {

                    }
                }).to(Elide.class).named("elide");

                // User function
                bindFactory(new Factory<JsonApiEndpoint.DefaultOpaqueUserFunction>() {
                    private final Integer user = 1;

                    @Override
                    public JsonApiEndpoint.DefaultOpaqueUserFunction provide() {
                        return v -> user;
                    }

                    @Override
                    public void dispose(JsonApiEndpoint.DefaultOpaqueUserFunction defaultOpaqueUserFunction) {
                    }
                }).to(JsonApiEndpoint.DefaultOpaqueUserFunction.class).named("elideUserExtractionFunction");
            }
        });
    }
}
