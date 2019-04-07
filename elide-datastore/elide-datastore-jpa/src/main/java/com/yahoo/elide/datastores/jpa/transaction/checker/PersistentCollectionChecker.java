/*
 * Copyright 2018, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.jpa.transaction.checker;

import com.yahoo.elide.datastores.jpa.transaction.checker.classes.EclipseLinkPersistentCollections;
import com.yahoo.elide.datastores.jpa.transaction.checker.classes.HibernatePersistentCollections;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Check is collection subclass of JPA provider special persistent collection.
 * Now supported EclipseLink and Hibernate.
 */
public class PersistentCollectionChecker implements Predicate<Collection<?>> {
    private final Set<String> classNames = new HashSet<>();

    public PersistentCollectionChecker() {
        classNames.addAll(Arrays.asList(HibernatePersistentCollections.CLASSES));
        classNames.addAll(Arrays.asList(EclipseLinkPersistentCollections.CLASSES));
    }

    @Override
    public boolean test(Collection<?> collection) {
        return classNames.contains(collection.getClass().getName());
    }
}
