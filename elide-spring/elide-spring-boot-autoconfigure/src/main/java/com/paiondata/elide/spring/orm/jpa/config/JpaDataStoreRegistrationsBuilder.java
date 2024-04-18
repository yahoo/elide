/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.spring.orm.jpa.config;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Builder used to configure registration entries for building a JpaDataStore.
 *
 * @see com.paiondata.elide.datastores.jpa.JpaDataStore
 * @see com.paiondata.elide.spring.orm.jpa.config.JpaDataStoreRegistrationsBuilderCustomizer
 */
public class JpaDataStoreRegistrationsBuilder {
    private final List<JpaDataStoreRegistration> registrations = new ArrayList<>();

    public JpaDataStoreRegistrationsBuilder registrations(Consumer<List<JpaDataStoreRegistration>> customizer) {
        customizer.accept(this.registrations);
        return this;
    }

    public JpaDataStoreRegistrationsBuilder registrations(List<JpaDataStoreRegistration> registrations) {
        this.registrations.clear();
        this.registrations.addAll(registrations);
        return this;
    }

    public JpaDataStoreRegistrationsBuilder add(JpaDataStoreRegistration registration) {
        this.registrations.add(registration);
        return this;
    }

    public List<JpaDataStoreRegistration> build() {
        return this.registrations;
    }
}
