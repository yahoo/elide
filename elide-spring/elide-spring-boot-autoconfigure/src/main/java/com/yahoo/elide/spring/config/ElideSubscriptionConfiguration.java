/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.spring.config;

import static com.yahoo.elide.graphql.subscriptions.websocket.SubscriptionWebSocket.DEFAULT_USER_FACTORY;

import com.yahoo.elide.core.audit.Slf4jLogger;
import com.yahoo.elide.core.exceptions.ErrorMapper;
import com.yahoo.elide.datastores.jms.websocket.SubscriptionWebSocketConfigurator;
import com.yahoo.elide.graphql.subscriptions.websocket.SubscriptionWebSocket;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

import javax.jms.ConnectionFactory;
import javax.websocket.server.ServerEndpointConfig;

/**
 * Configures GraphQL subscription web sockets for Elide.
 */
@Configuration
@ConditionalOnProperty(name = "elide.graphql.enabled", havingValue = "true")
@EnableConfigurationProperties(ElideConfigProperties.class)
public class ElideSubscriptionConfiguration {
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnExpression("${elide.subscription.enabled:false}")
    ServerEndpointConfig serverEndpointConfig(
            ElideConfigProperties config,
            SubscriptionWebSocket.UserFactory userFactory,
            ConnectionFactory connectionFactory,
            ErrorMapper errorMapper
    ) {
        return ServerEndpointConfig.Builder
                .create(SubscriptionWebSocket.class, config.getSubscription().getPath())
                .configurator(SubscriptionWebSocketConfigurator.builder()
                        .baseUrl(config.getSubscription().getPath())
                        .sendPingOnSubscribe(config.getSubscription().isSendPingOnSubscribe())
                        .connectionTimeoutMs(config.getSubscription().getConnectionTimeoutMs())
                        .maxSubscriptions(config.getSubscription().maxSubscriptions)
                        .maxMessageSize(config.getSubscription().maxMessageSize)
                        .maxIdleTimeoutMs(config.getSubscription().idleTimeoutMs)
                        .connectionFactory(connectionFactory)
                        .userFactory(userFactory)
                        .auditLogger(new Slf4jLogger())
                        .verboseErrors(config.isVerboseErrors())
                        .errorMapper(errorMapper)
                        .build())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnExpression("${elide.subscription.enabled:false}")
    ServerEndpointExporter serverEndpointExporter() {
        ServerEndpointExporter exporter = new ServerEndpointExporter();
        return exporter;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnExpression("${elide.subscription.enabled:false}")
    SubscriptionWebSocket.UserFactory getUserFactory() {
        return DEFAULT_USER_FACTORY;
    }
}
