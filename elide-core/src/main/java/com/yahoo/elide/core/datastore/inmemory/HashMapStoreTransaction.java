/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.datastore.inmemory;

import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.pagination.Pagination;
import com.yahoo.elide.core.sort.Sorting;
import com.yahoo.elide.utils.coerce.CoerceUtil;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.persistence.Id;

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
        String id = String.valueOf(nextId.getAndIncrement());
        setId(entity, id);
        operations.add(new Operation(id, entity, entity.getClass(), false));
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
                                log.error("set {}", setMethod, e);
                            }
                            return;
                        }
                    }
                }
            }
        }
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
                    .collect(Collectors.toMap(dictionary::getId, Function.identity())));
        } else {
            throw new IllegalStateException("An unexpected error occurred querying a relationship");
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
    public void close() throws IOException {
        operations.clear();
    }

    /**
     * Get the comparator for sorting.
     *
     * @param path Path to field for sorting
     * @param order Order to sort
     * @param requestScope Request scope
     * @return Comparator for sorting
     */
    private Comparator<Object> getComparator(Path path, Sorting.SortOrder order, RequestScope requestScope) {
        return (left, right) -> {
            Object leftCompare = left;
            Object rightCompare = right;

            // Drill down into path to find value for comparison
            for (Path.PathElement pathElement : path.getPathElements()) {
                leftCompare = PersistentResource.getValue(leftCompare, pathElement.getFieldName(), requestScope);
                rightCompare = PersistentResource.getValue(rightCompare, pathElement.getFieldName(), requestScope);
            }

            // Make sure value is comparable and perform comparison
            if (leftCompare instanceof Comparable) {
                int result = ((Comparable<Object>) leftCompare).compareTo(rightCompare);
                if (order == Sorting.SortOrder.asc) {
                    return result;
                }
                return -result;
            }

            throw new IllegalStateException("Trying to comparing non-comparable types!");
        };
    }
}
