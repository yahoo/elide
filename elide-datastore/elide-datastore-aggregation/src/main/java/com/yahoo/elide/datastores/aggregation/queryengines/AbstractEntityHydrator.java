/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.datastores.aggregation.QueryEngine;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ValueType;
import com.yahoo.elide.datastores.aggregation.metadata.models.Dimension;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.MetricProjection;
import com.yahoo.elide.datastores.aggregation.query.Query;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.mutable.MutableInt;

import lombok.AccessLevel;
import lombok.Getter;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * {@link AbstractEntityHydrator} hydrates the entity loaded by
 * {@link QueryEngine#executeQuery(Query, QueryEngine.Transaction)}.
 * <p>
 * {@link AbstractEntityHydrator} is not thread-safe and should be accessed by only 1 thread in this application,
 * because it uses {@link StitchList}. See {@link StitchList} for more details.
 */
public abstract class AbstractEntityHydrator {

    @Getter(AccessLevel.PROTECTED)
    private final EntityDictionary entityDictionary;

    @Getter(AccessLevel.PRIVATE)
    private final StitchList stitchList;

    @Getter(AccessLevel.PROTECTED)
    private final List<Map<String, Object>> results = new ArrayList<>();

    @Getter(AccessLevel.PRIVATE)
    private final Query query;

    /**
     * Constructor.
     *
     * @param results The loaded objects from {@link QueryEngine#executeQuery(Query, QueryEngine.Transaction)}
     * @param query  The query passed to {@link QueryEngine#executeQuery(Query, QueryEngine.Transaction)} to load the
     *               objects
     * @param entityDictionary  An object that sets entity instance values and provides entity metadata info
     */
    public AbstractEntityHydrator(List<Object> results, Query query, EntityDictionary entityDictionary) {
        this.stitchList = new StitchList(entityDictionary);
        this.query = query;
        this.entityDictionary = entityDictionary;

        //Get all the projections from the client query.
        List<String> projections = this.query.getMetrics().stream()
                .map(MetricProjection::getAlias)
                .collect(Collectors.toList());

        projections.addAll(this.query.getDimensions().stream()
                .map(ColumnProjection::getAlias)
                .collect(Collectors.toList()));

        results.forEach(result -> {
            Map<String, Object> row = new HashMap<>();

            Object[] resultValues = result instanceof Object[] ? (Object[]) result : new Object[] { result };

            Preconditions.checkArgument(projections.size() == resultValues.length);

            for (int idx = 0; idx < resultValues.length; idx++) {
                Object value = resultValues[idx];
                String fieldName = projections.get(idx);
                row.put(fieldName, value);
            }

            this.results.add(row);
        });
    }

    public AbstractEntityHydrator(ResultSet rs, Query query, EntityDictionary entityDictionary) {
        this.stitchList = new StitchList(entityDictionary);
        this.query = query;
        this.entityDictionary = entityDictionary;

        //Get all the projections from the client query.
        List<String> projections = this.query.getMetrics().stream()
                .map(MetricProjection::getAlias)
                .collect(Collectors.toList());

        projections.addAll(this.query.getDimensions().stream()
                .map(ColumnProjection::getAlias)
                .collect(Collectors.toList()));

        try {
            Preconditions.checkArgument(projections.size() == rs.getMetaData().getColumnCount());
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();

                for (int idx = 0; idx < projections.size(); idx++) {
                    Object value = rs.getObject(idx + 1);
                    String fieldName = projections.get(idx);
                    row.put(fieldName, value);
                }

                this.results.add(row);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Loads a map of relationship object ID to relationship object instance.
     * <p>
     * Note the relationship cannot be toMany. This method will be invoked for every relationship field of the
     * requested entity. Its implementation should return the result of the following query
     * <p>
     * <b>Given a relationship with type {@code relationshipType} in an entity, loads all relationship
     * objects whose foreign keys are one of the specified list, {@code joinFieldIds}</b>.
     * <p>
     * For example, when the relationship is loaded from SQL and we have the following example identity:
     * <pre>
     * public class PlayerStats {
     *     private String id;
     *     private Country country;
     *
     *     &#64;OneToOne
     *     &#64;JoinColumn(name = "country_id")
     *     public Country getCountry() {
     *         return country;
     *     }
     * }
     * </pre>
     * In this case {@code relationshipType = Country.class}. If {@code country} is
     * requested in {@code PlayerStats} query and 3 stats, for example, are found in database whose country ID's are
     * {@code joinFieldIds = [840, 344, 840]}, then this method should effectively run the following query (JPQL as
     * example)
     * <pre>
     * {@code
     *     SELECT e FROM country_table e WHERE country_id IN (840, 344);
     * }
     * </pre>
     * and returns the map of [840: Country(id:840), 344: Country(id:344)]
     *
     * @param relationshipType  The type of relationship
     * @param joinFieldIds  The specified list of join ID's against the relationship
     *
     * @return a list of hydrating values
     */
    protected abstract Map<Object, Object> getRelationshipValues(
            Class<?> relationshipType,
            List<Object> joinFieldIds
    );

    public Iterable<Object> hydrate() {
        //Coerce the results into entity objects.
        MutableInt counter = new MutableInt(0);

        List<Object> queryResults = getResults().stream()
                .map((result) -> coerceObjectToEntity(result, counter))
                .collect(Collectors.toList());

//        if (getStitchList().shouldStitch()) {
//            // relationship is requested, stitch relationship then
//            populateObjectLookupTable();
//            getStitchList().stitch();
//        }

        return queryResults;
    }

    /**
     * Coerces results from a {@link Query} into an Object.
     *
     * @param result a fieldName-value map
     * @param counter Monotonically increasing number to generate IDs.
     * @return A hydrated entity object.
     */
    protected Object coerceObjectToEntity(Map<String, Object> result, MutableInt counter) {
        Table table = query.getTable();
        Class<?> entityClass = entityDictionary.getEntityClass(table.getName(), table.getVersion());

        //Construct the object.
        Object entityInstance;
        try {
            entityInstance = entityClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }

        result.forEach((fieldName, value) -> {
            Dimension dim = query.getTable().getDimension(fieldName);

            if (dim != null && dim.getValueType().equals(ValueType.RELATIONSHIP)) {
                getStitchList().todo(entityInstance, fieldName, value); // We don't hydrate relationships here.
            } else {
                getEntityDictionary().setValue(entityInstance, fieldName, value);
            }
        });

        //Set the ID (it must be coerced from an integer)
        getEntityDictionary().setValue(
                entityInstance,
                getEntityDictionary().getIdFieldName(entityClass),
                counter.getAndIncrement()
        );

        return entityInstance;
    }

    /**
     * Foe each requested relationship, run a single query to load all relationship objects whose ID's are involved in
     * the request.
     */
    private void populateObjectLookupTable() {
        // mapping: relationship field name -> join ID's
        Map<String, List<Object>> hydrationIdsByRelationship = getStitchList().getHydrationMapping();

        // hydrate each relationship
        for (Map.Entry<String, List<Object>> entry : hydrationIdsByRelationship.entrySet()) {
            String joinField = entry.getKey();
            List<Object> joinFieldIds = entry.getValue();
            Table table = getQuery().getTable();
            Class<?> relationshipType = getEntityDictionary().getParameterizedType(
                    entityDictionary.getEntityClass(table.getName(), table.getVersion()),
                    joinField);

            getStitchList().populateLookup(relationshipType, getRelationshipValues(relationshipType, joinFieldIds));
        }
    }
}
