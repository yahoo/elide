/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.initialization;

import static com.yahoo.elide.initialization.IntegrationTest.getDataStore;
import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.audit.AuditLogger;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.core.filter.dialect.jsonapi.DefaultFilterDialect;
import com.yahoo.elide.core.filter.dialect.jsonapi.MultipleFilterDialect;
import com.yahoo.elide.core.security.PermissionExecutor;
import com.yahoo.elide.core.security.executors.VerbosePermissionExecutor;

import example.TestCheckMappings;
import example.models.triggers.Invoice;
import example.models.triggers.services.BillingService;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.function.Function;

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

                DataStore ds = getDataStore();
                DataStore newDS = new DataStore() {

                    @Override
                    public void populateEntityDictionary(EntityDictionary dictionary) {
                        ds.populateEntityDictionary(dictionary);
                    }

                    @Override
                    public DataStoreTransaction beginTransaction() {
                        return ds.beginTransaction();
                    }

                    @Override
                    public DataStoreTransaction beginReadTransaction() {
                        return ds.beginReadTransaction();
                    }

                    @Override
                    public Function<RequestScope, PermissionExecutor> getPermissionExecutorFunction() {
                        return VerbosePermissionExecutor::new;
                    }
                };
                return new Elide(new ElideSettingsBuilder(newDS)
                        .withAuditLogger(auditLogger)
                        .withJoinFilterDialect(multipleFilterStrategy)
                        .withSubqueryFilterDialect(multipleFilterStrategy)
                        .withEntityDictionary(dictionary)
                        .build());
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
