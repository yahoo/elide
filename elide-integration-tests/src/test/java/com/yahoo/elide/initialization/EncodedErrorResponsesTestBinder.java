/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.initialization;

import static com.yahoo.elide.initialization.IntegrationTest.getDataStore;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.audit.AuditLogger;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.filter.dialect.DefaultFilterDialect;
import com.yahoo.elide.core.filter.dialect.MultipleFilterDialect;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.resources.DefaultOpaqueUserFunction;
import com.yahoo.elide.security.executors.ActivePermissionExecutor;
import com.yahoo.elide.security.executors.VerbosePermissionExecutor;

import example.TestCheckMappings;

import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

/**
 * Binder for integration tests related to encoding error responses.
 * Sets {@link ElideSettings#isEncodeErrorResponses()} to true.
 */
@Slf4j
public class EncodedErrorResponsesTestBinder extends AbstractBinder {
    private final AuditLogger auditLogger;
    private final ServiceLocator injector;
    private final boolean verboseErrors;
    private final boolean errorObjects;

    public EncodedErrorResponsesTestBinder(final AuditLogger auditLogger, ServiceLocator injector,
                                           boolean verboseErrors, boolean errorObjects) {
        this.auditLogger = auditLogger;
        this.injector = injector;
        this.verboseErrors = verboseErrors;
        this.errorObjects = errorObjects;
    }

    @Override
    protected void configure() {
        // Elide instance
        bindFactory(new Factory<Elide>() {
            @Override
            public Elide provide() {
                EntityDictionary dictionary = new EntityDictionary(TestCheckMappings.MAPPINGS, injector::inject);
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
                        .withEncodeErrorResponses(true)
                        .withReturnErrorObjects(errorObjects)
                        .withPermissionExecutor(verboseErrors ? VerbosePermissionExecutor::new : ActivePermissionExecutor::new)
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
