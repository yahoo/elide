/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.TimedFunction;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.expression.PredicateExtractionVisitor;
import com.yahoo.elide.datastores.aggregation.QueryEngine;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.metadata.models.Dimension;
import com.yahoo.elide.datastores.aggregation.metadata.models.Metric;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimension;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.MetricProjection;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.query.QueryResult;
import com.yahoo.elide.datastores.aggregation.query.TimeDimensionProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.EntityHydrator;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.VersionQuery;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialect;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialectFactory;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.QueryTranslator;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLDimensionProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLMetricProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLQuery;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLTimeDimensionProjection;
import com.yahoo.elide.request.Argument;
import com.yahoo.elide.request.Pagination;
import com.yahoo.elide.utils.coerce.CoerceUtil;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.sql.DataSource;

/**
 * QueryEngine for SQL backed stores.
 */
@Slf4j
public class SQLQueryEngine extends QueryEngine {

    @Getter
    private final SQLReferenceTable referenceTable;
    private final ConnectionDetails defaultConnectionDetails;
    private final Map<String, ConnectionDetails> connectionDetailsMap = new HashMap<>();

    public SQLQueryEngine(MetaDataStore metaDataStore,
                    com.yahoo.elide.contrib.dynamicconfighelpers.compile.ConnectionDetails defaultConnectionDetails) {
        super(metaDataStore);
        referenceTable = new SQLReferenceTable(metaDataStore);
        this.defaultConnectionDetails = new ConnectionDetails(defaultConnectionDetails.getDataSource(),
                        SQLDialectFactory.getDialect(defaultConnectionDetails.getDialect()));
    }

    /**
     * Constructor.
     * @param metaDataStore : MetaDataStore.
     * @param defaultConnectionDetails : default DataSource Object and SQLDialect Object.
     * @param detailsMap : Connection Name to DataSource Object and SQL Dialect Object mapping.
     */
    public SQLQueryEngine(MetaDataStore metaDataStore,
                    com.yahoo.elide.contrib.dynamicconfighelpers.compile.ConnectionDetails defaultConnectionDetails,
                    Map<String, com.yahoo.elide.contrib.dynamicconfighelpers.compile.ConnectionDetails> detailsMap) {
        this(metaDataStore, defaultConnectionDetails);
        detailsMap.forEach((name, details) -> {
            this.connectionDetailsMap.put(name, new ConnectionDetails(details.getDataSource(),
                            SQLDialectFactory.getDialect(details.getDialect())));
        });
    }

    private static final Function<ResultSet, Object> SINGLE_RESULT_MAPPER = new Function<ResultSet, Object>() {
        @Override
        public Object apply(ResultSet rs) {
            try {
                if (rs.next()) {
                    return rs.getObject(1);
                } else {
                    return null;
                }
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        }
    };

    @Override
    protected Table constructTable(Class<?> entityClass, EntityDictionary metaDataDictionary) {
        return new SQLTable(entityClass, metaDataDictionary, this);
    }

    @Override
    public ColumnProjection constructDimensionProjection(Dimension dimension,
                                                         String alias,
                                                         Map<String, Argument> arguments) {
        return new SQLDimensionProjection(dimension, alias, arguments, referenceTable);
    }

    @Override
    public TimeDimensionProjection constructTimeDimensionProjection(TimeDimension dimension,
                                                                    String alias,
                                                                    Map<String, Argument> arguments) {
        return new SQLTimeDimensionProjection(dimension, dimension.getTimezone(), referenceTable, alias, arguments);
    }

    @Override
    public MetricProjection constructMetricProjection(Metric metric,
                                                      String alias,
                                                      Map<String, Argument> arguments) {
        return new SQLMetricProjection(metric, referenceTable, alias, arguments);
    }

    /**
     * State needed for SQLQueryEngine to execute queries.
     */
    static class SqlTransaction implements QueryEngine.Transaction {

        private Connection conn;
        private final List<NamedParamPreparedStatement> stmts = new ArrayList<>();

        private void initializeConnection(DataSource dataSource) {
            try {
                this.conn = dataSource.getConnection();
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        }

        public NamedParamPreparedStatement initializeStatement(String namedParamQuery, DataSource dataSource) {
            NamedParamPreparedStatement stmt;
            try {
                if (conn == null || !conn.isValid(10)) {
                    initializeConnection(dataSource);
                }
                stmt = new NamedParamPreparedStatement(conn, namedParamQuery);
                stmts.add(stmt);
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
            return stmt;
        }

        @Override
        public void close() {
            stmts.forEach(stmt -> cancelAndCloseSoftly(stmt));
            closeSoftly(conn);
        }

        @Override
        public void cancel() {
            stmts.forEach(stmt -> cancelSoftly(stmt));
        }
    }

    @Override
    public QueryEngine.Transaction beginTransaction() {
        return new SqlTransaction();
    }

    @Override
    public QueryResult executeQuery(Query query, Transaction transaction) {
        SqlTransaction sqlTransaction = (SqlTransaction) transaction;

        String connectionName = query.getDbConnectionName();
        ConnectionDetails details = getConnectionDetails(connectionName);
        DataSource dataSource = details.getDataSource();
        SQLDialect dialect = details.getDialect();

        // Translate the query into SQL.
        SQLQuery sql = toSQL(query, dialect);
        String queryString = sql.toString();

        QueryResult.QueryResultBuilder resultBuilder = QueryResult.builder();
        NamedParamPreparedStatement stmt;

        Pagination pagination = query.getPagination();
        if (returnPageTotals(pagination)) {
            resultBuilder.pageTotals(getPageTotal(query, sql, sqlTransaction));
        }

        log.debug("SQL Query: " + queryString);
        stmt = sqlTransaction.initializeStatement(queryString, dataSource);

        // Supply the query parameters to the query
        supplyFilterQueryParameters(query, stmt);

        // Run the primary query and log the time spent.
        ResultSet resultSet = runQuery(stmt, queryString, Function.identity());

        resultBuilder.data(new EntityHydrator(resultSet, query, metadataDictionary).hydrate());
        return resultBuilder.build();
    }

    private long getPageTotal(Query query, SQLQuery sql, SqlTransaction sqlTransaction) {
        String connectionName = query.getDbConnectionName();
        ConnectionDetails details = getConnectionDetails(connectionName);
        DataSource dataSource = details.getDataSource();
        SQLDialect dialect = details.getDialect();
        String paginationSQL = toPageTotalSQL(sql, dialect).toString();

        NamedParamPreparedStatement stmt = sqlTransaction.initializeStatement(paginationSQL, dataSource);

        // Supply the query parameters to the query
        supplyFilterQueryParameters(query, stmt);

        // Run the Pagination query and log the time spent.
        return CoerceUtil.coerce(runQuery(stmt, paginationSQL, SINGLE_RESULT_MAPPER), Long.class);
    }

    @Override
    public String getTableVersion(Table table, Transaction transaction) {

        String tableVersion = null;
        Class<?> tableClass = metadataDictionary.getEntityClass(table.getName(), table.getVersion());
        VersionQuery versionAnnotation = tableClass.getAnnotation(VersionQuery.class);
        if (versionAnnotation != null) {
            String versionQueryString = versionAnnotation.sql();
            SqlTransaction sqlTransaction = (SqlTransaction) transaction;
            ConnectionDetails details = getConnectionDetails(table.getDbConnectionName());
            DataSource dataSource = details.getDataSource();
            NamedParamPreparedStatement stmt = sqlTransaction.initializeStatement(versionQueryString, dataSource);
            tableVersion = CoerceUtil.coerce(runQuery(stmt, versionQueryString, SINGLE_RESULT_MAPPER), String.class);
        }
        return tableVersion;
    }

    private <R> R runQuery(NamedParamPreparedStatement stmt, String queryString, Function<ResultSet, R> resultMapper) {

        // Run the query and log the time spent.
        return new TimedFunction<>(() -> {
            try {
                ResultSet rs = stmt.executeQuery();
                return resultMapper.apply(rs);
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        }, "Running Query: " + queryString
        ).get();
    }

    /**
     * Returns the actual query string(s) that would be executed for the input {@link Query}.
     *
     * @param query The query customized for a particular persistent storage or storage client.
     * @param dialect SQL dialect to use for this storage.
     * @return List of SQL string(s) corresponding to the given query.
     */
    public List<String> explain(Query query, SQLDialect dialect) {
        List<String> queries = new ArrayList<String>();
        SQLQuery sql = toSQL(query, dialect);

        Pagination pagination = query.getPagination();
        if (returnPageTotals(pagination)) {
            queries.add(toPageTotalSQL(sql, dialect).toString());
        }
        queries.add(sql.toString());
        return queries;
    }

    @Override
    public List<String> explain(Query query) {
        String connectionName = query.getDbConnectionName();
        return explain(query, getConnectionDetails(connectionName).getDialect());
    }

    /**
     * Translates the client query into SQL.
     *
     * @param query the client query.
     * @param sqlDialect the SQL dialect.
     * @return the SQL query.
     */
    private SQLQuery toSQL(Query query, SQLDialect sqlDialect) {
        //TODO - The result of merging the queries can result in multiple incompatible queries that should be split
        //apart, executed in parallel, and then stitched back together.

        Query merged = null;

        //Expand each metric into its own Query.  Merge them all together.
        for (MetricProjection metricProjection : query.getMetricProjections()) {
            Query metricQuery = metricProjection.getMetricFunction().resolve(query, metricProjection);
            merged = merge(merged, metricQuery);
        }

        merged = (merged == null) ? query : merged;

        QueryTranslator translator = new QueryTranslator(referenceTable, sqlDialect);

        return merged.accept(translator).clientQuery(query).build();
    }


    /**
     * Given a Prepared Statement, replaces any parameters with their values from client query.
     *
     * @param query The client query
     * @param stmt Customized Prepared Statement
     */
    private void supplyFilterQueryParameters(Query query, NamedParamPreparedStatement stmt) {

        Collection<FilterPredicate> predicates = new ArrayList<>();
        if (query.getWhereFilter() != null) {
            predicates.addAll(query.getWhereFilter().accept(new PredicateExtractionVisitor()));
        }

        if (query.getHavingFilter() != null) {
            predicates.addAll(query.getHavingFilter().accept(new PredicateExtractionVisitor()));
        }

        for (FilterPredicate filterPredicate : predicates) {
            if (filterPredicate.getOperator().isParameterized()) {
                boolean shouldEscape = filterPredicate.isMatchingOperator();
                filterPredicate.getParameters().forEach(param -> {
                    try {
                        stmt.setObject(param.getName(), shouldEscape ? param.escapeMatching() : param.getValue());
                    } catch (SQLException e) {
                        throw new IllegalStateException(e);
                    }
                });
            }
        }
    }

    /**
     * Takes a SQLQuery and creates a new clone that instead returns the total number of records of the original
     * query.
     *
     * @param sql The original query
     * @param sqlDialect the SQL dialect
     * @return A new query that returns the total number of records.
     */
    private SQLQuery toPageTotalSQL(SQLQuery sql, SQLDialect sqlDialect) {
        // TODO: refactor this method
        String groupByDimensions =
                sql.getClientQuery().getAllDimensionProjections()
                        .stream()
                        .map(dimension -> referenceTable.getResolvedReference(
                                sql.getClientQuery().getSource(),
                                dimension.getName()))
                        .collect(Collectors.joining(", "));

        String projectionClause = sqlDialect.generateCountDistinctClause(groupByDimensions);

        return SQLQuery.builder()
                .clientQuery(sql.getClientQuery())
                .projectionClause(projectionClause)
                .fromClause(sql.getFromClause())
                .joinClause(sql.getJoinClause())
                .whereClause(sql.getWhereClause())
                .havingClause(sql.getHavingClause())
                .build();
    }

    /**
     * Merges two SQL queries into one (if possible).
     *
     * @param second One query to merge
     * @param second The other query to merge
     * @return merged query
     */
    protected Query merge(Query first, Query second) {

        if (first == null) {
            return second;
        } else if (second == null) {
            return first;
        }

        assert first.getSource().equals(second.getSource());
        assert ((first.getWhereFilter() == null && second.getWhereFilter() == null)
                || first.getWhereFilter().equals(second.getWhereFilter()));
        assert ((first.getHavingFilter() == null && second.getHavingFilter() == null)
                || first.getHavingFilter().equals(second.getHavingFilter()));
        assert first.getTimeDimensionProjections().equals(second.getTimeDimensionProjections());
        assert first.getDimensionProjections().equals(second.getDimensionProjections());
        assert ((first.getSorting() == null && second.getSorting() == null)
                || first.getSorting().equals(second.getSorting()));
        assert ((first.getPagination() == null && second.getPagination() == null)
                || first.getPagination().equals(second.getPagination()));

        List<MetricProjection> merged = new ArrayList<>(first.getMetricProjections());
        merged.addAll(second.getMetricProjections());

        return Query.builder()
                .source(first.getSource())
                .metricProjections(merged)
                .dimensionProjections(first.getDimensionProjections())
                .timeDimensionProjections(first.getTimeDimensionProjections())
                .whereFilter(first.getWhereFilter())
                .havingFilter(first.getHavingFilter())
                .sorting(first.getSorting())
                .havingFilter(first.getHavingFilter())
                .pagination(first.getPagination())
                .build();
    }

    private static boolean returnPageTotals(Pagination pagination) {
        return pagination != null && pagination.returnPageTotals();
    }

    /**
     * Gets required ConnectionDetails.
     * @param connectionName Connection Name.
     * @return ConnectionDetails ConnectionDetails Object for this connection.
     */
    private ConnectionDetails getConnectionDetails(String connectionName) {
        if (connectionName == null || connectionName.trim().isEmpty()) {
            return defaultConnectionDetails;
        } else {
            return Optional.ofNullable(connectionDetailsMap.get(connectionName))
                            .orElseThrow(() -> new IllegalStateException(
                                            "ConnectionDetails undefined for DB Connection Name: " + connectionName));
        }
    }

    /**
     * Cancels NamedParamPreparedStatement, hides and logs any SQLException.
     * @param stmt NamedParamPreparedStatement to cancel.
     */
    private static void cancelSoftly(NamedParamPreparedStatement stmt) {
        try {
            if (stmt != null && !stmt.isClosed()) {
                stmt.cancel();
            }
        } catch (SQLException e) {
            log.error("Exception encountered during cancel statement.", e);
        }
    }

    /**
     * Cancels and Closes NamedParamPreparedStatement, hides and logs any SQLException.
     * @param stmt NamedParamPreparedStatement to close.
     */
    private static void cancelAndCloseSoftly(NamedParamPreparedStatement stmt) {
        cancelSoftly(stmt);
        try {
            if (stmt != null && !stmt.isClosed()) {
                stmt.close();
            }
        } catch (SQLException e) {
            log.error("Exception encountered during close statement.", e);
        }
    }

    /**
     * Closes Connection, hides and logs any SQLException.
     * @param conn Connection to close.
     */
    private static void closeSoftly(Connection conn) {
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException e) {
            log.error("Exception encountered during close connection.", e);
        }
    }
}
