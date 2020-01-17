/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.initialization;

import static com.yahoo.elide.initialization.IntegrationTest.getDataStore;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.audit.AuditLogger;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.filter.dialect.DefaultFilterDialect;
import com.yahoo.elide.core.filter.dialect.MultipleFilterDialect;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.resources.DefaultOpaqueUserFunction;

import example.TestCheckMappings;

import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

import java.util.Arrays;

/**
 * Typical-use test binder for integration test resource configs.
 */
public class ErrorObjectsTestBinder extends AbstractBinder {
    private final AuditLogger auditLogger;
    private final ServiceLocator injector;

    public ErrorObjectsTestBinder(final AuditLogger auditLogger, ServiceLocator injector) {
        this.auditLogger = auditLogger;
        this.injector = injector;
    }

    @Override
    protected void configure() {
        EntityDictionary dictionary = new EntityDictionary(TestCheckMappings.MAPPINGS, injector::inject);

        bind(dictionary).to(EntityDictionary.class);

        // Elide instance
        bindFactory(new Factory<Elide>() {
            @Override
            public Elide provide() {
                DefaultFilterDialect defaultFilterStrategy = new DefaultFilterDialect(dictionary);
                RSQLFilterDialect rsqlFilterStrategy = new RSQLFilterDialect(dictionary);

                MultipleFilterDialect multipleFilterStrategy = new MultipleFilterDialect(
                        Arrays.asList(rsqlFilterStrategy, defaultFilterStrategy),
                        Arrays.asList(rsqlFilterStrategy, defaultFilterStrategy)
                );

                return new Elide(new ElideSettingsBuilder(getDataStore())
                        .withAuditLogger(auditLogger)
                        .withJoinFilterDialect(multipleFilterStrategy)
                        .withSubqueryFilterDialect(multipleFilterStrategy)
                        .withEntityDictionary(dictionary)
                        .withReturnErrorObjects(true)
                        .build());
            }

            @Override
            public void dispose(Elide elide) {
                // do nothing
            }
        }).to(Elide.class).named("elide");

        // User function
        bindFactory(new Factory<DefaultOpaqueUserFunction>() {
            private final Integer user = 1;

            @Override
            public DefaultOpaqueUserFunction provide() {
                return v -> user;
            }

            @Override
            public void dispose(DefaultOpaqueUserFunction defaultOpaqueUserFunction) {
                // do nothing
            }
        }).to(DefaultOpaqueUserFunction.class).named("elideUserExtractionFunction");
    }
}
