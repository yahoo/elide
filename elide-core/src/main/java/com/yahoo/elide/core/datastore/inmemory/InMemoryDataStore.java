/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.datastore.inmemory;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import lombok.Getter;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import javax.persistence.Entity;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory only database.
 */
public class InMemoryDataStore implements DataStore {
    private final ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, Object>> dataStore = new ConcurrentHashMap<>();
    @Getter private EntityDictionary dictionary;
    @Getter private final Package beanPackage;

    public InMemoryDataStore(Package beanPackage) {
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
                    dataStore.put(cls, new ConcurrentHashMap<>());
                });
        this.dictionary = dictionary;
    }

    @Override
    public DataStoreTransaction beginTransaction() {
        return new InMemoryTransaction(dataStore, dictionary);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Data store contents ");
        for (Class<?> cls : dataStore.keySet()) {
            sb.append("\n Table ").append(cls).append(" contents \n");
            ConcurrentHashMap<String, Object> data = dataStore.get(cls);
            for (Map.Entry<String, Object> e : data.entrySet()) {
                sb.append(" Id: ").append(e.getKey()).append(" Value: ").append(e.getValue());
            }
        }
        return sb.toString();
    }
}
