/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.initialization;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.audit.AuditLogger;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.filter.dialect.DefaultFilterDialect;
import com.yahoo.elide.core.filter.dialect.MultipleFilterDialect;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.resources.DefaultOpaqueUserFunction;

import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

import java.util.Arrays;
import java.util.HashMap;

/**
 * Typical-use test binder for integration test resource configs.
 */
public class AssignedIdLongStandardTestBinder extends AbstractBinder {
    private final AuditLogger auditLogger;

    public AssignedIdLongStandardTestBinder(final AuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }

    @Override
    protected void configure() {
        // Elide instance
        bindFactory(new Factory<Elide>() {
            @Override
            public Elide provide() {
                EntityDictionary dictionary = new EntityDictionary(new HashMap<>());
                DefaultFilterDialect defaultFilterStrategy = new DefaultFilterDialect(dictionary);
                RSQLFilterDialect rsqlFilterStrategy = new RSQLFilterDialect(dictionary);

                MultipleFilterDialect multipleFilterStrategy = new MultipleFilterDialect(
                        Arrays.asList(rsqlFilterStrategy, defaultFilterStrategy),
                        Arrays.asList(rsqlFilterStrategy, defaultFilterStrategy)
                );

                return new Elide(new ElideSettingsBuilder(AbstractIntegrationTestInitializer.getDatabaseManager())
                        .withAuditLogger(auditLogger)
                        .withJoinFilterDialect(multipleFilterStrategy)
                        .withSubqueryFilterDialect(multipleFilterStrategy)
                        .withEntityDictionary(dictionary)
                        .withUpdate200Status()
                        .build());
            }

            @Override
            public void dispose(Elide elide) {

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
            }
        }).to(DefaultOpaqueUserFunction.class).named("elideUserExtractionFunction");
    }
}
