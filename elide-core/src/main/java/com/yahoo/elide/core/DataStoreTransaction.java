/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import com.yahoo.elide.core.filter.InPredicate;
import com.yahoo.elide.core.filter.expression.AndFilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.request.Attribute;
import com.yahoo.elide.request.EntityProjection;
import com.yahoo.elide.request.Relationship;
import com.yahoo.elide.request.Sorting;

import java.io.Closeable;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Set;
/**
 * Wraps the Database Transaction type.
 */
public interface DataStoreTransaction extends Closeable {

    /**
     * The extent to which the transaction supports a particular feature.
     */
    public enum FeatureSupport {
        FULL,
        PARTIAL,
        NONE
    }
    /**
     * Save the updated object.
     *
     * @param entity - the object to save.
     * @param scope - contains request level metadata.
     */
    void save(Object entity, RequestScope scope);

    /**
     * Delete the object.
     *
     * @param entity - the object to delete.
     * @param scope - contains request level metadata.
     */
    void delete(Object entity, RequestScope scope);

    /**
     * Write any outstanding entities before processing response.
     *
     * @param scope the request scope for the current request
     */
    void flush(RequestScope scope);

    /**
     * End the current transaction.
     *
     * @param scope the request scope for the current request
     */
    void commit(RequestScope scope);

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
     *
     * @param entity - the object to create in the data store.
     * @param scope - contains request level metadata.
     */
    void createObject(Object entity, RequestScope scope);

    /**
     * Create a new instance of an object.
     *
     * @param entityClass the class
     * @param <T> the class to create
     * @return a new instance of type T
     */
    default <T> T createNewObject(Class<T> entityClass) {
        T obj;
        try {
            obj = entityClass.newInstance();
        } catch (java.lang.InstantiationException | IllegalAccessException e) {
            obj = null;
        }
        return obj;
    }

    /**
     * Loads an object by ID.  The reason we support both load by ID and load by filter is that
     * some legacy stores are optimized to load by ID.
     *
     * @param entityProjection the collection to load.
     * @param id - the ID of the object to load.
     * @param scope - the current request scope
     * It is optional for the data store to attempt evaluation.
     * @return the loaded object if it exists AND any provided security filters pass.
     */
    default Object loadObject(EntityProjection entityProjection,
                              Serializable id,
                              RequestScope scope) {
        Class<?> entityClass = entityProjection.getType();
        FilterExpression filterExpression = entityProjection.getFilterExpression();

        EntityDictionary dictionary = scope.getDictionary();
        Class idType = dictionary.getIdType(entityClass);
        String idField = dictionary.getIdFieldName(entityClass);
        FilterExpression idFilter = new InPredicate(
                new Path.PathElement(entityClass, idType, idField),
                id
        );
        FilterExpression joinedFilterExpression = (filterExpression != null)
                ? new AndFilterExpression(idFilter, filterExpression)
                : idFilter;

        Iterable<Object> results = loadObjects(entityProjection.copyOf()
                .filterExpression(joinedFilterExpression)
                .build(),
                scope);

        Iterator<Object> it = results == null ? null : results.iterator();
        if (it != null && it.hasNext()) {
            return it.next();
        }
        return null;
    }

    /**
     * Loads a collection of objects.
     *
     * @param entityProjection - the class to load
     * @param scope - contains request level metadata.
     * @return a collection of the loaded objects
     */
    Iterable<Object> loadObjects(
            EntityProjection entityProjection,
            RequestScope scope);

    /**
     * Retrieve a relation from an object.
     *
     * @param relationTx - The datastore that governs objects of the relationhip's type.
     * @param entity - The object which owns the relationship.
     * @param relationship - the relationship to fetch.
     * @param scope - contains request level metadata.
     * @return the object in the relation
     */
    default Object getRelation(
            DataStoreTransaction relationTx,
            Object entity,
            Relationship relationship,
            RequestScope scope) {

        return PersistentResource.getValue(entity, relationship.getName(), scope);
    }

    /**
     * Elide core will update the in memory representation of the objects to the requested state.
     * These functions allow a data store to optionally persist the relationship if needed.
     *
     * @param relationTx - The datastore that governs objects of the relationhip's type.
     * @param entity - The object which owns the relationship.
     * @param relationName - name of the relationship.
     * @param newRelationships - the set of the added relationship to the collection.
     * @param deletedRelationships - the set of the deleted relationship to the collection.
     * @param scope - contains request level metadata.
     */
    default void updateToManyRelation(DataStoreTransaction relationTx,
                                      Object entity,
                                      String relationName,
                                      Set<Object> newRelationships,
                                      Set<Object> deletedRelationships,
                                      RequestScope scope) {
    }

    /**
     * Elide core will update the in memory representation of the objects to the requested state.
     * These functions allow a data store to optionally persist the relationship if needed.
     *
     * @param relationTx - The datastore that governs objects of the relationhip's type.
     * @param entity - The object which owns the relationship.
     * @param relationName - name of the relationship.
     * @param relationshipValue - the new value of the updated one-to-one relationship
     * @param scope - contains request level metadata.
     */
    default void updateToOneRelation(DataStoreTransaction relationTx,
                                     Object entity,
                                     String relationName,
                                     Object relationshipValue,
                                     RequestScope scope) {
    }

    /**
     * Get an attribute from an object.
     *
     * @param entity - The object which owns the attribute.
     * @param attribute - The attribute to fetch
     * @param scope - contains request level metadata.
     * @return the value of the attribute
     */
    default Object getAttribute(Object entity,
                                Attribute attribute,
                                RequestScope scope) {
        return PersistentResource.getValue(entity, attribute.getName(), scope);

    }

    /**
     * Set an attribute on an object in the data store.
     * <p>
     * Elide core will update the in memory representation of the objects to the requested state.
     * This function allow a data store to optionally persist the attribute if needed.
     *
     * @param entity - The object which owns the attribute.
     * @param attribute - the attribute to set.
     * @param scope - contains request level metadata.
      */
    default void setAttribute(Object entity,
                              Attribute attribute,
                              RequestScope scope) {
    }

    /**
     * Whether or not the transaction can filter the provided class with the provided expression.
     * @param entityClass The class to filter
     * @param expression The filter expression
     * @return FULL, PARTIAL, or NONE
     */
    default FeatureSupport supportsFiltering(Class<?> entityClass, FilterExpression expression) {
        return FeatureSupport.FULL;
    }

    /**
     * Whether or not the transaction can sort the provided class.
     * @param entityClass The model type
     * @param sorting Whether or not the store supports sorting for the given model
     * @return true if sorting is possible
     */
    default boolean supportsSorting(Class<?> entityClass, Sorting sorting) {
        return true;
    }

    /**
     * Whether or not the transaction can paginate the provided class.
     * @param entityClass The entity class that is being paged.
     * @param expression The filter expression
     * @return true if pagination is possible
     */
    default boolean supportsPagination(Class<?> entityClass, FilterExpression expression) {
        return true;
    }

    /**
     * Cancel running transaction.
     * Implementation must be thread-safe.
     * @param scope contains request level metadata.
     */
    void cancel(RequestScope scope);
}
