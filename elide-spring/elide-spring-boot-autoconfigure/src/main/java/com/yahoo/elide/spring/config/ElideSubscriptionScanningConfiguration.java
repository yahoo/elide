/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.spring.config;

import com.yahoo.elide.graphql.subscriptions.hooks.SubscriptionScanner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import javax.jms.ConnectionFactory;
import javax.jms.Message;

/**
 * Scans for GraphQL subscriptions and registers lifecycle hooks.
 */
@Configuration
public class ElideSubscriptionScanningConfiguration {
    private ElideAutoConfiguration.GlobalElide elide;
    private ConnectionFactory connectionFactory;

    @Autowired
    public ElideSubscriptionScanningConfiguration(
            ElideAutoConfiguration.GlobalElide elide,
            ConnectionFactory connectionFactory
    ) {
        this.elide = elide;
        this.connectionFactory = connectionFactory;
    }

    @EventListener(value = { ContextRefreshedEvent.class, RefreshScopeRefreshedEvent.class })
    public void onStartOrRefresh(ApplicationEvent event) {
        SubscriptionScanner scanner = SubscriptionScanner.builder()

                //Things you may want to override...
                .deliveryDelay(Message.DEFAULT_DELIVERY_DELAY)
                .messagePriority(Message.DEFAULT_PRIORITY)
                .timeToLive(Message.DEFAULT_TIME_TO_LIVE)
                .deliveryMode(Message.DEFAULT_DELIVERY_MODE)

                //Things you probably don't care about...
                .scanner(elide.getElide().getScanner())
                .dictionary(elide.getElide().getElideSettings().getDictionary())
                .connectionFactory(connectionFactory)
                .build();

        scanner.bindLifecycleHooks();
    }
}
