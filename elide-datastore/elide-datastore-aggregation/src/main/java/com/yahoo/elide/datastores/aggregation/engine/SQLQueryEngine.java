/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.engine;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.filter.HQLFilterOperation;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.datastores.aggregation.Query;
import com.yahoo.elide.datastores.aggregation.QueryEngine;
import com.yahoo.elide.datastores.aggregation.dimension.Dimension;
import com.yahoo.elide.datastores.aggregation.dimension.DimensionType;
import com.yahoo.elide.datastores.aggregation.engine.annotation.FromSubquery;
import com.yahoo.elide.datastores.aggregation.engine.annotation.FromTable;
import com.yahoo.elide.datastores.aggregation.engine.schema.SQLSchema;
import com.yahoo.elide.datastores.aggregation.metric.Metric;

import com.google.common.base.Preconditions;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.dialect.CalciteSqlDialect;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParser;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;

/**
 * QueryEngine for SQL backed stores.
 */
@Slf4j
public class SQLQueryEngine implements QueryEngine {

    SqlDialect dialect;

    private EntityManager entityManager;
    private EntityDictionary dictionary;
    private Map<Class<?>, SQLSchema> schemas;

    public SQLQueryEngine(EntityManager entityManager, EntityDictionary dictionary, SqlDialect dialect) {
        this.entityManager = entityManager;
        this.dictionary = dictionary;
        this.dialect = dialect;
        schemas = dictionary.getBindings()
                .stream()
                .filter((clazz) ->
                        dictionary.getAnnotation(clazz, FromTable.class) != null
                        || dictionary.getAnnotation(clazz, FromSubquery.class) != null
                )
                .collect(Collectors.toMap(
                        Function.identity(),
                        (clazz) -> (new SQLSchema(clazz, dictionary))
                ));
    }

    public SQLQueryEngine(EntityManager entityManager, EntityDictionary dictionary) {
        this(entityManager, dictionary, CalciteSqlDialect.DEFAULT);
    }

    @Override
    public Iterable<Object> executeQuery(Query query) {
        SQLSchema schema = schemas.get(query.getEntityClass());

        Preconditions.checkNotNull(schema);

        String tableName = schema.getTableDefinition();
        String tableAlias = schema.getAlias();

        List<String> projections = query.getMetrics().stream()
                .map(Metric::getName)
                .collect(Collectors.toList());

        projections.addAll(query.getGroupDimensions().stream()
                .map(Dimension::getName)
                .collect(Collectors.toList()));

        projections.addAll(query.getTimeDimensions().stream()
                .map(Dimension::getName)
                .collect(Collectors.toList()));

        String projectionClause = projections.stream()
                .map((name) -> tableAlias + "." + name)
                .collect(Collectors.joining(","));

        String sql = String.format("SELECT %s FROM %s AS %s",
                projectionClause, tableName, tableAlias);
        String nativeSql = translateSqlToNative(sql, dialect);

        log.debug("Running native SQL query: {}", nativeSql);

        javax.persistence.Query jpaQuery = entityManager.createNativeQuery(nativeSql);
        List<Object[]> results = jpaQuery.getResultList();

        return results.stream()
                .map((result) -> coerceObjectToEntity(query.getEntityClass(), projections, result))
                .collect(Collectors.toList());
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

    protected Object coerceObjectToEntity(Class<?> entityClass, List<String> projections, Object[] result) {
        SQLSchema schema = schemas.get(entityClass);

        Preconditions.checkNotNull(schema);
        Preconditions.checkArgument(result.length == projections.size());

        Object entityInstance;
        try {
            entityInstance = entityClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }

        for (int idx = 0; idx < result.length; idx++) {
            Object value = result[idx];
            String fieldName = projections.get(idx);

            Dimension dim = schema.getDimension(fieldName);
            if (dim != null && dim.getDimensionType() == DimensionType.ENTITY) {

                //We don't hydrate relationships here.
                continue;
            }

            dictionary.setValue(entityInstance, fieldName, value);
        }

        return entityInstance;
    }

    public String getWhereClause(SQLSchema schema, FilterExpression expression) {

        HQLFilterOperation filterVisitor = new HQLFilterOperation();
        return "";
    }
}
