/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.initialization;

import static com.paiondata.elide.annotation.LifeCycleHookBinding.Operation.CREATE;
import static com.paiondata.elide.annotation.LifeCycleHookBinding.Operation.UPDATE;
import static com.paiondata.elide.annotation.LifeCycleHookBinding.TransactionPhase.PRECOMMIT;

import com.paiondata.elide.Elide;
import com.paiondata.elide.ElideSettings;
import com.paiondata.elide.core.audit.InMemoryLogger;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.filter.dialect.RSQLFilterDialect;
import com.paiondata.elide.core.filter.dialect.jsonapi.DefaultFilterDialect;
import com.paiondata.elide.core.filter.dialect.jsonapi.MultipleFilterDialect;
import com.paiondata.elide.jsonapi.JsonApiSettings;

import example.TestCheckMappings;
import example.models.triggers.Invoice;
import example.models.triggers.InvoiceCompletionHook;
import example.models.triggers.services.BillingService;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;

import jakarta.inject.Inject;

import java.util.Arrays;
import java.util.Calendar;

public class LifeCycleIntegrationTestApplicationResourceConfig extends ResourceConfig {
    public static final InMemoryLogger LOGGER = new InMemoryLogger();

    @Inject
    public LifeCycleIntegrationTestApplicationResourceConfig(ServiceLocator injector) {
        // Bind to injector
        register(new AbstractBinder() {
            @Override
            protected void configure() {
                EntityDictionary dictionary = EntityDictionary.builder()
                        .injector(injector::inject).checks(TestCheckMappings.MAPPINGS).build();

                bind(dictionary).to(EntityDictionary.class);

                DefaultFilterDialect defaultFilterStrategy = new DefaultFilterDialect(dictionary);
                RSQLFilterDialect rsqlFilterStrategy = RSQLFilterDialect.builder().dictionary(dictionary).build();

                MultipleFilterDialect multipleFilterStrategy = new MultipleFilterDialect(
                        Arrays.asList(rsqlFilterStrategy, defaultFilterStrategy),
                        Arrays.asList(rsqlFilterStrategy, defaultFilterStrategy)
                );
                JsonApiSettings.JsonApiSettingsBuilder jsonApiSettings = JsonApiSettings.builder()
                        .joinFilterDialect(multipleFilterStrategy)
                        .subqueryFilterDialect(multipleFilterStrategy);

                Elide elide = new Elide(ElideSettings.builder().dataStore(IntegrationTest.getDataStore())
                        .auditLogger(LOGGER)
                        .entityDictionary(dictionary)
                        .serdes(serdes -> serdes.withISO8601Dates("yyyy-MM-dd'T'HH:mm'Z'", Calendar.getInstance().getTimeZone()))
                        .settings(jsonApiSettings)
                        .build());
                elide.doScans();
                bind(elide).to(Elide.class).named("elide");

                BillingService billingService = new BillingService() {
                    @Override
                    public long purchase(Invoice invoice) {
                        return 100;
                    }
                };

                bind(billingService).to(BillingService.class);

                InvoiceCompletionHook invoiceCompletionHook = new InvoiceCompletionHook(billingService);
                dictionary.bindTrigger(Invoice.class, "complete", CREATE, PRECOMMIT, invoiceCompletionHook);
                dictionary.bindTrigger(Invoice.class, "complete", UPDATE, PRECOMMIT, invoiceCompletionHook);
            }
        });
    }
}
