/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.initialization;

import static com.yahoo.elide.initialization.IntegrationTest.getDataStore;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.core.audit.AuditLogger;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.core.filter.dialect.jsonapi.DefaultFilterDialect;
import com.yahoo.elide.core.filter.dialect.jsonapi.MultipleFilterDialect;
import example.TestCheckMappings;
import example.models.triggers.Invoice;
import example.models.triggers.services.BillingService;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

/**
 * Binder for integration tests related to verbose error responses.
 */
@Slf4j
public class VerboseErrorResponsesTestBinder extends AbstractBinder {
    private final AuditLogger auditLogger;
    private final ServiceLocator injector;

    public VerboseErrorResponsesTestBinder(final AuditLogger auditLogger, ServiceLocator injector) {
        this.auditLogger = auditLogger;
        this.injector = injector;
    }

    @Override
    protected void configure() {
        EntityDictionary dictionary = EntityDictionary.builder()
                .injector(injector::inject)
                .checks(TestCheckMappings.MAPPINGS)
                .build();

        bind(dictionary).to(EntityDictionary.class);

        // Elide instance
        bindFactory(new Factory<Elide>() {
            @Override
            public Elide provide() {
                DefaultFilterDialect defaultFilterStrategy = new DefaultFilterDialect(dictionary);
                RSQLFilterDialect rsqlFilterStrategy = RSQLFilterDialect.builder().dictionary(dictionary).build();

                MultipleFilterDialect multipleFilterStrategy = new MultipleFilterDialect(
                        Arrays.asList(rsqlFilterStrategy, defaultFilterStrategy),
                        Arrays.asList(rsqlFilterStrategy, defaultFilterStrategy)
                );

                Elide elide = new Elide(new ElideSettingsBuilder(getDataStore())
                        .withAuditLogger(auditLogger)
                        .withJoinFilterDialect(multipleFilterStrategy)
                        .withSubqueryFilterDialect(multipleFilterStrategy)
                        .withEntityDictionary(dictionary)
                        .withVerboseErrors()
                        .build());

                elide.doScans();
                return elide;
            }

            @Override
            public void dispose(Elide elide) {
                // do nothing
            }
        }).to(Elide.class).named("elide");

        bind(new BillingService() {
                 @Override
                 public long purchase(Invoice invoice) {
                     return 0;
                 }
             }
        ).to(BillingService.class);
    }
}
