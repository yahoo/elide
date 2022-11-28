/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.jms.websocket;

import static com.yahoo.elide.graphql.subscriptions.websocket.SubscriptionWebSocket.DEFAULT_USER_FACTORY;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.core.audit.AuditLogger;
import com.yahoo.elide.core.audit.Slf4jLogger;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.exceptions.ErrorMapper;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.datastores.jms.JMSDataStore;
import com.yahoo.elide.graphql.ExecutionResultDeserializer;
import com.yahoo.elide.graphql.ExecutionResultSerializer;
import com.yahoo.elide.graphql.GraphQLErrorDeserializer;
import com.yahoo.elide.graphql.GraphQLErrorSerializer;
import com.yahoo.elide.graphql.subscriptions.websocket.SubscriptionWebSocket;
import com.fasterxml.jackson.databind.module.SimpleModule;

import graphql.ExecutionResult;
import graphql.GraphQLError;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.Calendar;

import javax.jms.ConnectionFactory;
import javax.websocket.server.ServerEndpointConfig;

/**
 * Initializes and configures the subscription web socket.
 */
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class SubscriptionWebSocketConfigurator extends ServerEndpointConfig.Configurator {

    protected ConnectionFactory connectionFactory;

    @Builder.Default
    protected AuditLogger auditLogger = new Slf4jLogger();

    @Builder.Default
    protected ErrorMapper errorMapper = error -> null;

    @Builder.Default
    protected String baseUrl = "/";

    @Builder.Default
    protected boolean verboseErrors = false;

    @Builder.Default
    protected int connectionTimeoutMs = 5000;

    @Builder.Default
    protected int maxSubscriptions = 30;

    @Builder.Default
    private long maxIdleTimeoutMs = 300000;

    @Builder.Default
    private int maxMessageSize = 10000;

    @Builder.Default
    protected SubscriptionWebSocket.UserFactory userFactory = DEFAULT_USER_FACTORY;

    @Builder.Default
    protected boolean sendPingOnSubscribe = false;

    @Override
    public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
        if (endpointClass.equals(SubscriptionWebSocket.class)) {

            EntityDictionary dictionary = EntityDictionary.builder().build();

            DataStore store = buildDataStore(dictionary);

            Elide elide = buildElide(store, dictionary);

            return (T) buildWebSocket(elide);
        }

        return super.getEndpointInstance(endpointClass);
    }

    protected Elide buildElide(DataStore store, EntityDictionary dictionary) {
        RSQLFilterDialect rsqlFilterStrategy = RSQLFilterDialect.builder().dictionary(dictionary).build();

        ElideSettingsBuilder builder = new ElideSettingsBuilder(store)
                .withAuditLogger(auditLogger)
                .withErrorMapper(errorMapper)
                .withBaseUrl(baseUrl)
                .withJoinFilterDialect(rsqlFilterStrategy)
                .withSubqueryFilterDialect(rsqlFilterStrategy)
                .withEntityDictionary(dictionary)
                .withISO8601Dates("yyyy-MM-dd'T'HH:mm'Z'", Calendar.getInstance().getTimeZone());

        if (verboseErrors) {
            builder = builder.withVerboseErrors();
        }

        Elide elide = new Elide(builder.build());

        elide.doScans();

        return elide;
    }

    protected DataStore buildDataStore(EntityDictionary dictionary) {
        return new JMSDataStore(
                dictionary.getScanner(),
                connectionFactory, dictionary, -1);
    }

    protected SubscriptionWebSocket buildWebSocket(Elide elide) {
        elide.getMapper().getObjectMapper().registerModule(new SimpleModule("ExecutionResult")
            .addDeserializer(GraphQLError.class, new GraphQLErrorDeserializer())
            .addDeserializer(ExecutionResult.class, new ExecutionResultDeserializer())
            .addSerializer(GraphQLError.class, new GraphQLErrorSerializer())
            .addSerializer(ExecutionResult.class, new ExecutionResultSerializer(new GraphQLErrorSerializer())));

        return SubscriptionWebSocket.builder()
                .elide(elide)
                .connectTimeoutMs(connectionTimeoutMs)
                .maxSubscriptions(maxSubscriptions)
                .maxMessageSize(maxMessageSize)
                .maxIdleTimeoutMs(maxIdleTimeoutMs)
                .userFactory(userFactory)
                .sendPingOnSubscribe(sendPingOnSubscribe)
                .verboseErrors(verboseErrors)
                .build();
    }
}
