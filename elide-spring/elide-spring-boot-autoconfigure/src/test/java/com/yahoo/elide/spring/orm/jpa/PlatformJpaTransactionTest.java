/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.spring.orm.jpa;

import static com.yahoo.elide.datastores.jpa.JpaDataStore.DEFAULT_LOGGER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Tests for PlatformJpaTransaction.
 */
@ExtendWith(MockitoExtension.class)
class PlatformJpaTransactionTest {
    @Mock
    PlatformTransactionManager transactionManager;

    @Mock
    EntityManagerFactory entityManagerFactory;

    // strictness set to lenient as entityManager.isOpen is no longer checked in the implementation
    // but it is useful to document the state of the entityManager
    @Mock(strictness = Mock.Strictness.LENIENT)
    EntityManager entityManager;

    @AfterEach
    void cleanup() {
        // Ensure the thread locals are cleaned up
        TransactionSynchronizationManager.clear();
        List<Object> keys = new ArrayList<>(TransactionSynchronizationManager.getResourceMap().keySet());
        for (Object key : keys) {
            TransactionSynchronizationManager.unbindResourceIfPossible(key);
        }
    }

    @Test
    void entityManagerShouldBeSupplierEntityManager() throws IOException {
        DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
        try (PlatformJpaTransaction jpaTransaction = new PlatformJpaTransaction(transactionManager,
                definition, entityManagerFactory, entityManager, entityManager -> {
                }, DEFAULT_LOGGER, true, true)) {
            assertThatThrownBy(() -> jpaTransaction.begin()).isInstanceOf(IllegalStateException.class);
            // getTransaction should not be called or it will leak transactional resources as commit/rollback not called
            verify(transactionManager, never()).getTransaction(definition);
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
        when(entityManager.isOpen()).thenReturn(true); // stub not used
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
        assertThat(TransactionSynchronizationManager.getResourceMap()).isEmpty();
    }

    /**
     * Ensure that the transactionManager.rollback is called even if the
     * entityManager is closed. This can happen when spring.jpa.open-in-view=true
     * (default setting) and the async request times out (default 30 seconds) which
     * causes the entity manager to be closed. Subsequent requests using the thread
     * will all fail when JpaTransactionManager attempts to start a transaction as
     * the ConnectionHolder is still bound to the thread local in
     * TransactionSynchronizationManager.
     *
     * @see org.springframework.orm.jpa.support.OpenEntityManagerInViewInterceptor#afterCompletion(org.springframework.web.context.request.WebRequest,
     *      Exception)
     * @see org.springframework.orm.jpa.JpaTransactionManager
     * @throws IOException when close
     */
    @Test
    void closeShouldRollbackTransactionEvenIfEntityManagerIsClosed() throws IOException {
        SimpleTransactionStatus status = new SimpleTransactionStatus();
        when(entityManager.isOpen()).thenReturn(false); // stub not used
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
        assertThat(TransactionSynchronizationManager.getResourceMap()).isEmpty();
        verify(transactionManager, times(1)).rollback(status);
    }
}
