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
import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

import com.yahoo.elide.RefreshableElide;
import com.yahoo.elide.async.AsyncSettings.AsyncSettingsBuilder;
import com.yahoo.elide.async.DefaultResultTypeFileExtensionMapper;
import com.yahoo.elide.async.ResultTypeFileExtensionMapper;
import com.yahoo.elide.async.export.formatter.CsvExportFormatter;
import com.yahoo.elide.async.export.formatter.JsonExportFormatter;
import com.yahoo.elide.async.export.formatter.TableExportFormatter;
import com.yahoo.elide.async.export.formatter.TableExportFormatters;
import com.yahoo.elide.async.export.formatter.TableExportFormatters.TableExportFormattersBuilder;
import com.yahoo.elide.async.export.formatter.TableExportFormattersBuilderCustomizer;
import com.yahoo.elide.async.export.formatter.XlsxExportFormatter;
import com.yahoo.elide.async.hooks.AsyncQueryHook;
import com.yahoo.elide.async.hooks.TableExportHook;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.ResultType;
import com.yahoo.elide.async.models.TableExport;
import com.yahoo.elide.async.service.AsyncCleanerService;
import com.yahoo.elide.async.service.AsyncExecutorService;
import com.yahoo.elide.async.service.AsyncProviderService;
import com.yahoo.elide.async.service.AsyncProviderService.AsyncProviderServiceBuilder;
import com.yahoo.elide.async.service.AsyncProviderServiceBuilderCustomizer;
import com.yahoo.elide.async.service.dao.AsyncApiDao;
import com.yahoo.elide.async.service.dao.DefaultAsyncApiDao;
import com.yahoo.elide.async.service.storageengine.FileResultStorageEngine;
import com.yahoo.elide.async.service.storageengine.ResultStorageEngine;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.graphql.QueryRunners;
import com.yahoo.elide.jsonapi.JsonApi;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Async Configuration For Elide Services.  Override any of the beans (by defining your own)
 * and setting flags to disable in properties to change the default behavior.
 */
@Configuration
@ConditionalOnClass(AsyncSettingsBuilder.class)
@ConditionalOnProperty(prefix = "elide.async", name = "enabled", matchIfMissing = false)
@AutoConfigurationPackage(basePackageClasses = AsyncQuery.class)
@EnableConfigurationProperties(ElideConfigProperties.class)
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
    TableExportFormattersBuilder tableExportFormattersBuilder(
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
     * @param tableExportFormattersBuilder TableExportFormattersBuilder.
     * @param asyncProviderService the AsyncProviderService.
     * @return a AsyncExecutorService.
     */
    @Bean
    @ConditionalOnMissingBean
    AsyncExecutorService asyncExecutorService(
            RefreshableElide elide,
            ElideConfigProperties settings,
            AsyncApiDao asyncQueryDao,
            Optional<ResultStorageEngine> optionalResultStorageEngine,
            TableExportFormattersBuilder tableExportFormattersBuilder,
            ResultTypeFileExtensionMapper resultTypeFileExtensionMapper,
            AsyncProviderService asyncProviderService
    ) {
        AsyncProperties asyncProperties = settings.getAsync();

        ExecutorService executor = Executors.newFixedThreadPool(asyncProperties.getThreadPoolSize());
        ExecutorService updater = Executors.newFixedThreadPool(asyncProperties.getThreadPoolSize());
        AsyncExecutorService asyncExecutorService = new AsyncExecutorService(elide.getElide(), executor,
                updater, asyncQueryDao, asyncProviderService);

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
    AsyncCleanerService asyncCleanerService(RefreshableElide elide,
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
    AsyncApiDao asyncApiDao(RefreshableElide elide) {
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
    ResultStorageEngine resultStorageEngine(ElideConfigProperties settings) {
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
    ResultTypeFileExtensionMapper resultTypeFileExtensionMapper() {
        return new DefaultResultTypeFileExtensionMapper();
    }

    /**
     * Creates the {@link AsyncProviderServiceBuilder} to provide the underlying
     * services like JsonApi and QueryRunners.
     * <p>
     * Defining a {@link AsyncProviderServiceBuilderCustomizer} will allow
     * customization of the default builder.
     *
     * @param customizerProvider the customizers
     * @return the AsyncProviderServiceBuilder
     */
    @Bean
    @ConditionalOnMissingBean
    @Scope(SCOPE_PROTOTYPE)
    AsyncProviderServiceBuilder asyncProviderServiceBuilder(
            ObjectProvider<AsyncProviderServiceBuilderCustomizer> customizerProvider) {
        AsyncProviderServiceBuilder builder = AsyncProviderService.builder();
        customizerProvider.orderedStream().forEach(customizer -> customizer.customize(builder));
        return builder;
    }

    /**
     * Configure the {@link AsyncProviderService} to provide the underlying services like
     * JsonApi and QueryRunners.
     *
     * @param builder the builder
     * @return the AsyncProviderService
     */
    @Bean
    @ConditionalOnMissingBean
    AsyncProviderService asyncProviderService(AsyncProviderServiceBuilder builder) {
        return builder.build();
    }

    @Configuration
    @ConditionalOnClass(QueryRunners.class)
    @ConditionalOnProperty(prefix = "elide.graphql", name = "enabled", matchIfMissing = false)
    public static class GraphQLConfiguration {
        /**
         * Adds the GraphQL QueryRunners to the AsyncProviderService.
         *
         * @param queryRunners the query runnners
         * @return the customizer
         */
        @Bean
        AsyncProviderServiceBuilderCustomizer graphQlAsyncProviderServiceBuilderCustomizer(QueryRunners queryRunners) {
            return builder -> {
                builder.provider(QueryRunners.class, queryRunners);
            };
        }
    }

    @Configuration
    @ConditionalOnClass(JsonApi.class)
    @ConditionalOnProperty(prefix = "elide.json-api", name = "enabled", matchIfMissing = false)
    public static class JsonApiConfiguration {
        /**
         * Adds the JsonApi to the AsyncProviderService.
         *
         * @param jsonApi the json api
         * @return the customizer
         */
        @Bean
        AsyncProviderServiceBuilderCustomizer jsonApiAsyncProviderServiceBuilderCustomizer(JsonApi jsonApi) {
            return builder -> {
                builder.provider(JsonApi.class, jsonApi);
            };
        }
    }
}
