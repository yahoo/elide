package com.yahoo.elide.datastores.aggregation.engine;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.datastores.aggregation.Query;

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

public class SQLEntityHydrator extends AbstractEntityHydrator {

    @Getter(AccessLevel.PRIVATE)
    private final EntityManager entityManager;

    public SQLEntityHydrator(
            final List<Object> results,
            final Query query,
            final EntityDictionary entityDictionary,
            final EntityManager entityManager
    ) {
        super(results, query, entityDictionary);
        this.entityManager = entityManager;
    }

    /**
     * Returns a list of relationship objects whose ID matches a specified list of ID's
     * <p>
     * Note the relationship cannot be toMany. Regardless of ID duplicates, this method returns the list of relationship
     * objects in the same size as the specified list of ID's, i.e. duplicate ID mapps to the same object in the
     * returned list. For example:
     * <p>
     * if {@code entityClass = Country.java}, {@code joinField = country}, and {@code joinFieldIds = [840, 840, 344]},
     * then this method returns {@code Country(id:840), Country(id:840), Country(id:344)}.
     * <p>
     * If the ID list is empty or no matching objects are found, this method returns {@link Collections#emptyList()}.
     *
     * @param entityClass  The type of relationship
     * @param joinField  The relationship field name
     * @param joinFieldIds  The specified list of join ID's against the relationshiop
     *
     * @return a list of hydrating values
     */
    @Override
    public Map<Object, Object> getRelationshipValues(Class<?> entityClass, String joinField, List<Object> joinFieldIds) {
        if (joinFieldIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Object> uniqueIds = joinFieldIds.stream().distinct().collect(Collectors.toCollection(LinkedList::new));

        List<Object> loaded = getEntityManager()
                .createQuery(
                        String.format(
                                "SELECT %s FROM %s WHERE %s IN :idList",
                                entityClass.getCanonicalName(),
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
                .collect(
                        Collectors.collectingAndThen(
                                Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue),
                                Collections::unmodifiableMap)
                );
    }
}
