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
import lombok.extern.slf4j.Slf4j;

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
@Slf4j
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

    /**
     * Do nothing.
     *
     * @param requestScope ignored in this method
     */
    @Override
    public void flush(RequestScope requestScope) {
        // Do nothing
    }

    /**
     * Queue an insert into the backing store.
     *
     * @param object the object to insert
     * @param requestScope ignored in this method
     */
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

    /**
     * Remove an object from the backing store.
     *
     * @param object the object to remove
     * @param requestScope ignored in this method
     */
    @Override
    public void delete(Object object, RequestScope requestScope) {
        if (object == null) {
            return;
        }

        String id = dictionary.getId(object);
        operations.add(new Operation(id, object, object.getClass(), true));
    }

    /**
     * Insert any object into the backing store as needed.
     *
     * @param scope ignored in this method
     */
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

    /**
     * Persist an object.
     *
     * @param entity - the object to create in the data store.
     * @param scope - contains request level metadata.
     */
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

    /**
     * Set the id of an object.
     *
     * @param value the object
     * @param id the new id value
     */
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
                                log.error("set {}", setMethod, e);
                            }
                            return;
                        }
                    }
                }
            }
        }
    }

    /**
     * Fetch an object from the backing store.
     *
     * @param entityClass the type of class to load
     * @param id - the ID of the object to load.
     * @param filterExpression - security filters that can be evaluated in the data store.
     * @param scope ignored in this method
     * @return the loaded object
     */
    @Override
    public Object loadObject(Class<?> entityClass, Serializable id,
                             Optional<FilterExpression> filterExpression, RequestScope scope) {
        return dataStore.get(entityClass).get(id.toString());
    }

    /**
     * Load a collection of objects from the backing store.
     *
     * @param entityClass - the class to load
     * @param filterExpression - filters that can be evaluated in the data store.
     * It is optional for the data store to attempt evaluation.
     * @param sorting - sorting which can be pushed down to the data store.
     * @param pagination - pagination which can be pushed down to the data store.
     * @param scope - ignored in this method
     * @return the loaded objects
     */
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

    /**
     * Clear any pending operations.
     *
     * @throws IOException never
     */
    @Override
    public void close() throws IOException {
        operations.clear();
    }
}
