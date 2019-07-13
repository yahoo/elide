/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.engine;

import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.datastores.aggregation.Query;
import com.yahoo.elide.datastores.aggregation.QueryEngine;

import com.yahoo.elide.datastores.aggregation.dimension.Dimension;
import com.yahoo.elide.datastores.aggregation.metric.Metric;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.dialect.CalciteSqlDialect;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParser;

import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;

/**
 * QueryEngine for SQL backed stores.
 */
public class SQLQueryEngine implements QueryEngine {

    SqlDialect dialect = CalciteSqlDialect.DEFAULT;

    private EntityManager entityManager;

    public SQLQueryEngine(EntityManager entityManager) {
        this.entityManager = entityManager;
    }


    @Override
    public Iterable<Object> executeQuery(Query query) {
        String tableName = query.getEntityClass().getSimpleName();
        String tableAlias = FilterPredicate.getTypeAlias(query.getEntityClass());

        List<String> projections = query.getMetrics().stream()
                .map(Metric::getName)
                .map((name) -> tableAlias + "." + name)
                .collect(Collectors.toList());

        projections.addAll(query.getGroupDimensions().stream()
                .map(Dimension::getName)
                .map((name) -> tableAlias + "." + name)
                .collect(Collectors.toList()));

        projections.addAll(query.getTimeDimensions().stream()
                .map(Dimension::getName)
                .map((name) -> tableAlias + "." + name)
                .collect(Collectors.toList()));

        String projectionClause = projections.stream().collect(Collectors.joining(","));


        String sql = String.format("SELECT %s FROM %s AS %s",
                projectionClause, tableName, tableAlias);
        String nativeSql = translateSqlToNative(sql, dialect);

        javax.persistence.Query jpaQuery = entityManager.createNativeQuery(nativeSql);

        List<Object> results = jpaQuery.getResultList();

        return results;
    }

    protected String translateSqlToNative(String sqlStatement, SqlDialect dialect) {
        SqlParser parser = SqlParser.create(sqlStatement);

        try {
            SqlNode ast = parser.parseQuery();
            return ast.toSqlString(dialect).getSql();

        } catch (SqlParseException e) {
            throw new IllegalStateException(e);
        }
    }

    protected Object coerceObjectToEntity(Object result) {
        return null;
    }
}
