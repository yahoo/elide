/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.initialization;

import static com.yahoo.elide.initialization.IntegrationTest.getDataStore;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.async.AsyncSettings;
import com.yahoo.elide.async.AsyncSettings.AsyncSettingsBuilder;
import com.yahoo.elide.core.audit.AuditLogger;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.core.filter.dialect.jsonapi.DefaultFilterDialect;
import com.yahoo.elide.core.filter.dialect.jsonapi.MultipleFilterDialect;
import com.yahoo.elide.graphql.GraphQLSettings.GraphQLSettingsBuilder;
import com.yahoo.elide.jsonapi.JsonApiSettings.JsonApiSettingsBuilder;

import example.TestCheckMappings;
import example.models.triggers.Invoice;
import example.models.triggers.services.BillingService;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

import java.util.Arrays;

/**
 * Binder for integration tests related to verbose error responses.
 */
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

                JsonApiSettingsBuilder jsonApiSettings = JsonApiSettingsBuilder.withDefaults(dictionary)
                        .joinFilterDialect(multipleFilterStrategy)
                        .subqueryFilterDialect(multipleFilterStrategy);

                GraphQLSettingsBuilder graphqlSettings = GraphQLSettingsBuilder.withDefaults(dictionary);

                AsyncSettingsBuilder asyncSettings = AsyncSettings.builder();

                Elide elide = new Elide(ElideSettings.builder().dataStore(getDataStore())
                        .auditLogger(auditLogger)
                        .settings(jsonApiSettings, graphqlSettings, asyncSettings)
                        .entityDictionary(dictionary)
                        .verboseErrors(true)
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
