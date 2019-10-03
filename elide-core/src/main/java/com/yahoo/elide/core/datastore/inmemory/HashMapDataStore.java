/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.datastore.inmemory;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;

import com.yahoo.elide.core.datastore.test.DataStoreTestHarness;

import com.google.common.collect.Sets;
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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.persistence.Entity;

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
        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();

        for (Package beanPackage : beanPackages) {
            configurationBuilder.addUrls(ClasspathHelper.forPackage(beanPackage.getName()));
        }
        configurationBuilder.setScanners(new SubTypesScanner(), new TypeAnnotationsScanner());

        Reflections reflections = new Reflections(configurationBuilder);

        reflections.getTypesAnnotatedWith(Entity.class).stream()
                .forEach((cls) -> {
                    for (Package beanPackage : beanPackages) {
                        if (cls.getName().startsWith(beanPackage.getName())) {
                            dataStore.put(cls, Collections.synchronizedMap(new LinkedHashMap<>()));
                            break;
                        }
                    }
                });
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
