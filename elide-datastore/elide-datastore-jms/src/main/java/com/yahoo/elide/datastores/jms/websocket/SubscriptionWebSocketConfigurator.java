/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.jms.websocket;

import static com.yahoo.elide.graphql.subscriptions.websocket.SubscriptionWebSocket.DEFAULT_USER_FACTORY;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.ElideSettingsBuilderCustomizer;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.dictionary.Injector;
import com.yahoo.elide.core.request.route.RouteResolver;
import com.yahoo.elide.datastores.jms.JMSDataStore;
import com.yahoo.elide.graphql.GraphQLSettings;
import com.yahoo.elide.graphql.GraphQLSettings.GraphQLSettingsBuilder;
import com.yahoo.elide.graphql.serialization.GraphQLModule;
import com.yahoo.elide.graphql.subscriptions.websocket.SubscriptionWebSocket;

import graphql.execution.DataFetcherExceptionHandler;
import graphql.execution.SimpleDataFetcherExceptionHandler;
import jakarta.jms.ConnectionFactory;
import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.net.URI;
import java.security.Principal;
import java.time.Duration;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

/**
 * Initializes and configures the subscription web socket.
 */
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class SubscriptionWebSocketConfigurator extends ServerEndpointConfig.Configurator {

    protected ConnectionFactory connectionFactory;

    @Builder.Default
    protected ElideSettingsBuilderCustomizer elideSettingsBuilderCustomizer = null;

    @Builder.Default
    protected String baseUrl = "/";

    @Builder.Default
    protected Duration connectionTimeout = Duration.ofMillis(5000L);

    @Builder.Default
    protected int maxSubscriptions = 30;

    @Builder.Default
    private Duration maxIdleTimeout = Duration.ofMillis(300000L);

    @Builder.Default
    private int maxMessageSize = 10000;

    @Builder.Default
    protected SubscriptionWebSocket.UserFactory userFactory = DEFAULT_USER_FACTORY;

    @Builder.Default
    protected boolean sendPingOnSubscribe = false;

    @Builder.Default
    protected DataFetcherExceptionHandler dataFetcherExceptionHandler = new SimpleDataFetcherExceptionHandler();

    @Builder.Default
    protected RouteResolver routeResolver = null;

    @Builder.Default
    protected Injector injector = null;

    @Override
    public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
        if (endpointClass.equals(SubscriptionWebSocket.class)) {

            EntityDictionary dictionary = EntityDictionary.builder().injector(injector).build();

            DataStore store = buildDataStore(dictionary);

            Elide elide = buildElide(store, dictionary);

            return endpointClass.cast(buildWebSocket(elide));
        }

        return super.getEndpointInstance(endpointClass);
    }

    @Override
    public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
        Map<String, Object> userProperties = sec.getUserProperties();
        URI requestURI = request.getRequestURI();
        if (requestURI != null)
        {
            userProperties.put("requestURI", requestURI);
        }

        Map<String, List<String>> headers = request.getHeaders();
        if (headers != null)
        {
            userProperties.put("headers", headers);
        }

        Map<String, List<String>> parameterMap = request.getParameterMap();
        if (parameterMap != null)
        {
            userProperties.put("parameterMap", parameterMap);
        }

        String queryString = request.getQueryString();
        if (queryString != null)
        {
            userProperties.put("queryString", queryString);
        }

        Object httpSession = request.getHttpSession();
        if (httpSession != null)
        {
            userProperties.put("session", httpSession);
        }

        Principal userPrincipal = request.getUserPrincipal();
        if (userPrincipal != null)
        {
            userProperties.put("userPrincipal", userPrincipal);
        }
    }

    protected Elide buildElide(DataStore store, EntityDictionary dictionary) {
        GraphQLSettings.GraphQLSettingsBuilder graphqlSettings = GraphQLSettingsBuilder.withDefaults(dictionary);

        ElideSettings.ElideSettingsBuilder builder = ElideSettings.builder().dataStore(store)
                .baseUrl(baseUrl)
                .settings(graphqlSettings)
                .entityDictionary(dictionary)
                .serdes(serdes -> serdes.withISO8601Dates("yyyy-MM-dd'T'HH:mm'Z'",
                        Calendar.getInstance().getTimeZone()));

        if (elideSettingsBuilderCustomizer != null) {
            elideSettingsBuilderCustomizer.customize(builder);
        }

        Elide elide = new Elide(builder.build());

        elide.doScans();

        return elide;
    }

    protected DataStore buildDataStore(EntityDictionary dictionary) {
        return new JMSDataStore(
                dictionary.getScanner(),
                connectionFactory, dictionary, null);
    }

    protected SubscriptionWebSocket buildWebSocket(Elide elide) {
        elide.getObjectMapper().registerModule(new GraphQLModule());

        return SubscriptionWebSocket.builder()
                .elide(elide)
                .connectionTimeout(connectionTimeout)
                .maxSubscriptions(maxSubscriptions)
                .maxMessageSize(maxMessageSize)
                .maxIdleTimeout(maxIdleTimeout)
                .userFactory(userFactory)
                .sendPingOnSubscribe(sendPingOnSubscribe)
                .dataFetcherExceptionHandler(dataFetcherExceptionHandler)
                .routeResolver(routeResolver)
                .build();
    }

    /**
     * A mutable builder for building {@link SubscriptionWebSocketConfigurator}.
     */
    public static class SubscriptionWebSocketConfiguratorBuilder {
    }
}
