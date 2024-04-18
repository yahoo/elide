/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.standalone.config;

import static com.paiondata.elide.graphql.subscriptions.websocket.SubscriptionWebSocket.DEFAULT_USER_FACTORY;

import com.paiondata.elide.Elide;
import com.paiondata.elide.ElideSettingsBuilderCustomizer;
import com.paiondata.elide.datastores.jms.websocket.SubscriptionWebSocketConfigurator;
import com.paiondata.elide.graphql.GraphQLSettings.GraphQLSettingsBuilder;
import com.paiondata.elide.graphql.subscriptions.hooks.SubscriptionScanner;
import com.paiondata.elide.graphql.subscriptions.websocket.SubscriptionWebSocket;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.Message;
import jakarta.websocket.server.ServerEndpointConfig;

import java.time.Duration;

/**
 * interface for configuring the GraphQL Subscriptions in the standalone application.
 */
public interface ElideStandaloneSubscriptionSettings {

    /**
     * Enable support for subscriptions.
     *
     * @return Default: False
     */
    default boolean enabled() {
        return false;
    }

    /**
     * Enable support for subscriptions.
     *
     * @return Default: False
     */
    default boolean publishingEnabled() {
        return enabled();
    }

    /**
     * Websocket root path for the subscription endpoint.
     *
     * @return Default: /subscription
     */
    default String getPath() {
        return "/subscription";
    }

    /**
     * Websocket sends a PING immediate after receiving a SUBSCRIBE.  Only useful for testing.
     *
     * @return Default false.
     * @see com.paiondata.elide.datastores.jms.websocket.SubscriptionWebSocketTestClient
     */
    default boolean shouldSendPingOnSubscribe() {
        return false;
    }

    /**
     * Time allowed in milliseconds from web socket creation to successfully receiving a CONNECTION_INIT message.
     *
     * @return Default 5000ms.
     */
    default Duration getConnectionTimeout() {
        return Duration.ofMillis(5000L);
    }

    /**
     * Maximum number of outstanding GraphQL queries per websocket.
     *
     * @return Default 30
     */
    default Integer getMaxSubscriptions() {
        return 30;
    }

    /**
     * Maximum message size that can be sent to the websocket.
     *
     * @return Default 10000
     */
    default Integer getMaxMessageSize() {
        return 10000;
    }

    /**
     * Maximum idle timeout in milliseconds with no websocket activity.
     *
     * @return default 300000ms
     */
    default Duration getIdleTimeout() {
        return Duration.ofMillis(300000L);
    }

    /**
     * Return JMS connection factory.
     *
     * @return Default null.  This must be implemented if leveraging GraphQL subscriptions.
     */
    default ConnectionFactory getConnectionFactory() {
        return null;
    }

    /**
     * Return the function which converts a web socket Session into an Elide user.
     *
     * @return default user factory.
     */
    default SubscriptionWebSocket.UserFactory getUserFactory() {
        return DEFAULT_USER_FACTORY;
    }

    /**
     * Returns the scanner that searches for subscription annotations and binds life cycle hooks for them.
     * @param elide The elide instance.
     * @param connectionFactory The JMS connection factory where subscription messages should be sent.
     * @return The scanner.
     */
    default SubscriptionScanner subscriptionScanner(Elide elide, ConnectionFactory connectionFactory) {
        SubscriptionScanner scanner = SubscriptionScanner.builder()

                //Things you may want to override...
                .deliveryDelay(Message.DEFAULT_DELIVERY_DELAY)
                .messagePriority(Message.DEFAULT_PRIORITY)
                .timeToLive(Message.DEFAULT_TIME_TO_LIVE)
                .deliveryMode(Message.DEFAULT_DELIVERY_MODE)

                //Things you probably don't care about...
                .scanner(elide.getScanner())
                .entityDictionary(elide.getElideSettings().getEntityDictionary())
                .connectionFactory(connectionFactory)
                .objectMapper(elide.getElideSettings().getObjectMapper())
                .build();

        scanner.bindLifecycleHooks();

        return scanner;
    }

    /**
     * Returns the web socket configuration for GraphQL subscriptions.
     * @param settings Elide settings.
     * @return Web socket configuration.
     */
    default ServerEndpointConfig serverEndpointConfig(
            ElideStandaloneSettings settings, boolean pathParameter
    ) {
        String path = getPath();
        if (pathParameter) {
            if (!path.endsWith("/")) {
                path = path + "/";
            }
            path += "{path}";
        }
        return ServerEndpointConfig.Builder
                .create(SubscriptionWebSocket.class, path)
                .subprotocols(SubscriptionWebSocket.SUPPORTED_WEBSOCKET_SUBPROTOCOLS)
                .configurator(SubscriptionWebSocketConfigurator.builder()
                        .baseUrl(getPath())
                        .sendPingOnSubscribe(shouldSendPingOnSubscribe())
                        .connectionTimeout(getConnectionTimeout())
                        .maxSubscriptions(getMaxSubscriptions())
                        .maxMessageSize(getMaxMessageSize())
                        .maxIdleTimeout(getIdleTimeout())
                        .connectionFactory(getConnectionFactory())
                        .userFactory(getUserFactory())
                        .elideSettingsBuilderCustomizer(getElideSettingsBuilderCustomizer(settings))
                        .dataFetcherExceptionHandler(settings.getDataFetcherExceptionHandler())
                        .build())
                .build();
    }

    /**
     * Gets the {@link ElideSettingsBuilderCustomizer} for customizing the subscription web socket.
     *
     * @param settings
     * @return
     */
    default ElideSettingsBuilderCustomizer getElideSettingsBuilderCustomizer(ElideStandaloneSettings settings) {
        return elideSettingsBuilder -> {
            elideSettingsBuilder
                    .maxPageSize(settings.getMaxPageSize())
                    .defaultPageSize(settings.getDefaultPageSize())
                    .objectMapper(settings.getObjectMapper())
                    .auditLogger(settings.getAuditLogger())
                    .verboseErrors(settings.verboseErrors());
            elideSettingsBuilder.getSettings(GraphQLSettingsBuilder.class)
                    .graphqlExceptionHandler(settings.getGraphQLExceptionHandler());
            elideSettingsBuilder.serdes(serdes -> serdes.entries(entries -> {
                entries.clear();
                settings.getSerdesBuilder().build().entrySet().stream().forEach(entry -> {
                    entries.put(entry.getKey(), entry.getValue());
                });
            }));
        };
    }
}
