/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.query;

import com.yahoo.elide.core.request.Argument;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ColumnType;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ValueType;
import com.yahoo.elide.datastores.aggregation.metadata.models.Metric;
import com.yahoo.elide.datastores.aggregation.query.DefaultQueryPlanResolver;
import com.yahoo.elide.datastores.aggregation.query.MetricProjection;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.query.QueryPlan;
import com.yahoo.elide.datastores.aggregation.query.QueryPlanResolver;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Metric projection that can expand the metric into a SQL projection fragment.
 */
@Value
@Builder
public class SQLMetricProjection implements MetricProjection, SQLColumnProjection {
    private String name;
    private ValueType valueType;
    private ColumnType columnType;
    private String expression;
    private String alias;
    private Map<String, Argument> arguments;

    private final boolean canNest;

    //TODO - Temporary hack just to prove out the concept.  This needs to be parameterized by the dialect
    //and do proper parenthesis matching - which means it can't be a regex.
    private static final String AGG_FUNCTION = "^(sum|min|max|avg|count)\\(.*?\\)$";
    private static final Pattern AGG_FUNCTION_MATCHER = Pattern.compile(AGG_FUNCTION, Pattern.CASE_INSENSITIVE);

    @EqualsAndHashCode.Exclude
    private QueryPlanResolver queryPlanResolver;

    @Override
    public QueryPlan resolve(Query query) {
        return queryPlanResolver.resolve(query, this);
    }

    public SQLMetricProjection(String name,
                               ValueType valueType,
                               ColumnType columnType,
                               String expression,
                               String  alias,
                               Map<String, Argument> arguments,
                               QueryPlanResolver queryPlanResolver) {
        this.name = name;
        this.valueType = valueType;
        this.columnType = columnType;
        this.expression = expression;
        this.alias = alias;
        this.arguments = arguments;
        this.queryPlanResolver = queryPlanResolver == null ? new DefaultQueryPlanResolver() : queryPlanResolver;

        Matcher matcher = AGG_FUNCTION_MATCHER.matcher(expression);
        canNest = matcher.matches();
    }

    public SQLMetricProjection(Metric metric,
                               String alias,
                               Map<String, Argument> arguments) {
        this(metric.getName(), metric.getValueType(),
                metric.getColumnType(), metric.getExpression(), alias, arguments, metric.getQueryPlanResolver());
    }

    @Override
    public SQLMetricProjection withExpression(String expression) {
        return SQLMetricProjection.builder()
                .name(name)
                .alias(alias)
                .valueType(valueType)
                .columnType(columnType)
                .expression(expression)
                .arguments(arguments)
                .queryPlanResolver(queryPlanResolver)
                .build();
    }

    @Override
    public boolean canNest() {
        return canNest;
    }

    @Override
    public SQLColumnProjection outerQuery() {
        if (!canNest) {
            throw new UnsupportedOperationException("Metric does not support nesting");
        }

        Matcher matcher = AGG_FUNCTION_MATCHER.matcher(expression);

        String aggFunction = matcher.group(1);
        return (SQLColumnProjection) withExpression(aggFunction + "({{" + this.getSafeAlias() + "}})");
    }

    @Override
    public Set<SQLColumnProjection> innerQuery() {
        if (!canNest) {
            throw new UnsupportedOperationException("Metric does not support nesting");
        }

        return new HashSet<>(Arrays.asList(this));
    }
}
