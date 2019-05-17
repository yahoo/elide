/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.datastore;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.annotations.JPQLFilterFragment;
import com.yahoo.elide.core.filter.FilterTranslator;

/**
 * Common DataStore functionality for stores that leverage JPQL.
 */
public interface JPQLDataStore extends DataStore {

    default void bindEntityClass(Class<?> entityClass, EntityDictionary dictionary) {


        try {
            // Ignore this result. We are just checking to see if it throws an exception meaning that
            // provided class was _not_ an entity.
            dictionary.lookupEntityClass(entityClass);
        } catch (IllegalArgumentException e)  {
            // Ignore this entity
            // Turns out that JPA may include non-entity types in this list when using things
            // like envers. Since they are not entities, we do not want to bind them into the entity
            // dictionary
            return;
        }

        //Register the entity class in the Entity Dictionary
        dictionary.bindEntity(entityClass);

        //Search for JPQFilterFragment annotations and register them.
        dictionary.getAttributes(entityClass)
           .stream()
           .forEach((attribute) -> {
               JPQLFilterFragment annotation = dictionary.getAttributeOrRelationAnnotation(entityClass,
                       JPQLFilterFragment.class, attribute);

               if (annotation != null) {
                   try {
                       FilterTranslator.registerJPQLGenerator(annotation.operator(),
                               entityClass, attribute, annotation.generator().newInstance());
                   } catch (InstantiationException | IllegalAccessException e) {
                       throw new IllegalStateException(e);
                   }
               }
           });
    }
}
