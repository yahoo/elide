/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.query;

import com.yahoo.elide.core.exceptions.InvalidOperationException;
import com.yahoo.elide.datastores.aggregation.metadata.enums.TimeGrain;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimension;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimensionGrain;
import com.yahoo.elide.datastores.aggregation.query.TimeDimensionProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable;
import com.yahoo.elide.request.Argument;

import lombok.Value;

import java.util.LinkedHashMap;
import java.util.Locale;
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

        Argument grainArgument = arguments.get("grain");

        TimeDimensionGrain resolvedGrain;
        if (grainArgument == null) {
            //The first grain is the default.
            resolvedGrain = column.getSupportedGrains().stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            String.format("Requested default grain, no grain defined on %s", column.getName())));
        } else {
            String requestedGrainName = grainArgument.getValue().toString().toLowerCase(Locale.ENGLISH);

            resolvedGrain = column.getSupportedGrains().stream()
                    .filter(supportedGrain -> supportedGrain.getGrain().name().toLowerCase(Locale.ENGLISH)
                    .equals(requestedGrainName))
                    .findFirst()
                    .orElseThrow(() -> new InvalidOperationException(String.format("Unsupported grain %s for field %s",
                            requestedGrainName,
                            column.getName())));
        }

        this.column = column;
        this.referenceTable = referenceTable;
        this.arguments = arguments;
        this.alias = alias;
        this.grain = resolvedGrain;
        this.timeZone = timeZone;
    }

    @Override
    public String toSQL(SQLQueryTemplate queryTemplate) {
        //TODO - We will likely migrate to a templating language when we support parameterized metrics.
        return grain.getExpression().replaceFirst(TIME_DIMENSION_REPLACEMENT_REGEX,
                        referenceTable.getResolvedReference(column.getTable(), column.getName()));
    }

    @Override
    public TimeGrain getGrain() {
        return grain.getGrain();
    }
}
