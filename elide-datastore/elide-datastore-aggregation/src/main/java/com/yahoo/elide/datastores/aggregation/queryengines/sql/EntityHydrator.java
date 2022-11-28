/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql;

import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.exceptions.InvalidValueException;
import com.yahoo.elide.core.request.Attribute;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.core.type.ParameterizedModel;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.core.utils.coerce.CoerceUtil;
import com.yahoo.elide.datastores.aggregation.QueryEngine;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ValueType;
import com.yahoo.elide.datastores.aggregation.metadata.models.Column;
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
import org.apache.commons.lang3.mutable.MutableInt;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * {@link EntityHydrator} hydrates the entity loaded by
 * {@link QueryEngine#executeQuery(Query, QueryEngine.Transaction)}.
 */
@Slf4j
public class EntityHydrator implements Iterable<Object> {

    @Getter(AccessLevel.PROTECTED)
    private final EntityDictionary entityDictionary;

    @Getter(AccessLevel.PRIVATE)
    private final Query query;

    private ResultSet resultSet;

    private Map<String, String> projections;

    public EntityHydrator(ResultSet resultSet, Query query, EntityDictionary entityDictionary) {
        this.query = query;
        this.entityDictionary = entityDictionary;
        this.resultSet = resultSet;

        //Get all the projections from the client query.
        projections = this.query.getMetricProjections().stream()
                .map(SQLMetricProjection.class::cast)
                .filter(SQLColumnProjection::isProjected)
                .filter(projection -> ! projection.getValueType().equals(ValueType.ID))
                .collect(Collectors.toMap(MetricProjection::getAlias, MetricProjection::getSafeAlias));

        projections.putAll(this.query.getAllDimensionProjections().stream()
                .map(SQLColumnProjection.class::cast)
                .filter(SQLColumnProjection::isProjected)
                .collect(Collectors.toMap(ColumnProjection::getAlias, ColumnProjection::getSafeAlias)));
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
            ColumnProjection columnProjection = query.getColumnProjection(fieldName);
            Column column = table.getColumn(Column.class, columnProjection.getName());

            Type<?> fieldType = getType(entityClass, columnProjection);
            Attribute attribute = projectionToAttribute(columnProjection, fieldType);

            ValueType valueType = column.getValueType();

            if (entityInstance instanceof ParameterizedModel) {

                // This is an ENUM_TEXT or ENUM_ORDINAL type.
                if (! fieldType.isEnum() //Java enums can be coerced directly via CoerceUtil - so skip them.
                        && valueType == ValueType.TEXT
                        && column.getValues() != null
                        && !column.getValues().isEmpty()) {
                    value = convertToEnumValue(value, column.getValues());
                }

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

    private String convertToEnumValue(Object value, LinkedHashSet<String> enumValues) {
        if (value == null) {
            return null;
        }

        if (Integer.class.isAssignableFrom(value.getClass())) {
            Integer valueIndex = (Integer) value;
            if (valueIndex < enumValues.size()) {
                return enumValues.toArray(new String[0])[valueIndex];
            }
        }
        else if (enumValues.contains(value.toString())) {
            return value.toString();
        }

        throw new InvalidValueException(value, "Value must map to a value in: " + enumValues);
    }

    @Override
    public Iterator<Object> iterator() {
        return new Iterator<> () {

            Object next = null;
            MutableInt counter = new MutableInt(0);

            @Override
            public boolean hasNext() {
                if (next == null) {
                    try {
                        next = next();
                    } catch (NoSuchElementException e) {
                        return false;
                    }
                }

                return true;
            }

            @Override
            public Object next() {

                if (next != null) {
                    Object result = next;
                    next = null;
                    return result;
                }

                try {
                    boolean hasNext = resultSet.next();
                    if (! hasNext) {
                        throw new NoSuchElementException();
                    }
                    Map<String, Object> row = new LinkedHashMap<>();

                    for (Map.Entry<String, String> entry : projections.entrySet()) {
                        Object value = resultSet.getObject(entry.getValue());
                        row.put(entry.getKey(), value);
                    }

                    return coerceObjectToEntity(row, counter);
                } catch (SQLException e) {
                    log.error("Error iterating over results {}", e.getMessage());
                }
                throw new NoSuchElementException();
            }
        };
    }
}
