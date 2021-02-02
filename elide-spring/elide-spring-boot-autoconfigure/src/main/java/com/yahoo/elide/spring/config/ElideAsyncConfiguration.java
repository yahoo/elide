/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.spring.config;

import static com.yahoo.elide.annotation.LifeCycleHookBinding.Operation.CREATE;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.Operation.READ;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.TransactionPhase.POSTCOMMIT;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.TransactionPhase.PRESECURITY;

import com.yahoo.elide.Elide;
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

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.async.CallableProcessingInterceptor;
import org.springframework.web.context.request.async.TimeoutCallableProcessingInterceptor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Async Configuration For Elide Services.  Override any of the beans (by defining your own)
 * and setting flags to disable in properties to change the default behavior.
 */
@Configuration
@EntityScan(basePackageClasses = AsyncQuery.class)
@EnableConfigurationProperties(ElideConfigProperties.class)
@ConditionalOnExpression("${elide.async.enabled:false}")
@EnableAsync
@EnableScheduling
public class ElideAsyncConfiguration implements AsyncConfigurer {

    /**
     * Configure the AsyncExecutorService used for submitting async query requests.
     * @param elide elideObject.
     * @param settings Elide settings.
     * @param asyncQueryDao AsyncDao object.
     * @param dictionary EntityDictionary.
     * @return a AsyncExecutorService.
     */
    @Bean
    @ConditionalOnMissingBean
    public AsyncExecutorService buildAsyncExecutorService(Elide elide, ElideConfigProperties settings,
            AsyncAPIDAO asyncQueryDao, EntityDictionary dictionary,
            @Autowired(required = false) ResultStorageEngine resultStorageEngine) {
    AsyncProperties asyncProperties = settings.getAsync();
        AsyncExecutorService.init(elide, asyncProperties.getThreadPoolSize(),
                asyncQueryDao, resultStorageEngine);
        AsyncExecutorService asyncExecutorService = AsyncExecutorService.getInstance();

        // Binding AsyncQuery LifeCycleHook
        AsyncQueryHook asyncQueryHook = new AsyncQueryHook(asyncExecutorService,
                asyncProperties.getMaxAsyncAfterSeconds());
        dictionary.bindTrigger(AsyncQuery.class, READ, PRESECURITY, asyncQueryHook, false);
        dictionary.bindTrigger(AsyncQuery.class, CREATE, POSTCOMMIT, asyncQueryHook, false);
        dictionary.bindTrigger(AsyncQuery.class, CREATE, PRESECURITY, asyncQueryHook, false);

        // Initialize the Formatters.
        boolean skipCSVHeader = asyncProperties.getExport() != null ? asyncProperties.getExport().isSkipCSVHeader()
            : false;
        Map<ResultType, TableExportFormatter> supportedFormatters = new HashMap<ResultType, TableExportFormatter>();
        supportedFormatters.put(ResultType.CSV, new CSVExportFormatter(elide, skipCSVHeader));
        supportedFormatters.put(ResultType.JSON, new JSONExportFormatter(elide));

        // Binding TableExport LifeCycleHook
        TableExportHook tableExportHook = getTableExportHook(asyncExecutorService, settings, supportedFormatters);
        dictionary.bindTrigger(TableExport.class, READ, PRESECURITY, tableExportHook, false);
        dictionary.bindTrigger(TableExport.class, CREATE, POSTCOMMIT, tableExportHook, false);
        dictionary.bindTrigger(TableExport.class, CREATE, PRESECURITY, tableExportHook, false);

        return AsyncExecutorService.getInstance();
    }

    // TODO Remove this method when ElideSettings has all the settings.
    // Then the check can be done in TableExportHook.
    // Trying to avoid adding too many individual properties to ElideSettings for now.
    // https://github.com/yahoo/elide/issues/1803
    private TableExportHook getTableExportHook(AsyncExecutorService asyncExecutorService,
            ElideConfigProperties settings, Map<ResultType, TableExportFormatter> supportedFormatters) {
        boolean exportEnabled = settings.getAsync().getExport() != null ? settings.getAsync().getExport().isEnabled()
                : false;
        TableExportHook tableExportHook = null;
        if (exportEnabled) {
            tableExportHook = new TableExportHook(asyncExecutorService, settings.getAsync().getMaxAsyncAfterSeconds(),
                    supportedFormatters);
        } else {
            tableExportHook = new TableExportHook(asyncExecutorService, settings.getAsync().getMaxAsyncAfterSeconds(),
                    supportedFormatters) {
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
    public AsyncCleanerService buildAsyncCleanerService(Elide elide, ElideConfigProperties settings,
            AsyncAPIDAO asyncQueryDao) {
        AsyncCleanerService.init(elide, settings.getAsync().getMaxRunTimeSeconds(),
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
    public AsyncAPIDAO buildAsyncAPIDAO(Elide elide) {
        return new DefaultAsyncAPIDAO(elide.getElideSettings(), elide.getDataStore());
    }

    /**
     * Configure the ResultStorageEngine used by async query requests.
     * @param elide elideObject.
     * @return an ResultStorageEngine object.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "elide.async.export", name = "enabled", matchIfMissing = false)
    public ResultStorageEngine buildResultStorageEngine(Elide elide, ElideConfigProperties settings,
            AsyncAPIDAO asyncQueryDAO) {
        FileResultStorageEngine resultStorageEngine = new FileResultStorageEngine(settings.getAsync().getExport()
                .getStorageDestination());
        return resultStorageEngine;
    }

    /**
     * Configure a task executor to use by the export controller.
     * We re-use the Executor we initialize as part of AsyncExecutorService for running export controller requests.
     * @param asyncExecutorService AsyncExecutorService instance.
     * @return AsyncTaskExecutor object
     */
    @Bean(name = "asyncExportTaskExecutor")
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "elide.async.export", name = "enabled", matchIfMissing = false)
    public AsyncTaskExecutor asyncDownloadTaskExecutor(AsyncExecutorService asyncExecutorService) {
        return (AsyncTaskExecutor) asyncExecutorService.getExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new SimpleAsyncUncaughtExceptionHandler();
    }

    /**
     * Configure WebMVC with settings.
     * @param asyncTaskExecutor AsyncTaskExecutor to execute requests asynchronously.
     * @param callableProcessingInterceptor CallableProcessingInterceptor instance.
     * @param settings Elide Spring Settings.
     * @return WebMvcConfigurer object.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "elide.async.export", name = "enabled", matchIfMissing = false)
    public WebMvcConfigurer webMvcConfigurerConfigurer(AsyncTaskExecutor asyncTaskExecutor,
            CallableProcessingInterceptor callableProcessingInterceptor, ElideConfigProperties settings) {
        return new WebMvcConfigurer() {
            @Override
            public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
                long downloadRunTimeMilliSeconds = settings.getAsync().getExport()
                        .getMaxDownloadRunTimeSeconds() * 1000;
                configurer
                        .setDefaultTimeout(downloadRunTimeMilliSeconds)
                        .setTaskExecutor(asyncTaskExecutor)
                        .registerCallableInterceptors(callableProcessingInterceptor);
                WebMvcConfigurer.super.configureAsyncSupport(configurer);
            }
        };
    }

    /**
     * Handles Concurrent Calls for the Export controller in the background.
     * @return CallableProcessingInterceptor
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "elide.async.export", name = "enabled", matchIfMissing = false)
    public CallableProcessingInterceptor callableProcessingInterceptor() {
        return new TimeoutCallableProcessingInterceptor() {
            @Override
            public <T> Object handleTimeout(NativeWebRequest webRequest, Callable<T> task) throws Exception {
                return super.handleTimeout(webRequest, task);
            }
        };
    }
}
