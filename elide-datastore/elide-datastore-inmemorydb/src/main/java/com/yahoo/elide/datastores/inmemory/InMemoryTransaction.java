/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.inmemory;

import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.filter.expression.InMemoryFilterVisitor;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.pagination.Pagination;
import com.yahoo.elide.core.sort.Sorting;
import com.yahoo.elide.utils.coerce.CoerceUtil;

import javax.persistence.Id;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
    public void flush(RequestScope requestScope) {
        // Do nothing
    }

    @Override
    public void save(Object object, RequestScope requestScope) {
        if (object == null) {
            return;
        }
        String id = dictionary.getId(object);
        if (id == null || id.equals("null") || id.equals("0")) {
            createObject(object, requestScope);
        }
        id = dictionary.getId(object);
        operations.add(new Operation(id, object, object.getClass(), false));
    }

    @Override
    public void delete(Object object, RequestScope requestScope) {
        if (object == null) {
            return;
        }
        String id = dictionary.getId(object);
        operations.add(new Operation(id, object, object.getClass(), true));
    }

    @Override
    public void commit(RequestScope requestScope) {
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
    public void createObject(Object entity, RequestScope scope) {
        Class entityClass = entity.getClass();
        if (dataStore.get(entityClass) == null) {
            dataStore.putIfAbsent(entityClass, new ConcurrentHashMap<>());
        }
        AtomicLong idValue = TYPEIDS.computeIfAbsent(entityClass, this::newRandomId);
        String id = String.valueOf(idValue.getAndIncrement());
        setId(entity, id);
        operations.add(new Operation(id, entity, entity.getClass(), false));
    }

    private AtomicLong newRandomId(Class<?> entityClass) {
        return new AtomicLong(new Random().nextLong());
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
    public Object loadObject(Class<?> entityClass,
                             Serializable id,
                             Optional<FilterExpression> filterExpression,
                             RequestScope scope) {
        ConcurrentHashMap<String, Object> objs = dataStore.get(entityClass);
        if (objs == null) {
            return null;
        }
        return objs.get(id.toString());
    }

    @Override
    public Iterable<Object> loadObjects(
            Class<?> entityClass,
            Optional<FilterExpression> filterExpression,
            Optional<Sorting> sorting,
            Optional<Pagination> pagination,
            RequestScope scope) {
        ConcurrentHashMap<String, Object> objs = dataStore.get(entityClass);
        if (objs == null) {
            return Collections.emptyList();
        }
        // Support for filtering
        if (filterExpression.isPresent()) {
            Predicate predicate = filterExpression.get()
                    .accept(new InMemoryFilterVisitor((com.yahoo.elide.core.RequestScope) scope));
            return (Collection) objs.values().stream()
                    .filter(predicate::test)
                    .collect(Collectors.toList());
        }
        List<Object> results = new ArrayList<>();
        objs.forEachValue(1, results::add);
        return results;
    }

    @Override
    public void close() throws IOException {
        operations.clear();
    }

    @Override
    public <T> Long getTotalRecords(Class<T> entityClass) {
        ConcurrentHashMap<String, Object> objs = dataStore.get(entityClass);
        return objs == null ? 0L : objs.size();
    }
}
