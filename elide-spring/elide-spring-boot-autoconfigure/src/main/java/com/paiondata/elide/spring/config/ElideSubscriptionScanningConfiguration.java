/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.spring.config;

import com.paiondata.elide.Elide;
import com.paiondata.elide.RefreshableElide;
import com.paiondata.elide.graphql.subscriptions.hooks.SubscriptionScanner;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.Message;

/**
 * Scans for GraphQL subscriptions and registers lifecycle hooks.
 */
@Configuration
@ConditionalOnProperty(name = "elide.graphql.enabled", havingValue = "true")
@ConditionalOnExpression(
    "${elide.graphql.subscription.enabled:false} && ${elide.graphql.subscription.publishing.enabled:true}")
public class ElideSubscriptionScanningConfiguration {
    private RefreshableElide refreshableElide;
    private ConnectionFactory connectionFactory;

    public ElideSubscriptionScanningConfiguration(
            RefreshableElide refreshableElide,
            ConnectionFactory connectionFactory
    ) {
        this.refreshableElide = refreshableElide;
        this.connectionFactory = connectionFactory;
    }

    @EventListener(value = { ContextRefreshedEvent.class, RefreshScopeRefreshedEvent.class })
    public void onStartOrRefresh(ApplicationEvent event) {

        Elide elide = refreshableElide.getElide();

        SubscriptionScanner scanner = SubscriptionScanner.builder()
                // Things you may want to override...
                .deliveryDelay(Message.DEFAULT_DELIVERY_DELAY)
                .messagePriority(Message.DEFAULT_PRIORITY)
                .timeToLive(Message.DEFAULT_TIME_TO_LIVE)
                .deliveryMode(Message.DEFAULT_DELIVERY_MODE)

                // Things you probably don't care about...
                .scanner(elide.getScanner())
                .entityDictionary(elide.getElideSettings().getEntityDictionary())
                .connectionFactory(connectionFactory)
                .objectMapper(elide.getElideSettings().getObjectMapper())
                .build();

        scanner.bindLifecycleHooks();
    }
}
