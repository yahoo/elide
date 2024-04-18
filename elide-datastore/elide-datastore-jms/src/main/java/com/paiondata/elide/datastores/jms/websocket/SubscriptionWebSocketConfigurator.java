/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.datastores.jms.websocket;

import static com.paiondata.elide.graphql.subscriptions.websocket.SubscriptionWebSocket.DEFAULT_USER_FACTORY;

import com.paiondata.elide.Elide;
import com.paiondata.elide.ElideSettings;
import com.paiondata.elide.ElideSettingsBuilderCustomizer;
import com.paiondata.elide.core.datastore.DataStore;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.dictionary.Injector;
import com.paiondata.elide.core.request.route.RouteResolver;
import com.paiondata.elide.datastores.jms.JMSDataStore;
import com.paiondata.elide.graphql.GraphQLSettings;
import com.paiondata.elide.graphql.GraphQLSettings.GraphQLSettingsBuilder;
import com.paiondata.elide.graphql.serialization.GraphQLModule;
import com.paiondata.elide.graphql.subscriptions.websocket.SubscriptionWebSocket;

import com.fasterxml.jackson.databind.ObjectMapper;

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

            ObjectMapper objectMapper;
            ElideSettings.ElideSettingsBuilder builder = getElideSettingsBuilder(dictionary);
            objectMapper = builder.build().getObjectMapper();
            DataStore store = buildDataStore(dictionary, objectMapper);
            builder.dataStore(store);

            Elide elide = buildElide(builder);

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

    protected ElideSettings.ElideSettingsBuilder getElideSettingsBuilder(EntityDictionary dictionary) {
        GraphQLSettings.GraphQLSettingsBuilder graphqlSettings = GraphQLSettingsBuilder.withDefaults(dictionary);

        ElideSettings.ElideSettingsBuilder builder = ElideSettings.builder()
                .baseUrl(baseUrl)
                .settings(graphqlSettings)
                .entityDictionary(dictionary)
                .serdes(serdes -> serdes.withISO8601Dates("yyyy-MM-dd'T'HH:mm'Z'",
                        Calendar.getInstance().getTimeZone()));

        if (elideSettingsBuilderCustomizer != null) {
            elideSettingsBuilderCustomizer.customize(builder);
        }
        return builder;
    }

    protected Elide buildElide(ElideSettings.ElideSettingsBuilder builder) {
        Elide elide = new Elide(builder.build());
        elide.doScans();
        return elide;
    }

    protected DataStore buildDataStore(EntityDictionary dictionary, ObjectMapper objectMapper) {
        return new JMSDataStore(
                dictionary.getScanner(),
                connectionFactory, dictionary, objectMapper, null);
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
