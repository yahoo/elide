/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.datastore.inmemory;

import com.paiondata.elide.annotation.Exclude;
import com.paiondata.elide.annotation.Include;
import com.paiondata.elide.core.datastore.DataStore;
import com.paiondata.elide.core.datastore.DataStoreTransaction;
import com.paiondata.elide.core.datastore.test.DataStoreTestHarness;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.type.ClassType;
import com.paiondata.elide.core.type.Type;
import com.paiondata.elide.core.utils.ClassScanner;
import com.paiondata.elide.core.utils.ObjectCloner;
import com.paiondata.elide.core.utils.ObjectCloners;

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
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Simple in-memory only database.
 */
public class HashMapDataStore implements DataStore, DataStoreTestHarness {
    protected final Map<Type<?>, Map<String, Object>> dataStore = Collections.synchronizedMap(new HashMap<>());
    @Getter protected EntityDictionary dictionary;
    @Getter private final ConcurrentHashMap<Type<?>, AtomicLong> typeIds = new ConcurrentHashMap<>();
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final ObjectCloner objectCloner;

    public HashMapDataStore(ClassScanner scanner, Package beanPackage) {
        this(scanner, beanPackage, ObjectCloners::clone);
    }

    public HashMapDataStore(ClassScanner scanner, Package beanPackage, ObjectCloner objectCloner) {
        this(scanner, Collections.singleton(beanPackage), objectCloner);
    }

    public HashMapDataStore(ClassScanner scanner, Set<Package> beanPackages) {
        this(scanner, beanPackages, ObjectCloners::clone);
    }

    public HashMapDataStore(ClassScanner scanner, Set<Package> beanPackages, ObjectCloner objectCloner) {
        this.objectCloner = objectCloner;
        for (Package beanPackage : beanPackages) {
            process(scanner.getAllClasses(beanPackage.getName()));
        }
    }

    public HashMapDataStore(Collection<Class<?>> beanClasses) {
        this(beanClasses, ObjectCloners::clone);
    }

    public HashMapDataStore(Collection<Class<?>> beanClasses, ObjectCloner objectCloner) {
        this.objectCloner = objectCloner;
        process(beanClasses);
    }

    protected void process(Collection<Class<?>> beanClasses) {
        beanClasses.stream().map(ClassType::of)
                .filter(modelType -> EntityDictionary.getFirstAnnotation(modelType,
                        Arrays.asList(Include.class, Exclude.class)) instanceof Include)
                .forEach(modelType -> dataStore.put(modelType, Collections.synchronizedMap(new LinkedHashMap<>())));
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
        return new HashMapStoreTransaction(this.readWriteLock, this.dataStore, this.dictionary,
                this.typeIds, this.objectCloner, false);
    }

    @Override
    public DataStoreTransaction beginReadTransaction() {
        return new HashMapStoreTransaction(this.readWriteLock, this.dataStore, this.dictionary,
                this.typeIds, this.objectCloner, true);
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
