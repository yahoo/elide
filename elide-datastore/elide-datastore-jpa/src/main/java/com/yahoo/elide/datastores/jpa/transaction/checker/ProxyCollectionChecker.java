/*
 * Copyright 2018, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.jpa.transaction.checker;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

/**
 * Check is collection subclass of JPA provider special persistent collection.
 * Now supported Hibernate, EclipseLink, OpenJPA.
 */
public class ProxyCollectionChecker implements Predicate<Collection<?>> {
    private static final String[] JPA_PROXY_COLLECTION_PACKAGES = {
            "org.hibernate.collection.internal",    // Hibernate
            "org.eclipse.persistence.indirection",  // EclipseLink
            "org.apache.openjpa.util"               // OpenJPA
    };

    private final List<String> proxyCollectionPackages;

    public ProxyCollectionChecker() {
        this(JPA_PROXY_COLLECTION_PACKAGES);
    }

    public ProxyCollectionChecker(String... proxyCollectionPackages) {
        this.proxyCollectionPackages = Arrays.asList(proxyCollectionPackages);
    }

    @Override
    public boolean test(Collection<?> collection) {
        String collectionClass = collection.getClass().getName();
        return proxyCollectionPackages.stream().anyMatch(collectionClass::startsWith);
    }
}
