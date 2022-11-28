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
import com.yahoo.elide.async.export.formatter.CSVExportFormatter;
import com.yahoo.elide.async.export.formatter.JSONExportFormatter;
import com.yahoo.elide.async.export.formatter.TableExportFormatter;
import com.yahoo.elide.async.hooks.AsyncQueryHook;
import com.yahoo.elide.async.hooks.TableExportHook;
import com.yahoo.elide.async.models.AsyncAPI;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.ResultType;
import com.yahoo.elide.async.models.TableExport;
import com.yahoo.elide.async.service.AsyncCleanerService;
import com.yahoo.elide.async.service.AsyncExecutorService;
import com.yahoo.elide.async.service.dao.AsyncAPIDAO;
import com.yahoo.elide.async.service.dao.DefaultAsyncAPIDAO;
import com.yahoo.elide.async.service.storageengine.FileResultStorageEngine;
import com.yahoo.elide.async.service.storageengine.ResultStorageEngine;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.exceptions.InvalidOperationException;
import com.yahoo.elide.core.security.RequestScope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;
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
     * @return a AsyncExecutorService.
     */
    @Bean
    @ConditionalOnMissingBean
    public AsyncExecutorService buildAsyncExecutorService(
            RefreshableElide elide,
            ElideConfigProperties settings,
            AsyncAPIDAO asyncQueryDao,
            @Autowired(required = false) ResultStorageEngine resultStorageEngine
    ) {
        AsyncProperties asyncProperties = settings.getAsync();

        ExecutorService executor = Executors.newFixedThreadPool(asyncProperties.getThreadPoolSize());
        ExecutorService updater = Executors.newFixedThreadPool(asyncProperties.getThreadPoolSize());
        AsyncExecutorService asyncExecutorService = new AsyncExecutorService(elide.getElide(), executor,
                updater, asyncQueryDao);

        // Binding AsyncQuery LifeCycleHook
        AsyncQueryHook asyncQueryHook = new AsyncQueryHook(asyncExecutorService,
                asyncProperties.getMaxAsyncAfterSeconds());

        EntityDictionary dictionary = elide.getElide().getElideSettings().getDictionary();

        dictionary.bindTrigger(AsyncQuery.class, CREATE, PREFLUSH, asyncQueryHook, false);
        dictionary.bindTrigger(AsyncQuery.class, CREATE, POSTCOMMIT, asyncQueryHook, false);
        dictionary.bindTrigger(AsyncQuery.class, CREATE, PRESECURITY, asyncQueryHook, false);

        boolean exportEnabled = ElideAutoConfiguration.isExportEnabled(asyncProperties);

        if (exportEnabled) {
            // Initialize the Formatters.
            boolean skipCSVHeader = asyncProperties.getExport() != null
                    && asyncProperties.getExport().isSkipCSVHeader();
            Map<ResultType, TableExportFormatter> supportedFormatters = new HashMap<>();
            supportedFormatters.put(ResultType.CSV, new CSVExportFormatter(elide.getElide(), skipCSVHeader));
            supportedFormatters.put(ResultType.JSON, new JSONExportFormatter(elide.getElide()));

            // Binding TableExport LifeCycleHook
            TableExportHook tableExportHook = getTableExportHook(asyncExecutorService, settings, supportedFormatters,
                    resultStorageEngine);
            dictionary.bindTrigger(TableExport.class, CREATE, PREFLUSH, tableExportHook, false);
            dictionary.bindTrigger(TableExport.class, CREATE, POSTCOMMIT, tableExportHook, false);
            dictionary.bindTrigger(TableExport.class, CREATE, PRESECURITY, tableExportHook, false);
        }

        return asyncExecutorService;
    }

    // TODO Remove this method when ElideSettings has all the settings.
    // Then the check can be done in TableExportHook.
    // Trying to avoid adding too many individual properties to ElideSettings for now.
    // https://github.com/yahoo/elide/issues/1803
    private TableExportHook getTableExportHook(AsyncExecutorService asyncExecutorService,
            ElideConfigProperties settings, Map<ResultType, TableExportFormatter> supportedFormatters,
            ResultStorageEngine resultStorageEngine) {
        boolean exportEnabled = ElideAutoConfiguration.isExportEnabled(settings.getAsync());

        TableExportHook tableExportHook = null;
        if (exportEnabled) {
            tableExportHook = new TableExportHook(asyncExecutorService, settings.getAsync().getMaxAsyncAfterSeconds(),
                    supportedFormatters, resultStorageEngine);
        } else {
            tableExportHook = new TableExportHook(asyncExecutorService, settings.getAsync().getMaxAsyncAfterSeconds(),
                    supportedFormatters, resultStorageEngine) {
                @Override
                public void validateOptions(AsyncAPI export, RequestScope requestScope) {
                    throw new InvalidOperationException("TableExport is not supported.");
                }
            };
        }
        return tableExportHook;
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
    @ConditionalOnProperty(prefix = "elide.async", name = "cleanupEnabled", matchIfMissing = false)
    public AsyncCleanerService buildAsyncCleanerService(RefreshableElide elide,
                                                        ElideConfigProperties settings,
                                                        AsyncAPIDAO asyncQueryDao) {
        AsyncCleanerService.init(elide.getElide(), settings.getAsync().getMaxRunTimeSeconds(),
                settings.getAsync().getQueryCleanupDays(),
                settings.getAsync().getQueryCancellationIntervalSeconds(), asyncQueryDao);
        return AsyncCleanerService.getInstance();
    }

    /**
     * Configure the AsyncQueryDAO used by async query requests.
     * @param elide elideObject.
     * @return an AsyncQueryDAO object.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "elide.async", name = "defaultAsyncAPIDAO", matchIfMissing = true)
    public AsyncAPIDAO buildAsyncAPIDAO(RefreshableElide elide) {
        return new DefaultAsyncAPIDAO(elide.getElide().getElideSettings(), elide.getElide().getDataStore());
    }

    /**
     * Configure the ResultStorageEngine used by async query requests.
     * @param settings Elide settings.
     * @return an ResultStorageEngine object.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "elide.async.export", name = "enabled", matchIfMissing = false)
    public ResultStorageEngine buildResultStorageEngine(ElideConfigProperties settings) {
        FileResultStorageEngine resultStorageEngine = new FileResultStorageEngine(settings.getAsync().getExport()
                .getStorageDestination(), settings.getAsync().getExport().isExtensionEnabled());
        return resultStorageEngine;
    }
}
