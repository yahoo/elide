/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.standalone.config;

import static com.yahoo.elide.graphql.subscriptions.websocket.SubscriptionWebSocket.DEFAULT_USER_FACTORY;
import com.yahoo.elide.Elide;
import com.yahoo.elide.datastores.jms.websocket.SubscriptionWebSocketConfigurator;
import com.yahoo.elide.graphql.subscriptions.hooks.SubscriptionScanner;
import com.yahoo.elide.graphql.subscriptions.websocket.SubscriptionWebSocket;

import javax.jms.ConnectionFactory;
import javax.websocket.server.ServerEndpointConfig;

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
     * @see com.yahoo.elide.datastores.jms.websocket.SubscriptionWebSocketTestClient
     */
    default boolean shouldSendPingOnSubscribe() {
        return false;
    }

    /**
     * Time allowed in milliseconds from web socket creation to successfully receiving a CONNECTION_INIT message.
     *
     * @return Default 5000.
     */
    default Integer getConnectionTimeoutMs() {
        return 5000;
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
                .scanner(elide.getScanner())
                .dictionary(elide.getElideSettings().getDictionary())
                .connectionFactory(connectionFactory)
                .mapper(elide.getMapper().getObjectMapper())
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
            ElideStandaloneSettings settings
    ) {
        return ServerEndpointConfig.Builder
                .create(SubscriptionWebSocket.class, getPath())
                .configurator(SubscriptionWebSocketConfigurator.builder()
                        .baseUrl(getPath())
                        .sendPingOnSubscribe(shouldSendPingOnSubscribe())
                        .connectionTimeoutMs(getConnectionTimeoutMs())
                        .maxSubscriptions(getMaxSubscriptions())
                        .connectionFactory(getConnectionFactory())
                        .userFactory(getUserFactory())
                        .auditLogger(settings.getAuditLogger())
                        .verboseErrors(settings.verboseErrors())
                        .errorMapper(settings.getErrorMapper())
                        .mapper(settings.getObjectMapper().getObjectMapper())
                        .build())
                .build();

    }
}
