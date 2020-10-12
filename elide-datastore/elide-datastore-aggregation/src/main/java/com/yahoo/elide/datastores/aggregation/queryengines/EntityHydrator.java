/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.datastores.aggregation.QueryEngine;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ValueType;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.MetricProjection;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.query.Queryable;

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
 * {@link EntityHydrator} hydrates the entity loaded by
 * {@link QueryEngine#executeQuery(Query, QueryEngine.Transaction)}.
 */
public class EntityHydrator {

    @Getter(AccessLevel.PROTECTED)
    private final EntityDictionary entityDictionary;

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
    public EntityHydrator(List<Object> results, Query query, EntityDictionary entityDictionary) {
        this.query = query;
        this.entityDictionary = entityDictionary;

        //Get all the projections from the client query.
        List<String> projections = this.query.getMetricProjections().stream()
                .map(MetricProjection::getAlias)
                .collect(Collectors.toList());

        projections.addAll(this.query.getAllDimensionProjections().stream()
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

    public EntityHydrator(ResultSet rs, Query query, EntityDictionary entityDictionary) {
        this.query = query;
        this.entityDictionary = entityDictionary;

        //Get all the projections from the client query.
        List<String> projections = this.query.getMetricProjections().stream()
                .map(MetricProjection::getAlias)
                .collect(Collectors.toList());

        projections.addAll(this.query.getAllDimensionProjections().stream()
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

    public Iterable<Object> hydrate() {
        //Coerce the results into entity objects.
        MutableInt counter = new MutableInt(0);

        List<Object> queryResults = getResults().stream()
                .map((result) -> coerceObjectToEntity(result, counter))
                .collect(Collectors.toList());

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
        Table table = getBaseTable(query);
        Class<?> entityClass = entityDictionary.getEntityClass(table.getName(), table.getVersion());

        //Construct the object.
        Object entityInstance;
        try {
            entityInstance = entityClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }

        result.forEach((fieldName, value) -> {
            ColumnProjection dim = query.getSource().getDimensionProjection(fieldName);

            if (dim != null && dim.getValueType().equals(ValueType.RELATIONSHIP)) {
                // We don't hydrate relationships here.
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

    private Table getBaseTable(Query query) {
        Queryable next = query;
        while (next.isNested()) {
            next = next.getSource();
        }

        return (Table) next.getSource();
    }
}
