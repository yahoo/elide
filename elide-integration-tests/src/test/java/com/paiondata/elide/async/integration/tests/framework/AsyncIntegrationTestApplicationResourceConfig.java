/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.async.integration.tests.framework;

import static com.paiondata.elide.annotation.LifeCycleHookBinding.Operation.CREATE;
import static com.paiondata.elide.annotation.LifeCycleHookBinding.Operation.UPDATE;
import static com.paiondata.elide.annotation.LifeCycleHookBinding.TransactionPhase.POSTCOMMIT;
import static com.paiondata.elide.annotation.LifeCycleHookBinding.TransactionPhase.PRECOMMIT;
import static com.paiondata.elide.annotation.LifeCycleHookBinding.TransactionPhase.PREFLUSH;
import static com.paiondata.elide.annotation.LifeCycleHookBinding.TransactionPhase.PRESECURITY;

import com.paiondata.elide.Elide;
import com.paiondata.elide.ElideSettings;
import com.paiondata.elide.async.AsyncSettings;
import com.paiondata.elide.async.AsyncSettings.AsyncSettingsBuilder;
import com.paiondata.elide.async.export.formatter.CsvExportFormatter;
import com.paiondata.elide.async.export.formatter.JsonExportFormatter;
import com.paiondata.elide.async.export.formatter.TableExportFormatter;
import com.paiondata.elide.async.export.formatter.XlsxExportFormatter;
import com.paiondata.elide.async.hooks.AsyncQueryHook;
import com.paiondata.elide.async.hooks.TableExportHook;
import com.paiondata.elide.async.integration.tests.AsyncIT;
import com.paiondata.elide.async.models.AsyncQuery;
import com.paiondata.elide.async.models.ResultType;
import com.paiondata.elide.async.models.TableExport;
import com.paiondata.elide.async.models.security.AsyncApiInlineChecks;
import com.paiondata.elide.async.resources.ExportApiEndpoint.ExportApiProperties;
import com.paiondata.elide.async.service.AsyncCleanerService;
import com.paiondata.elide.async.service.AsyncExecutorService;
import com.paiondata.elide.async.service.dao.AsyncApiDao;
import com.paiondata.elide.async.service.dao.DefaultAsyncApiDao;
import com.paiondata.elide.async.service.storageengine.FileResultStorageEngine;
import com.paiondata.elide.async.service.storageengine.ResultStorageEngine;
import com.paiondata.elide.core.audit.InMemoryLogger;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.filter.dialect.RSQLFilterDialect;
import com.paiondata.elide.core.filter.dialect.jsonapi.DefaultFilterDialect;
import com.paiondata.elide.core.filter.dialect.jsonapi.MultipleFilterDialect;
import com.paiondata.elide.core.security.checks.Check;
import com.paiondata.elide.graphql.GraphQLSettings.GraphQLSettingsBuilder;
import com.paiondata.elide.jsonapi.JsonApiSettings.JsonApiSettingsBuilder;

import example.TestCheckMappings;
import example.models.triggers.Invoice;
import example.models.triggers.InvoiceCompletionHook;
import example.models.triggers.services.BillingService;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;

import graphql.execution.SimpleDataFetcherExceptionHandler;

import jakarta.inject.Inject;
import jakarta.servlet.ServletContext;
import jakarta.ws.rs.core.Context;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

public class AsyncIntegrationTestApplicationResourceConfig extends ResourceConfig {
    public static final InMemoryLogger LOGGER = new InMemoryLogger();
    public static final String ASYNC_EXECUTOR_ATTR = "asyncExecutor";
    public static final String STORAGE_DESTINATION_ATTR = "storageDestination";

    public static final Map<String, Class<? extends Check>> MAPPINGS = defineMappings();

    private static Map<String, Class<? extends Check>> defineMappings() {
        Map<String, Class<? extends Check>> map = new HashMap<>(TestCheckMappings.MAPPINGS);
        map.put(AsyncApiInlineChecks.AsyncApiOwner.PRINCIPAL_IS_OWNER,
                        AsyncApiInlineChecks.AsyncApiOwner.class);
        map.put(AsyncApiInlineChecks.AsyncApiAdmin.PRINCIPAL_IS_ADMIN,
                        AsyncApiInlineChecks.AsyncApiAdmin.class);
        map.put(AsyncApiInlineChecks.AsyncApiStatusValue.VALUE_IS_CANCELLED,
                        AsyncApiInlineChecks.AsyncApiStatusValue.class);
        map.put(AsyncApiInlineChecks.AsyncApiStatusQueuedValue.VALUE_IS_QUEUED,
                        AsyncApiInlineChecks.AsyncApiStatusQueuedValue.class);
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

                JsonApiSettingsBuilder jsonApiSettings = JsonApiSettingsBuilder.withDefaults(dictionary).joinFilterDialect(multipleFilterStrategy)
                        .subqueryFilterDialect(multipleFilterStrategy);

                GraphQLSettingsBuilder graphqlSettings = GraphQLSettingsBuilder.withDefaults(dictionary).path("/graphQL");

                AsyncSettingsBuilder asyncSettings = AsyncSettings.builder().export(export -> export.enabled(true).path("/export"));

                Elide elide = new Elide(ElideSettings.builder().dataStore(AsyncIT.getDataStore())
                        .auditLogger(LOGGER)
                        .entityDictionary(dictionary)
                        .serdes(serdes -> serdes.withISO8601Dates("yyyy-MM-dd'T'HH:mm'Z'", Calendar.getInstance().getTimeZone()))
                        .settings(jsonApiSettings)
                        .settings(graphqlSettings)
                        .settings(asyncSettings)
                        .build());
                bind(elide).to(Elide.class).named("elide");

                elide.doScans();

                AsyncApiDao asyncAPIDao = new DefaultAsyncApiDao(elide.getElideSettings(), elide.getDataStore());
                bind(asyncAPIDao).to(AsyncApiDao.class);

                ExecutorService executorService = (ExecutorService) servletContext.getAttribute(ASYNC_EXECUTOR_ATTR);
                AsyncExecutorService asyncExecutorService = new AsyncExecutorService(elide,
                        executorService, executorService, asyncAPIDao, Optional.of(new SimpleDataFetcherExceptionHandler()));

                // Create ResultStorageEngine
                Path storageDestination = (Path) servletContext.getAttribute(STORAGE_DESTINATION_ATTR);
                if (storageDestination != null) { // TableExport is enabled
                    ResultStorageEngine resultStorageEngine = new FileResultStorageEngine(storageDestination.toAbsolutePath().toString());
                    bind(resultStorageEngine).to(ResultStorageEngine.class).named("resultStorageEngine");

                    Map<String, TableExportFormatter> supportedFormatters = new HashMap<>();
                    supportedFormatters.put(ResultType.CSV, new CsvExportFormatter(elide, true));
                    supportedFormatters.put(ResultType.JSON, new JsonExportFormatter(elide));
                    supportedFormatters.put(ResultType.XLSX, new XlsxExportFormatter(elide, true));

                    // Binding TableExport LifeCycleHook
                    TableExportHook tableExportHook = new TableExportHook(asyncExecutorService, Duration.ofSeconds(10L),
                            supportedFormatters, resultStorageEngine, null);
                    dictionary.bindTrigger(TableExport.class, CREATE, PREFLUSH, tableExportHook, false);
                    dictionary.bindTrigger(TableExport.class, CREATE, POSTCOMMIT, tableExportHook, false);
                    dictionary.bindTrigger(TableExport.class, CREATE, PRESECURITY, tableExportHook, false);

                    ExportApiProperties exportApiProperties = new ExportApiProperties(executorService, Duration.ofSeconds(10L));
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
                AsyncQueryHook asyncQueryHook = new AsyncQueryHook(asyncExecutorService, Duration.ofSeconds(10L));

                InvoiceCompletionHook invoiceCompletionHook = new InvoiceCompletionHook(billingService);

                dictionary.bindTrigger(AsyncQuery.class, CREATE, PREFLUSH, asyncQueryHook, false);
                dictionary.bindTrigger(AsyncQuery.class, CREATE, POSTCOMMIT, asyncQueryHook, false);
                dictionary.bindTrigger(AsyncQuery.class, CREATE, PRESECURITY, asyncQueryHook, false);
                dictionary.bindTrigger(Invoice.class, "complete", CREATE, PRECOMMIT, invoiceCompletionHook);
                dictionary.bindTrigger(Invoice.class, "complete", UPDATE, PRECOMMIT, invoiceCompletionHook);

                AsyncCleanerService.init(elide, Duration.ofSeconds(30L), Duration.ofDays(5L), Duration.ofSeconds(150L),
                        asyncAPIDao);
                bind(AsyncCleanerService.getInstance()).to(AsyncCleanerService.class);
            }
        });
    }
}
