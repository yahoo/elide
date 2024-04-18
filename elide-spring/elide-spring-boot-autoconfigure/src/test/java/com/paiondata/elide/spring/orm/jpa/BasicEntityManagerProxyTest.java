/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.spring.orm.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.jpa.EntityManagerProxy;

import jakarta.persistence.EntityManager;

/**
 * Tests for BasicEntityManagerProxy.
 */
@ExtendWith(MockitoExtension.class)
class BasicEntityManagerProxyTest {

    @Mock
    EntityManagerProxy entityManagerProxy;

    @Mock
    EntityManager entityManager;

    @Test
    void targetEntityManagerProxy() {
        try (BasicEntityManagerProxy proxy = new BasicEntityManagerProxy(entityManagerProxy)) {
            proxy.getTargetEntityManager();
            verify(entityManagerProxy).getTargetEntityManager();
        }
    }

    @Test
    void targetEntityManagerProxySupplier() {
        try (BasicEntityManagerProxy proxy = new BasicEntityManagerProxy(() -> entityManagerProxy)) {
            proxy.getTargetEntityManager();
            verify(entityManagerProxy).getTargetEntityManager();
        }
    }

    @Test
    void targetEntityManager() {
        try (BasicEntityManagerProxy proxy = new BasicEntityManagerProxy(entityManager)) {
            assertThat(proxy.getTargetEntityManager()).isEqualTo(entityManager);
        }
    }

    @Test
    void targetEntityManagerNull() {
        try (BasicEntityManagerProxy proxy = new BasicEntityManagerProxy()) {
            assertThat(proxy.getTargetEntityManager()).isNull();
        }
    }
}
