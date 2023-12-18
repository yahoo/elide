/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.spring.config;

import static com.yahoo.elide.annotation.LifeCycleHookBinding.Operation.CREATE;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.TransactionPhase.POSTCOMMIT;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.TransactionPhase.PREFLUSH;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.TransactionPhase.PRESECURITY;

import com.yahoo.elide.RefreshableElide;
import com.yahoo.elide.async.export.formatter.CsvExportFormatter;
import com.yahoo.elide.async.export.formatter.JsonExportFormatter;
import com.yahoo.elide.async.export.formatter.TableExportFormatter;
import com.yahoo.elide.async.hooks.AsyncQueryHook;
import com.yahoo.elide.async.hooks.TableExportHook;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.ResultType;
import com.yahoo.elide.async.models.TableExport;
import com.yahoo.elide.async.service.AsyncCleanerService;
import com.yahoo.elide.async.service.AsyncExecutorService;
import com.yahoo.elide.async.service.dao.AsyncApiDao;
import com.yahoo.elide.async.service.dao.DefaultAsyncApiDao;
import com.yahoo.elide.async.service.storageengine.FileResultStorageEngine;
import com.yahoo.elide.async.service.storageengine.ResultStorageEngine;
import com.yahoo.elide.core.dictionary.EntityDictionary;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import graphql.execution.DataFetcherExceptionHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Async Configuration For Elide Services.  Override any of the beans (by defining your own)
 * and setting flags to disable in properties to change the default behavior.
 */
@Configuration
@EntityScan(basePackageClasses = AsyncQuery.class)
@EnableConfigurationProperties(ElideConfigProperties.class)
@ConditionalOnExpression("${elide.async.enabled:false}")
public class ElideAsyncConfiguration {

    /**
     * Configure the AsyncExecutorService used for submitting async query requests.
     * @param elide elideObject.
     * @param settings Elide settings.
     * @param asyncQueryDao AsyncDao object.
     * @param optionalResultStorageEngine Result Storage Engine.
     * @param optionalDataFetcherExceptionHandler GraphQL data fetcher exception handler.
     * @return a AsyncExecutorService.
     */
    @Bean
    @ConditionalOnMissingBean
    public AsyncExecutorService asyncExecutorService(
            RefreshableElide elide,
            ElideConfigProperties settings,
            AsyncApiDao asyncQueryDao,
            Optional<ResultStorageEngine> optionalResultStorageEngine,
            Optional<DataFetcherExceptionHandler> optionalDataFetcherExceptionHandler
    ) {
        AsyncProperties asyncProperties = settings.getAsync();

        ExecutorService executor = Executors.newFixedThreadPool(asyncProperties.getThreadPoolSize());
        ExecutorService updater = Executors.newFixedThreadPool(asyncProperties.getThreadPoolSize());
        AsyncExecutorService asyncExecutorService = new AsyncExecutorService(elide.getElide(), executor,
                updater, asyncQueryDao, optionalDataFetcherExceptionHandler);

        // Binding AsyncQuery LifeCycleHook
        AsyncQueryHook asyncQueryHook = new AsyncQueryHook(asyncExecutorService,
                asyncProperties.getMaxAsyncAfter());

        EntityDictionary dictionary = elide.getElide().getElideSettings().getEntityDictionary();

        dictionary.bindTrigger(AsyncQuery.class, CREATE, PREFLUSH, asyncQueryHook, false);
        dictionary.bindTrigger(AsyncQuery.class, CREATE, POSTCOMMIT, asyncQueryHook, false);
        dictionary.bindTrigger(AsyncQuery.class, CREATE, PRESECURITY, asyncQueryHook, false);

        boolean exportEnabled = ElideAutoConfiguration.isExportEnabled(asyncProperties);

        if (exportEnabled) {
            // Initialize the Formatters.
            boolean writeCSVHeader = asyncProperties.getExport() != null
                    && asyncProperties.getExport().getFormat().getCsv().isWriteHeader();
            Map<ResultType, TableExportFormatter> supportedFormatters = new HashMap<>();
            supportedFormatters.put(ResultType.CSV, new CsvExportFormatter(elide.getElide(), writeCSVHeader));
            supportedFormatters.put(ResultType.JSON, new JsonExportFormatter(elide.getElide()));

            // Binding TableExport LifeCycleHook
            TableExportHook tableExportHook = getTableExportHook(asyncExecutorService, settings, supportedFormatters,
                    optionalResultStorageEngine.orElse(null));
            dictionary.bindTrigger(TableExport.class, CREATE, PREFLUSH, tableExportHook, false);
            dictionary.bindTrigger(TableExport.class, CREATE, POSTCOMMIT, tableExportHook, false);
            dictionary.bindTrigger(TableExport.class, CREATE, PRESECURITY, tableExportHook, false);
        }

        return asyncExecutorService;
    }

    private TableExportHook getTableExportHook(AsyncExecutorService asyncExecutorService,
            ElideConfigProperties settings, Map<ResultType, TableExportFormatter> supportedFormatters,
            ResultStorageEngine resultStorageEngine) {
        return new TableExportHook(asyncExecutorService, settings.getAsync().getMaxAsyncAfter(), supportedFormatters,
                resultStorageEngine);
    }

    /**
     * Configure the AsyncCleanerService used for cleaning up async query requests.
     * @param elide elideObject.
     * @param settings Elide settings.
     * @param asyncQueryDao AsyncDao object.
     * @return a AsyncCleanerService.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "elide.async.cleanup", name = "enabled", matchIfMissing = false)
    public AsyncCleanerService asyncCleanerService(RefreshableElide elide,
                                                        ElideConfigProperties settings,
                                                        AsyncApiDao asyncQueryDao) {
        AsyncCleanerService.init(elide.getElide(), settings.getAsync().getCleanup().getQueryMaxRunTime(),
                settings.getAsync().getCleanup().getQueryRetentionDuration(),
                settings.getAsync().getCleanup().getQueryCancellationCheckInterval(), asyncQueryDao);
        return AsyncCleanerService.getInstance();
    }

    /**
     * Configure the AsyncQueryDAO used by async query requests.
     * @param elide elideObject.
     * @return an AsyncQueryDAO object.
     */
    @Bean
    @ConditionalOnMissingBean
    public AsyncApiDao asyncApiDao(RefreshableElide elide) {
        return new DefaultAsyncApiDao(elide.getElide().getElideSettings(), elide.getElide().getDataStore());
    }

    /**
     * Configure the ResultStorageEngine used by async query requests.
     * @param settings Elide settings.
     * @return an ResultStorageEngine object.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "elide.async.export", name = "enabled", matchIfMissing = false)
    public ResultStorageEngine resultStorageEngine(ElideConfigProperties settings) {
        FileResultStorageEngine resultStorageEngine = new FileResultStorageEngine(settings.getAsync().getExport()
                .getStorageDestination(), settings.getAsync().getExport().isAppendFileExtension());
        return resultStorageEngine;
    }
}
