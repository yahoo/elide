/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.queryengines.AbstractEntityHydrator;
import com.yahoo.elide.utils.coerce.CoerceUtil;
import lombok.AccessLevel;
import lombok.Getter;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

/**
 * {@link SQLEntityHydrator} hydrates the entity loaded by {@link SQLQueryEngine#executeQuery(Query, boolean)}.
 */
public class SQLEntityHydrator extends AbstractEntityHydrator {

    @Getter(AccessLevel.PRIVATE)
    private final EntityManager entityManager;

    /**
     * Constructor.
     *
     * @param results The loaded objects from {@link SQLQueryEngine#executeQuery(Query, boolean)}
     * @param query  The query passed to {@link SQLQueryEngine#executeQuery(Query, boolean)} to load the objects
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
    protected Map<Object, Object> getRelationshipValues(
            Class<?> relationshipType,
            List<Object> joinFieldIds
    ) {
        if (joinFieldIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Object> uniqueIds = joinFieldIds.stream()
                .distinct()
                .collect(Collectors.toCollection(LinkedList::new));

        List<Object> loaded = getEntityManager()
                .createQuery(
                        String.format(
                                "SELECT e FROM %s e WHERE %s IN (:idList)",
                                relationshipType.getCanonicalName(),
                                getEntityDictionary().getIdFieldName(relationshipType)
                        )
                )
                .setParameter("idList", uniqueIds)
                .getResultList();

        return loaded.stream()
                .map(obj -> new AbstractMap.SimpleImmutableEntry<>(
                        CoerceUtil.coerce(
                                (Object) getEntityDictionary().getId(obj),
                                getEntityDictionary().getIdType(relationshipType)
                        ),
                        obj))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
