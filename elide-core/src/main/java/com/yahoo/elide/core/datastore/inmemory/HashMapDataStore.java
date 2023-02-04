/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.datastore.inmemory;

import com.yahoo.elide.annotation.Exclude;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.datastore.test.DataStoreTestHarness;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.core.utils.ClassScanner;
import com.google.common.collect.Sets;

import lombok.Getter;

import java.util.Arrays;
import java.util.Collection;
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
    protected final Map<Type<?>, Map<String, Object>> dataStore = Collections.synchronizedMap(new HashMap<>());
    @Getter protected EntityDictionary dictionary;
    @Getter private final ConcurrentHashMap<Type<?>, AtomicLong> typeIds = new ConcurrentHashMap<>();

    public HashMapDataStore(ClassScanner scanner, Package beanPackage) {
        this(scanner, Sets.newHashSet(beanPackage));
    }

    public HashMapDataStore(ClassScanner scanner, Set<Package> beanPackages) {
        for (Package beanPackage : beanPackages) {
            scanner.getAllClasses(beanPackage.getName()).stream()
                .map(ClassType::new)
                .filter(modelType -> EntityDictionary.getFirstAnnotation(modelType,
                        Arrays.asList(Include.class, Exclude.class)) instanceof Include)
                .forEach(modelType -> dataStore.put(modelType,
                        Collections.synchronizedMap(new LinkedHashMap<>())));
        }
    }

    public HashMapDataStore(Collection<Class<?>> beanClasses) {
        beanClasses.stream()
                .map(ClassType::new)
                .filter(modelType -> EntityDictionary.getFirstAnnotation(modelType,
                        Arrays.asList(Include.class, Exclude.class)) instanceof Include)
                .forEach(modelType -> dataStore.put(modelType,
                        Collections.synchronizedMap(new LinkedHashMap<>())));
    }

    @Override
    public void populateEntityDictionary(EntityDictionary dictionary) {
        for (Type<?> clazz : dataStore.keySet()) {
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
        for (Map.Entry<Type<?>, Map<String, Object>> dse : dataStore.entrySet()) {
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

    /**
     * Returns metadata mapping for an entity class.
     * @param cls entity class
     * @return Map
     */
    public Map<String, Object> get(Type<?> cls) {
        return dataStore.get(cls);
    }

    @Override
    public void cleanseTestData() {
        for (Map<String, Object> objects : dataStore.values()) {
            objects.clear();
        }
        typeIds.clear();
    }
}
