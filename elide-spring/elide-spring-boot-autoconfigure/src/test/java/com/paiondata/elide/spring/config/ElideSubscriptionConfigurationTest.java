/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.spring.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

import jakarta.jms.ConnectionFactory;
import jakarta.websocket.server.ServerEndpointConfig;

import java.util.List;

/**
 * Test for ElideSubscriptionConfiguration.
 */
class ElideSubscriptionConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ElideAutoConfiguration.class, DataSourceAutoConfiguration.class,
                    HibernateJpaAutoConfiguration.class, TransactionAutoConfiguration.class,
                    RefreshAutoConfiguration.class, ElideSubscriptionConfiguration.class));

    @Configuration
    public static class JmsConfiguration {
        @Bean
        public ConnectionFactory connectionFactory() {
            return Mockito.mock(ConnectionFactory.class);
        }

        @Bean
        public ServerEndpointExporter serverEndpointExporter() {
            return Mockito.mock(ServerEndpointExporter.class);
        }
    }

    @Test
    void configured() {
        contextRunner.withPropertyValues("elide.graphql.subscription.enabled=true", "elide.graphql.enabled=true")
                .withUserConfiguration(JmsConfiguration.class).run(context -> {
                    ObjectProvider<ServerEndpointConfig> provider = context
                            .getBeanProvider(ServerEndpointConfig.class);
                    // 2 configurations to handle path api versioning strategy
                    List<ServerEndpointConfig> config = provider.orderedStream().toList();
                    assertThat(config).hasSize(2);
                });
    }

    @Test
    void notConfigured() {
        contextRunner.withPropertyValues("elide.graphql.subscription.enabled=false", "elide.graphql.enabled=false")
                .withUserConfiguration(JmsConfiguration.class).run(context -> {
                    assertThatThrownBy(() -> context.getBean(ServerEndpointConfig.class))
                            .isInstanceOf(NoSuchBeanDefinitionException.class);
                });
    }
}
