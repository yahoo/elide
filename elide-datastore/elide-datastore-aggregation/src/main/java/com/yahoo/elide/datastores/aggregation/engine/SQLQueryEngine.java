/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.engine;

import com.yahoo.elide.datastores.aggregation.Query;
import com.yahoo.elide.datastores.aggregation.QueryEngine;

import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.dialect.CalciteSqlDialect;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParser;

import java.util.List;
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

        String sql = String.format("SELECT * FROM %s", query.getEntityClass().getSimpleName());
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
}
