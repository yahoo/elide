/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.integration.tests.framework;

import static com.yahoo.elide.annotation.LifeCycleHookBinding.Operation.CREATE;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.Operation.READ;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.Operation.UPDATE;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.TransactionPhase.POSTCOMMIT;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.TransactionPhase.PRECOMMIT;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.TransactionPhase.PRESECURITY;
import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.async.hooks.AsyncQueryHook;
import com.yahoo.elide.async.integration.tests.AsyncIT;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.security.AsyncQueryInlineChecks;
import com.yahoo.elide.async.service.AsyncCleanerService;
import com.yahoo.elide.async.service.AsyncExecutorService;
import com.yahoo.elide.async.service.dao.AsyncAPIDAO;
import com.yahoo.elide.async.service.dao.DefaultAsyncAPIDAO;
import com.yahoo.elide.async.service.storageengine.FileResultStorageEngine;
import com.yahoo.elide.async.service.storageengine.ResultStorageEngine;
import com.yahoo.elide.core.audit.InMemoryLogger;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.core.filter.dialect.jsonapi.DefaultFilterDialect;
import com.yahoo.elide.core.filter.dialect.jsonapi.MultipleFilterDialect;
import com.yahoo.elide.core.security.checks.Check;
import example.TestCheckMappings;
import example.models.triggers.Invoice;
import example.models.triggers.InvoiceCompletionHook;
import example.models.triggers.services.BillingService;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;

public class AsyncIntegrationTestApplicationResourceConfig extends ResourceConfig {
    public static final InMemoryLogger LOGGER = new InMemoryLogger();

    public static final Map<String, Class<? extends Check>> MAPPINGS = defineMappings();

    private static Map<String, Class<? extends Check>> defineMappings() {
        Map<String, Class<? extends Check>> map = new HashMap<>(TestCheckMappings.MAPPINGS);
        map.put(AsyncQueryInlineChecks.AsyncQueryOwner.PRINCIPAL_IS_OWNER,
                        AsyncQueryInlineChecks.AsyncQueryOwner.class);
        map.put(AsyncQueryInlineChecks.AsyncQueryAdmin.PRINCIPAL_IS_ADMIN,
                        AsyncQueryInlineChecks.AsyncQueryAdmin.class);
        map.put(AsyncQueryInlineChecks.AsyncQueryStatusValue.VALUE_IS_CANCELLED,
                        AsyncQueryInlineChecks.AsyncQueryStatusValue.class);
        map.put(AsyncQueryInlineChecks.AsyncQueryStatusQueuedValue.VALUE_IS_QUEUED,
                        AsyncQueryInlineChecks.AsyncQueryStatusQueuedValue.class);
        return Collections.unmodifiableMap(map);
    }

    @Inject
    public AsyncIntegrationTestApplicationResourceConfig(ServiceLocator injector) {
        // Bind to injector
        register(new AbstractBinder() {
            @Override
            protected void configure() {
                EntityDictionary dictionary = new EntityDictionary(MAPPINGS, injector::inject);

                bind(dictionary).to(EntityDictionary.class);

                DefaultFilterDialect defaultFilterStrategy = new DefaultFilterDialect(dictionary);
                RSQLFilterDialect rsqlFilterStrategy = new RSQLFilterDialect(dictionary);

                MultipleFilterDialect multipleFilterStrategy = new MultipleFilterDialect(
                        Arrays.asList(rsqlFilterStrategy, defaultFilterStrategy),
                        Arrays.asList(rsqlFilterStrategy, defaultFilterStrategy)
                );

                Elide elide = new Elide(new ElideSettingsBuilder(AsyncIT.getDataStore())
                        .withAuditLogger(LOGGER)
                        .withJoinFilterDialect(multipleFilterStrategy)
                        .withSubqueryFilterDialect(multipleFilterStrategy)
                        .withEntityDictionary(dictionary)
                        .withISO8601Dates("yyyy-MM-dd'T'HH:mm'Z'", Calendar.getInstance().getTimeZone())
                        .build());
                bind(elide).to(Elide.class).named("elide");

                AsyncAPIDAO asyncAPIDao = new DefaultAsyncAPIDAO(elide.getElideSettings(), elide.getDataStore());
                bind(asyncAPIDao).to(AsyncAPIDAO.class);

                ResultStorageEngine resultStorageEngine =
                        new FileResultStorageEngine(System.getProperty("java.io.tmpDir"));
                AsyncExecutorService.init(elide, 5, asyncAPIDao, resultStorageEngine);
                bind(AsyncExecutorService.getInstance()).to(AsyncExecutorService.class);

                BillingService billingService = new BillingService() {
                    @Override
                    public long purchase(Invoice invoice) {
                        return 0;
                    }
                };

                bind(billingService).to(BillingService.class);

                // Binding AsyncQuery LifeCycleHook
                AsyncQueryHook asyncQueryHook = new AsyncQueryHook(AsyncExecutorService.getInstance(), 10, true);

                InvoiceCompletionHook invoiceCompletionHook = new InvoiceCompletionHook(billingService);

                dictionary.bindTrigger(AsyncQuery.class, READ, PRESECURITY, asyncQueryHook, false);
                dictionary.bindTrigger(AsyncQuery.class, CREATE, POSTCOMMIT, asyncQueryHook, false);
                dictionary.bindTrigger(AsyncQuery.class, CREATE, PRESECURITY, asyncQueryHook, false);
                dictionary.bindTrigger(Invoice.class, "complete", CREATE, PRECOMMIT, invoiceCompletionHook);
                dictionary.bindTrigger(Invoice.class, "complete", UPDATE, PRECOMMIT, invoiceCompletionHook);

                AsyncCleanerService.init(elide, 30, 5, 150, asyncAPIDao);
                bind(AsyncCleanerService.getInstance()).to(AsyncCleanerService.class);
            }
        });
    }
}
