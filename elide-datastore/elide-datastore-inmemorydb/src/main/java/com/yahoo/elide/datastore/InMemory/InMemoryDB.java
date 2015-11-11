/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastore.InMemory;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.PersistentResource;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * Simple non-persistent in-memory database.
 */
public class InMemoryDB implements DataStore {
    private final ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, Object>> database =
            new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, AtomicLong> TYPEIDS = new ConcurrentHashMap<>();
    @Getter private EntityDictionary dictionary;
    @Getter private Package beanPackage;

    public InMemoryDB(Package beanPackage) {
        this.beanPackage = beanPackage;
    }

    /**
     * InMemoryDB transaction handler.
     */
    public class InMemoryTransaction implements DataStoreTransaction {
        private final List<Operation> operations;
        private final EntityDictionary dictionary;

        public InMemoryTransaction(EntityDictionary dictionary) {
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
                ConcurrentHashMap<String, Object> data = database.get(cls);
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
                        database.put(cls, data);
                    }
                    data.put(id, instance);
                }
            });
            operations.clear();
        }

        @Override
        public <T> T createObject(Class<T> entityClass) {
            if (database.get(entityClass) == null) {
                database.put(entityClass, new ConcurrentHashMap<>());
                TYPEIDS.put(entityClass, new AtomicLong(1));
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
                                        setMethod.invoke(value, coerce(id, setMethod.getParameters()[0].getType()));
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

        private Object coerce(Object value, Class<?> fieldClass) {
            if (value == null || fieldClass == null || fieldClass.isAssignableFrom(value.getClass())) {
                return value;
            }

            if (short.class.isAssignableFrom(fieldClass) && value instanceof String) {
                return Short.valueOf((String) value);
            }

            if (byte.class.isAssignableFrom(fieldClass) && value instanceof String) {
                return Byte.valueOf((String) value);
            }

            if (float.class.isAssignableFrom(fieldClass) && value instanceof String) {
                return Float.valueOf((String) value);
            }

            if (double.class.isAssignableFrom(fieldClass) && value instanceof String) {
                return Double.valueOf((String) value);
            }

            if (int.class.isAssignableFrom(fieldClass) && value instanceof String) {
                return Integer.valueOf((String) value);
            }

            if (long.class.isAssignableFrom(fieldClass) && value instanceof String) {
                return Long.valueOf((String) value);
            }

            if (Short.class.isAssignableFrom(fieldClass) && value instanceof String) {
                return Short.valueOf((String) value);
            }

            if (Byte.class.isAssignableFrom(fieldClass) && value instanceof String) {
                return Byte.valueOf((String) value);
            }

            if (Float.class.isAssignableFrom(fieldClass) && value instanceof String) {
                return Float.valueOf((String) value);
            }

            if (Double.class.isAssignableFrom(fieldClass) && value instanceof String) {
                return Double.valueOf((String) value);
            }

            if (Integer.class.isAssignableFrom(fieldClass) && value instanceof String) {
                return Integer.valueOf((String) value);
            }

            if (Long.class.isAssignableFrom(fieldClass) && value instanceof String) {
                return Long.valueOf((String) value);
            }

            return PersistentResource.coerce(value, fieldClass);
        }

        @Override
        public <T> T loadObject(Class<T> loadClass, String id) {
            ConcurrentHashMap<String, Object> objs = database.get(loadClass);
            if (objs == null) {
                return null;
            }
            return (T) objs.get(id);
        }

        @Override
        public <T> List<T> loadObjects(Class<T> loadClass) {
            ConcurrentHashMap<String, Object> objs = database.get(loadClass);
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

    @Override
    public void populateEntityDictionary(EntityDictionary dictionary) {
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .addUrls(ClasspathHelper.forPackage(beanPackage.getName()))
                .setScanners(new SubTypesScanner(), new TypeAnnotationsScanner()));
        for (Class<?> a : reflections.getTypesAnnotatedWith(Entity.class)) {
            if (a.getPackage().getName().startsWith(beanPackage.getName())) {
                dictionary.bindEntity(a);
            }
        }
        this.dictionary = dictionary;
    }

    @Override
    public DataStoreTransaction beginTransaction() {
        return new InMemoryTransaction(dictionary);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Database contents ");
        for (Class<?> cls : database.keySet()) {
            sb.append("\n Table " + cls + " contents \n");
            ConcurrentHashMap<String, Object> data = database.get(cls);
            for (String id : data.keySet()) {
                sb.append(" Id: " + id + " Value: " + data.get(id).toString());
            }
        }
        return sb.toString();
    }
}
