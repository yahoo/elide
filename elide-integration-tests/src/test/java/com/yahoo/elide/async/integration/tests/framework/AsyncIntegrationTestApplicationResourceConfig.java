/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.integration.tests.framework;

import static com.yahoo.elide.annotation.LifeCycleHookBinding.Operation.CREATE;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.Operation.UPDATE;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.TransactionPhase.POSTCOMMIT;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.TransactionPhase.PRECOMMIT;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.TransactionPhase.PREFLUSH;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.TransactionPhase.PRESECURITY;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.async.export.formatter.CSVExportFormatter;
import com.yahoo.elide.async.export.formatter.JSONExportFormatter;
import com.yahoo.elide.async.export.formatter.TableExportFormatter;
import com.yahoo.elide.async.hooks.AsyncQueryHook;
import com.yahoo.elide.async.hooks.TableExportHook;
import com.yahoo.elide.async.integration.tests.AsyncIT;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.ResultType;
import com.yahoo.elide.async.models.TableExport;
import com.yahoo.elide.async.models.security.AsyncAPIInlineChecks;
import com.yahoo.elide.async.resources.ExportApiEndpoint.ExportApiProperties;
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

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;

public class AsyncIntegrationTestApplicationResourceConfig extends ResourceConfig {
    public static final InMemoryLogger LOGGER = new InMemoryLogger();
    public static final String ASYNC_EXECUTOR_ATTR = "asyncExecutor";
    public static final String STORAGE_DESTINATION_ATTR = "storageDestination";

    public static final Map<String, Class<? extends Check>> MAPPINGS = defineMappings();

    private static Map<String, Class<? extends Check>> defineMappings() {
        Map<String, Class<? extends Check>> map = new HashMap<>(TestCheckMappings.MAPPINGS);
        map.put(AsyncAPIInlineChecks.AsyncAPIOwner.PRINCIPAL_IS_OWNER,
                        AsyncAPIInlineChecks.AsyncAPIOwner.class);
        map.put(AsyncAPIInlineChecks.AsyncAPIAdmin.PRINCIPAL_IS_ADMIN,
                        AsyncAPIInlineChecks.AsyncAPIAdmin.class);
        map.put(AsyncAPIInlineChecks.AsyncAPIStatusValue.VALUE_IS_CANCELLED,
                        AsyncAPIInlineChecks.AsyncAPIStatusValue.class);
        map.put(AsyncAPIInlineChecks.AsyncAPIStatusQueuedValue.VALUE_IS_QUEUED,
                        AsyncAPIInlineChecks.AsyncAPIStatusQueuedValue.class);
        return Collections.unmodifiableMap(map);
    }

    @Inject
    public AsyncIntegrationTestApplicationResourceConfig(ServiceLocator injector, @Context ServletContext servletContext) {
        // Bind to injector
        register(new AbstractBinder() {
            @Override
            protected void configure() {
                EntityDictionary dictionary = EntityDictionary.builder()
                        .injector(injector::inject).checks(MAPPINGS).build();

                bind(dictionary).to(EntityDictionary.class);

                DefaultFilterDialect defaultFilterStrategy = new DefaultFilterDialect(dictionary);
                RSQLFilterDialect rsqlFilterStrategy = RSQLFilterDialect.builder().dictionary(dictionary).build();

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
                        .withExportApiPath("/export")
                        .build());
                bind(elide).to(Elide.class).named("elide");

                elide.doScans();

                AsyncAPIDAO asyncAPIDao = new DefaultAsyncAPIDAO(elide.getElideSettings(), elide.getDataStore());
                bind(asyncAPIDao).to(AsyncAPIDAO.class);

                ExecutorService executorService = (ExecutorService) servletContext.getAttribute(ASYNC_EXECUTOR_ATTR);
                AsyncExecutorService asyncExecutorService = new AsyncExecutorService(elide,
                        executorService, executorService, asyncAPIDao);

                // Create ResultStorageEngine
                Path storageDestination = (Path) servletContext.getAttribute(STORAGE_DESTINATION_ATTR);
                if (storageDestination != null) { // TableExport is enabled
                    ResultStorageEngine resultStorageEngine = new FileResultStorageEngine(storageDestination.toAbsolutePath().toString(), false);
                    bind(resultStorageEngine).to(ResultStorageEngine.class).named("resultStorageEngine");

                    Map<ResultType, TableExportFormatter> supportedFormatters = new HashMap<>();
                    supportedFormatters.put(ResultType.CSV, new CSVExportFormatter(elide, false));
                    supportedFormatters.put(ResultType.JSON, new JSONExportFormatter(elide));

                    // Binding TableExport LifeCycleHook
                    TableExportHook tableExportHook = new TableExportHook(asyncExecutorService, 10, supportedFormatters,
                            resultStorageEngine);
                    dictionary.bindTrigger(TableExport.class, CREATE, PREFLUSH, tableExportHook, false);
                    dictionary.bindTrigger(TableExport.class, CREATE, POSTCOMMIT, tableExportHook, false);
                    dictionary.bindTrigger(TableExport.class, CREATE, PRESECURITY, tableExportHook, false);

                    ExportApiProperties exportApiProperties = new ExportApiProperties(executorService, 10);
                    bind(exportApiProperties).to(ExportApiProperties.class).named("exportApiProperties");
                }

                BillingService billingService = new BillingService() {
                    @Override
                    public long purchase(Invoice invoice) {
                        return 0;
                    }
                };

                bind(billingService).to(BillingService.class);

                // Binding AsyncQuery LifeCycleHook
                AsyncQueryHook asyncQueryHook = new AsyncQueryHook(asyncExecutorService, 10);

                InvoiceCompletionHook invoiceCompletionHook = new InvoiceCompletionHook(billingService);

                dictionary.bindTrigger(AsyncQuery.class, CREATE, PREFLUSH, asyncQueryHook, false);
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
