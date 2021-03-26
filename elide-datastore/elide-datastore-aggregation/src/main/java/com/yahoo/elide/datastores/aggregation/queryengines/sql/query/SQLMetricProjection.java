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
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.DefaultQueryPlanResolver;
import com.yahoo.elide.datastores.aggregation.query.MetricProjection;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.query.QueryPlan;
import com.yahoo.elide.datastores.aggregation.query.QueryPlanResolver;
import com.yahoo.elide.datastores.aggregation.query.Queryable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.calcite.CalciteInnerAggregationExtractor;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.calcite.CalciteOuterAggregationExtractor;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialect;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParser;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

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

    @EqualsAndHashCode.Exclude
    private QueryPlanResolver queryPlanResolver;
    @Builder.Default
    private boolean projected = true;


    @Override
    public QueryPlan resolve(Query query) {
        return queryPlanResolver.resolve(query, this);
    }

    @Builder
    public SQLMetricProjection(String name,
                               ValueType valueType,
                               ColumnType columnType,
                               String expression,
                               String  alias,
                               Map<String, Argument> arguments,
                               QueryPlanResolver queryPlanResolver,
                               boolean projected) {
        this.name = name;
        this.valueType = valueType;
        this.columnType = columnType;
        this.expression = expression;
        this.alias = alias;
        this.arguments = arguments;
        this.queryPlanResolver = queryPlanResolver == null ? new DefaultQueryPlanResolver() : queryPlanResolver;
        this.projected = projected;
    }

    public SQLMetricProjection(Metric metric,
                               String alias,
                               Map<String, Argument> arguments) {
        this(metric.getName(), metric.getValueType(),
                metric.getColumnType(), metric.getExpression(), alias, arguments,
                metric.getQueryPlanResolver(), true);
    }

    @Override
    public boolean canNest(Queryable source, SQLReferenceTable lookupTable) {
        SQLDialect dialect = source.getConnectionDetails().getDialect();
        String sql = toSQL(source.getSource(), lookupTable);

        if (lookupTable.getResolvedJoinProjections(source.getSource(), name).size() > 0) {
            //We currently don't support nesting metrics with joins.
            //A join could be part of the aggregation (inner) or post aggregation (outer) expression.
            return false;
        }

        SqlParser sqlParser = SqlParser.create(sql, SqlParser.config().withLex(dialect.getCalciteLex()));

        try {
            sqlParser.parseExpression();
        } catch (SqlParseException e) {

            //If calcite can't parse the expression, we can't nest it.
            return false;
        }

        //TODO - Phase 2: return true if Calcite can parse & determine joins independently for inner & outer query
        return true;
    }

    @Override
    public ColumnProjection outerQuery(Queryable source, SQLReferenceTable lookupTable, boolean joinInOuter) {
        SQLDialect dialect = source.getConnectionDetails().getDialect();
        String sql = toSQL(source, lookupTable);
        SqlParser sqlParser = SqlParser.create(sql, SqlParser.config().withLex(dialect.getCalciteLex()));

        SqlNode node;
        try {
            node = sqlParser.parseExpression();
        } catch (SqlParseException e) {
            throw new IllegalStateException(e);
        }

        CalciteInnerAggregationExtractor innerExtractor = new CalciteInnerAggregationExtractor();
        List<String> innerAggExpressions = node.accept(innerExtractor);

        List<String> innerAggLabels = innerAggExpressions.stream()
                .map((expression) -> "INNER_AGG_" + expression.hashCode())
                .collect(Collectors.toList());


        CalciteOuterAggregationExtractor outerExtractor =
                new CalciteOuterAggregationExtractor(innerAggLabels.iterator());

        SqlNode transformedParseTree = node.accept(outerExtractor);

        String outerAggExpression = transformedParseTree.toSqlString(dialect.getCalciteDialect()).getSql();

        //replace INNER_AGG_... with {{INNER_AGG...}}
        outerAggExpression.replaceAll("(INNER_AGG_\\w+)", "{{$1}}");

        return SQLMetricProjection.builder()
                .projected(true)
                .expression(outerAggExpression)
                .name(name)
                .alias(alias)
                .valueType(valueType)
                .columnType(columnType)
                .arguments(arguments)
                .build();
    }

    @Override
    public Set<ColumnProjection> innerQuery(Queryable source, SQLReferenceTable lookupTable, boolean joinInOuter) {
        SQLDialect dialect = source.getConnectionDetails().getDialect();
        String sql = toSQL(source, lookupTable);
        SqlParser sqlParser = SqlParser.create(sql, SqlParser.config().withLex(dialect.getCalciteLex()));

        SqlNode node;
        try {
            node = sqlParser.parseExpression();
        } catch (SqlParseException e) {
            throw new IllegalStateException(e);
        }

        CalciteInnerAggregationExtractor innerExtractor = new CalciteInnerAggregationExtractor();
        List<String> innerAggExpressions = node.accept(innerExtractor);

        List<String> innerAggLabels = innerAggExpressions.stream()
                .map((expression) -> "INNER_AGG_" + expression.hashCode())
                .collect(Collectors.toList());

        Set<ColumnProjection> innerAggProjections = new LinkedHashSet<>();

        for (int idx = 0; idx < innerAggExpressions.size(); idx++) {
            String innerAggExpression = innerAggExpressions.get(idx);
            String innerAggLabel = innerAggLabels.get(idx);

            innerAggProjections.add(SQLMetricProjection.builder()
                    .projected(true)
                    .name(innerAggLabel)
                    .alias(innerAggLabel)
                    .expression(innerAggExpression)
                    .columnType(columnType)
                    .valueType(valueType)
                    .build());
        }

        return innerAggProjections;
    }

    @Override
    public boolean isProjected() {
        return projected;
    }
}
