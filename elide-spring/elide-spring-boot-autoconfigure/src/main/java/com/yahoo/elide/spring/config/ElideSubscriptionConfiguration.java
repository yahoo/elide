/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.spring.config;

import static com.yahoo.elide.graphql.subscriptions.websocket.SubscriptionWebSocket.DEFAULT_USER_FACTORY;
import com.yahoo.elide.Elide;
import com.yahoo.elide.core.audit.Slf4jLogger;
import com.yahoo.elide.core.exceptions.ErrorMapper;
import com.yahoo.elide.datastores.jms.websocket.SubscriptionWebSocketConfigurator;
import com.yahoo.elide.graphql.subscriptions.hooks.SubscriptionScanner;
import com.yahoo.elide.graphql.subscriptions.websocket.SubscriptionWebSocket;
import com.yahoo.elide.jsonapi.JsonApiMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
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
    ServerEndpointConfig serverEndpointConfig(
            ElideConfigProperties config,
            SubscriptionWebSocket.UserFactory userFactory,
            ConnectionFactory connectionFactory,
            ErrorMapper errorMapper,
            JsonApiMapper mapper
    ) {
        return ServerEndpointConfig.Builder
                .create(SubscriptionWebSocket.class, "/subscription")
                .configurator(SubscriptionWebSocketConfigurator.builder()
                        .baseUrl(config.getSubscription().getPath())
                        .sendPingOnSubscribe(config.getSubscription().isSendPingOnSubscribe())
                        .connectionTimeoutMs(config.getSubscription().getConnectionTimeoutMs())
                        .maxSubscriptions(config.getSubscription().maxSubscriptions)
                        .connectionFactory(connectionFactory)
                        .userFactory(userFactory)
                        .auditLogger(new Slf4jLogger())
                        .verboseErrors(config.isVerboseErrors())
                        .errorMapper(errorMapper)
                        .mapper(mapper.getObjectMapper())
                        .build())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    ServerEndpointExporter serverEndpointExporter() {
        ServerEndpointExporter exporter = new ServerEndpointExporter();
        return exporter;
    }

    @Bean
    @ConditionalOnMissingBean
    SubscriptionWebSocket.UserFactory getUserFactory() {
        return DEFAULT_USER_FACTORY;
    }
}
