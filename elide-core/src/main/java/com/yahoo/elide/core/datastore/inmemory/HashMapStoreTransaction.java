/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.datastore.inmemory;

import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.exceptions.TransactionException;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.pagination.Pagination;
import com.yahoo.elide.core.sort.Sorting;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.persistence.GeneratedValue;

/**
 * HashMapDataStore transaction handler.
 */
@Slf4j
public class HashMapStoreTransaction implements DataStoreTransaction {
    private final Map<Class<?>, Map<String, Object>> dataStore;
    private final List<Operation> operations;
    private final EntityDictionary dictionary;
    private final Map<Class<?>, AtomicLong> typeIds;

    public HashMapStoreTransaction(Map<Class<?>, Map<String, Object>> dataStore,
                                   EntityDictionary dictionary, Map<Class<?>, AtomicLong> typeIds) {
        this.dataStore = dataStore;
        this.dictionary = dictionary;
        this.operations = new ArrayList<>();
        this.typeIds = typeIds;
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
        if (id == null || "null".equals(id) || "0".equals(id)) {
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
        synchronized (dataStore) {
            operations.stream()
                    .filter(op -> op.getInstance() != null)
                    .forEach(op -> {
                        Object instance = op.getInstance();
                        String id = op.getId();
                        Map<String, Object> data = dataStore.get(op.getType());
                        if (op.getDelete()) {
                            data.remove(id);
                        } else {
                            data.put(id, instance);
                        }
                    });
            operations.clear();
        }
    }

    @Override
    public void createObject(Object entity, RequestScope scope) {
        Class entityClass = entity.getClass();

        String idFieldName = dictionary.getIdFieldName(entityClass);
        String id;
        if (dictionary.getAttributeOrRelationAnnotation(entityClass, GeneratedValue.class, idFieldName) != null) {
            // TODO: Id's are not necessarily numeric.
            AtomicLong nextId;
            synchronized (dataStore) {
                nextId = typeIds.computeIfAbsent(entityClass,
                        (key) -> {
                            long maxId = dataStore.get(key).keySet().stream()
                                    .mapToLong(Long::parseLong)
                                    .max()
                                    .orElse(0);
                            return new AtomicLong(maxId + 1);
                        });
            }
            id = String.valueOf(nextId.getAndIncrement());
            setId(entity, id);
        } else {
            id = dictionary.getId(entity);
        }

        operations.add(new Operation(id, entity, entity.getClass(), false));
    }

    public void setId(Object value, String id) {
        dictionary.setValue(value, dictionary.getIdFieldName(value.getClass()), id);
    }

    @Override
    public Object getRelation(DataStoreTransaction relationTx,
                              Object entity,
                              String relationName,
                              Optional<FilterExpression> filterExpression,
                              Optional<Sorting> sorting,
                              Optional<Pagination> pagination,
                              RequestScope scope) {
        Object values = PersistentResource.getValue(entity, relationName, scope);

        // Gather list of valid id's from this parent
        Map<String, Object> idToChildResource = new HashMap<>();
        if (dictionary.getRelationshipType(entity, relationName).isToOne()) {
            if (values == null) {
                return null;
            }
            idToChildResource.put(dictionary.getId(values), values);
        } else if (values instanceof Collection) {
            idToChildResource.putAll((Map) ((Collection) values).stream()
                    .collect(Collectors.toMap(dictionary::getId,
                            Function.identity(),
                            (a, b) -> {
                                throw new TransactionException(new IllegalStateException("Duplicate key"));
                            })));
        } else {
            return null;
        }

        return idToChildResource.values();
    }

    @Override
    public Iterable<Object> loadObjects(Class<?> entityClass, Optional<FilterExpression> filterExpression,
                                        Optional<Sorting> sorting, Optional<Pagination> pagination,
                                        RequestScope scope) {
        synchronized (dataStore) {
            Map<String, Object> data = dataStore.get(entityClass);
            return data.values();
        }
    }

    @Override
    public Object loadObject(Class<?> entityClass, Serializable id,
                             Optional<FilterExpression> filterExpression,
                             RequestScope scope) {

        synchronized (dataStore) {
            Map<String, Object> data = dataStore.get(entityClass);
            if (data == null) {
                return null;
            }
            return data.get(id.toString());
        }
    }

    @Override
    public void close() throws IOException {
        operations.clear();
    }

    @Override
    public FeatureSupport supportsFiltering(Class<?> entityClass, FilterExpression expression) {
        return FeatureSupport.NONE;
    }

    @Override
    public boolean supportsSorting(Class<?> entityClass, Sorting sorting) {
        return false;
    }

    @Override
    public boolean supportsPagination(Class<?> entityClass) {
        return false;
    }
}
