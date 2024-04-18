/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.spring.config;

import static com.paiondata.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static org.assertj.core.api.Assertions.assertThat;

import com.paiondata.elide.RefreshableElide;
import com.paiondata.elide.core.RequestScope;
import com.paiondata.elide.core.datastore.DataStore;
import com.paiondata.elide.core.datastore.DataStoreTransaction;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.request.EntityProjection;
import com.paiondata.elide.core.request.route.Route;
import com.paiondata.elide.datastores.jpa.JpaDataStore;
import com.paiondata.elide.datastores.multiplex.MultiplexManager;
import com.paiondata.elide.spring.orm.jpa.config.EnableJpaDataStore;
import com.paiondata.elide.spring.orm.jpa.config.JpaDataStoreRegistration;
import com.paiondata.elide.spring.orm.jpa.config.JpaDataStoreRegistrations;
import com.atomikos.spring.AtomikosAutoConfiguration;
import com.atomikos.spring.AtomikosDataSourceBean;

import example.models.jpa.ArtifactGroup;
import example.models.jpa.v2.ArtifactGroupV2;
import example.models.jpa.v3.ArtifactGroupV3;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.transaction.jta.platform.internal.NoJtaPlatform;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.EntityManagerFactoryBuilderCustomizer;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.boot.orm.jpa.hibernate.SpringJtaPlatform;
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
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;
import javax.sql.XADataSource;

/**
 * Tests for ElideAutoConfiguration transaction.
 */
class ElideAutoConfigurationTransactionTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ElideAutoConfiguration.class, DataSourceAutoConfiguration.class,
                    HibernateJpaAutoConfiguration.class, TransactionAutoConfiguration.class, RefreshAutoConfiguration.class));

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
    public static class MultipleDataSourceJpaConfiguration {
        @Bean
        public DataSource dataSourceV2() {
            return DataSourceBuilder.create().url("jdbc:h2:mem:db1;DB_CLOSE_DELAY=-1").username("sa").password("").build();
        }

        @Bean
        public DataSource dataSourceV3() {
            return DataSourceBuilder.create().url("jdbc:h2:mem:db2;DB_CLOSE_DELAY=-1").username("sa").password("").build();
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

    /**
     * This creates 2 entity manager factories each with its own JPA transaction
     * manager. As they are using separate transaction managers, commits and
     * rollbacks don't affect each other.
     */
    @Configuration(proxyBeanMethods = false)
    @EnableJpaDataStore(entityManagerFactoryRef = "entityManagerFactoryV2", transactionManagerRef = "transactionManagerV2")
    @EnableJpaDataStore(entityManagerFactoryRef = "entityManagerFactoryV3", transactionManagerRef = "transactionManagerV3")
    public static class MultipleEntityManagerFactoryJpaConfiguration {
        @Bean
        public LocalContainerEntityManagerFactoryBean entityManagerFactoryV2(EntityManagerFactoryBuilder builder,
                DefaultListableBeanFactory beanFactory, DataSource dataSourceV2) {
            Map<String, Object> vendorProperties = new HashMap<>();
            vendorProperties.put(AvailableSettings.HBM2DDL_AUTO, "create-drop");
            vendorProperties.put(AvailableSettings.JTA_PLATFORM, new NoJtaPlatform());
            final LocalContainerEntityManagerFactoryBean emf = builder.dataSource(dataSourceV2)
                    .packages("example.models.jpa.v2").properties(vendorProperties).build();
            return emf;
        }

        @Bean
        public LocalContainerEntityManagerFactoryBean entityManagerFactoryV3(EntityManagerFactoryBuilder builder,
                DefaultListableBeanFactory beanFactory, DataSource dataSourceV3) {
            Map<String, Object> vendorProperties = new HashMap<>();
            vendorProperties.put(AvailableSettings.HBM2DDL_AUTO, "create-drop");
            vendorProperties.put(AvailableSettings.JTA_PLATFORM, new NoJtaPlatform());
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
    void multiplexDataStoreJpaTransaction() {
        contextRunner.withPropertyValues("spring.cloud.refresh.enabled=false")
                .withUserConfiguration(MultipleDataSourceJpaConfiguration.class, MultipleEntityManagerFactoryJpaConfiguration.class).run(context -> {
                    DataStore dataStore = context.getBean(DataStore.class);
                    assertThat(dataStore).isInstanceOf(MultiplexManager.class);

                    // The data store will only be initialized properly by elide to populate the dictionary
                    RefreshableElide refreshableElide = context.getBean(RefreshableElide.class);
                    dataStore = refreshableElide.getElide().getDataStore();

                    Route route = Route.builder().apiVersion(NO_VERSION).build();
                    RequestScope scope = RequestScope.builder().route(route).requestId(UUID.randomUUID())
                            .elideSettings(refreshableElide.getElide().getElideSettings()).build();

                    try (DataStoreTransaction transaction = dataStore.beginTransaction()) {
                        ArtifactGroupV2 artifactGroupV2 = new ArtifactGroupV2();
                        artifactGroupV2.setName("JPA Group V2a");
                        transaction.save(artifactGroupV2, null);
                        transaction.commit(null);
                    }

                    try (DataStoreTransaction transaction = dataStore.beginTransaction()) {
                        ArtifactGroupV3 artifactGroupV3 = new ArtifactGroupV3();
                        artifactGroupV3.setName("JPA Group V3a");
                        transaction.save(artifactGroupV3, null);
                        transaction.commit(null);
                    }

                    try (DataStoreTransaction transaction = dataStore.beginTransaction()) {
                        ArtifactGroupV2 artifactGroupV2 = new ArtifactGroupV2();
                        artifactGroupV2.setName("JPA Group V2b");

                        ArtifactGroupV3 artifactGroupV3 = new ArtifactGroupV3();
                        artifactGroupV3.setName("JPA Group V3b");

                        transaction.save(artifactGroupV2, null);
                        transaction.save(artifactGroupV3, null);

                        transaction.commit(null);
                    }

                    try (DataStoreTransaction transaction = dataStore.beginTransaction()) {
                        ArtifactGroupV2 artifactGroupV2 = transaction.loadObject(
                                EntityProjection.builder().type(ArtifactGroupV2.class).build(), "JPA Group V2b", scope);
                        assertThat(artifactGroupV2).isNotNull();

                        ArtifactGroupV3 artifactGroupV3 = transaction.loadObject(
                                EntityProjection.builder().type(ArtifactGroupV3.class).build(), "JPA Group V3b", scope);
                        assertThat(artifactGroupV3).isNotNull();
                    }

                    try (DataStoreTransaction transaction = dataStore.beginReadTransaction()) {
                        ArtifactGroupV2 artifactGroupV2 = transaction.loadObject(
                                EntityProjection.builder().type(ArtifactGroupV2.class).build(), "JPA Group V2b", scope);
                        assertThat(artifactGroupV2).isNotNull();

                        ArtifactGroupV3 artifactGroupV3 = transaction.loadObject(
                                EntityProjection.builder().type(ArtifactGroupV3.class).build(), "JPA Group V3b", scope);
                        assertThat(artifactGroupV3).isNotNull();
                    }
                });
    }

    @Test
    void multipleDataStoreJpaTransaction() {
        contextRunner.withPropertyValues("spring.cloud.refresh.enabled=false")
                .withUserConfiguration(MultipleDataSourceJpaConfiguration.class, MultipleEntityManagerFactoryJpaConfiguration.class)
                .run(context -> {
                    RefreshableElide refreshableElide = context.getBean(RefreshableElide.class);
                    Route route = Route.builder().apiVersion(NO_VERSION).build();
                    RequestScope scope = RequestScope.builder().route(route).requestId(UUID.randomUUID())
                            .elideSettings(refreshableElide.getElide().getElideSettings()).build();
                    EntityManagerFactory entityManagerFactoryV2 = context.getBean("entityManagerFactoryV2",
                            EntityManagerFactory.class);
                    EntityManagerFactory entityManagerFactoryV3 = context.getBean("entityManagerFactoryV3",
                            EntityManagerFactory.class);
                    JpaTransactionManager transactionManagerV2 = context.getBean("transactionManagerV2", JpaTransactionManager.class);
                    JpaTransactionManager transactionManagerV3 = context.getBean("transactionManagerV3", JpaTransactionManager.class);
                    ElideConfigProperties settings = new ElideConfigProperties();
                    settings.getJpaStore().setDelegateToInMemoryStore(true);
                    JpaDataStoreRegistration registrationV2 = JpaDataStoreRegistrations.buildJpaDataStoreRegistration(
                            "entityManagerFactoryV2", entityManagerFactoryV2, "transactionManagerV2",
                            transactionManagerV2, settings, Optional.empty(), null);
                    JpaDataStoreRegistration registrationV3 = JpaDataStoreRegistrations.buildJpaDataStoreRegistration(
                            "entityManagerFactoryV3", entityManagerFactoryV3, "transactionManagerV3",
                            transactionManagerV3, settings, Optional.empty(), null);

                    EntityDictionary entityDictionary = EntityDictionary.builder().build();

                    JpaDataStore jpaDataStoreV2 = new JpaDataStore(registrationV2.getEntityManagerSupplier(),
                            registrationV2.getReadTransactionSupplier(), registrationV2.getWriteTransactionSupplier(),
                            registrationV2.getQueryLogger(), registrationV2.getMetamodelSupplier());

                    JpaDataStore jpaDataStoreV3 = new JpaDataStore(registrationV3.getEntityManagerSupplier(),
                            registrationV3.getReadTransactionSupplier(), registrationV3.getWriteTransactionSupplier(),
                            registrationV3.getQueryLogger(), registrationV3.getMetamodelSupplier());

                    jpaDataStoreV2.populateEntityDictionary(entityDictionary);
                    jpaDataStoreV3.populateEntityDictionary(entityDictionary);

                    // Test that the rollback in the outer transaction works
                    try (DataStoreTransaction transaction1 = jpaDataStoreV2.beginTransaction()) {
                        ArtifactGroupV2 artifactGroupV2 = new ArtifactGroupV2();
                        artifactGroupV2.setName("JPA V2");
                        transaction1.save(artifactGroupV2, null);

                        try (DataStoreTransaction transaction2 = jpaDataStoreV3.beginTransaction()) {
                            ArtifactGroupV3 artifactGroupV3 = new ArtifactGroupV3();
                            artifactGroupV3.setName("JPA V3");
                            transaction2.save(artifactGroupV3, null);
                            transaction2.commit(null);
                        }
                        // transaction1 wasn't committed and should rollback
                    }

                    try (DataStoreTransaction transaction2 = jpaDataStoreV3.beginTransaction()) {
                        ArtifactGroupV3 artifactGroupV3 = transaction2.loadObject(
                                EntityProjection.builder().type(ArtifactGroupV3.class).build(), "JPA V3", scope);
                        // although the outer transaction was rolled back the entity was created as the
                        // 2 transaction managers are separate a compensating transaction would be
                        // required in this case to reverse the inner transaction
                        assertThat(artifactGroupV3).isNotNull();
                        transaction2.delete(artifactGroupV3, null);
                    }

                    // Test that the commit in the outer transaction works
                    try (DataStoreTransaction transaction1 = jpaDataStoreV2.beginTransaction()) {
                        ArtifactGroupV2 artifactGroupV2 = new ArtifactGroupV2();
                        artifactGroupV2.setName("JPA V2");
                        transaction1.save(artifactGroupV2, null);

                        try (DataStoreTransaction transaction2 = jpaDataStoreV3.beginTransaction()) {
                            ArtifactGroupV3 artifactGroupV3 = new ArtifactGroupV3();
                            artifactGroupV3.setName("JPA V3");
                            transaction2.save(artifactGroupV3, null);
                            transaction2.commit(null);
                        }
                        transaction1.commit(null);
                    }

                    try (DataStoreTransaction transaction1 = jpaDataStoreV2.beginTransaction()) {
                        ArtifactGroupV2 artifactGroupV2 = transaction1.loadObject(
                                EntityProjection.builder().type(ArtifactGroupV2.class).build(), "JPA V2", scope);
                        assertThat(artifactGroupV2).isNotNull();

                        try (DataStoreTransaction transaction2 = jpaDataStoreV3.beginTransaction()) {
                            ArtifactGroupV3 artifactGroupV3 = transaction2.loadObject(
                                    EntityProjection.builder().type(ArtifactGroupV3.class).build(), "JPA V3", scope);
                            assertThat(artifactGroupV3).isNotNull();
                        }
                    }
                });
    }

    @Configuration(proxyBeanMethods = false)
    public static class MultipleDataSourceJtaConfiguration {
        @Bean
        public DataSource dataSourceV2() {
            XADataSource xaDataSource = DataSourceBuilder.create().url("jdbc:h2:mem:db1;DB_CLOSE_DELAY=-1")
                    .driverClassName("org.h2.Driver").type(org.h2.jdbcx.JdbcDataSource.class).username("sa")
                    .password("").build();
            AtomikosDataSourceBean atomikosDataSource = new AtomikosDataSourceBean();
            atomikosDataSource.setXaDataSource(xaDataSource);
            return atomikosDataSource;
        }

        @Bean
        public DataSource dataSourceV3() {
            XADataSource xaDataSource = DataSourceBuilder.create().url("jdbc:h2:mem:db2;DB_CLOSE_DELAY=-1")
                    .driverClassName("org.h2.Driver").type(org.h2.jdbcx.JdbcDataSource.class).username("sa")
                    .password("").build();
            AtomikosDataSourceBean atomikosDataSource = new AtomikosDataSourceBean();
            atomikosDataSource.setXaDataSource(xaDataSource);
            return atomikosDataSource;
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

    /**
     * This creates 2 entity manager factories with a shared JTA transaction
     * manager. They will participate in a shared transaction.
     */
    @Configuration(proxyBeanMethods = false)
    @EnableJpaDataStore(entityManagerFactoryRef = "entityManagerFactoryV2", transactionManagerRef = "transactionManager")
    @EnableJpaDataStore(entityManagerFactoryRef = "entityManagerFactoryV3", transactionManagerRef = "transactionManager")
    public static class MultipleEntityManagerFactoryJtaConfiguration {
        @Bean
        public LocalContainerEntityManagerFactoryBean entityManagerFactoryV2(EntityManagerFactoryBuilder builder,
                DefaultListableBeanFactory beanFactory, DataSource dataSourceV2, JtaTransactionManager transactionManager) {
            Map<String, Object> vendorProperties = new HashMap<>();
            vendorProperties.put(AvailableSettings.HBM2DDL_AUTO, "create-drop");
            vendorProperties.put(AvailableSettings.JTA_PLATFORM, new SpringJtaPlatform(transactionManager));
            final LocalContainerEntityManagerFactoryBean emf = builder.dataSource(dataSourceV2)
                    .packages("example.models.jpa.v2").properties(vendorProperties).jta(true).build();
            return emf;
        }

        @Bean
        public LocalContainerEntityManagerFactoryBean entityManagerFactoryV3(EntityManagerFactoryBuilder builder,
                DefaultListableBeanFactory beanFactory, DataSource dataSourceV3, JtaTransactionManager transactionManager) {
            Map<String, Object> vendorProperties = new HashMap<>();
            vendorProperties.put(AvailableSettings.HBM2DDL_AUTO, "create-drop");
            vendorProperties.put(AvailableSettings.JTA_PLATFORM, new SpringJtaPlatform(transactionManager));
            final LocalContainerEntityManagerFactoryBean emf = builder.dataSource(dataSourceV3)
                    .packages("example.models.jpa.v3").properties(vendorProperties).jta(true).build();
            return emf;
        }
    }

    @Test
    void multiplexDataStoreJtaTransaction() {
        contextRunner
                .withPropertyValues("spring.cloud.refresh.enabled=false",
                        "atomikos.properties.max-timeout=0")
                .withConfiguration(AutoConfigurations.of(AtomikosAutoConfiguration.class))
                .withUserConfiguration(MultipleDataSourceJtaConfiguration.class, MultipleEntityManagerFactoryJtaConfiguration.class).run(context -> {
                    DataStore dataStore = context.getBean(DataStore.class);
                    assertThat(dataStore).isInstanceOf(MultiplexManager.class);

                    // The data store will only be initialized properly by elide to populate the dictionary
                    RefreshableElide refreshableElide = context.getBean(RefreshableElide.class);
                    dataStore = refreshableElide.getElide().getDataStore();
                    Route route = Route.builder().apiVersion(NO_VERSION).build();
                    RequestScope scope = RequestScope.builder().route(route).requestId(UUID.randomUUID())
                            .elideSettings(refreshableElide.getElide().getElideSettings()).build();

                    try (DataStoreTransaction transaction = dataStore.beginTransaction()) {
                        ArtifactGroupV2 artifactGroupV2 = new ArtifactGroupV2();
                        artifactGroupV2.setName("JTA Group V2a");
                        transaction.save(artifactGroupV2, null);
                        transaction.commit(null);
                    }

                    try (DataStoreTransaction transaction = dataStore.beginTransaction()) {
                        ArtifactGroupV3 artifactGroupV3 = new ArtifactGroupV3();
                        artifactGroupV3.setName("JTA Group V3a");
                        transaction.save(artifactGroupV3, null);
                        transaction.commit(null);
                    }

                    try (DataStoreTransaction transaction = dataStore.beginTransaction()) {
                        ArtifactGroupV2 artifactGroupV2 = new ArtifactGroupV2();
                        artifactGroupV2.setName("JTA Group V2b");

                        ArtifactGroupV3 artifactGroupV3 = new ArtifactGroupV3();
                        artifactGroupV3.setName("JTA Group V3b");

                        transaction.save(artifactGroupV2, null);
                        transaction.save(artifactGroupV3, null);

                        transaction.commit(null);
                    }

                    try (DataStoreTransaction transaction = dataStore.beginTransaction()) {
                        ArtifactGroupV2 artifactGroupV2 = transaction.loadObject(
                                EntityProjection.builder().type(ArtifactGroupV2.class).build(), "JTA Group V2b", scope);
                        assertThat(artifactGroupV2).isNotNull();

                        ArtifactGroupV3 artifactGroupV3 = transaction.loadObject(
                                EntityProjection.builder().type(ArtifactGroupV3.class).build(), "JTA Group V3b", scope);
                        assertThat(artifactGroupV3).isNotNull();
                    }

                    try (DataStoreTransaction transaction = dataStore.beginReadTransaction()) {
                        ArtifactGroupV2 artifactGroupV2 = transaction.loadObject(
                                EntityProjection.builder().type(ArtifactGroupV2.class).build(), "JTA Group V2b", scope);
                        assertThat(artifactGroupV2).isNotNull();

                        ArtifactGroupV3 artifactGroupV3 = transaction.loadObject(
                                EntityProjection.builder().type(ArtifactGroupV3.class).build(), "JTA Group V3b", scope);
                        assertThat(artifactGroupV3).isNotNull();
                    }
                });
    }

    @Test
    void multipleDataStoreJtaTransaction() {
        contextRunner.withPropertyValues("spring.cloud.refresh.enabled=false", "atomikos.properties.max-timeout=0")
                .withConfiguration(AutoConfigurations.of(AtomikosAutoConfiguration.class))
                .withUserConfiguration(MultipleDataSourceJtaConfiguration.class,
                        MultipleEntityManagerFactoryJtaConfiguration.class)
                .run(context -> {
                    RefreshableElide refreshableElide = context.getBean(RefreshableElide.class);
                    Route route = Route.builder().apiVersion(NO_VERSION).build();
                    RequestScope scope = RequestScope.builder().route(route).requestId(UUID.randomUUID())
                            .elideSettings(refreshableElide.getElide().getElideSettings()).build();
                    EntityManagerFactory entityManagerFactoryV2 = context.getBean("entityManagerFactoryV2",
                            EntityManagerFactory.class);
                    EntityManagerFactory entityManagerFactoryV3 = context.getBean("entityManagerFactoryV3",
                            EntityManagerFactory.class);
                    JtaTransactionManager jtaTransactionManager = context.getBean(JtaTransactionManager.class);
                    ElideConfigProperties settings = new ElideConfigProperties();
                    settings.getJpaStore().setDelegateToInMemoryStore(true);
                    JpaDataStoreRegistration registrationV2 = JpaDataStoreRegistrations.buildJpaDataStoreRegistration(
                            "entityManagerFactoryV2", entityManagerFactoryV2, "transactionManager",
                            jtaTransactionManager, settings, Optional.empty(), null);
                    JpaDataStoreRegistration registrationV3 = JpaDataStoreRegistrations.buildJpaDataStoreRegistration(
                            "entityManagerFactoryV3", entityManagerFactoryV3, "transactionManager",
                            jtaTransactionManager, settings, Optional.empty(), null);

                    EntityDictionary entityDictionary = EntityDictionary.builder().build();

                    JpaDataStore jpaDataStoreV2 = new JpaDataStore(registrationV2.getEntityManagerSupplier(),
                            registrationV2.getReadTransactionSupplier(), registrationV2.getWriteTransactionSupplier(),
                            registrationV2.getQueryLogger(), registrationV2.getMetamodelSupplier());

                    JpaDataStore jpaDataStoreV3 = new JpaDataStore(registrationV3.getEntityManagerSupplier(),
                            registrationV3.getReadTransactionSupplier(), registrationV3.getWriteTransactionSupplier(),
                            registrationV3.getQueryLogger(), registrationV3.getMetamodelSupplier());

                    jpaDataStoreV2.populateEntityDictionary(entityDictionary);
                    jpaDataStoreV3.populateEntityDictionary(entityDictionary);

                    // Test that the rollback in the outer transaction works
                    try (DataStoreTransaction transaction1 = jpaDataStoreV2.beginTransaction()) {
                        ArtifactGroupV2 artifactGroupV2 = new ArtifactGroupV2();
                        artifactGroupV2.setName("JTA V2");
                        transaction1.save(artifactGroupV2, null);

                        try (DataStoreTransaction transaction2 = jpaDataStoreV3.beginTransaction()) {
                            ArtifactGroupV3 artifactGroupV3 = new ArtifactGroupV3();
                            artifactGroupV3.setName("JTA V3");
                            transaction2.save(artifactGroupV3, null);
                            transaction2.commit(null);
                        }
                        // transaction1 wasn't committed and should rollback
                    }

                    try (DataStoreTransaction transaction2 = jpaDataStoreV3.beginTransaction()) {
                        ArtifactGroupV3 artifactGroupV3 = transaction2.loadObject(
                                EntityProjection.builder().type(ArtifactGroupV3.class).build(), "JTA V3", scope);
                        // as the outer transaction was rolled back the entity isn't created
                        assertThat(artifactGroupV3).isNull();
                    }

                    // Test that the commit in the outer transaction works
                    try (DataStoreTransaction transaction1 = jpaDataStoreV2.beginTransaction()) {
                        ArtifactGroupV2 artifactGroupV2 = new ArtifactGroupV2();
                        artifactGroupV2.setName("JTA V2");
                        transaction1.save(artifactGroupV2, null);

                        try (DataStoreTransaction transaction2 = jpaDataStoreV3.beginTransaction()) {
                            ArtifactGroupV3 artifactGroupV3 = new ArtifactGroupV3();
                            artifactGroupV3.setName("JTA V3");
                            transaction2.save(artifactGroupV3, null);
                            transaction2.commit(null);
                        }
                        transaction1.commit(null);
                    }

                    try (DataStoreTransaction transaction1 = jpaDataStoreV2.beginTransaction()) {
                        ArtifactGroupV2 artifactGroupV2 = transaction1.loadObject(
                                EntityProjection.builder().type(ArtifactGroupV2.class).build(), "JTA V2", scope);
                        assertThat(artifactGroupV2).isNotNull();

                        try (DataStoreTransaction transaction2 = jpaDataStoreV3.beginTransaction()) {
                            ArtifactGroupV3 artifactGroupV3 = transaction2.loadObject(
                                    EntityProjection.builder().type(ArtifactGroupV3.class).build(), "JTA V3", scope);
                            assertThat(artifactGroupV3).isNotNull();
                        }
                    }

                });
    }
}
