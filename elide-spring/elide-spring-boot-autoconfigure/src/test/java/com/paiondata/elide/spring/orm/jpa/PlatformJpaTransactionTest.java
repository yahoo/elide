/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.spring.orm.jpa;

import static com.paiondata.elide.datastores.jpa.JpaDataStore.DEFAULT_LOGGER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.io.IOException;

/**
 * Tests for PlatformJpaTransaction.
 */
@ExtendWith(MockitoExtension.class)
class PlatformJpaTransactionTest {
    @Mock
    PlatformTransactionManager transactionManager;

    @Mock
    EntityManagerFactory entityManagerFactory;

    @Mock
    EntityManager entityManager;

    @Test
    void entityManagerShouldBeSupplierEntityManager() throws IOException {
        try (PlatformJpaTransaction jpaTransaction = new PlatformJpaTransaction(transactionManager,
                new DefaultTransactionDefinition(), entityManagerFactory, entityManager, entityManager -> {
                }, DEFAULT_LOGGER, true, true)) {
            assertThatThrownBy(() -> jpaTransaction.begin()).isInstanceOf(IllegalStateException.class);
        }
    }

    /**
     * The PlatformTransactionManager is responsible for closing the entity manager
     * and the PlatformJpaTransaction should not close it.
     *
     * @throws IOException when close
     */
    @Test
    void closeShouldNotCloseEntityManager() throws IOException {
        SimpleTransactionStatus status = new SimpleTransactionStatus();
        when(entityManager.isOpen()).thenReturn(true);
        when(transactionManager.getTransaction(any())).thenReturn(status);
        doAnswer(invocation -> {
            TransactionSynchronizationManager.unbindResource(this.entityManagerFactory);
            status.setCompleted();
            return null;
        }).when(transactionManager).rollback(any());
        EntityManagerHolder holder = new EntityManagerHolder(entityManager);
        TransactionSynchronizationManager.bindResource(this.entityManagerFactory, holder);
        BasicEntityManagerProxy proxy = new BasicEntityManagerProxy();
        try (PlatformJpaTransaction jpaTransaction = new PlatformJpaTransaction(transactionManager,
                new DefaultTransactionDefinition(), entityManagerFactory, proxy, entityManager -> {
                }, DEFAULT_LOGGER, true, true)) {
            jpaTransaction.begin();
        }
        assertThat(proxy.getTargetEntityManager()).isEqualTo(entityManager);
        verify(entityManager, times(0)).close();
        assertThat(TransactionSynchronizationManager.getResource(this.entityManagerFactory)).isNull();
    }
}
