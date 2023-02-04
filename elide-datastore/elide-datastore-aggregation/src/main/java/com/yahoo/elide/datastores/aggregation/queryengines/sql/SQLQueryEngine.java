/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql;

import static com.yahoo.elide.datastores.aggregation.metadata.ColumnContext.applyQuotes;

import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.filter.expression.PredicateExtractionVisitor;
import com.yahoo.elide.core.filter.predicates.FilterPredicate;
import com.yahoo.elide.core.request.Argument;
import com.yahoo.elide.core.request.Pagination;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.core.utils.TimedFunction;
import com.yahoo.elide.core.utils.coerce.CoerceUtil;
import com.yahoo.elide.datastores.aggregation.DefaultQueryValidator;
import com.yahoo.elide.datastores.aggregation.QueryEngine;
import com.yahoo.elide.datastores.aggregation.QueryValidator;
import com.yahoo.elide.datastores.aggregation.dynamic.NamespacePackage;
import com.yahoo.elide.datastores.aggregation.metadata.FormulaValidator;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ValueType;
import com.yahoo.elide.datastores.aggregation.metadata.models.Column;
import com.yahoo.elide.datastores.aggregation.metadata.models.Dimension;
import com.yahoo.elide.datastores.aggregation.metadata.models.Metric;
import com.yahoo.elide.datastores.aggregation.metadata.models.Namespace;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimension;
import com.yahoo.elide.datastores.aggregation.query.DefaultQueryPlanMerger;
import com.yahoo.elide.datastores.aggregation.query.DimensionProjection;
import com.yahoo.elide.datastores.aggregation.query.MetricProjection;
import com.yahoo.elide.datastores.aggregation.query.Optimizer;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.query.QueryPlan;
import com.yahoo.elide.datastores.aggregation.query.QueryPlanMerger;
import com.yahoo.elide.datastores.aggregation.query.QueryResult;
import com.yahoo.elide.datastores.aggregation.query.TimeDimensionProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromSubquery;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromTable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.VersionQuery;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialect;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.NativeQuery;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.QueryPlanTranslator;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.QueryTranslator;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLColumnProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLDimensionProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLTimeDimensionProjection;
import com.yahoo.elide.datastores.aggregation.timegrains.Time;
import com.yahoo.elide.datastores.aggregation.validator.ColumnArgumentValidator;
import com.yahoo.elide.datastores.aggregation.validator.TableArgumentValidator;
import com.google.common.base.Preconditions;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.Annotation;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.sql.DataSource;

/**
 * QueryEngine for SQL backed stores.
 */
@Slf4j
public class SQLQueryEngine extends QueryEngine {

    @Getter
    private final Set<Optimizer> optimizers;
    private final QueryValidator validator;
    private final FormulaValidator formulaValidator;
    private final Function<String, ConnectionDetails> connectionDetailsLookup;
    private final QueryPlanMerger merger;

    public SQLQueryEngine(MetaDataStore metaDataStore, Function<String, ConnectionDetails> connectionDetailsLookup) {
        this(metaDataStore, connectionDetailsLookup, new HashSet<>(), new DefaultQueryPlanMerger(metaDataStore),
                new DefaultQueryValidator(metaDataStore.getMetadataDictionary()));
    }

    /**
     * Constructor.
     * @param metaDataStore : MetaDataStore.
     * @param connectionDetailsLookup : maps a connection name to meta info about the connection.
     * @param optimizers The set of enabled optimizers.
     * @param merger Merges multiple plans into a smaller set (one if possible)
     * @param validator Validates each incoming client query.
     */
    public SQLQueryEngine(
            MetaDataStore metaDataStore,
            Function<String, ConnectionDetails> connectionDetailsLookup,
            Set<Optimizer> optimizers,
            QueryPlanMerger merger,
            QueryValidator validator
    ) {

        Preconditions.checkNotNull(connectionDetailsLookup);

        this.connectionDetailsLookup = connectionDetailsLookup;
        this.metaDataStore = metaDataStore;
        this.validator = validator;
        this.formulaValidator = new FormulaValidator(metaDataStore);
        this.metadataDictionary = metaDataStore.getMetadataDictionary();
        populateMetaData(metaDataStore);
        this.optimizers = optimizers;
        this.merger = merger;
    }

    private static final Function<ResultSet, Object> SINGLE_RESULT_MAPPER = rs -> {
        try {
            if (rs.next()) {
                return rs.getObject(1);
            }
            return null;
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    };

    @Override
    protected Namespace constructNamespace(NamespacePackage namespacePackage) {
        return new Namespace(namespacePackage);
    }

    @Override
    protected Table constructTable(Namespace namespace, Type<?> entityClass, EntityDictionary metaDataDictionary) {
        String dbConnectionName = null;
        Annotation annotation = EntityDictionary.getFirstAnnotation(entityClass,
                        Arrays.asList(FromTable.class, FromSubquery.class));
        if (annotation instanceof FromTable) {
            dbConnectionName = ((FromTable) annotation).dbConnectionName();
        } else if (annotation instanceof FromSubquery) {
            dbConnectionName = ((FromSubquery) annotation).dbConnectionName();
        }

        ConnectionDetails connectionDetails = connectionDetailsLookup.apply(dbConnectionName);
        return new SQLTable(namespace, entityClass, metaDataDictionary, connectionDetails);
    }

    @Override
    public DimensionProjection constructDimensionProjection(Dimension dimension,
                                                            String alias,
                                                            Map<String, Argument> arguments) {
        return new SQLDimensionProjection(dimension, alias, arguments, true);
    }

    @Override
    public TimeDimensionProjection constructTimeDimensionProjection(TimeDimension dimension,
                                                                    String alias,
                                                                    Map<String, Argument> arguments) {
        return new SQLTimeDimensionProjection(dimension, dimension.getTimezone(), alias, arguments, true);
    }

    @Override
    public MetricProjection constructMetricProjection(Metric metric,
                                                      String alias,
                                                      Map<String, Argument> arguments) {
        return metric.getMetricProjectionMaker().make(metric, alias, arguments);
    }

    @Override
    protected void verifyMetaData(MetaDataStore metaDataStore) {
        metaDataStore.getTables().forEach(table -> {
            SQLTable sqlTable = (SQLTable) table;

            checkForCycles(sqlTable);

            TableArgumentValidator tableArgValidator = new TableArgumentValidator(metaDataStore, sqlTable);
            tableArgValidator.validate();

            sqlTable.getAllColumns().forEach(column -> {
                ColumnArgumentValidator colArgValidator = new ColumnArgumentValidator(metaDataStore, sqlTable, column);
                colArgValidator.validate();
            });
        });
    }

    /**
     * Verify that there is no reference loop for given {@link SQLTable}.
     * @param sqlTable Queryable to validate.
     */
    private void checkForCycles(SQLTable sqlTable) {
        sqlTable.getColumnProjections().forEach(column -> formulaValidator.parse(sqlTable, column));
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
            stmts.forEach(SQLQueryEngine::cancelAndCloseSoftly);
            closeSoftly(conn);
        }

        @Override
        public void cancel() {
            stmts.forEach(SQLQueryEngine::cancelSoftly);
        }
    }

    @Override
    public QueryEngine.Transaction beginTransaction() {
        return new SqlTransaction();
    }

    @Override
    public QueryResult executeQuery(Query query, Transaction transaction) {
        SqlTransaction sqlTransaction = (SqlTransaction) transaction;
        ConnectionDetails details = query.getConnectionDetails();
        DataSource dataSource = details.getDataSource();
        SQLDialect dialect = details.getDialect();

        Query expandedQuery = expandMetricQueryPlans(query);

        // Translate the query into SQL.
        NativeQuery sql = toSQL(expandedQuery, dialect);
        String queryString = sql.toString();

        QueryResult.QueryResultBuilder resultBuilder = QueryResult.builder();
        NamedParamPreparedStatement stmt;

        Pagination pagination = query.getPagination();
        if (returnPageTotals(pagination)) {
            resultBuilder.pageTotals(getPageTotal(expandedQuery, sql, query, sqlTransaction));
        }

        log.debug("SQL Query: " + queryString);
        stmt = sqlTransaction.initializeStatement(queryString, dataSource);

        // Supply the query parameters to the query
        supplyFilterQueryParameters(query, stmt, dialect);

        // Run the primary query and log the time spent.
        ResultSet resultSet = runQuery(stmt, queryString, Function.identity());

        resultBuilder.data(new EntityHydrator(resultSet, query, metadataDictionary));
        return resultBuilder.build();
    }

    private long getPageTotal(Query expandedQuery, NativeQuery sql, Query clientQuery, SqlTransaction sqlTransaction) {
        ConnectionDetails details = expandedQuery.getConnectionDetails();
        DataSource dataSource = details.getDataSource();
        SQLDialect dialect = details.getDialect();
        NativeQuery paginationSQL = toPageTotalSQL(expandedQuery, sql, dialect);

        if (paginationSQL == null) {
            // The query returns the aggregated metric without any dimension.
            // Only 1 record will be returned.
            return 1;
        }

        NamedParamPreparedStatement stmt = sqlTransaction.initializeStatement(paginationSQL.toString(), dataSource);

        // Supply the query parameters to the query
        supplyFilterQueryParameters(clientQuery, stmt, dialect);

        // Run the Pagination query and log the time spent.
        Long result = CoerceUtil.coerce(runQuery(stmt, paginationSQL.toString(), SINGLE_RESULT_MAPPER), Long.class);

        return (result != null) ? result : 0;
    }

    @Override
    public String getTableVersion(Table table, Transaction transaction) {

        String tableVersion = null;
        SQLTable sqlTable = (SQLTable) table;
        Type<?> tableClass = metadataDictionary.getEntityClass(table.getName(), table.getVersion());
        VersionQuery versionAnnotation = tableClass.getAnnotation(VersionQuery.class);
        if (versionAnnotation != null) {
            String versionQueryString = versionAnnotation.sql();
            SqlTransaction sqlTransaction = (SqlTransaction) transaction;
            ConnectionDetails details = sqlTable.getConnectionDetails();
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
        List<String> queries = new ArrayList<>();
        Query expandedQuery = expandMetricQueryPlans(query);
        NativeQuery sql = toSQL(expandedQuery, dialect);

        Pagination pagination = query.getPagination();
        if (returnPageTotals(pagination)) {
            NativeQuery paginationSql = toPageTotalSQL(expandedQuery, sql, dialect);
            if (paginationSql != null) {
                queries.add(paginationSql.toString());
            }
        }
        queries.add(sql.toString());
        return queries;
    }

    @Override
    public List<String> explain(Query query) {
        return explain(query, query.getConnectionDetails().getDialect());
    }

    @Override
    public QueryValidator getValidator() {
        return validator;
    }

    /**
     * Translates the client query into SQL.
     *
     * @param query the transformed client query.
     * @param sqlDialect the SQL dialect.
     * @return the SQL query.
     */
    private NativeQuery toSQL(Query query, SQLDialect sqlDialect) {
        QueryTranslator translator = new QueryTranslator(metaDataStore, sqlDialect, query);

        return query.accept(translator).build();
    }

    /**
     * Transforms a client query into a potentially nested/complex query by expanding each metric into
     * its respective query plan - and then merging the plans together into a consolidated query.
     * @param query The client query.
     * @return A query that reflects each metric's individual query plan.
     */
    private Query expandMetricQueryPlans(Query query) {

        //Expand each metric into its own query plan.
        List<QueryPlan> toMerge = query.getMetricProjections().stream()
                .map(projection -> projection.resolve(query))
                .collect(Collectors.toList());

        //Merge all the queries together.
        List<QueryPlan> mergedPlans = merger.merge(toMerge);

        //TODO - Support joins across plans rather than rejecting plans that don't merge.
        if (mergedPlans.size() != 1) {
            throw new UnsupportedOperationException("Incompatible metrics in client query.  Cannot merge "
                    + "into a single query");
        }

        QueryPlan mergedPlan = mergedPlans.get(0);

        QueryPlanTranslator queryPlanTranslator = new QueryPlanTranslator(query, metaDataStore, merger);

        Query merged = (mergedPlan == null)
                ? QueryPlanTranslator.addHiddenProjections(metaDataStore, query).build()
                : queryPlanTranslator.translate(mergedPlan);

        for (Optimizer optimizer : optimizers) {
            SQLTable table = (SQLTable) query.getSource();

            //TODO - support hints in table joins & query header.  Query Header hints override join hints which
            //override table hints.
            if (table.getHints().contains(optimizer.negateHint())) {
                continue;
            }

            if (! table.getHints().contains(optimizer.hint())) {
                continue;
            }

            if (optimizer.canOptimize(merged)) {
                merged = optimizer.optimize(merged);
            }
        }

        return merged;
    }

    /**
     * Given a Prepared Statement, replaces any parameters with their values from client query.
     *
     * @param query The client query
     * @param stmt Customized Prepared Statement
     * @param dialect the SQL dialect
     */
    private void supplyFilterQueryParameters(Query query, NamedParamPreparedStatement stmt, SQLDialect dialect) {

        Collection<FilterPredicate> predicates = new ArrayList<>();
        if (query.getWhereFilter() != null) {
            predicates.addAll(query.getWhereFilter().accept(new PredicateExtractionVisitor()));
        }

        if (query.getHavingFilter() != null) {
            predicates.addAll(query.getHavingFilter().accept(new PredicateExtractionVisitor()));
        }

        for (FilterPredicate filterPredicate : predicates) {
            Column column = metaDataStore.getColumn(filterPredicate.getEntityType(), filterPredicate.getField());
            if (filterPredicate.getOperator().isParameterized()) {
                boolean shouldEscape = filterPredicate.isMatchingOperator();
                filterPredicate.getParameters().forEach(param -> {
                    try {
                        Object value = param.getValue();

                        value = convertForJdbc(filterPredicate.getEntityType(), column, value, dialect);

                        stmt.setObject(param.getName(), shouldEscape ? param.escapeMatching() : value);
                    } catch (SQLException e) {
                        throw new IllegalStateException(e);
                    }
                });
            }
        }
    }

    private Object convertForJdbc(Type<?> parent, Column column, Object value, SQLDialect dialect) {
        if (column.getValueType().equals(ValueType.TIME) && (Time.class).isAssignableFrom(value.getClass())) {
            return dialect.translateTimeToJDBC((Time) value);
        }

        if (value.getClass().isEnum()) {
            Enumerated enumerated =
                    metadataDictionary.getAttributeOrRelationAnnotation(parent, Enumerated.class, column.getName());

            if (enumerated != null && enumerated.value().equals(EnumType.ORDINAL)) {
                return ((Enum) value).ordinal();
            } else {
                return value.toString();
            }
        }

        if ((column.getValueType().equals(ValueType.TEXT)
                && column.getValues() != null
                && column.getValues().isEmpty() == false)) {
            Enumerated enumerated =
                    metadataDictionary.getAttributeOrRelationAnnotation(parent, Enumerated.class, column.getName());

            if (enumerated != null && enumerated.value().equals(EnumType.ORDINAL)) {

                String [] enumValues = column.getValues().toArray(new String[0]);
                for (int idx = 0; idx < column.getValues().size(); idx++) {
                    if (enumValues[idx].equals(value)) {
                        return idx;
                    }
                }

                throw new IllegalStateException(String.format("Invalid value %s for column %s",
                        value, column.getName()));
            }
        }

        return value;
    }

    /**
     * Takes a SQLQuery and creates a new clone that instead returns the total number of records of the original
     * query.
     *
     * @param query The client query
     * @param sql The generated SQL query
     * @param sqlDialect the SQL dialect
     * @return A new query that returns the total number of records.
     */
    private NativeQuery toPageTotalSQL(Query query, NativeQuery sql, SQLDialect sqlDialect) {

        // TODO: refactor this method
        String groupByDimensions =
                query.getAllDimensionProjections()
                        .stream()
                        .map(SQLColumnProjection.class::cast)
                        .filter(SQLColumnProjection::isProjected)
                        .map((column) -> column.toSQL(query, metaDataStore))
                        .collect(Collectors.joining(", "));

        if (groupByDimensions.isEmpty()) {
            // When no dimension projection is available, assume that metric projection is used.
            // Metric projection without group by dimension will return onely 1 record.
            return null;
        }

        NativeQuery innerQuery =  NativeQuery.builder()
                .projectionClause(groupByDimensions)
                .fromClause(sql.getFromClause())
                .joinClause(sql.getJoinClause())
                .whereClause(sql.getWhereClause())
                .groupByClause(String.format("GROUP BY %s", groupByDimensions))
                .havingClause(sql.getHavingClause())
                .build();

        return NativeQuery.builder()
                .projectionClause("COUNT(*)")
                .fromClause(QueryTranslator.getFromClause("(" + innerQuery + ")",
                                                          applyQuotes("pagination_subquery", sqlDialect),
                                                          sqlDialect))
                .build();
    }

    private static boolean returnPageTotals(Pagination pagination) {
        return pagination != null && pagination.returnPageTotals();
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
