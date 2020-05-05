/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.integration.tests.framework;

import static com.yahoo.elide.annotation.LifeCycleHookBinding.Operation.CREATE;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.TransactionPhase.POSTCOMMIT;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.TransactionPhase.PRESECURITY;
import static org.mockito.Mockito.mock;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.async.hooks.ExecuteQueryHook;
import com.yahoo.elide.async.hooks.UpdatePrincipalNameHook;
import com.yahoo.elide.async.integration.tests.AsyncIT;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.security.AsyncQueryOperationChecks;
import com.yahoo.elide.async.service.AsyncExecutorService;
import com.yahoo.elide.async.service.AsyncQueryDAO;
import com.yahoo.elide.async.service.DefaultAsyncQueryDAO;
import com.yahoo.elide.audit.AuditLogger;
import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.filter.dialect.DefaultFilterDialect;
import com.yahoo.elide.core.filter.dialect.MultipleFilterDialect;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;

import com.yahoo.elide.security.checks.Check;
import example.TestCheckMappings;
import example.models.triggers.Invoice;
import example.models.triggers.services.BillingService;

import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Typical-use test binder for integration test resource configs.
 */
public class AsyncTestBinder extends AbstractBinder {
    private final AuditLogger auditLogger;
    private final ServiceLocator injector;

    public static final BillingService BILLING_SERVICE = mock(BillingService.class);

    public AsyncTestBinder(final AuditLogger auditLogger, final ServiceLocator injector) {
        this.auditLogger = auditLogger;
        this.injector = injector;
    }

    @Override
    protected void configure() {
        Map<String, Class<? extends Check>> checkMappings = new HashMap<>(TestCheckMappings.MAPPINGS);
        checkMappings.put(AsyncQueryOperationChecks.AsyncQueryOwner.PRINCIPAL_IS_OWNER, AsyncQueryOperationChecks.AsyncQueryOwner.class);
        checkMappings.put(AsyncQueryOperationChecks.AsyncQueryStatusValue.VALUE_IS_CANCELLED, AsyncQueryOperationChecks.AsyncQueryStatusValue.class);

        EntityDictionary dictionary = new EntityDictionary(checkMappings, injector::inject);

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

                Elide elide = new Elide(new ElideSettingsBuilder(AsyncIT.getDataStore())
                        .withAuditLogger(auditLogger)
                        .withJoinFilterDialect(multipleFilterStrategy)
                        .withSubqueryFilterDialect(multipleFilterStrategy)
                        .withEntityDictionary(dictionary)
                        .withISO8601Dates("yyyy-MM-dd'T'HH:mm'Z'", Calendar.getInstance().getTimeZone())
                        .build());

                return elide;
            }

            @Override
            public void dispose(Elide elide) {

            }
        }).to(Elide.class).named("elide");

        bind(new BillingService() {
                 @Override
                 public long purchase(Invoice invoice) {
                     return 0;
                 }
             }
        ).to(BillingService.class);

        Elide elide = injector.getService(Elide.class);
        AsyncQueryDAO asyncQueryDao = new DefaultAsyncQueryDAO(elide, elide.getDataStore());
        bind(asyncQueryDao).to(AsyncQueryDAO.class);
        AsyncExecutorService.init(elide, 5, 60, asyncQueryDao);
        bind(AsyncExecutorService.getInstance()).to(AsyncExecutorService.class);
        // Binding AsyncQuery LifeCycleHook
        ExecuteQueryHook executeQueryHook = new ExecuteQueryHook(AsyncExecutorService.getInstance());
        UpdatePrincipalNameHook updatePrincipalNameHook = new UpdatePrincipalNameHook();
        dictionary.bindTrigger(AsyncQuery.class, CREATE, POSTCOMMIT, executeQueryHook, false);
        dictionary.bindTrigger(AsyncQuery.class, CREATE, PRESECURITY, updatePrincipalNameHook, false);

        // Bind additional elements
        bind(elide.getElideSettings()).to(ElideSettings.class);
        bind(elide.getElideSettings().getDictionary()).to(EntityDictionary.class);
        bind(elide.getElideSettings().getDataStore()).to(DataStore.class).named("elideDataStore");

    }
}
