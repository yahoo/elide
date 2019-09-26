/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.engine;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.datastores.aggregation.Query;
import com.yahoo.elide.datastores.aggregation.QueryEngine;

import lombok.AccessLevel;
import lombok.Getter;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.persistence.EntityManager;

/**
 * {@link SQLEntityHydrator} hydrates the entity loaded by {@link SQLQueryEngine#executeQuery(Query)}.
 */
public class SQLEntityHydrator extends AbstractEntityHydrator {

    @Getter(AccessLevel.PRIVATE)
    private final EntityManager entityManager;

    /**
     * Constructor.
     *
     * @param results The loaded objects from {@link SQLQueryEngine#executeQuery(Query)}
     * @param query  The query passed to {@link SQLQueryEngine#executeQuery(Query)} to load the objects
     * @param entityDictionary  An object that sets entity instance values and provides entity metadata info
     * @param entityManager  An service that issues JPQL queries to load relationship objects
     */
    public SQLEntityHydrator(
            List<Object> results,
            Query query,
            EntityDictionary entityDictionary,
            EntityManager entityManager
    ) {
        super(results, query, entityDictionary);
        this.entityManager = entityManager;
    }

    @Override
    public Map<Object, Object> getRelationshipValues(Class<?> entityClass, String joinField, List<Object> joinFieldIds) {
        if (joinFieldIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Object> uniqueIds = joinFieldIds.stream().distinct().collect(Collectors.toCollection(LinkedList::new));

        List<Object> loaded = getEntityManager()
                .createQuery(
                        String.format(
                                "SELECT e FROM %s e WHERE %s IN (:idList)",
                                entityClass.getCanonicalName(),
                                getEntityDictionary().getIdFieldName(entityClass)
                        )
                )
                .setParameter("idList", uniqueIds)
                .getResultList();

        // returns a mapping as [joinId(0) -> loaded(0), joinId(1) -> loaded(1), ...]
        return IntStream.range(0, loaded.size())
                .boxed()
                .map(i -> new AbstractMap.SimpleImmutableEntry<>(uniqueIds.get(i), loaded.get(i)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
