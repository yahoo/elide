/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.query;

import com.yahoo.elide.core.exceptions.InvalidParameterizedAttributeException;
import com.yahoo.elide.core.request.Argument;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ColumnType;
import com.yahoo.elide.datastores.aggregation.metadata.enums.TimeGrain;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ValueType;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimension;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimensionGrain;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.Queryable;
import com.yahoo.elide.datastores.aggregation.query.TimeDimensionProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.expression.ExpressionParser;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.expression.Reference;
import org.apache.commons.lang3.tuple.Pair;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

/**
 * Column projection that can expand the column into a SQL projection fragment.
 */
@Value
@Builder
@AllArgsConstructor
public class SQLTimeDimensionProjection implements SQLColumnProjection, TimeDimensionProjection {

    private static final String TIME_DIMENSION_REPLACEMENT_REGEX = "\\{\\{(\\s*)}}";

    private String alias;
    private String name;
    private String expression;
    private ValueType valueType;
    private ColumnType columnType;
    private TimeDimensionGrain grain;
    private TimeZone timeZone;
    private Map<String, Argument> arguments;
    private boolean projected;

    /**
     * All argument constructor.
     * @param column The column being projected.
     * @param timeZone The selected time zone.
     * @param alias The client provided alias.
     * @param arguments List of client provided arguments.
     * @param projected Whether or not this column is part of the projected output.
     */
    public SQLTimeDimensionProjection(TimeDimension column,
                                      TimeZone timeZone,
                                      String alias,
                                      Map<String, Argument> arguments,
                                      boolean projected) {
        //TODO remove arguments
        this.columnType = column.getColumnType();
        this.valueType = column.getValueType();
        this.expression = column.getExpression();
        this.name = column.getName();
        this.grain = getGrainFromArguments(arguments, column);
        this.arguments = arguments;
        this.alias = alias;
        this.timeZone = timeZone;
        this.projected = projected;
    }

    @Override
    public String toSQL(Queryable query, MetaDataStore metaDataStore) {

        String resolvedExpr = SQLColumnProjection.super.toSQL(query, metaDataStore);

        // TODO - We will likely migrate to a templating language when we support parameterized metrics.
        return grain.getExpression().replaceAll(TIME_DIMENSION_REPLACEMENT_REGEX, resolvedExpr);
    }

    @Override
    public TimeGrain getGrain() {
        return grain.getGrain();
    }

    @Override
    public Pair<ColumnProjection, Set<ColumnProjection>> nest(Queryable source,
                                                              MetaDataStore store,
                                                              boolean joinInOuter) {

        List<Reference> references = new ExpressionParser(store).parse(source, getExpression());

        boolean requiresJoin = SQLColumnProjection.requiresJoin(references);

        boolean inProjection = source.getColumnProjection(getName(), getArguments(), true) != null;

        ColumnProjection outerProjection;
        Set<ColumnProjection> innerProjections;

        if (requiresJoin && joinInOuter) {
            outerProjection = withExpression(getExpression(), inProjection);

            //TODO - the expression needs to be rewritten to leverage the inner column physical projections.
            innerProjections = SQLColumnProjection.extractPhysicalReferences(references, store);
        } else {
            outerProjection = SQLTimeDimensionProjection.builder()
                    .name(name)
                    .alias(alias)
                    .valueType(valueType)
                    .columnType(columnType)

                    //This grain removes the extra time grain formatting on the outer query.
                    .grain(new TimeDimensionGrain(
                            this.getName(),
                            grain.getGrain()))
                    .expression("{{$" + this.getSafeAlias() + "}}")
                    .projected(isProjected())
                    .arguments(arguments)
                    .timeZone(timeZone)
                    .build();
            innerProjections = new LinkedHashSet<>(Arrays.asList(this));
        }

        return Pair.of(outerProjection, innerProjections);
    }

    @Override
    public SQLTimeDimensionProjection withProjected(boolean projected) {
        return newSQLTimeDimensionProjection(expression, arguments, projected);
    }

    @Override
    public SQLTimeDimensionProjection withExpression(String expression, boolean projected) {
        return newSQLTimeDimensionProjection(expression, arguments, projected);
    }

    private TimeDimensionGrain getGrainFromArguments(Map<String, Argument> arguments, TimeDimension column) {
        Argument grainArgument = arguments.get("grain");

        if (grainArgument == null) {
            return column.getDefaultGrain();
        }

        String grainName = grainArgument.getValue().toString().toLowerCase(Locale.ENGLISH);

        return column.getSupportedGrains().stream()
                .filter(grain -> grain.getGrain().name().toLowerCase(Locale.ENGLISH).equals(grainName))
                .findFirst()
                .orElseThrow(() -> new InvalidParameterizedAttributeException(name, grainArgument));
    }

    @Override
    public boolean isProjected() {
        return projected;
    }

    @Override
    public ColumnProjection withArguments(Map<String, Argument> arguments) {
        return newSQLTimeDimensionProjection(expression, arguments, projected);
    }

    private SQLTimeDimensionProjection newSQLTimeDimensionProjection(String expression,
                                                                     Map<String, Argument> arguments,
                                                                     boolean projected) {
        return SQLTimeDimensionProjection.builder()
                        .name(name)
                        .alias(alias)
                        .valueType(valueType)
                        .columnType(columnType)
                        .expression(expression)
                        .arguments(arguments)
                        .projected(projected)
                        .grain(grain)
                        .timeZone(timeZone)
                        .build();
    }
}
