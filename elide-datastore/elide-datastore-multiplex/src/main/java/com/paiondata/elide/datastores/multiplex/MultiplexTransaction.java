/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.multiplex;

import com.paiondata.elide.core.RequestScope;
import com.paiondata.elide.core.datastore.DataStore;
import com.paiondata.elide.core.datastore.DataStoreIterable;
import com.paiondata.elide.core.datastore.DataStoreTransaction;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.exceptions.InvalidCollectionException;
import com.paiondata.elide.core.filter.Operator;
import com.paiondata.elide.core.filter.expression.FilterExpression;
import com.paiondata.elide.core.filter.expression.PredicateExtractionVisitor;
import com.paiondata.elide.core.filter.predicates.FilterPredicate;
import com.paiondata.elide.core.request.Attribute;
import com.paiondata.elide.core.request.EntityProjection;
import com.paiondata.elide.core.request.Relationship;
import com.paiondata.elide.core.type.ClassType;
import com.paiondata.elide.core.type.Type;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.ListIterator;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Multiplex transaction handler.  Process each sub-database transactions within a single transaction.
 * If any commit fails in process, reverse any commits already completed.
 */
public abstract class MultiplexTransaction implements DataStoreTransaction {
    protected LinkedHashMap<DataStore, DataStoreTransaction> transactions;
    protected final MultiplexManager multiplexManager;

    /**
     * Multiplex transaction handler.
     * @param multiplexManager associated manager
     */
    public MultiplexTransaction(MultiplexManager multiplexManager) {
        this.multiplexManager = multiplexManager;
        this.transactions = new LinkedHashMap<>(multiplexManager.dataStores.size());
    }

    protected abstract DataStoreTransaction beginTransaction(DataStore dataStore);

    @Override
    public <T> void createObject(T entity, RequestScope scope) {
        getTransaction(EntityDictionary.getType(entity)).createObject(entity, scope);
    }

    @Override
    public <T> T loadObject(EntityProjection projection,
                             Serializable id,
                             RequestScope scope) {
        return getTransaction(projection.getType()).loadObject(projection, id, scope);
    }

    @Override
    public <T> DataStoreIterable<T> loadObjects(
            EntityProjection projection,
            RequestScope scope) {
        return getTransaction(projection.getType()).loadObjects(projection, scope);
    }

    @Override
    public void flush(RequestScope scope) {
        processTransactions(dataStoreTransaction -> dataStoreTransaction.flush(scope));
    }

    @Override
    public void preCommit(RequestScope scope) {
        processTransactions(dataStoreTransaction -> dataStoreTransaction.preCommit(scope));
    }

    @Override
    public void commit(RequestScope scope) {
        // flush all before commit
        flush(scope);
        processTransactions(dataStoreTransaction -> dataStoreTransaction.commit(scope));
    }

    /**
     * Processes the transactions in reverse order and is non null.
     *
     * @param processor process the transaction
     */
    protected void processTransactions(Consumer<DataStoreTransaction> processor) {
        // Transactions must be processed in reverse order
        ListIterator<DataStoreTransaction> iterator = new ArrayList<>(transactions.values())
                .listIterator(transactions.size());
        while (iterator.hasPrevious()) {
            DataStoreTransaction dataStoreTransaction = iterator.previous();
            if (dataStoreTransaction != null) {
                processor.accept(dataStoreTransaction);
            }
        }
    }

    @Override
    public void close() throws IOException {

        IOException cause = null;

        // Transactions must be processed in reverse order
        ListIterator<DataStoreTransaction> iterator = new ArrayList<>(transactions.values())
                .listIterator(transactions.size());
        while (iterator.hasPrevious()) {
            DataStoreTransaction dataStoreTransaction = iterator.previous();
            if (dataStoreTransaction != null) {
                try {
                    dataStoreTransaction.close();
                } catch (IOException | Error | RuntimeException e) {
                    if (cause != null) {
                        cause.addSuppressed(e);
                    } else if (e instanceof IOException ioException) {
                        cause = ioException;
                    } else {
                        cause = new IOException(e);
                    }
                }
            }
        }
        transactions.clear();
        if (cause != null) {
            throw cause;
        }
    }

    protected DataStoreTransaction getTransaction(Object object) {
        return getTransaction(ClassType.of(object.getClass()));
    }

    protected DataStoreTransaction getTransaction(Type<?> cls) {
        DataStore lookupDataStore = this.multiplexManager.getSubManager(cls);

        DataStoreTransaction transaction = transactions.get(lookupDataStore);
        if (transaction == null) {
            for (DataStore dataStore : multiplexManager.dataStores) {
                if (dataStore.equals(lookupDataStore)) {
                    transaction = beginTransaction(dataStore);
                    transactions.put(dataStore, transaction);
                    break;
                }
            }
            if (transaction == null) {
                throw new InvalidCollectionException(cls.getName());
            }
        }
        return transaction;
    }

    protected DataStoreTransaction getRelationTransaction(Object object, String relationName) {
        EntityDictionary dictionary = multiplexManager.getDictionary();
        Type<?> relationClass = dictionary.getParameterizedType(EntityDictionary.getType(object), relationName);
        return getTransaction(relationClass);
    }

    @Override
    public <T, R> DataStoreIterable<R> getToManyRelation(
            DataStoreTransaction tx,
            T entity,
            Relationship relation,
            RequestScope scope
    ) {
        DataStoreTransaction relationTx = getRelationTransaction(entity, relation.getName());
        Type<Object> entityType = EntityDictionary.getType(entity);
        DataStoreTransaction entityTransaction = getTransaction(entityType);

        return entityTransaction.getToManyRelation(relationTx, entity, relation, scope);
    }

    @Override
    public <T, R> R getToOneRelation(
            DataStoreTransaction tx,
            T entity,
            Relationship relation,
            RequestScope scope
    ) {
        DataStoreTransaction relationTx = getRelationTransaction(entity, relation.getName());
        Type<Object> entityType = EntityDictionary.getType(entity);
        DataStoreTransaction entityTransaction = getTransaction(entityType);

        return entityTransaction.getToOneRelation(relationTx, entity, relation, scope);
    }

    @Override
    public <T, R> void updateToManyRelation(DataStoreTransaction tx,
                                     T entity, String relationName,
                                     Set<R> newRelationships,
                                     Set<R> deletedRelationships,
                                     RequestScope scope) {
        DataStoreTransaction relationTx = getRelationTransaction(entity, relationName);
        DataStoreTransaction entityTransaction = getTransaction(EntityDictionary.getType(entity));
        entityTransaction.updateToManyRelation(relationTx, entity, relationName,
                newRelationships, deletedRelationships, scope);
    }

    @Override
    public <T, R> void updateToOneRelation(DataStoreTransaction tx, T entity,
                                    String relationName, R relationshipValue, RequestScope scope) {
        DataStoreTransaction relationTx = getRelationTransaction(entity, relationName);
        DataStoreTransaction entityTransaction = getTransaction(EntityDictionary.getType(entity));
        entityTransaction.updateToOneRelation(relationTx, entity, relationName, relationshipValue, scope);
    }

    @Override
    public <T, R> R getAttribute(T entity, Attribute attribute, RequestScope scope) {
        DataStoreTransaction transaction = getTransaction(EntityDictionary.getType(entity));
        return transaction.getAttribute(entity, attribute, scope);
    }

    @Override
    public <T> void setAttribute(T entity, Attribute attribute, RequestScope scope) {
        DataStoreTransaction transaction = getTransaction(EntityDictionary.getType(entity));
        transaction.setAttribute(entity, attribute, scope);
    }

    private Serializable extractId(FilterExpression filterExpression,
                                   String idFieldName,
                                   Type<?> relationClass) {
        Collection<FilterPredicate> predicates = filterExpression.accept(new PredicateExtractionVisitor());
        return predicates.stream()
                .filter(p -> p.getEntityType() == relationClass && p.getOperator() == Operator.IN)
                .filter(p -> p.getValues().size() == 1)
                .filter(p -> p.getField().equals(idFieldName))
                .findFirst()
                .map(p -> (Serializable) p.getValues().get(0))
                .orElse(null);
    }

    @Override
    public void cancel(RequestScope scope) {
        transactions.values().forEach(dataStoreTransaction -> dataStoreTransaction.cancel(scope));
    }

    @Override
    public <T> T getProperty(String propertyName) {
        DataStore matchingStore = multiplexManager.dataStores.stream()
                .filter(store -> propertyName.startsWith(store.getClass().getPackage().getName()))
                .findFirst().orElse(null);

        //Data store transaction properties must be prefixed with their package name.
        if (matchingStore == null) {
            return null;
        }

        if (! transactions.containsKey(matchingStore)) {
            DataStoreTransaction tx = beginTransaction(matchingStore);
            transactions.put(matchingStore, tx);
        }

        return transactions.get(matchingStore).getProperty(propertyName);
    }
}
