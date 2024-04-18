/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.spring.orm.jpa;

import com.paiondata.elide.datastores.jpa.SupplierEntityManager;

import org.springframework.orm.jpa.EntityManagerProxy;
import jakarta.persistence.EntityManager;

import java.util.function.Supplier;

/**
 * Basic EntityManagerProxy implementation.
 */
public class BasicEntityManagerProxy extends SupplierEntityManager implements EntityManagerProxy {
    public BasicEntityManagerProxy() {
        super();
    }

    public BasicEntityManagerProxy(EntityManager entityManager) {
        super(entityManager);
    }

    public BasicEntityManagerProxy(Supplier<EntityManager> entityManagerSupplier) {
        super(entityManagerSupplier);
    }

    @Override
    public EntityManager getTargetEntityManager() throws IllegalStateException {
        EntityManager entityManager = getEntityManager();
        if (entityManager instanceof EntityManagerProxy entityManagerProxy) {
            return entityManagerProxy.getTargetEntityManager();
        }
        return entityManager;
    }
}
