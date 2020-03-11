/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.spring.config;

import com.yahoo.elide.Elide;
import com.yahoo.elide.async.service.AsyncCleanerService;
import com.yahoo.elide.async.service.AsyncQueryDAO;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Async Configuration For Elide Services.  Override any of the beans (by defining your own) to change
 * the default behavior.
 */
@Configuration
@EnableConfigurationProperties(ElideConfigProperties.class)
@ConditionalOnExpression("${elide.async.cleanupEnabled:false}")
public class ElideAsyncCleanupConfiguration {

    /**
     * Configure the AsyncCleanerService used for cleaning up async query requests.
     * @param elide elideObject.
     * @param settings Elide settings.
     * @return a AsyncCleanerService.
     */
    @Bean
    @ConditionalOnMissingBean
    public AsyncCleanerService buildAsyncCleanerService(Elide elide, ElideConfigProperties settings,
            AsyncQueryDAO asyncQueryDao) {
        return new AsyncCleanerService(elide, settings.getAsync().getMaxRunTimeMinutes(),
                settings.getAsync().getQueryCleanupDays(), asyncQueryDao);
    }
}
