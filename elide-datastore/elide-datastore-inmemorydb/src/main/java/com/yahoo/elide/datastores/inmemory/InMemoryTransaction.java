/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.inmemory;

import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.utils.coerce.CoerceUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.persistence.Id;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * InMemoryDataStore transaction handler.
 */
public class InMemoryTransaction implements DataStoreTransaction {
    private static final ConcurrentHashMap<Class<?>, AtomicLong> TYPEIDS = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, Object>> dataStore;
    private final List<Operation> operations;
    private final EntityDictionary dictionary;

    public InMemoryTransaction(ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, Object>> dataStore,
                               EntityDictionary dictionary) {
        this.dataStore = dataStore;
        this.dictionary = dictionary;
        this.operations = new ArrayList<>();
    }

    @Override
    public void flush() {
        // Do nothing
    }

    @Override
    public void save(Object object) {
        if (object == null) {
            return;
        }
        String id = dictionary.getId(object);
        if (id.equals("0")) {
            setId(object, dictionary.getId(createObject(object.getClass())));
        }
        id = dictionary.getId(object);
        operations.add(new Operation(id, object, object.getClass(), false));
    }

    @Override
    public void delete(Object object) {
        if (object == null) {
            return;
        }
        String id = dictionary.getId(object);
        operations.add(new Operation(id, object, object.getClass(), true));
    }

    @Override
    public void commit() {
        operations.forEach(op -> {
            Class<?> cls = op.getType();
            ConcurrentHashMap<String, Object> data = dataStore.get(cls);
            Object instance = op.getInstance();
            if (instance == null) {
                return;
            }
            String id = op.getId();
            if (op.getDelete()) {
                if (data != null) {
                    data.remove(id);
                }
            } else {
                if (data == null) {
                    data = new ConcurrentHashMap<>();
                    dataStore.put(cls, data);
                }
                data.put(id, instance);
            }
        });
        operations.clear();
    }

    @Override
    public <T> T createObject(Class<T> entityClass) {
        if (dataStore.get(entityClass) == null) {
            dataStore.putIfAbsent(entityClass, new ConcurrentHashMap<>());
            TYPEIDS.putIfAbsent(entityClass, new AtomicLong(1));
        }
        AtomicLong idValue = TYPEIDS.get(entityClass);
        String id = String.valueOf(idValue.getAndIncrement());
        try {
            T instance = entityClass.newInstance();
            setId(instance, id);
            return instance;
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setId(Object value, String id) {
        for (Class<?> cls = value.getClass(); cls != null; cls = cls.getSuperclass()) {
            for (Method method : cls.getMethods()) {
                if (method.isAnnotationPresent(Id.class)) {
                    if (method.getName().startsWith("get")) {
                        String setName = "set" + method.getName().substring(3);
                        for (Method setMethod : cls.getMethods()) {
                            if (setMethod.getName().equals(setName) && setMethod.getParameterCount() == 1) {
                                try {
                                    setMethod.invoke(value,
                                            CoerceUtil.coerce(id, setMethod.getParameters()[0].getType()));
                                } catch (ReflectiveOperationException e) {
                                    e.printStackTrace();
                                }
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public <T> T loadObject(Class<T> loadClass, Serializable id) {
        ConcurrentHashMap<String, Object> objs = dataStore.get(loadClass);
        if (objs == null) {
            return null;
        }
        return (T) objs.get(id.toString());
    }

    @Override
    public <T> List<T> loadObjects(Class<T> loadClass) {
        ConcurrentHashMap<String, Object> objs = dataStore.get(loadClass);
        if (objs == null) {
            return Collections.emptyList();
        }
        List<Object> results = new ArrayList<>();
        objs.forEachValue(1, results::add);
        return (List<T>) results;
    }

    @Override
    public void close() throws IOException {
        operations.clear();
    }

    @AllArgsConstructor
    private class Operation {
        @Getter private final String id;
        @Getter private final Object instance;
        @Getter private final Class<?> type;
        @Getter private final Boolean delete;
    }
}
