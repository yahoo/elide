/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.spring.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import example.AppConfiguration;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.jms.ConnectionFactory;

/**
 * Test for ElideSubscriptionScanningConfiguration.
 */
class ElideSubscriptionScanningConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(UserConfigurations.of(AppConfiguration.class))
            .withConfiguration(AutoConfigurations.of(ElideAutoConfiguration.class, DataSourceAutoConfiguration.class,
                    HibernateJpaAutoConfiguration.class, TransactionAutoConfiguration.class,
                    RefreshAutoConfiguration.class, ElideSubscriptionScanningConfiguration.class));

    @Configuration
    public static class JmsConfiguration {
        @Bean
        ConnectionFactory connectionFactory() {
            return Mockito.mock(ConnectionFactory.class);
        }
    }

    @Test
    void configured() {
        contextRunner.withPropertyValues("elide.graphql.enabled=true", "elide.graphql.subscription.enabled=true")
                .withUserConfiguration(JmsConfiguration.class).run(context -> {
                    ElideSubscriptionScanningConfiguration config = context
                            .getBean(ElideSubscriptionScanningConfiguration.class);
                    assertThat(config).isNotNull();
                });
    }

    @Test
    void notConfigured() {
        contextRunner
                .withPropertyValues("elide.graphql.enabled=true", "elide.graphql.subscription.enabled=true",
                        "elide.graphql.subscription.publishing.enabled=false")
                .withUserConfiguration(JmsConfiguration.class).run(context -> {
                    assertThatThrownBy(() -> context.getBean(ElideSubscriptionScanningConfiguration.class))
                            .isInstanceOf(NoSuchBeanDefinitionException.class);
                });
    }
}
