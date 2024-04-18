/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.jpql;

import com.paiondata.elide.core.datastore.DataStore;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.type.Type;
import com.paiondata.elide.datastores.jpql.annotations.JPQLFilterFragment;
import com.paiondata.elide.datastores.jpql.filter.FilterTranslator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common DataStore functionality for stores that leverage JPQL.
 */
public interface JPQLDataStore extends DataStore {
    Logger LOGGER = LoggerFactory.getLogger(JPQLDataStore.class);

    default void bindEntityClass(Type<?> entityClass, EntityDictionary dictionary) {
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
                       LOGGER.error("Unable to instantiate the JPQL fragment generator: {}", e.getMessage());
                       throw new IllegalStateException(e);
                   }
               }
           });
    }
}
