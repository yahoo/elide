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

import graphql.execution.DataFetcherExceptionHandler;

import jakarta.jms.ConnectionFactory;
import jakarta.websocket.server.ServerEndpointConfig;

/**
 * Configures GraphQL subscription web sockets for Elide.
 */
@Configuration
@ConditionalOnProperty(name = "elide.graphql.enabled", havingValue = "true")
@EnableConfigurationProperties(ElideConfigProperties.class)
public class ElideSubscriptionConfiguration {
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnExpression("${elide.graphql.subscription.enabled:false}")
    ServerEndpointConfig serverEndpointConfig(
            ElideConfigProperties config,
            SubscriptionWebSocket.UserFactory userFactory,
            ConnectionFactory connectionFactory,
            ErrorMapper errorMapper,
            DataFetcherExceptionHandler dataFetcherExceptionHandler
    ) {
        return ServerEndpointConfig.Builder
                .create(SubscriptionWebSocket.class, config.getGraphql().getSubscription().getPath())
                .subprotocols(SubscriptionWebSocket.SUPPORTED_WEBSOCKET_SUBPROTOCOLS)
                .configurator(SubscriptionWebSocketConfigurator.builder()
                        .baseUrl(config.getGraphql().getSubscription().getPath())
                        .sendPingOnSubscribe(config.getGraphql().getSubscription().isSendPingOnSubscribe())
                        .connectionTimeout(config.getGraphql().getSubscription().getConnectionTimeout())
                        .maxSubscriptions(config.getGraphql().getSubscription().maxSubscriptions)
                        .maxMessageSize(config.getGraphql().getSubscription().maxMessageSize)
                        .maxIdleTimeout(config.getGraphql().getSubscription().getIdleTimeout())
                        .connectionFactory(connectionFactory)
                        .userFactory(userFactory)
                        .auditLogger(new Slf4jLogger())
                        .verboseErrors(config.isVerboseErrors())
                        .errorMapper(errorMapper)
                        .dataFetcherExceptionHandler(dataFetcherExceptionHandler)
                        .build())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnExpression("${elide.graphql.subscription.enabled:false}")
    ServerEndpointExporter serverEndpointExporter() {
        ServerEndpointExporter exporter = new ServerEndpointExporter();
        return exporter;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnExpression("${elide.graphql.subscription.enabled:false}")
    SubscriptionWebSocket.UserFactory userFactory() {
        return DEFAULT_USER_FACTORY;
    }
}
