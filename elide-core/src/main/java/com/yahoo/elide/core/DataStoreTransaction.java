/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.pagination.Pagination;
import com.yahoo.elide.core.sort.Sorting;
import com.yahoo.elide.security.User;
import com.yahoo.elide.security.RequestScope;

import java.io.Closeable;
import java.io.Serializable;
import java.util.Collection;
import java.util.Optional;

/**
 * Wraps the Database Transaction type.
 */
public interface DataStoreTransaction extends Closeable {

    /**
     * Wrap the opaque user.
     *
     * @param opaqueUser the opaque user
     * @return wrapped user context
     */
    default User accessUser(Object opaqueUser) {
        return new User(opaqueUser);
    }

    /**
     * Save the updated object.
     * @param entity - the object to save.
     * @param scope - contains request level metadata.
     */
    void save(Object entity, RequestScope scope);

    /**
     * Delete the object.
     * @param entity - the object to delete.
     * @param scope - contains request level metadata.
     */
    void delete(Object entity, RequestScope scope);

    /**
     * Write any outstanding entities before processing response.
     */
    default void flush(RequestScope scope) {
    }

    /**
     * End the current transaction.
     */
    void commit(RequestScope requestScope);

    /**
     * Called before commit checks are evaluated and before save, flush, and commit are called.
     * The sequence goes:
     * 1. transaction.preCommit();
     * 2. Invoke security checks evaluated at commit time
     * 3. transaction.save(...); - Invoked for every object which changed in the transaction.
     * 4. transaction.flush();
     * 5. transaction.commit();
     */
    default void preCommit() {

    }


    /**
     * Elide will create and populate the object with the attributes and relationships before
     * calling this method.  Operation security checks will be evaluated before invocation but commit
     * security checks will be called later immediately prior to `commit` being called.
     * @param entity - the object to create in the data store.
     * @param scope - contains request level metadata.
     */
    void createObject(Object entity, RequestScope scope);

    default <T> T createNewObject(Class<T> entityClass) {
        T obj = null;
        try {
            obj = entityClass.newInstance();
        } catch (java.lang.InstantiationException | IllegalAccessException e) {
            //do nothing
        }
        return obj;
    }


    /**
     * Loads an object by ID.
     * @param id - the ID of the object to load.
     * @param filterExpression - security filters that can be evaluated in the data store.
     *                         It is optional for the data store to attempt evaluation.
     * @return the loaded object if it exists AND any provided security filters pass.
     */
    Object loadObject(Class<?> entityClass,
                      Serializable id,
                      Optional<FilterExpression> filterExpression,
                      RequestScope scope);


    /**
     * Loads a collection of objects.
     * @param filterExpression - filters that can be evaluated in the data store.
     *                         It is optional for the data store to attempt evaluation.
     * @param sorting - sorting which can be pushed down to the data store.
     * @param pagination - pagination which can be pushed down to the data store.
     * @param scope - contains request level metadata.
     */
    Iterable<Object> loadObjects(
            Class<?> entityClass,
            Optional<FilterExpression> filterExpression,
            Optional<Sorting> sorting,
            Optional<Pagination> pagination,
            RequestScope scope);


    /**
     * Get total count of entity records satisfying the given filter.
     *
     * @param <T>         the type parameter
     * @param entityClass the entity class
     * @return total matching entities
     */
    default <T> Long getTotalRecords(Class<T> entityClass) {
        // default to no records
        return 0L;
    }

    /**
     * @param relationTx - The datastore that governs objects of the relationhip's type.
     * @param entity - The object which owns the relationship.
     * @param relationName - name of the relationship.
     * @param filterExpression - filtering which can be pushed down to the data store.
     *                         It is optional for the data store to attempt evaluation.
     * @param sorting - sorting which can be pushed down to the data store.
     * @param pagination - pagination which can be pushed down to the data store.
     * @param scope - contains request level metadata.
     */
    default Object getRelation(
            DataStoreTransaction relationTx,
            Object entity,
            String relationName,
            Optional<FilterExpression> filterExpression,
            Optional<Sorting> sorting,
            Optional<Pagination> pagination,
            RequestScope scope) {
        try {
            com.yahoo.elide.core.RequestScope requestScope = (com.yahoo.elide.core.RequestScope) scope;
            EntityDictionary dictionary = requestScope.getDictionary();
            Object val = PersistentResource.getValue(entity, relationName, dictionary);
            if (val instanceof Collection) {
                Collection filteredVal = (Collection) val;
                return filteredVal;
            }

            return val;
        } catch (ClassCastException e) {
            throw new ClassCastException("Fail trying to cast requestscope");
        }
    };


    /**
     * @param relationTx - The datastore that governs objects of the relationhip's type.
     * @param entity - The object which owns the relationship.
     * @param relationName - name of the relationship.
     * @param relationValue - the desired contents of the relationship.
     * @param scope - contains request level metadata.
     */
    default void setRelation(
            DataStoreTransaction relationTx,
            Object entity,
            String relationName,
            Object relationValue,
            RequestScope scope) { }

    /**
     * @param entity - The object which owns the attribute.
     * @param attributeName - name of the attribute.
     * @param filterExpression - securing filtering which can be pushed down to the data store.
     *                         It is optional for the data store to attempt evaluation.
     * @param scope - contains request level metadata.
     */
    default Object getAttribute(
            Object entity,
            String attributeName,
            Optional<FilterExpression> filterExpression,
            RequestScope scope) {
        try {
            com.yahoo.elide.core.RequestScope requestScope = (com.yahoo.elide.core.RequestScope) scope;
            Object val = PersistentResource.getValue(entity, attributeName, requestScope.getDictionary());
            return val;
        } catch (ClassCastException e) {
            throw new ClassCastException("Fail trying to cast requestscope");
        }
    };

    /**
     * @param entity - The object which owns the attribute.
     * @param attributeName - name of the attribute.
     * @param attributeValue - the desired attribute value.
     * @param scope - contains request level metadata.
     */
    default void setAttribute(
            Object entity,
            String attributeName,
            Object attributeValue,
            RequestScope scope) {
    };
}
