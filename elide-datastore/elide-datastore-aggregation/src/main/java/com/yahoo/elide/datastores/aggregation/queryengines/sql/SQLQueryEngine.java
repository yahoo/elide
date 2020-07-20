/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql;

import static com.yahoo.elide.utils.TypeHelper.getTypeAlias;

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
import com.yahoo.elide.utils.coerce.CoerceUtil;

import org.hibernate.jpa.QueryHints;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;

/**
 * QueryEngine for SQL backed stores.
 */
@Slf4j
public class SQLQueryEngine extends QueryEngine {
    private final EntityManagerFactory entityManagerFactory;
    private final Consumer<EntityManager> transactionCancel;
    private final SQLReferenceTable referenceTable;

    public SQLQueryEngine(MetaDataStore metaDataStore, EntityManagerFactory eMFactory, Consumer<EntityManager> txC) {
        super(metaDataStore);
        this.entityManagerFactory = eMFactory;
        this.referenceTable = new SQLReferenceTable(metaDataStore);
        this.transactionCancel = txC;
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
    static class SqlTransaction implements QueryEngine.Transaction  {

        private final EntityManager entityManager;
        private final EntityTransaction transaction;
        private final Consumer<EntityManager> transactionCancel;

        SqlTransaction(EntityManagerFactory emf, Consumer<EntityManager> transactionCancel) {

            entityManager = emf.createEntityManager();
            transaction = entityManager.getTransaction();
            this.transactionCancel = transactionCancel;
            if (!transaction.isActive()) {
                transaction.begin();
            }
        }

        @Override
        public void close() {
            if (transaction != null && transaction.isActive()) {
                transaction.commit();
            }
            if (entityManager != null) {
                entityManager.close();
            }
        }

        @Override
        public void cancel() {
            transactionCancel.accept(entityManager);
        }

    }

    @Override
    public QueryEngine.Transaction beginTransaction() {
        return new SqlTransaction(entityManagerFactory, transactionCancel);
    }

    @Override
    public QueryResult executeQuery(Query query, Transaction transaction) {
        EntityManager entityManager = ((SqlTransaction) transaction).entityManager;

        // Translate the query into SQL.
        SQLQuery sql = toSQL(query);
        String queryString = sql.toString();
        log.debug("SQL Query: " + queryString);
        javax.persistence.Query jpaQuery = entityManager.createNativeQuery(queryString);

        QueryResult.QueryResultBuilder resultBuilder = QueryResult.builder();

        Pagination pagination = query.getPagination();
        if (pagination != null) {
            jpaQuery.setFirstResult(pagination.getOffset());
            jpaQuery.setMaxResults(pagination.getLimit());
            if (pagination.returnPageTotals()) {
                resultBuilder.pageTotals(getPageTotal(query, sql, entityManager));
            }
        }

        // Supply the query parameters to the query
        supplyFilterQueryParameters(query, jpaQuery);

        // Run the primary query and log the time spent.
        List<Object> results = new TimedFunction<List<Object>>(
                () -> jpaQuery.setHint(QueryHints.HINT_READONLY, true).getResultList(),
                "Running Query: " + queryString).get();

        resultBuilder.data(new SQLEntityHydrator(results, query, getMetadataDictionary(), entityManager).hydrate());
        return resultBuilder.build();
    }

    private long getPageTotal(Query query, SQLQuery sql, EntityManager entityManager) {
        String paginationSQL = toPageTotalSQL(sql).toString();

        javax.persistence.Query pageTotalQuery =
                entityManager.createNativeQuery(paginationSQL)
                        .setHint(QueryHints.HINT_READONLY, true);

        //Supply the query parameters to the query
        supplyFilterQueryParameters(query, pageTotalQuery);

        //Run the Pagination query and log the time spent.
        return new TimedFunction<>(
                () -> CoerceUtil.coerce(pageTotalQuery.getSingleResult(), Long.class),
                "Running Query: " + paginationSQL
        ).get();
    }

    @Override
    public String getTableVersion(Table table, Transaction transaction) {
        EntityManager entityManager = ((SqlTransaction) transaction).entityManager;

        String tableVersion = null;
        Class<?> tableClass = getMetadataDictionary().getEntityClass(table.getName(), table.getVersion());
        VersionQuery versionAnnotation = tableClass.getAnnotation(VersionQuery.class);
        if (versionAnnotation != null) {
            String versionQueryString = versionAnnotation.sql();
            javax.persistence.Query versionQuery =
                    entityManager.createNativeQuery(versionQueryString)
                            .setHint(QueryHints.HINT_READONLY, true);
            tableVersion = new TimedFunction<>(
                    () -> CoerceUtil.coerce(versionQuery.getSingleResult(), String.class),
                    "Running Query: " + versionQueryString
            ).get();
        }
        return tableVersion;
    }

    @Override
    public String explain(Query query) {
        return toSQL(query).toString();
    }

    /**
     * Translates the client query into SQL.
     *
     * @param query the client query.
     * @return the SQL query.
     */
    private SQLQuery toSQL(Query query) {
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

        return new SQLQueryConstructor(referenceTable).resolveTemplate(
                query,
                queryTemplate,
                query.getSorting(),
                query.getWhereFilter(),
                query.getHavingFilter());
    }


    /**
     * Given a JPA query, replaces any parameters with their values from client query.
     *
     * @param query The client query
     * @param jpaQuery The JPA query
     */
    private void supplyFilterQueryParameters(Query query,
                                             javax.persistence.Query jpaQuery) {

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
                    jpaQuery.setParameter(param.getName(), shouldEscape ? param.escapeMatching() : param.getValue());
                });
            }
        }
    }

    /**
     * Takes a SQLQuery and creates a new clone that instead returns the total number of records of the original
     * query.
     *
     * @param sql The original query
     * @return A new query that returns the total number of records.
     */
    private SQLQuery toPageTotalSQL(SQLQuery sql) {
        // TODO: refactor this method
        String groupByDimensions =
                extractSQLDimensions(sql.getClientQuery(), sql.getClientQuery().getTable())
                        .stream()
                        .map(dimension -> referenceTable.getResolvedReference(
                                sql.getClientQuery().getTable(),
                                dimension.getName()))
                        .collect(Collectors.joining(", "));

        String projectionClause = String.format("COUNT(DISTINCT(%s))", groupByDimensions);

        return SQLQuery.builder()
                .clientQuery(sql.getClientQuery())
                .projectionClause(projectionClause)
                .fromClause(sql.getFromClause())
                .joinClause(sql.getJoinClause())
                .whereClause(sql.getWhereClause())
                .build();
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
