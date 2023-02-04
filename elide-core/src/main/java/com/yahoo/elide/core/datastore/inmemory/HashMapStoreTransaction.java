/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.datastore.inmemory;

import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.DataStoreIterable;
import com.yahoo.elide.core.datastore.DataStoreIterableBuilder;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.exceptions.TransactionException;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.request.Relationship;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.core.utils.coerce.converters.Serde;

import jakarta.persistence.GeneratedValue;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * HashMapDataStore transaction handler.
 */
public class HashMapStoreTransaction implements DataStoreTransaction {
    private final Map<Type<?>, Map<String, Object>> dataStore;
    private final List<Operation> operations;
    private final EntityDictionary dictionary;
    private final Map<Type<?>, AtomicLong> typeIds;

    public HashMapStoreTransaction(Map<Type<?>, Map<String, Object>> dataStore,
                                   EntityDictionary dictionary, Map<Type<?>, AtomicLong> typeIds) {
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
        operations.add(new Operation(id, object, EntityDictionary.getType(object), Operation.OpType.UPDATE));
        replicateOperationToParent(object, Operation.OpType.UPDATE);
    }

    @Override
    public void delete(Object object, RequestScope requestScope) {
        if (object == null) {
            return;
        }

        String id = dictionary.getId(object);
        operations.add(new Operation(id, object, EntityDictionary.getType(object), Operation.OpType.DELETE));
        replicateOperationToParent(object, Operation.OpType.DELETE);
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
        Type entityClass = EntityDictionary.getType(entity);

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
                nextId = getId(entityClass);
            }
            id = String.valueOf(nextId.getAndIncrement());
            setId(entity, id);
        } else {
            id = dictionary.getId(entity);
        }

        replicateOperationToParent(entity, Operation.OpType.CREATE);
        operations.add(new Operation(id, entity, EntityDictionary.getType(entity), Operation.OpType.CREATE));
    }

    public void setId(Object value, String id) {
        dictionary.setValue(value, dictionary.getIdFieldName(EntityDictionary.getType(value)), id);
    }

    @Override
    public DataStoreIterable<Object> getToManyRelation(DataStoreTransaction relationTx,
                                                       Object entity,
                                                       Relationship relationship,
                                                       RequestScope scope) {
        return new DataStoreIterableBuilder(
                (Iterable) dictionary.getValue(entity, relationship.getName(), scope)).allInMemory().build();
    }

    @Override
    public DataStoreIterable<Object> loadObjects(EntityProjection projection,
                                                          RequestScope scope) {
        synchronized (dataStore) {
            Map<String, Object> data = dataStore.get(projection.getType());
            return new DataStoreIterableBuilder<>(data.values()).allInMemory().build();
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

    private boolean containsObject(Object obj) {
        return containsObject(EntityDictionary.getType(obj), obj);
    }

    private boolean containsObject(Type<?> clazz, Object obj) {
        return dataStore.get(clazz).containsValue(obj);
    }

    @Override
    public void cancel(RequestScope scope) {
        //nothing to cancel in HashMap store transaction
    }

    private void replicateOperationToParent(Object entity, Operation.OpType opType) {
        dictionary.getSuperClassEntities(EntityDictionary.getType(entity)).stream()
            .forEach(superClass -> {
                if (opType.equals(Operation.OpType.CREATE) && containsObject(superClass, entity)) {
                    throw new TransactionException(new IllegalStateException("Duplicate key in Parent"));
                }
                String id = dictionary.getId(entity);
                operations.add(new Operation(id, entity, superClass, opType));
            });
    }

    /**
     * Get shared ID from Parent for inherited classes.
     * If not inherited, generate new ID.
     * @param entityClass Class Type of Entity
     * @return AtomicLong instance for Id generation.
     */
    private AtomicLong getId(Type<?> entityClass) {
        return dictionary.getSuperClassEntities(entityClass).stream()
                .findFirst()
                .map(this::getId)
                .orElseGet(() -> typeIds.computeIfAbsent(entityClass,
                    (key) -> {
                        long maxId = dataStore.get(key).keySet().stream()
                                .mapToLong(Long::parseLong)
                                .max()
                                .orElse(0);
                        return new AtomicLong(maxId + 1);
                    }
                ));
    }
}
