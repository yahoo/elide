/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.jpa.transaction.checker.classes;

/**
 * All Hibernate PersistentCollection subclasses.
 * Needed to exclude Hibernate dependency.
 */
public class HibernatePersistentCollections {
    public static final String[] CLASSES = {
            "org.hibernate.collection.internal.PersistentArrayHolder",
            "org.hibernate.collection.internal.PersistentBag",
            "org.hibernate.collection.internal.PersistentIdentifierBag",
            "org.hibernate.collection.internal.PersistentList",
            "org.hibernate.collection.internal.PersistentMap",
            "org.hibernate.collection.internal.PersistentSet",
            "org.hibernate.collection.internal.PersistentSortedMap",
            "org.hibernate.collection.internal.PersistentSortedSet",
            "org.hibernate.collection.spi.PersistentArrayHolder",
            "org.hibernate.collection.spi.PersistentBag",
            "org.hibernate.collection.spi.PersistentIdentifierBag",
            "org.hibernate.collection.spi.PersistentList",
            "org.hibernate.collection.spi.PersistentMap",
            "org.hibernate.collection.spi.PersistentSet",
            "org.hibernate.collection.spi.PersistentSortedMap",
            "org.hibernate.collection.spi.PersistentSortedSet"
    };
}
