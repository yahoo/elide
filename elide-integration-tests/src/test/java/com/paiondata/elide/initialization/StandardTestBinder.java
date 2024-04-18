/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.initialization;

import static org.mockito.Mockito.mock;

import com.paiondata.elide.Elide;
import com.paiondata.elide.ElideSettings;
import com.paiondata.elide.async.AsyncSettings;
import com.paiondata.elide.async.AsyncSettings.AsyncSettingsBuilder;
import com.paiondata.elide.core.audit.AuditLogger;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.filter.dialect.RSQLFilterDialect;
import com.paiondata.elide.core.filter.dialect.jsonapi.DefaultFilterDialect;
import com.paiondata.elide.core.filter.dialect.jsonapi.MultipleFilterDialect;
import com.paiondata.elide.graphql.GraphQLSettings.GraphQLSettingsBuilder;
import com.paiondata.elide.jsonapi.JsonApiSettings.JsonApiSettingsBuilder;

import example.TestCheckMappings;
import example.models.triggers.services.BillingService;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

import java.util.Arrays;
import java.util.Calendar;

/**
 * Typical-use test binder for integration test resource configs.
 */
public class StandardTestBinder extends AbstractBinder {
    private final AuditLogger auditLogger;
    private final ServiceLocator injector;

    public static final BillingService BILLING_SERVICE = mock(BillingService.class);

    public StandardTestBinder(final AuditLogger auditLogger, final ServiceLocator injector) {
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

                Elide elide = new Elide(ElideSettings.builder().dataStore(IntegrationTest.getDataStore())
                        .auditLogger(auditLogger)
                        .entityDictionary(dictionary)
                        .serdes(serdes -> serdes.withISO8601Dates("yyyy-MM-dd'T'HH:mm'Z'", Calendar.getInstance().getTimeZone()))
                        .settings(jsonApiSettings, graphqlSettings, asyncSettings)
                        .build());

                elide.doScans();
                return elide;
            }

            @Override
            public void dispose(Elide elide) {

            }
        }).to(Elide.class).named("elide");

        bind(BILLING_SERVICE).to(BillingService.class);
    }
}
