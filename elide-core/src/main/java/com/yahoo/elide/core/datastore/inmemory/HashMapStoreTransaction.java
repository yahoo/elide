/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.datastore.inmemory;

import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.exceptions.TransactionException;
import com.yahoo.elide.core.filter.expression.FilterExpression;

import com.yahoo.elide.request.EntityProjection;
import com.yahoo.elide.request.Relationship;
import com.yahoo.elide.request.Sorting;
import com.yahoo.elide.utils.coerce.converters.Serde;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.persistence.GeneratedValue;

/**
 * HashMapDataStore transaction handler.
 */
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
        operations.add(new Operation(id, object, object.getClass(), Operation.OpType.UPDATE));
    }

    @Override
    public void delete(Object object, RequestScope requestScope) {
        if (object == null) {
            return;
        }

        String id = dictionary.getId(object);
        operations.add(new Operation(id, object, object.getClass(), Operation.OpType.DELETE));
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
                        if (op.getOpType() == Operation.OpType.DELETE) {
                            data.remove(id);
                        } else {
                            if (op.getOpType() == Operation.OpType.CREATE && data.get(id) != null) {
                                throw new TransactionException(new IllegalStateException("Duplicate key"));
                            }
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

        if (containsObject(entity)) {
            throw new TransactionException(new IllegalStateException("Duplicate key"));
        }

        //GeneratedValue means the DB needs to assign the ID.
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

        operations.add(new Operation(id, entity, entity.getClass(), Operation.OpType.CREATE));
    }

    public void setId(Object value, String id) {
        dictionary.setValue(value, dictionary.getIdFieldName(value.getClass()), id);
    }

    @Override
    public Object getRelation(DataStoreTransaction relationTx,
                              Object entity,
                              Relationship relationship,
                              RequestScope scope) {
        return dictionary.getValue(entity, relationship.getName(), scope);
    }

    @Override
    public Iterable<Object> loadObjects(EntityProjection projection,
                                        RequestScope scope) {
        synchronized (dataStore) {
            Map<String, Object> data = dataStore.get(projection.getType());
            return data.values();
        }
    }

    @Override
    public Object loadObject(EntityProjection projection, Serializable id, RequestScope scope) {

        EntityDictionary dictionary = scope.getDictionary();

        synchronized (dataStore) {
            Map<String, Object> data = dataStore.get(projection.getType());
            if (data == null) {
                return null;
            }
            Serde serde = dictionary.getSerdeLookup().apply(id.getClass());

            String idString = (serde == null) ? id.toString() : (String) serde.serialize(id);
            return data.get(idString);
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
    public boolean supportsPagination(Class<?> entityClass, FilterExpression expression) {
        return false;
    }

    private boolean containsObject(Object obj) {
        return dataStore.get(obj.getClass()).containsValue(obj);
    }

    @Override
    public void cancel(RequestScope scope) {
        //nothing to cancel in HashMap store transaction
    }
}
