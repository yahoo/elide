/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.query;

import com.yahoo.elide.datastores.aggregation.metadata.enums.TimeGrain;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimension;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimensionGrain;
import com.yahoo.elide.datastores.aggregation.query.TimeDimensionProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable;
import com.yahoo.elide.request.Argument;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;

/**
 * Column projection that can expand the column into a SQL projection fragment.
 */
public class SQLTimeDimensionProjection implements SQLColumnProjection<TimeDimension>, TimeDimensionProjection {

    private final TimeDimension column;
    private final TimeGrain grain;
    private final TimeZone timezone;
    private final SQLReferenceTable sqlReferenceTable;
    private final String alias;
    private final Map<String, Argument> arguments;

    /**
     * Default constructor for columns that are projected in filter and sorting clauses.
     * @param column The column in the filter/sorting clause.
     * @param sqlReferenceTable The reference table.
     */
    public SQLTimeDimensionProjection(TimeDimension column,
                                      SQLReferenceTable sqlReferenceTable) {
        this(
                column,
                column.getSupportedGrains().iterator().next().getGrain(),
                column.getTimezone(),
                sqlReferenceTable,
                column.getName(),
                new LinkedHashMap<>()
        );
    }

    /**
     * Default constructor to convert a timeDimensionProjection into its SQL equivalent.
     * @param timeDimensionProjection The timeDimensionProjection to convert.
     * @param sqlReferenceTable The reference table.
     */
    public SQLTimeDimensionProjection(TimeDimensionProjection timeDimensionProjection,
                                      SQLReferenceTable sqlReferenceTable) {
        this(
                timeDimensionProjection.getColumn(),
                timeDimensionProjection.getGrain(),
                timeDimensionProjection.getTimeZone(),
                sqlReferenceTable,
                timeDimensionProjection.getAlias(),
                timeDimensionProjection.getArguments()
        );
    }


    /**
     * All argument constructor.
     * @param column The column being projected.
     * @param grain The selected time grain.
     * @param timezone The selected time zone.
     * @param sqlReferenceTable The reference table.
     * @param alias The client provided alias.
     * @param arguments List of client provided arguments.
     */
    public SQLTimeDimensionProjection(TimeDimension column,
                                      TimeGrain grain,
                                      TimeZone timezone,
                                      SQLReferenceTable sqlReferenceTable,
                                      String alias,
                                      Map<String, Argument> arguments) {
        this.column = column;
        this.sqlReferenceTable = sqlReferenceTable;
        this.arguments = arguments;
        this.alias = alias;
        this.grain = grain;
        this.timezone = timezone;
    }

    @Override
    public SQLReferenceTable getReferenceTable() {
        return sqlReferenceTable;
    }

    @Override
    public TimeDimension getColumn() {
        return column;
    }

    @Override
    public String toSQL() {
        for (TimeDimensionGrain grainInfo : column.getSupportedGrains()) {
            if (grainInfo.getGrain().equals(this.getGrain())) {
                return String.format(
                        grainInfo.getExpression(),
                        sqlReferenceTable.getResolvedReference(column.getTable(), column.getName()));
            }
        }


        TimeDimensionGrain grainInfo = column.getSupportedGrains().stream()
                .filter(g -> g.getGrain().equals(this.getGrain()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Requested time grain not supported."));

        //TODO - We will likely migrate to a templating language when we support parameterized metrics.
        return String.format(
                grainInfo.getExpression(),
                sqlReferenceTable.getResolvedReference(column.getTable(), column.getName()));
    }

    @Override
    public String getAlias() {
        return alias;
    }

    @Override
    public TimeGrain getGrain() {
        return grain;
    }

    @Override
    public TimeZone getTimeZone() {
        return timezone;
    }

    @Override
    public Map<String, Argument> getArguments() {
        return arguments;
    }
}
