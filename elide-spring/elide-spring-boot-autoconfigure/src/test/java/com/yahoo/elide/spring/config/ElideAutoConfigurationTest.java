/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.spring.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yahoo.elide.RefreshableElide;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.exceptions.TransactionException;
import com.yahoo.elide.datastores.multiplex.MultiplexManager;
import com.yahoo.elide.spring.orm.jpa.config.EnableJpaDataStore;

import example.models.jpa.ArtifactGroup;
import example.models.jpa.v2.ArtifactGroupV2;
import example.models.jpa.v3.ArtifactGroupV3;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.EntityManagerFactoryBuilderCustomizer;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitManager;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.RestController;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

/**
 * Tests for ElideAutoConfiguration.
 */
class ElideAutoConfigurationTest {
    private static final String TARGET_NAME_PREFIX = "scopedTarget.";
    private static final String SCOPE_REFRESH = "refresh";
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ElideAutoConfiguration.class, DataSourceAutoConfiguration.class,
                    HibernateJpaAutoConfiguration.class, TransactionAutoConfiguration.class, RefreshAutoConfiguration.class));

    @Test
    void nonRefreshable() {
        Set<String> nonRefreshableBeans = new HashSet<>();

        contextRunner.withPropertyValues("spring.cloud.refresh.enabled=false", "elide.json-api.enabled=true",
                "elide.api-docs.enabled=true", "elide.graphql.enabled=true").run(context -> {
                    Arrays.stream(context.getBeanDefinitionNames()).forEach(beanDefinitionName -> {
                        if (context.getBeanFactory() instanceof BeanDefinitionRegistry beanDefinitionRegistry) {
                            BeanDefinition beanDefinition = beanDefinitionRegistry
                                    .getBeanDefinition(beanDefinitionName);
                            assertThat(beanDefinition.getScope()).isNotEqualTo(SCOPE_REFRESH);
                            nonRefreshableBeans.add(beanDefinitionName);
                        }
                    });
                });
        assertThat(nonRefreshableBeans).contains("refreshableElide", "graphqlController", "queryRunners",
                "apiDocsController", "apiDocsRegistrations", "jsonApiController");
    }

    enum RefreshableInput {
        GRAPHQL(new String[] { "elide.graphql.enabled=true" },
                new String[] { "refreshableElide", "graphqlController", "queryRunners" }),
        OPENAPI(new String[] { "elide.api-docs.enabled=true" },
                new String[] { "refreshableElide", "apiDocsController", "apiDocsRegistrations" }),
        JSONAPI(new String[] { "elide.json-api.enabled=true" },
                new String[] { "refreshableElide", "jsonApiController" });

        String[] propertyValues;
        String[] beanNames;

        RefreshableInput(String[] propertyValues, String[] beanNames) {
            this.propertyValues = propertyValues;
            this.beanNames = beanNames;
        }
    }

    @ParameterizedTest
    @EnumSource(RefreshableInput.class)
    void refreshable(RefreshableInput refreshableInput) {
        contextRunner.withPropertyValues(refreshableInput.propertyValues).run(context -> {

            Set<String> refreshableBeans = new HashSet<>();

            Arrays.stream(context.getBeanDefinitionNames()).forEach(beanDefinitionName -> {
                if (context.getBeanFactory() instanceof BeanDefinitionRegistry beanDefinitionRegistry) {
                    BeanDefinition beanDefinition = beanDefinitionRegistry.getBeanDefinition(beanDefinitionName);
                    if (SCOPE_REFRESH.equals(beanDefinition.getScope())) {
                        refreshableBeans.add(beanDefinitionName.replace(TARGET_NAME_PREFIX, ""));
                        assertThat(context.getBean(beanDefinitionName)).isNotNull();
                    }
                }
            });

            assertThat(refreshableBeans).contains(refreshableInput.beanNames);
        });
    }

    @RestController
    public static class UserGraphqlController {
    }

    @Configuration(proxyBeanMethods = false)
    public static class UserGraphqlControllerConfiguration {
        @Bean
        public UserGraphqlController graphqlController() {
            return new UserGraphqlController();
        }
    }

    @RestController
    public static class UserApiDocsController {
    }

    @Configuration(proxyBeanMethods = false)
    public static class UserApiDocsControllerConfiguration {
        @Bean
        public UserApiDocsController apiDocsController() {
            return new UserApiDocsController();
        }
    }

    @RestController
    public static class UserJsonApiController {
    }

    @Configuration(proxyBeanMethods = false)
    public static class UserJsonApiControllerConfiguration {
        @Bean
        public UserJsonApiController jsonApiController() {
            return new UserJsonApiController();
        }
    }

    enum OverrideControllerInput {
        GRAPHQL(new String[] { "elide.graphql.enabled=true" }, UserGraphqlControllerConfiguration.class,
                "graphqlController"),
        OPENAPI(new String[] { "elide.api-docs.enabled=true" }, UserApiDocsControllerConfiguration.class,
                "apiDocsController"),
        JSONAPI(new String[] { "elide.json-api.enabled=true" }, UserJsonApiControllerConfiguration.class,
                "jsonApiController");

        String[] propertyValues;
        Class<?> userConfiguration;
        String beanName;

        OverrideControllerInput(String[] propertyValues, Class<?> userConfiguration, String beanName) {
            this.propertyValues = propertyValues;
            this.userConfiguration = userConfiguration;
            this.beanName = beanName;
        }
    }

    @ParameterizedTest
    @EnumSource(OverrideControllerInput.class)
    void overrideController(OverrideControllerInput input) {
        contextRunner.withPropertyValues("spring.cloud.refresh.enabled=false").withPropertyValues(input.propertyValues)
                .withConfiguration(UserConfigurations.of(input.userConfiguration)).run(context -> {
                    if (context.getBeanFactory() instanceof BeanDefinitionRegistry beanDefinitionRegistry) {
                        BeanDefinition beanDefinition = beanDefinitionRegistry.getBeanDefinition(input.beanName);
                        assertThat(beanDefinition.getFactoryBeanName())
                                .endsWith(input.userConfiguration.getSimpleName());
                    }
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EntityScan(basePackages = "example.models.jpa")
    public static class EntityConfiguration {
    }

    @Test
    void dataStoreTransaction() {
        contextRunner.withPropertyValues("spring.cloud.refresh.enabled=false")
                .withUserConfiguration(EntityConfiguration.class).run(context -> {

            DataStore dataStore = context.getBean(DataStore.class);
            PlatformTransactionManager transactionManager = context.getBean(PlatformTransactionManager.class);
            EntityManagerFactory entityManagerFactory = context.getBean(EntityManagerFactory.class);

            TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

            try (DataStoreTransaction transaction = dataStore.beginTransaction()) {
                ArtifactGroup artifactGroup = new ArtifactGroup();
                artifactGroup.setName("Group");
                transaction.createObject(artifactGroup, null);
                transaction.flush(null);

                ArtifactGroup found = transactionTemplate.execute(status -> {
                    assertThat(status.isNewTransaction()).isFalse();
                    EntityManager entityManager = EntityManagerFactoryUtils.getTransactionalEntityManager(entityManagerFactory);
                    return entityManager.find(ArtifactGroup.class, artifactGroup.getName());
                });
                assertThat(artifactGroup).isEqualTo(found);
                // Not committed so should rollback
            }

            transactionTemplate.execute(status -> {
                assertThat(status.isNewTransaction()).isTrue();
                EntityManager entityManager = EntityManagerFactoryUtils.getTransactionalEntityManager(entityManagerFactory);
                ArtifactGroup found = entityManager.find(ArtifactGroup.class, "Group");
                assertThat(found).isNull();

                try (DataStoreTransaction transaction = dataStore.beginTransaction()) {
                    ArtifactGroup artifactGroup = new ArtifactGroup();
                    artifactGroup.setName("Group");
                    transaction.createObject(artifactGroup, null);
                    transaction.commit(null);
                } catch (IOException e) {
                }

                found = entityManager.find(ArtifactGroup.class, "Group");
                assertThat(found).isNotNull();
                status.setRollbackOnly(); // Rollback
                return null;
            });

            // Verify it has been rolled back
            transactionTemplate.execute(status -> {
                assertThat(status.isNewTransaction()).isTrue();
                EntityManager entityManager = EntityManagerFactoryUtils.getTransactionalEntityManager(entityManagerFactory);
                ArtifactGroup found = entityManager.find(ArtifactGroup.class, "Group");
                assertThat(found).isNull();
                return null;
            });

            try (DataStoreTransaction transaction = dataStore.beginTransaction()) {
                ArtifactGroup artifactGroup = new ArtifactGroup();
                artifactGroup.setName("Group");
                transaction.createObject(artifactGroup, null);
                transaction.flush(null);

                ArtifactGroup found = transactionTemplate.execute(status -> {
                    assertThat(status.isNewTransaction()).isFalse();
                    EntityManager entityManager = EntityManagerFactoryUtils.getTransactionalEntityManager(entityManagerFactory);
                    return entityManager.find(ArtifactGroup.class, artifactGroup.getName());
                });
                assertThat(artifactGroup).isEqualTo(found);
                transaction.commit(null);
            }

            // Verify that it has been committed
            transactionTemplate.execute(status -> {
                assertThat(status.isNewTransaction()).isTrue();
                EntityManager entityManager = EntityManagerFactoryUtils.getTransactionalEntityManager(entityManagerFactory);
                ArtifactGroup found = entityManager.find(ArtifactGroup.class, "Group");
                assertThat(found).isNotNull();
                return null;
            });

        });
    }

    @Configuration(proxyBeanMethods = false)
    public static class MultipleDataSourceConfiguration {
        @Bean
        public DataSource dataSourceV2() {
            return DataSourceBuilder.create().url("jdbc:h2:mem:db;DB_CLOSE_DELAY=-1").build();
        }

        @Bean
        public DataSource dataSourceV3() {
            return DataSourceBuilder.create().url("jdbc:h2:mem:db;DB_CLOSE_DELAY=-1").build();
        }

        @Bean
        public EntityManagerFactoryBuilder entityManagerFactoryBuilder(
                ObjectProvider<PersistenceUnitManager> persistenceUnitManager,
                ObjectProvider<EntityManagerFactoryBuilderCustomizer> customizers) {
            EntityManagerFactoryBuilder builder = new EntityManagerFactoryBuilder(new HibernateJpaVendorAdapter(),
                    new HashMap<>(), persistenceUnitManager.getIfAvailable());
            customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
            return builder;
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableJpaDataStore(entityManagerFactoryRef = "entityManagerFactoryV2", transactionManagerRef = "transactionManagerV2")
    @EnableJpaDataStore(entityManagerFactoryRef = "entityManagerFactoryV3", transactionManagerRef = "transactionManagerV3")
    public static class MultipleEntityManagerFactoryConfiguration {
        @Bean
        public LocalContainerEntityManagerFactoryBean entityManagerFactoryV2(EntityManagerFactoryBuilder builder,
                DefaultListableBeanFactory beanFactory, DataSource dataSourceV2) {
            Map<String, Object> vendorProperties = new HashMap<>();
            vendorProperties.put("hibernate.hbm2ddl.auto", "create-drop");
            final LocalContainerEntityManagerFactoryBean emf = builder.dataSource(dataSourceV2)
                    .packages("example.models.jpa.v2").properties(vendorProperties).build();
            return emf;
        }

        @Bean
        public LocalContainerEntityManagerFactoryBean entityManagerFactoryV3(EntityManagerFactoryBuilder builder,
                DefaultListableBeanFactory beanFactory, DataSource dataSourceV3) {
            Map<String, Object> vendorProperties = new HashMap<>();
            vendorProperties.put("hibernate.hbm2ddl.auto", "create-drop");
            final LocalContainerEntityManagerFactoryBean emf = builder.dataSource(dataSourceV3)
                    .packages("example.models.jpa.v3").properties(vendorProperties).build();
            return emf;
        }

        @Bean
        public PlatformTransactionManager transactionManagerV2(EntityManagerFactory entityManagerFactoryV2) {
            return new JpaTransactionManager(entityManagerFactoryV2);
        }

        @Bean
        public PlatformTransactionManager transactionManagerV3(EntityManagerFactory entityManagerFactoryV3) {
            return new JpaTransactionManager(entityManagerFactoryV3);
        }
    }

    @Test
    void multiplexDataStoreTransaction() {
        contextRunner.withPropertyValues("spring.cloud.refresh.enabled=false")
                .withUserConfiguration(MultipleDataSourceConfiguration.class, MultipleEntityManagerFactoryConfiguration.class).run(context -> {
                    DataStore dataStore = context.getBean(DataStore.class);
                    assertThat(dataStore).isInstanceOf(MultiplexManager.class);

                    // The data store will only be initialized properly by elide to populate the dictionary
                    RefreshableElide refreshableElide = context.getBean(RefreshableElide.class);
                    dataStore = refreshableElide.getElide().getDataStore();

                    try (DataStoreTransaction transaction = dataStore.beginTransaction()) {
                        ArtifactGroupV2 artifactGroupV2 = new ArtifactGroupV2();
                        artifactGroupV2.setName("Group V2");
                        transaction.save(artifactGroupV2, null);
                        transaction.commit(null);
                    }

                    try (DataStoreTransaction transaction = dataStore.beginTransaction()) {
                        ArtifactGroupV3 artifactGroupV3 = new ArtifactGroupV3();
                        artifactGroupV3.setName("Group V3");
                        transaction.save(artifactGroupV3, null);
                        transaction.commit(null);
                    }

                    try (DataStoreTransaction transaction = dataStore.beginTransaction()) {
                        ArtifactGroupV2 artifactGroupV2 = new ArtifactGroupV2();
                        artifactGroupV2.setName("Group V2");

                        ArtifactGroupV3 artifactGroupV3 = new ArtifactGroupV3();
                        artifactGroupV3.setName("Group V3");

                        transaction.save(artifactGroupV2, null);
                        transaction.save(artifactGroupV3, null);

                        assertThatThrownBy(() -> transaction.commit(null)).isInstanceOf(TransactionException.class)
                                .message().isEqualTo("Transaction synchronization is not active");
                    }
                });
    }
}
