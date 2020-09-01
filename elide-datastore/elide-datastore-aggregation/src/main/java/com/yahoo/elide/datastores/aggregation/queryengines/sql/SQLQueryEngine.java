/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql;

import static com.yahoo.elide.utils.TypeHelper.getTypeAlias;

import com.yahoo.elide.contrib.dynamicconfighelpers.compile.ElideDynamicEntityCompiler;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.TimedFunction;
import com.yahoo.elide.core.exceptions.InvalidPredicateException;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.expression.PredicateExtractionVisitor;
import com.yahoo.elide.datastores.aggregation.QueryEngine;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.metadata.models.Column;
import com.yahoo.elide.datastores.aggregation.metadata.models.Dimension;
import com.yahoo.elide.datastores.aggregation.metadata.models.Metric;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimension;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.MetricProjection;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.query.QueryResult;
import com.yahoo.elide.datastores.aggregation.query.TimeDimensionProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.VersionQuery;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialect;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialectFactory;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLMetric;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metric.SQLMetricFunction;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLColumnProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLMetricProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLQuery;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLQueryConstructor;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLQueryTemplate;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLTimeDimensionProjection;
import com.yahoo.elide.request.Argument;
import com.yahoo.elide.request.Pagination;

import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.sql.DataSource;

/**
 * QueryEngine for SQL backed stores.
 */
@Slf4j
public class SQLQueryEngine extends QueryEngine {
    private final SQLReferenceTable referenceTable;
    private final DataSource defaultDataSource;
    private final Map<String, DataSource> dataSourceMap = new HashMap<>();
    private final SQLDialect defaultDialect;
    private final Map<String, SQLDialect> dialectMap = new HashMap<>();

    public SQLQueryEngine(MetaDataStore metaDataStore, DataSource defaultDataSource, String defaultDialect) {
        super(metaDataStore);
        this.referenceTable = new SQLReferenceTable(metaDataStore);
        this.defaultDataSource = defaultDataSource;
        this.defaultDialect = SQLDialectFactory.getDialect(defaultDialect);
    }

    /**
     * Constructor.
     * @param metaDataStore : MetaDataStore.
     * @param defaultDataSource : default DataSource.
     * @param defaultDialect : default Sql Dialect.
     * @param dataSourceMap : Connection Name to DataSource mapping.
     * @param dialectMap : Connection Name to Sql Dialect Name mapping.
     */
    public SQLQueryEngine(MetaDataStore metaDataStore, DataSource defaultDataSource, String defaultDialect,
                    Map<String, DataSource> dataSourceMap, Map<String, String> dialectMap) {
        this(metaDataStore, defaultDataSource, defaultDialect);
        this.dataSourceMap.putAll(dataSourceMap);
        dialectMap.forEach((k, v) -> this.dialectMap.put(k, SQLDialectFactory.getDialect(v)));
    }

    public SQLQueryEngine(MetaDataStore metaDataStore, DataSource defaultDataSource, String defaultDialect,
                    ElideDynamicEntityCompiler compiler) {
        this(metaDataStore, defaultDataSource, defaultDialect, compiler.getDataSourceMap(), compiler.getDialectMap());
    }

    @Override
    protected Table constructTable(Class<?> entityClass, EntityDictionary metaDataDictionary) {
        return new SQLTable(entityClass, metaDataDictionary);
    }

    @Override
    public ColumnProjection constructDimensionProjection(Dimension dimension,
                                                         String alias,
                                                         Map<String, Argument> arguments) {
        return new SQLColumnProjection() {
            @Override
            public SQLReferenceTable getReferenceTable() {
                return referenceTable;
            }

            @Override
            public Column getColumn() {
                return dimension;
            }

            @Override
            public String getAlias() {
                return alias;
            }

            @Override
            public Map<String, Argument> getArguments() {
                return arguments;
            }
        };
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

        private final Connection conn;
        private final List<NamedParamPreparedStatement> stmts = new ArrayList<>();
        private final SQLDialect dialect;

        SqlTransaction(DataSource dataSource, SQLDialect dialect) {
            this.dialect = dialect;
            try {
                this.conn = dataSource.getConnection();
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        }

        public NamedParamPreparedStatement initializeStatement(String namedParamQuery) {
            NamedParamPreparedStatement stmt;
            try {
                stmt = new NamedParamPreparedStatement(conn, namedParamQuery);
                stmts.add(stmt);
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
            return stmt;
        }

        @Override
        public void close() {
            stmts.forEach(stmt -> {
                try {
                    if (stmt != null && !stmt.isClosed()) {
                        stmt.close();
                    }
                } catch (SQLException e) {
                    throw new IllegalStateException(e);
                }
            });
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public void cancel() {
            stmts.forEach(stmt -> {
                try {
                    if (stmt != null && !stmt.isClosed()) {
                        stmt.cancel();
                    }
                } catch (SQLException e) {
                    throw new IllegalStateException(e);
                }
            });
        }
    }

    @Override
    public QueryEngine.Transaction beginTransaction() {
        return new SqlTransaction(defaultDataSource, defaultDialect);
    }

    @Override
    public Transaction beginTransaction(String dbConnectionName) {
        DataSource dataSource = Optional.ofNullable(dataSourceMap.get(dbConnectionName))
                        .orElseThrow(() -> new IllegalStateException(
                                        "DataSource or DB Configuration undefined for DB Connection Name: "
                                                        + dbConnectionName));
        SQLDialect dialect = Optional.ofNullable(dialectMap.get(dbConnectionName))
                        .orElseThrow(() -> new IllegalStateException(
                                        "SQL Dialect Type undefined for DB Connection Name: "
                                                        + dbConnectionName));
        return new SqlTransaction(dataSource, dialect);
    }

    @Override
    public QueryResult executeQuery(Query query, Transaction transaction) {
        SqlTransaction sqlTransaction = (SqlTransaction) transaction;
        SQLDialect dialect = sqlTransaction.dialect;

        // Translate the query into SQL.
        SQLQuery sql = toSQL(query, dialect);
        String queryString = sql.toString();

        QueryResult.QueryResultBuilder resultBuilder = QueryResult.builder();
        NamedParamPreparedStatement stmt;

        Pagination pagination = query.getPagination();
        if (pagination != null) {
            queryString = appendOffsetLimit(queryString, dialect, pagination.getOffset(), pagination.getLimit());
            if (pagination.returnPageTotals()) {
                resultBuilder.pageTotals(getPageTotal(query, sql, sqlTransaction));
            }
        }

        log.debug("SQL Query: " + queryString);
        stmt = sqlTransaction.initializeStatement(queryString);

        // Supply the query parameters to the query
        supplyFilterQueryParameters(query, stmt);

        // Run the primary query and log the time spent.
        ResultSet resultSet = new TimedFunction<ResultSet>(() -> {
            try {
                return stmt.executeQuery();
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        }, "Running Query: " + queryString
        ).get();

        resultBuilder.data(new SQLEntityHydrator(resultSet, query, getMetadataDictionary()).hydrate());
        return resultBuilder.build();
    }

    private long getPageTotal(Query query, SQLQuery sql, SqlTransaction sqlTransaction) {
        String paginationSQL = toPageTotalSQL(sql, sqlTransaction.dialect).toString();

        NamedParamPreparedStatement stmt = sqlTransaction.initializeStatement(paginationSQL);

        // Supply the query parameters to the query
        supplyFilterQueryParameters(query, stmt);

        // Run the Pagination query and log the time spent.
        return new TimedFunction<>(() -> {
            try {
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return rs.getLong(1);
                } else {
                    return null;
                }
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        }, "Running Query: " + paginationSQL
        ).get();
    }

    @Override
    public String getTableVersion(Table table, Transaction transaction) {
        SqlTransaction sqlTransaction = (SqlTransaction) transaction;

        String tableVersion = null;
        Class<?> tableClass = getMetadataDictionary().getEntityClass(table.getName(), table.getVersion());
        VersionQuery versionAnnotation = tableClass.getAnnotation(VersionQuery.class);
        if (versionAnnotation != null) {
            String versionQueryString = versionAnnotation.sql();
            NamedParamPreparedStatement stmt = sqlTransaction.initializeStatement(versionQueryString);
            tableVersion = new TimedFunction<>(() -> {
                try {
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        return rs.getString(1);
                    } else {
                        return null;
                    }
                } catch (SQLException e) {
                    throw new IllegalStateException(e);
                }
            }, "Running Query: " + versionQueryString
            ).get();
        }
        return tableVersion;
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
        if (pagination != null) {
            if (pagination.returnPageTotals()) {
                queries.add(toPageTotalSQL(sql, dialect).toString());
            }
            queries.add(appendOffsetLimit(sql.toString(), dialect, pagination.getOffset(), pagination.getLimit()));
        } else {
            queries.add(sql.toString());
        }
        return queries;
    }

    @Override
    public List<String> explain(Query query) {
        return explain(query, defaultDialect);
    }

    @Override
    public List<String> explain(Query query, String dbConnectionName) {
        SQLDialect dialect = dialectMap.get(dbConnectionName);
        return explain(query, dialect);
    }

    /**
     * Translates the client query into SQL.
     *
     * @param query the client query.
     * @param sqlDialect the SQL dialect.
     * @return the SQL query.
     */
    private SQLQuery toSQL(Query query, SQLDialect sqlDialect) {
        Set<ColumnProjection> groupByDimensions = new LinkedHashSet<>(query.getGroupByDimensions());
        Set<TimeDimensionProjection> timeDimensions = new LinkedHashSet<>(query.getTimeDimensions());

        SQLQueryTemplate queryTemplate = query.getMetrics().stream()
                .map(metricProjection -> {
                    if (!(metricProjection.getColumn().getMetricFunction() instanceof SQLMetricFunction)) {
                        throw new InvalidPredicateException(
                                "Non-SQL metric function on " + metricProjection.getAlias());
                    }

                    return ((SQLMetric) metricProjection.getColumn()).resolve(query, metricProjection, referenceTable);
                })
                .reduce(SQLQueryTemplate::merge)
                .orElse(new SQLQueryTemplate(query));

        return new SQLQueryConstructor(referenceTable, sqlDialect).resolveTemplate(
                query,
                queryTemplate,
                query.getSorting(),
                query.getWhereFilter(),
                query.getHavingFilter());
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
                extractSQLDimensions(sql.getClientQuery(), sql.getClientQuery().getTable())
                        .stream()
                        .map(dimension -> referenceTable.getResolvedReference(
                                sql.getClientQuery().getTable(),
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
     * Appends offset and limit to input SQL clause.
     * @param sql The original query string
     * @param dialect the SQL dialect
     * @param offset position of the first record.
     * @param limit maximum number of record.
     * @return A new query string with offset and limit.
     */
    private String appendOffsetLimit(String sql, SQLDialect dialect, int offset, int limit) {
        return dialect.appendOffsetLimit(sql, offset, limit);
    }

    /**
     * Extract dimension projects in a query to sql dimensions.
     *
     * @param query requested query
     * @param table queried table
     * @return sql dimensions in this query
     */
    private List<Dimension> extractSQLDimensions(Query query, Table table) {
        return query.getDimensions().stream()
                .map(projection -> table.getDimension(projection.getColumn().getName()))
                .collect(Collectors.toList());
    }

    /**
     * Get alias for an entity class.
     *
     * @param entityClass entity class
     * @return alias
     */
    public static String getClassAlias(Class<?> entityClass) {
        return getTypeAlias(entityClass);
    }
}
