/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.datastore;

import com.paiondata.elide.core.Path;
import com.paiondata.elide.core.PersistentResource;
import com.paiondata.elide.core.RequestScope;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.dictionary.Injector;
import com.paiondata.elide.core.exceptions.InvalidObjectIdentifierException;
import com.paiondata.elide.core.filter.expression.AndFilterExpression;
import com.paiondata.elide.core.filter.expression.FilterExpression;
import com.paiondata.elide.core.filter.predicates.InPredicate;
import com.paiondata.elide.core.request.Attribute;
import com.paiondata.elide.core.request.EntityProjection;
import com.paiondata.elide.core.request.Relationship;
import com.paiondata.elide.core.type.ParameterizedModel;
import com.paiondata.elide.core.type.Type;

import java.io.Closeable;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Set;
/**
 * Wraps the Database Transaction type.
 */
public interface DataStoreTransaction extends Closeable {
    /**
     * Save the updated object.
     *
     * @param entity - the object to save.
     * @param scope - contains request level metadata.
     * @param <T> The model type being saved.
     */
    <T> void save(T entity, RequestScope scope);

    /**
     * Delete the object.
     *
     * @param entity - the object to delete.
     * @param scope - contains request level metadata.
     * @param <T> The model type being deleted.
     */
    <T> void delete(T entity, RequestScope scope);

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
    default void preCommit(RequestScope scope) {
    }

    /**
     * Elide will create and populate the object with the attributes and relationships before
     * calling this method.  Operation security checks will be evaluated before invocation but commit
     * security checks will be called later immediately prior to `commit` being called.
     *
     * @param entity - the object to create in the data store.
     * @param scope - contains request level metadata.
     * @param <T> The model type being created.
     */
    <T> void createObject(T entity, RequestScope scope);

    /**
     * Create a new instance of an object.
     *
     * @param entityClass the class
     * @param <T> the model type to create
     * @return a new instance of type T
     */
    default <T> T createNewObject(Type<T> entityClass, RequestScope scope) {
        Injector injector = scope.getDictionary().getInjector();

        T obj;
        if (entityClass.getUnderlyingClass().isPresent()) {
            obj = injector.instantiate(entityClass.getUnderlyingClass().get());
        } else {
            try {
                obj = entityClass.newInstance();
            } catch (java.lang.InstantiationException | IllegalAccessException e) {
                obj = null;
            }
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
     * @param <T> The model type being loaded.
     * It is optional for the data store to attempt evaluation.
     * @return the loaded object if it exists AND any provided security filters pass.
     */
    default <T> T loadObject(EntityProjection entityProjection,
                             Serializable id,
                             RequestScope scope) {
        Type<?> entityClass = entityProjection.getType();
        FilterExpression filterExpression = entityProjection.getFilterExpression();

        EntityDictionary dictionary = scope.getDictionary();
        Type idType = dictionary.getIdType(entityClass);
        String idField = dictionary.getIdFieldName(entityClass);
        FilterExpression idFilter = new InPredicate(
                new Path.PathElement(entityClass, idType, idField),
                id
        );
        FilterExpression joinedFilterExpression = (filterExpression != null)
                ? new AndFilterExpression(idFilter, filterExpression)
                : idFilter;

        Iterable<T> results = loadObjects(entityProjection.copyOf()
                .filterExpression(joinedFilterExpression)
                .build(),
                scope);

        Iterator<T> it = results == null ? null : results.iterator();
        if (it != null && it.hasNext()) {
            T obj = it.next();
            if (!it.hasNext()) {
              return obj;
            }

            //Multiple objects with the same ID.
            throw new InvalidObjectIdentifierException(id.toString(), dictionary.getJsonAliasFor(entityClass));
        }
        return null;
    }

    /**
     * Loads a collection of objects.
     *
     * @param entityProjection - the class to load
     * @param scope - contains request level metadata.
     * @param <T> - The model type being loaded.
     * @return a collection of the loaded objects
     */
    <T> DataStoreIterable<T> loadObjects(
            EntityProjection entityProjection,
            RequestScope scope);

    /**
     * Retrieve a to-many relation from an object.
     *
     * @param relationTx - The datastore that governs objects of the relationhip's type.
     * @param entity - The object which owns the relationship.
     * @param relationship - the relationship to fetch.
     * @param scope - contains request level metadata.
     * @param <T> - The model type which owns the relationship.
     * @param <R> - The model type of the relationship.
     * @return the object in the relation
     */
    default <T, R> DataStoreIterable<R> getToManyRelation(
            DataStoreTransaction relationTx,
            T entity,
            Relationship relationship,
            RequestScope scope) {

        return new DataStoreIterableBuilder(
                (Iterable) PersistentResource.getValue(entity, relationship.getName(), scope)).allInMemory().build();
    }

    /**
     * Retrieve a to-one relation from an object.
     *
     * @param relationTx - The datastore that governs objects of the relationhip's type.
     * @param entity - The object which owns the relationship.
     * @param relationship - the relationship to fetch.
     * @param scope - contains request level metadata.
     * @param <T> - The model type which owns the relationship.
     * @param <R> - The model type of the relationship.
     * @return the object in the relation
     */
    default <T, R> R getToOneRelation(
            DataStoreTransaction relationTx,
            T entity,
            Relationship relationship,
            RequestScope scope) {

        return (R) PersistentResource.getValue(entity, relationship.getName(), scope);
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
     * @param <T> - The model type which owns the relationship.
     * @param <R> - The model type of the relationship.
     */
    default <T, R> void updateToManyRelation(DataStoreTransaction relationTx,
                                      T entity,
                                      String relationName,
                                      Set<R> newRelationships,
                                      Set<R> deletedRelationships,
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
     * @param <T> - The model type which owns the relationship.
     * @param <R> - The model type of the relationship.
     */
    default <T, R> void updateToOneRelation(DataStoreTransaction relationTx,
                                     T entity,
                                     String relationName,
                                     R relationshipValue,
                                     RequestScope scope) {
    }

    /**
     * Get an attribute from an object.
     *
     * @param entity - The object which owns the attribute.
     * @param attribute - The attribute to fetch
     * @param scope - contains request level metadata.
     * @param <T> - The model type which owns the attribute.
     * @param <R> - The type of the attribute.
     * @return the value of the attribute
     */
    default <T, R> R getAttribute(T entity,
                                  Attribute attribute,
                                  RequestScope scope) {
        if (entity instanceof ParameterizedModel) {
            return ((ParameterizedModel) entity).invoke(attribute);
        }
        return (R) PersistentResource.getValue(entity, attribute.getName(), scope);

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
     * @param <T> - The model type which owns the attribute.
     */
    default <T> void setAttribute(T entity,
                                  Attribute attribute,
                                  RequestScope scope) {
    }

    /**
     * Cancel running transaction.
     * Implementation must be thread-safe.
     * @param scope contains request level metadata.
     */
    void cancel(RequestScope scope);

    /**
     * Returns a data store transaction property.
     * @param propertyName The property name.
     * @param <T> The type of the property.
     * @return The property.
     */
    default <T> T getProperty(String propertyName) {
        return null;
    }
}
