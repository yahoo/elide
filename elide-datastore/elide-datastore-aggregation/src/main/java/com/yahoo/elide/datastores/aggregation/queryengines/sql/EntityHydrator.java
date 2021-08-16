/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql;

import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.request.Attribute;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.core.type.ParameterizedModel;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.core.utils.coerce.CoerceUtil;
import com.yahoo.elide.datastores.aggregation.QueryEngine;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ValueType;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.MetricProjection;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.query.Queryable;
import com.yahoo.elide.datastores.aggregation.query.TimeDimensionProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLColumnProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLMetricProjection;
import com.yahoo.elide.datastores.aggregation.timegrains.Day;
import com.yahoo.elide.datastores.aggregation.timegrains.Hour;
import com.yahoo.elide.datastores.aggregation.timegrains.ISOWeek;
import com.yahoo.elide.datastores.aggregation.timegrains.Minute;
import com.yahoo.elide.datastores.aggregation.timegrains.Month;
import com.yahoo.elide.datastores.aggregation.timegrains.Quarter;
import com.yahoo.elide.datastores.aggregation.timegrains.Second;
import com.yahoo.elide.datastores.aggregation.timegrains.Week;
import com.yahoo.elide.datastores.aggregation.timegrains.Year;
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

    public EntityHydrator(ResultSet rs, Query query, EntityDictionary entityDictionary) {
        this.query = query;
        this.entityDictionary = entityDictionary;

        //Get all the projections from the client query.
        Map<String, String> projections = this.query.getMetricProjections().stream()
                .map(SQLMetricProjection.class::cast)
                .filter(SQLColumnProjection::isProjected)
                .filter(projection -> ! projection.getValueType().equals(ValueType.ID))
                .collect(Collectors.toMap(MetricProjection::getAlias, MetricProjection::getSafeAlias));

        projections.putAll(this.query.getAllDimensionProjections().stream()
                .map(SQLColumnProjection.class::cast)
                .filter(SQLColumnProjection::isProjected)
                .collect(Collectors.toMap(ColumnProjection::getAlias, ColumnProjection::getSafeAlias)));

        try {
            Preconditions.checkArgument(projections.size() == rs.getMetaData().getColumnCount());
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();

                for (Map.Entry<String, String> entry : projections.entrySet()) {
                    Object value = rs.getObject(entry.getValue());
                    row.put(entry.getKey(), value);
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
        Type<?> entityClass = entityDictionary.getEntityClass(table.getName(), table.getVersion());

        //Construct the object.
        Object entityInstance;
        try {
            entityInstance = entityClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }

        result.forEach((fieldName, value) -> {
            ColumnProjection dim = query.getColumnProjection(fieldName);
            Type<?> fieldType = getType(entityClass, dim);
            Attribute attribute = projectionToAttribute(dim, fieldType);

            if (entityInstance instanceof ParameterizedModel) {
                ((ParameterizedModel) entityInstance).addAttributeValue(
                    attribute,
                    CoerceUtil.coerce(value, fieldType));
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

    private Attribute projectionToAttribute(ColumnProjection projection, Type valueType) {
        return Attribute.builder()
                .alias(projection.getAlias())
                .name(projection.getName())
                .arguments(projection.getArguments().values())
                .type(valueType)
                .build();
    }

    private Type getType(Type modelType, ColumnProjection column) {
        if (! (column instanceof TimeDimensionProjection)) {
            return entityDictionary.getType(modelType, column.getName());
        }

        switch (((TimeDimensionProjection) column).getGrain()) {
            case SECOND:
                return ClassType.of(Second.class);
            case MINUTE:
                return ClassType.of(Minute.class);
            case HOUR:
                return ClassType.of(Hour.class);
            case DAY:
                return ClassType.of(Day.class);
            case ISOWEEK:
                return ClassType.of(ISOWeek.class);
            case WEEK:
                return ClassType.of(Week.class);
            case MONTH:
                return ClassType.of(Month.class);
            case QUARTER:
                return ClassType.of(Quarter.class);
            case YEAR:
                return ClassType.of(Year.class);
            default:
                throw new IllegalStateException("Invalid grain type");
        }
    }
}
