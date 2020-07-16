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
import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.ElideSettingsBuilder;
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

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

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
     * @param dictionary EntityDictionary.
     * @return a AsyncExecutorService.
     */
    @Bean
    @ConditionalOnMissingBean
    public AsyncExecutorService buildAsyncExecutorService(Elide elide, ElideConfigProperties settings,
        AsyncQueryDAO asyncQueryDao, EntityDictionary dictionary, ResultStorageEngine resultStorageEngine) {
        AsyncExecutorService.init(elide, settings.getAsync().getThreadPoolSize(),
                settings.getAsync().getMaxRunTimeMinutes(), asyncQueryDao, resultStorageEngine);
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
        AsyncQueryDAO asyncQueryDao, ResultStorageEngine resultStorageEngine) {
        AsyncCleanerService.init(elide, settings.getAsync().getMaxRunTimeMinutes(),
                settings.getAsync().getQueryCleanupDays(), asyncQueryDao, resultStorageEngine);
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
        // Creating a new ElideSettings and Elide object for Async services
        // which will have ISO8601 Dates. Used for DefaultAsyncQueryDAO.
        Elide asyncElide = getAsyncElideInstance(elide);
        return new DefaultAsyncQueryDAO(asyncElide, asyncElide.getDataStore());
    }

    /**
     * Configure the ResultStorageEngine used by async query requests.
     * @param elide elideObject.
     * @return an ResultStorageEngine object.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "elide.async", name = "defaultResultStorageEngine", matchIfMissing = true)
    public ResultStorageEngine buildResultStorageEngine(Elide elide, ElideConfigProperties settings) {
        // Creating a new ElideSettings and Elide object for Async services
        // which will have ISO8601 Dates. Used for DefaultResultStorageEngine.
        Elide asyncElide = getAsyncElideInstance(elide);
        return new DefaultResultStorageEngine(asyncElide, asyncElide.getDataStore(),
                settings.getAsync().getDownloadBaseURL());
    }

    /**
     * Creating a new Elide object for Async services.
     * @param elide elideObject
     * @return Elide object
     */
    private Elide getAsyncElideInstance(Elide elide) {
        ElideSettings asyncElideSettings = new ElideSettingsBuilder(elide.getDataStore())
                .withEntityDictionary(elide.getElideSettings().getDictionary())
                .withISO8601Dates("yyyy-MM-dd'T'HH:mm'Z'", TimeZone.getTimeZone("UTC"))
                .build();
        Elide asyncElide = new Elide(asyncElideSettings);
        return asyncElide;
    }
}
