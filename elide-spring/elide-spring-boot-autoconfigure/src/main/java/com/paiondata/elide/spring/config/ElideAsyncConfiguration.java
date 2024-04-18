/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.spring.config;

import static com.paiondata.elide.annotation.LifeCycleHookBinding.Operation.CREATE;
import static com.paiondata.elide.annotation.LifeCycleHookBinding.TransactionPhase.POSTCOMMIT;
import static com.paiondata.elide.annotation.LifeCycleHookBinding.TransactionPhase.PREFLUSH;
import static com.paiondata.elide.annotation.LifeCycleHookBinding.TransactionPhase.PRESECURITY;
import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

import com.paiondata.elide.RefreshableElide;
import com.paiondata.elide.async.DefaultResultTypeFileExtensionMapper;
import com.paiondata.elide.async.ResultTypeFileExtensionMapper;
import com.paiondata.elide.async.export.formatter.CsvExportFormatter;
import com.paiondata.elide.async.export.formatter.JsonExportFormatter;
import com.paiondata.elide.async.export.formatter.TableExportFormatter;
import com.paiondata.elide.async.export.formatter.TableExportFormatters;
import com.paiondata.elide.async.export.formatter.TableExportFormatters.TableExportFormattersBuilder;
import com.paiondata.elide.async.export.formatter.TableExportFormattersBuilderCustomizer;
import com.paiondata.elide.async.export.formatter.XlsxExportFormatter;
import com.paiondata.elide.async.hooks.AsyncQueryHook;
import com.paiondata.elide.async.hooks.TableExportHook;
import com.paiondata.elide.async.models.AsyncQuery;
import com.paiondata.elide.async.models.ResultType;
import com.paiondata.elide.async.models.TableExport;
import com.paiondata.elide.async.service.AsyncCleanerService;
import com.paiondata.elide.async.service.AsyncExecutorService;
import com.paiondata.elide.async.service.dao.AsyncApiDao;
import com.paiondata.elide.async.service.dao.DefaultAsyncApiDao;
import com.paiondata.elide.async.service.storageengine.FileResultStorageEngine;
import com.paiondata.elide.async.service.storageengine.ResultStorageEngine;
import com.paiondata.elide.core.dictionary.EntityDictionary;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import graphql.execution.DataFetcherExceptionHandler;

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
     * Creates the {@link TableExportFormattersBuilder}.
     * <p>
     * Defining a {@link TableExportFormattersBuilderCustomizer} will allow customization of the default builder.
     *
     * @param elide elideObject
     * @param settings Elide settings.
     * @param customizerProvider the customizers
     * @return the TableExportFormattersBuilder
     */
    @Bean
    @ConditionalOnMissingBean
    @Scope(SCOPE_PROTOTYPE)
    public TableExportFormattersBuilder tableExportFormattersBuilder(
            RefreshableElide elide,
            ElideConfigProperties settings,
            ObjectProvider<TableExportFormattersBuilderCustomizer> customizerProvider) {
        AsyncProperties asyncProperties = settings.getAsync();

        TableExportFormattersBuilder builder = TableExportFormatters.builder();
        // Initialize the Formatters.
        boolean writeCSVHeader = asyncProperties.getExport() != null
                && asyncProperties.getExport().getFormat().getCsv().isWriteHeader();
        builder.entry(ResultType.CSV, new CsvExportFormatter(elide.getElide(), writeCSVHeader));
        builder.entry(ResultType.JSON, new JsonExportFormatter(elide.getElide()));
        builder.entry(ResultType.XLSX, new XlsxExportFormatter(elide.getElide(), true));
        customizerProvider.orderedStream().forEach(customizer -> customizer.customize(builder));
        return builder;
    }

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
            Optional<DataFetcherExceptionHandler> optionalDataFetcherExceptionHandler,
            TableExportFormattersBuilder tableExportFormattersBuilder,
            ResultTypeFileExtensionMapper resultTypeFileExtensionMapper
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
            // Binding TableExport LifeCycleHook
            TableExportHook tableExportHook = getTableExportHook(asyncExecutorService, settings,
                    tableExportFormattersBuilder.build(), optionalResultStorageEngine.orElse(null),
                    asyncProperties.getExport().isAppendFileExtension() ? resultTypeFileExtensionMapper : null);
            dictionary.bindTrigger(TableExport.class, CREATE, PREFLUSH, tableExportHook, false);
            dictionary.bindTrigger(TableExport.class, CREATE, POSTCOMMIT, tableExportHook, false);
            dictionary.bindTrigger(TableExport.class, CREATE, PRESECURITY, tableExportHook, false);
        }

        return asyncExecutorService;
    }

    private TableExportHook getTableExportHook(AsyncExecutorService asyncExecutorService,
            ElideConfigProperties settings, Map<String, TableExportFormatter> supportedFormatters,
            ResultStorageEngine resultStorageEngine, ResultTypeFileExtensionMapper resultTypeFileExtensionMapper) {
        return new TableExportHook(asyncExecutorService, settings.getAsync().getMaxAsyncAfter(), supportedFormatters,
                resultStorageEngine, resultTypeFileExtensionMapper);
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
        FileResultStorageEngine resultStorageEngine = new FileResultStorageEngine(
                settings.getAsync().getExport().getStorageDestination());
        return resultStorageEngine;
    }

    /**
     * Configure the mapping of result type file extension.
     * @return the ResultTypeFileExtensionMapper
     */
    @Bean
    @ConditionalOnMissingBean
    public ResultTypeFileExtensionMapper resultTypeFileExtensionMapper() {
        return new DefaultResultTypeFileExtensionMapper();
    }
}
