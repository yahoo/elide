/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.query;

import com.yahoo.elide.datastores.aggregation.metadata.enums.ColumnType;
import com.yahoo.elide.datastores.aggregation.metadata.enums.TimeGrain;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ValueType;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimension;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimensionGrain;
import com.yahoo.elide.datastores.aggregation.query.Queryable;
import com.yahoo.elide.datastores.aggregation.query.TimeDimensionProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable;
import com.yahoo.elide.request.Argument;

import lombok.Value;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;

/**
 * Column projection that can expand the column into a SQL projection fragment.
 */
@Value
public class SQLTimeDimensionProjection implements SQLColumnProjection<TimeDimension>, TimeDimensionProjection {

    private static final String TIME_DIMENSION_REPLACEMENT_REGEX = "\\{\\{(\\s*)}}";

    TimeDimension column;
    TimeDimensionGrain grain;
    TimeZone timeZone;
    SQLReferenceTable referenceTable;
    String alias;
    Map<String, Argument> arguments;

    /**
     * Default constructor for columns that are projected in filter and sorting clauses.
     * @param column The column in the filter/sorting clause.
     * @param referenceTable The reference table.
     */
    public SQLTimeDimensionProjection(TimeDimension column,
                                      SQLReferenceTable referenceTable) {
        this(
                column,
                column.getTimezone(),
                referenceTable,
                column.getName(),
                new LinkedHashMap<>()
        );
    }

    /**
     * All argument constructor.
     * @param column The column being projected.
     * @param timeZone The selected time zone.
     * @param referenceTable The reference table.
     * @param alias The client provided alias.
     * @param arguments List of client provided arguments.
     */
    public SQLTimeDimensionProjection(TimeDimension column,
                                      TimeZone timeZone,
                                      SQLReferenceTable referenceTable,
                                      String alias,
                                      Map<String, Argument> arguments) {
        //TODO remove arguments
        this.column = column;
        this.referenceTable = referenceTable;
        this.arguments = arguments;
        this.alias = alias;
        this.grain = column.getSupportedGrain();
        this.timeZone = timeZone;
    }

    @Override
    public String toSQL(Queryable query) {
        //TODO - We will likely migrate to a templating language when we support parameterized metrics.
        return grain.getExpression().replaceFirst(TIME_DIMENSION_REPLACEMENT_REGEX,
                        referenceTable.getResolvedReference(column.getTable(), column.getName()));
    }

    @Override
    public TimeGrain getGrain() {
        return grain.getGrain();
    }

    @Override
    public Queryable getSource() {
        return column.getTable();
    }

    @Override
    public String getId() {
        return column.getId();
    }

    @Override
    public String getName() {
        return column.getName();
    }

    @Override
    public String getExpression() {
        return column.getExpression();
    }

    @Override
    public ValueType getValueType() {
        return column.getValueType();
    }

    @Override
    public ColumnType getColumnType() {
        return column.getColumnType();
    }
}
