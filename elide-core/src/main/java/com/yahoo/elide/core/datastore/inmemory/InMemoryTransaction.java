/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.datastore.inmemory;

import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.InMemoryFilterVisitor;
import com.yahoo.elide.core.pagination.Pagination;
import com.yahoo.elide.core.sort.Sorting;
import com.yahoo.elide.utils.coerce.CoerceUtil;

import javax.persistence.Id;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
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
    public void commit(RequestScope scope) {
        operations.stream()
                .filter(op -> op.getInstance() != null)
                .forEach(op -> {
                    Object instance = op.getInstance();
                    String id = op.getId();
                    ConcurrentHashMap<String, Object> data = dataStore.get(op.getType());
                    if (op.getDelete()) {
                        data.remove(id);
                    } else {
                        data.put(id, instance);
                    }
                });
        operations.clear();
    }

    @Override
    public void createObject(Object entity, RequestScope scope) {
        Class entityClass = entity.getClass();
        AtomicLong nextId = TYPEIDS.computeIfAbsent(entityClass, this::newRandomId);
        String id = String.valueOf(nextId.getAndIncrement());
        setId(entity, id);
        operations.add(new Operation(id, entity, entity.getClass(), false));
    }

    private AtomicLong newRandomId(Class<?> ignored) {
        return new AtomicLong(ThreadLocalRandom.current().nextLong());
    }

    public void setId(Object value, String id) {
        for (Class<?> cls = value.getClass(); cls != null; cls = cls.getSuperclass()) {
            for (Method method : cls.getMethods()) {
                if (method.isAnnotationPresent(Id.class) && method.getName().startsWith("get")) {
                    String setName = "set" + method.getName().substring(3);
                    for (Method setMethod : cls.getMethods()) {
                        if (setMethod.getName().equals(setName) && setMethod.getParameterCount() == 1) {
                            try {
                                setMethod.invoke(value, CoerceUtil.coerce(id, setMethod.getParameters()[0].getType()));
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

    @Override
    public Object loadObject(Class<?> entityClass, Serializable id,
                             Optional<FilterExpression> filterExpression, RequestScope scope) {
        return dataStore.get(entityClass).get(id.toString());
    }

    @Override
    public Iterable<Object> loadObjects(Class<?> entityClass, Optional<FilterExpression> filterExpression,
                                        Optional<Sorting> sorting, Optional<Pagination> pagination,
                                        RequestScope scope) {
        ConcurrentHashMap<String, Object> data = dataStore.get(entityClass);

        // Support for filtering
        if (filterExpression.isPresent()) {
            Predicate predicate = filterExpression.get().accept(new InMemoryFilterVisitor(scope));
            return (Collection<Object>) data.values().stream()
                    .filter(predicate::test)
                    .collect(Collectors.toList());
        }

        List<Object> results = new ArrayList<>();
        data.forEachValue(1, results::add);
        return results;
    }

    @Override
    public void close() throws IOException {
        operations.clear();
    }
}
