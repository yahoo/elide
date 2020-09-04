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
import com.yahoo.elide.async.hooks.CompleteQueryHook;
import com.yahoo.elide.async.hooks.ExecuteQueryHook;
import com.yahoo.elide.async.hooks.UpdatePrincipalNameHook;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.service.AsyncCleanerService;
import com.yahoo.elide.async.service.AsyncExecutorService;
import com.yahoo.elide.async.service.AsyncQueryDAO;
import com.yahoo.elide.async.service.DefaultAsyncQueryDAO;
import com.yahoo.elide.async.service.DefaultResultStorageEngine;
import com.yahoo.elide.async.service.ResultStorageEngine;
import com.yahoo.elide.core.EntityDictionary;

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
            AsyncQueryDAO asyncQueryDao, EntityDictionary dictionary, ResultStorageEngine resultStorageEngine) {

        AsyncExecutorService.init(elide, settings.getAsync().getThreadPoolSize(),
                settings.getAsync().getMaxRunTimeSeconds(), asyncQueryDao, resultStorageEngine,
                settings.getAsync().getDownload().getPath());

        AsyncExecutorService asyncExecutorService = AsyncExecutorService.getInstance();

        // Binding AsyncQuery LifeCycleHook
        ExecuteQueryHook executeQueryHook = new ExecuteQueryHook(asyncExecutorService);
        CompleteQueryHook completeQueryHook = new CompleteQueryHook(asyncExecutorService);
        UpdatePrincipalNameHook updatePrincipalNameHook = new UpdatePrincipalNameHook();
        dictionary.bindTrigger(AsyncQuery.class, READ, PRESECURITY, executeQueryHook, false);
        dictionary.bindTrigger(AsyncQuery.class, CREATE, POSTCOMMIT, completeQueryHook, false);
        dictionary.bindTrigger(AsyncQuery.class, CREATE, PRESECURITY, updatePrincipalNameHook, false);

        return AsyncExecutorService.getInstance();
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
            AsyncQueryDAO asyncQueryDao, @Autowired(required = false) ResultStorageEngine resultStorageEngine) {
        AsyncCleanerService.init(elide, settings.getAsync().getMaxRunTimeSeconds(),
                settings.getAsync().getQueryCleanupDays(), settings.getAsync().getQueryCancellationIntervalSeconds(),
                asyncQueryDao, resultStorageEngine);
        return AsyncCleanerService.getInstance();
    }

    /**
     * Configure the AsyncQueryDAO used by async query requests.
     * @param elide elideObject.
     * @return an AsyncQueryDAO object.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "elide.async", name = "defaultAsyncQueryDAO", matchIfMissing = true)
    public AsyncQueryDAO buildAsyncQueryDAO(Elide elide) {
        return new DefaultAsyncQueryDAO(elide.getElideSettings(), elide.getDataStore());
    }

    /**
     * Configure the ResultStorageEngine used by async query requests.
     * @param elide elideObject.
     * @return an ResultStorageEngine object.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "elide.async.download", name = "enabled", matchIfMissing = false)
    public ResultStorageEngine buildResultStorageEngine(Elide elide, ElideConfigProperties settings,
            AsyncQueryDAO asyncQueryDAO) {
        DefaultResultStorageEngine resultStorageEngine = new DefaultResultStorageEngine(
                elide.getElideSettings(), asyncQueryDAO);
        return resultStorageEngine;
    }

    @Bean(name = "asyncDownloadTaskExecutor")
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "elide.async.download", name = "enabled", matchIfMissing = false)
    public AsyncTaskExecutor asyncDownloadTaskExecutor(AsyncExecutorService asyncExecutorService) {
        return (AsyncTaskExecutor) asyncExecutorService.getExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new SimpleAsyncUncaughtExceptionHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "elide.async.download", name = "enabled", matchIfMissing = false)
    public WebMvcConfigurer webMvcConfigurerConfigurer(AsyncTaskExecutor asyncTaskExecutor,
            CallableProcessingInterceptor callableProcessingInterceptor, ElideConfigProperties settings) {
        return new WebMvcConfigurer() {
            @Override
            public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
                long downloadRunTimeMilliSeconds = settings.getAsync().getDownload()
                        .getMaxDownloadRunTimeSeconds() * 1000;
                configurer
                        .setDefaultTimeout(downloadRunTimeMilliSeconds)
                        .setTaskExecutor(asyncTaskExecutor)
                        .registerCallableInterceptors(callableProcessingInterceptor);
                WebMvcConfigurer.super.configureAsyncSupport(configurer);
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "elide.async.download", name = "enabled", matchIfMissing = false)
    public CallableProcessingInterceptor callableProcessingInterceptor() {
        return new TimeoutCallableProcessingInterceptor() {
            @Override
            public <T> Object handleTimeout(NativeWebRequest webRequest, Callable<T> task) throws Exception {
                return super.handleTimeout(webRequest, task);
            }
        };
    }
}
