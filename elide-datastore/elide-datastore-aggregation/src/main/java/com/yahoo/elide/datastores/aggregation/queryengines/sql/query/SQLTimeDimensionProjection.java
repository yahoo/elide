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
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable;
import com.yahoo.elide.request.Argument;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.util.Map;
import java.util.TimeZone;

/**
 * Column projection that can expand the column into a SQL projection fragment.
 */
@Value
@Builder
@AllArgsConstructor
public class SQLTimeDimensionProjection implements SQLColumnProjection, TimeDimensionProjection {

    private static final String TIME_DIMENSION_REPLACEMENT_REGEX = "\\{\\{(\\s*)}}";

    private Queryable source;
    private String alias;
    private String id;
    private String name;
    private String expression;
    private ValueType valueType;
    private ColumnType columnType;
    private TimeDimensionGrain grain;
    private TimeZone timeZone;
    private SQLReferenceTable referenceTable;
    private Map<String, Argument> arguments;

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
        this.columnType = column.getColumnType();
        this.valueType = column.getValueType();
        this.expression = column.getExpression();
        this.id = column.getId();
        this.name = column.getName();
        this.source = (SQLTable) column.getTable();
        this.grain = column.getSupportedGrain();
        this.referenceTable = referenceTable;
        this.arguments = arguments;
        this.alias = alias;
        this.timeZone = timeZone;
    }

    @Override
    public String toSQL(Queryable query) {
        //TODO - We will likely migrate to a templating language when we support parameterized metrics.
        return grain.getExpression().replaceFirst(TIME_DIMENSION_REPLACEMENT_REGEX,
                        referenceTable.getResolvedReference(source, name));
    }

    @Override
    public TimeGrain getGrain() {
        return grain.getGrain();
    }
}
