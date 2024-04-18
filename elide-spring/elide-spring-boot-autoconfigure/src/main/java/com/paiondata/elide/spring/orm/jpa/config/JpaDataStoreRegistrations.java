/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.spring.orm.jpa.config;


import com.paiondata.elide.core.type.ClassType;
import com.paiondata.elide.core.type.Type;
import com.paiondata.elide.datastores.jpa.JpaDataStore;
import com.paiondata.elide.datastores.jpa.JpaDataStore.EntityManagerSupplier;
import com.paiondata.elide.datastores.jpa.JpaDataStore.JpaTransactionSupplier;
import com.paiondata.elide.spring.config.ElideConfigProperties;
import com.paiondata.elide.spring.orm.jpa.EntityManagerProxySupplier;
import com.paiondata.elide.spring.orm.jpa.PlatformJpaTransactionSupplier;
import com.paiondata.elide.spring.orm.jpa.config.JpaDataStoreRegistration.JpaDataStoreRegistrationBuilder;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import jakarta.persistence.EntityManagerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * JpaDataStoreRegistrations.
 */
public class JpaDataStoreRegistrations {
    private JpaDataStoreRegistrations() {
    }

    /**
     * Creates a JpaDataStore registration from inputs.
     *
     * @param entityManagerFactory the bean name of the entity manager factory
     * @param platformTransactionManager the bean name of the platform transaction manager
     * @param settings the settings
     * @param optionalQueryLogger the optional query logger
     * @return the JpaDataStore registration.
     */
    public static JpaDataStoreRegistration buildJpaDataStoreRegistration(String entityManagerFactoryName,
            EntityManagerFactory entityManagerFactory, String platformTransactionManagerName,
            PlatformTransactionManager platformTransactionManager, ElideConfigProperties settings,
            Optional<com.paiondata.elide.datastores.jpql.porting.QueryLogger> optionalQueryLogger,
            Class<?>[] managedClasses) {
        DefaultTransactionDefinition writeJpaTransactionDefinition = new DefaultTransactionDefinition(
                TransactionDefinition.PROPAGATION_REQUIRED);
        writeJpaTransactionDefinition.setName(
                "Elide Write Transaction (" + entityManagerFactoryName + "," + platformTransactionManagerName + ")");
        JpaTransactionSupplier writeJpaTransactionSupplier = buildJpaTransactionSupplier(platformTransactionManager,
                entityManagerFactory, writeJpaTransactionDefinition, settings);

        DefaultTransactionDefinition readJpaTransactionDefinition = new DefaultTransactionDefinition(
                TransactionDefinition.PROPAGATION_REQUIRED);
        readJpaTransactionDefinition.setName(
                "Elide Read Transaction (" + entityManagerFactoryName + "," + platformTransactionManagerName + ")");
        readJpaTransactionDefinition.setReadOnly(true);
        JpaTransactionSupplier readJpaTransactionSupplier = buildJpaTransactionSupplier(platformTransactionManager,
                entityManagerFactory, readJpaTransactionDefinition, settings);

        JpaDataStoreRegistrationBuilder builder = JpaDataStoreRegistration.builder().name(entityManagerFactoryName)
                .entityManagerSupplier(buildEntityManagerSupplier())
                .readTransactionSupplier(readJpaTransactionSupplier)
                .writeTransactionSupplier(writeJpaTransactionSupplier)
                .queryLogger(optionalQueryLogger.orElse(JpaDataStore.DEFAULT_LOGGER));
        if (managedClasses != null && managedClasses.length > 0) {
            Set<Type<?>> models = new HashSet<>();
            Arrays.stream(managedClasses).map(ClassType::of).forEach(models::add);
            builder.managedClasses(models);
        } else {
            builder.metamodelSupplier(entityManagerFactory::getMetamodel);
        }

        return builder.build();
    }

    /**
     * Create a JPA Transaction Supplier to use.
     * @param transactionManager Spring Platform Transaction Manager
     * @param entityManagerFactory An instance of EntityManagerFactory
     * @param settings Elide configuration settings.
     * @return the JpaTransactionSupplier.
     */
    public static JpaTransactionSupplier buildJpaTransactionSupplier(PlatformTransactionManager transactionManager,
            EntityManagerFactory entityManagerFactory, TransactionDefinition transactionDefinition,
            ElideConfigProperties settings) {
        return new PlatformJpaTransactionSupplier(
                transactionDefinition, transactionManager,
                entityManagerFactory, settings.getJpaStore().isDelegateToInMemoryStore());
    }

    /**
     * Create an Entity Manager Supplier to use.
     * @return a EntityManagerProxySupplier.
     */
    public static EntityManagerSupplier buildEntityManagerSupplier() {
        return new EntityManagerProxySupplier();
    }
}
