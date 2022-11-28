/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.query;

import com.yahoo.elide.core.request.Argument;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ColumnType;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ValueType;
import com.yahoo.elide.datastores.aggregation.metadata.models.Metric;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.MetricProjection;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.query.QueryPlan;
import com.yahoo.elide.datastores.aggregation.query.Queryable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.calcite.CalciteInnerAggregationExtractor;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.calcite.CalciteOuterAggregationExtractor;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.calcite.CalciteUtils;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialect;
import org.apache.calcite.avatica.util.Casing;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.commons.lang3.tuple.Pair;

import lombok.Builder;
import lombok.Data;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Metric projection that can expand the metric into a SQL projection fragment.
 */
@Data
@Builder
public class SQLMetricProjection implements MetricProjection, SQLColumnProjection {
    private static final Pattern QUERY_PATTERN = Pattern.compile(".*\\{\\{.*\\}\\}.*");
    private String name;
    private ValueType valueType;
    private ColumnType columnType;
    private String expression;
    private String alias;
    private Map<String, Argument> arguments;

    @Builder.Default
    private boolean projected = true;

    @Override
    public QueryPlan resolve(Query query) {
        return QueryPlan.builder()
                .source(query.getSource())
                .metricProjection(this)
                .build();
    }

    @Builder
    public SQLMetricProjection(String name,
                               ValueType valueType,
                               ColumnType columnType,
                               String expression,
                               String  alias,
                               Map<String, Argument> arguments,
                               boolean projected) {
        this.name = name;
        this.valueType = valueType;
        this.columnType = columnType;
        this.expression = expression;
        this.alias = alias;
        this.arguments = arguments;
        this.projected = projected;
    }

    public SQLMetricProjection(Metric metric,
                               String alias,
                               Map<String, Argument> arguments) {
        this(metric.getName(), metric.getValueType(),
                metric.getColumnType(), metric.getExpression(), alias, arguments, true);
    }

    @Override
    public String toSQL(Queryable query, MetaDataStore metaDataStore) {
        if (QUERY_PATTERN.matcher(expression).matches()) {
            return SQLColumnProjection.super.toSQL(query, metaDataStore);
        }
        return expression;
    }

    @Override
    public boolean canNest(Queryable source, MetaDataStore store) {
        boolean requiresJoin = SQLColumnProjection.requiresJoin(source, this, store);
        if (requiresJoin) {
            //We currently don't support nesting metrics with joins.
            //A join could be part of the aggregation (inner) or post aggregation (outer) expression.
            return false;
        }

        //TODO - Phase 2: return true if Calcite can parse & determine joins independently for inner & outer query
        return SQLColumnProjection.super.canNest(source, store);
    }

    @Override
    public Pair<ColumnProjection, Set<ColumnProjection>> nest(Queryable source,
                                                              MetaDataStore metaDataStore,
                                                              boolean joinInOuter) {
        SQLDialect dialect = source.getConnectionDetails().getDialect();
        String sql = toSQL(source, metaDataStore);
        SqlParser sqlParser = SqlParser.create(sql, CalciteUtils.constructParserConfig(dialect));

        SqlNode node;
        try {
            node = sqlParser.parseExpression();
        } catch (SqlParseException e) {
            throw new IllegalStateException(e);
        }

        CalciteInnerAggregationExtractor innerExtractor = new CalciteInnerAggregationExtractor(dialect);
        List<List<String>> innerAggExpressions = node.accept(innerExtractor);

        List<List<String>> innerAggLabels = innerAggExpressions.stream()
                .map(list -> list.stream()
                        .map((expression) -> getAggregationLabel(dialect.getCalciteDialect(), expression))
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());

        Set<ColumnProjection> innerAggProjections = new LinkedHashSet<>();

        Iterator<String> labelIt = innerAggLabels.stream().flatMap(List::stream).iterator();
        Iterator<String> expressionIt = innerAggExpressions.stream().flatMap(List::stream).iterator();

        while (labelIt.hasNext() && expressionIt.hasNext()) {
            String innerAggExpression = expressionIt.next();
            String innerAggLabel = labelIt.next();

            innerAggProjections.add(SQLMetricProjection.builder()
                    .projected(true)
                    .name(innerAggLabel)
                    .alias(innerAggLabel)
                    .expression(innerAggExpression)
                    .columnType(columnType)
                    .valueType(valueType)
                    .arguments(arguments)
                    .build());
        }

        CalciteOuterAggregationExtractor outerExtractor =
                new CalciteOuterAggregationExtractor(dialect, innerAggLabels);

        SqlNode transformedParseTree = node.accept(outerExtractor);

        String outerAggExpression = transformedParseTree.toSqlString(dialect.getCalciteDialect()).getSql();

        //replace INNER_AGG_... with {{$INNER_AGG...}}
        outerAggExpression = outerAggExpression.replaceAll(
                dialect.getBeginQuote()
                        + "?(" + getAggregationLabelPrefix(dialect.getCalciteDialect()) + "\\w+)"
                        + dialect.getEndQuote()
                        + "?", "{{\\$" + "$1" + "}}");

        String columnId = source.isRoot() ? getName() : getAlias();
        boolean inProjection = source.getColumnProjection(columnId, arguments, true) != null;

        ColumnProjection outerProjection = SQLMetricProjection.builder()
                .projected(inProjection)
                .expression(outerAggExpression)
                .name(name)
                .alias(alias)
                .valueType(valueType)
                .columnType(columnType)
                .arguments(arguments)
                .build();

        return Pair.of(outerProjection, innerAggProjections);
    }

    @Override
    public SQLMetricProjection withProjected(boolean projected) {
        return new SQLMetricProjection(name, valueType, columnType, expression, alias, arguments, projected);
    }

    @Override
    public boolean isProjected() {
        return projected;
    }

    @Override
    public SQLMetricProjection withExpression(String expression, boolean projected) {
        return new SQLMetricProjection(name, valueType, columnType, expression, alias, arguments, projected);
    }

    private static String getAggregationLabelPrefix(SqlDialect dialect) {
        if (dialect.getUnquotedCasing().equals(Casing.TO_LOWER)) {
            return "inner_agg_";
        }
        return "INNER_AGG_";
    }

    private static String getAggregationLabel(SqlDialect dialect, String expression) {
        return getAggregationLabelPrefix(dialect) + (expression.hashCode() & 0xfffffff);
    }

    @Override
    public SQLMetricProjection withArguments(Map<String, Argument> arguments) {
        return new SQLMetricProjection(name, valueType, columnType, expression, alias, arguments, projected);
    }
}
