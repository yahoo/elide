/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.spring.config;

import com.yahoo.elide.Elide;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.core.utils.ClassScanner;
import com.yahoo.elide.datastores.jms.websocket.SubscriptionWebSocketConfigurator;
import com.yahoo.elide.graphql.subscriptions.annotations.Subscription;
import com.yahoo.elide.graphql.subscriptions.hooks.SubscriptionScanner;
import com.yahoo.elide.graphql.subscriptions.websocket.SubscriptionWebSocket;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

import java.util.Set;
import java.util.stream.Collectors;
import javax.jms.ConnectionFactory;
import javax.websocket.server.ServerEndpointConfig;

/**
 * Configures GraphQL subscription web sockets for Elide.
 */
@Configuration
@EnableConfigurationProperties(ElideConfigProperties.class)
@ConditionalOnExpression("${elide.subscription.enabled:false}")
public class ElideSubscriptionConfiguration {
    public static final String SUBSCRIPTION_MODELS = "subscriptionModels";

    @Bean
    @ConditionalOnMissingBean
    SubscriptionScanner subscriptionScanner(Elide elide, ConnectionFactory connectionFactory) {
        SubscriptionScanner scanner = SubscriptionScanner.builder()
                .scanner(elide.getScanner())
                .dictionary(elide.getElideSettings().getDictionary())
                .connectionFactory(connectionFactory)
                .mapper(elide.getMapper().getObjectMapper())
                .build();

        scanner.bindLifecycleHooks();

        return scanner;
    }

    @Bean
    @ConditionalOnMissingBean
    @Qualifier(SUBSCRIPTION_MODELS)
    Set<Type<?>> subscriptionModels(ClassScanner scanner) {
        return scanner.getAnnotatedClasses(Subscription.class).stream()
                .map(ClassType::of)
                .collect(Collectors.toSet());
    }

    @Bean
    @ConditionalOnMissingBean
    ServerEndpointConfig serverEndpointConfig(
            ElideConfigProperties config,
            ConnectionFactory connectionFactory,
            @Qualifier(SUBSCRIPTION_MODELS) Set<Type<?>> subscriptionModels
    ) {
        return ServerEndpointConfig.Builder
                .create(SubscriptionWebSocket.class, "/subscription")
                .configurator(SubscriptionWebSocketConfigurator.builder()
                        .baseUrl("/subscription")
                        .sendPingOnSubscribe(config.getSubscription().isSendPingOnSubscribe())
                        .connectionFactory(connectionFactory)
                        .build())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    ServerEndpointExporter serverEndpointExporter() {
        ServerEndpointExporter exporter = new ServerEndpointExporter();
        return exporter;
    }
}
