/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.spring.orm.jpa.config;

import com.paiondata.elide.core.type.Type;
import com.paiondata.elide.datastores.jpa.JpaDataStore.EntityManagerSupplier;
import com.paiondata.elide.datastores.jpa.JpaDataStore.JpaTransactionSupplier;
import com.paiondata.elide.datastores.jpa.JpaDataStore.MetamodelSupplier;
import com.paiondata.elide.datastores.jpql.porting.QueryLogger;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Set;

/**
 * Registration entry to configure a JpaDataStore.
 *
 * @see com.paiondata.elide.datastores.jpa.JpaDataStore
 */
@Builder
@AllArgsConstructor
public class JpaDataStoreRegistration {
    @Getter
    private final String name;
    @Getter
    private final EntityManagerSupplier entityManagerSupplier;
    @Getter
    private final JpaTransactionSupplier readTransactionSupplier;
    @Getter
    private final JpaTransactionSupplier writeTransactionSupplier;
    @Getter
    private final MetamodelSupplier metamodelSupplier;
    @Getter
    private final Set<Type<?>> managedClasses;
    @Getter
    private final QueryLogger queryLogger;

    /**
     * Used to build a JpaDataStore registration.
     */
    public static class JpaDataStoreRegistrationBuilder {
    }
}
