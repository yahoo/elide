/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.datastore.inmemory;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.datastore.test.DataStoreTestHarness;
import com.yahoo.elide.utils.ClassScanner;

import com.google.common.collect.Sets;
import lombok.Getter;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple in-memory only database.
 */
public class HashMapDataStore implements DataStore, DataStoreTestHarness {
    private final Map<Class<?>, Map<String, Object>> dataStore = Collections.synchronizedMap(new HashMap<>());
    @Getter private EntityDictionary dictionary;
    @Getter private final Set<Package> beanPackages;
    @Getter private final ConcurrentHashMap<Class<?>, AtomicLong> typeIds = new ConcurrentHashMap<>();

    public HashMapDataStore(Package beanPackage) {
        this(Sets.newHashSet(beanPackage));
    }

    public HashMapDataStore(Set<Package> beanPackages) {
        this.beanPackages = beanPackages;

        for (Package beanPackage : beanPackages) {
            ClassScanner.getAnnotatedClasses(beanPackage, Include.class).stream()
                .filter(modelClass -> modelClass.getName().startsWith(beanPackage.getName()))
                .forEach(modelClass -> dataStore.put(modelClass, Collections.synchronizedMap(new LinkedHashMap<>())));
        }
    }

    @Override
    public void populateEntityDictionary(EntityDictionary dictionary) {
        for (Class<?> clazz : dataStore.keySet()) {
            dictionary.bindEntity(clazz);
        }

        this.dictionary = dictionary;
    }

    @Override
    public DataStoreTransaction beginTransaction() {
        return new HashMapStoreTransaction(dataStore, dictionary, typeIds);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Data store contents ");
        for (Map.Entry<Class<?>, Map<String, Object>> dse : dataStore.entrySet()) {
            sb.append("\n Table ").append(dse.getKey()).append(" contents \n");
            for (Map.Entry<String, Object> e : dse.getValue().entrySet()) {
                sb.append(" Id: ").append(e.getKey()).append(" Value: ").append(e.getValue());
            }
        }
        return sb.toString();
    }

    @Override
    public DataStore getDataStore() {
        return this;
    }

    @Override
    public void cleanseTestData() {
        for (Map<String, Object> objects : dataStore.values()) {
            objects.clear();
        }
        typeIds.clear();
    }
}
