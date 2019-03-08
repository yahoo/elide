/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.datastore.inmemory;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;

import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import lombok.Getter;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.persistence.Entity;

/**
 * Simple in-memory only database.
 */
public class HashMapDataStore implements DataStore {
    private final Map<Class<?>, Map<String, Object>> dataStore = Collections.synchronizedMap(new HashMap<>());
    @Getter private EntityDictionary dictionary;
    @Getter private final Package beanPackage;
    @Getter private final ConcurrentHashMap<Class<?>, AtomicLong> typeIds = new ConcurrentHashMap<>();

    public HashMapDataStore(Package beanPackage) {
        this.beanPackage = beanPackage;
    }

    @Override
    public void populateEntityDictionary(EntityDictionary dictionary) {
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .addUrls(ClasspathHelper.forPackage(beanPackage.getName()))
                .setScanners(new SubTypesScanner(), new TypeAnnotationsScanner()));
        reflections.getTypesAnnotatedWith(Entity.class).stream()
                .filter(entityAnnotatedClass -> entityAnnotatedClass.getPackage().getName()
                        .startsWith(beanPackage.getName()))
                .forEach((cls) -> {
                    dictionary.bindEntity(cls);
                    dataStore.put(cls, Collections.synchronizedMap(new LinkedHashMap<>()));
                });
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
        for (Class<?> cls : dataStore.keySet()) {
            sb.append("\n Table ").append(cls).append(" contents \n");
            Map<String, Object> data = dataStore.get(cls);
            for (Map.Entry<String, Object> e : data.entrySet()) {
                sb.append(" Id: ").append(e.getKey()).append(" Value: ").append(e.getValue());
            }
        }
        return sb.toString();
    }

    @Override
    public boolean supportsFiltering() {
        return false;
    }

    @Override
    public boolean supportsSorting() {
        return false;
    }

    @Override
    public boolean supportsPagination() {
        return false;
    }
}
