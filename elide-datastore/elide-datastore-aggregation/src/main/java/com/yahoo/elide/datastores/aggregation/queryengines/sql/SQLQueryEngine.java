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
import com.yahoo.elide.datastores.aggregation.metadata.metric.MetricFunctionInvocation;
import com.yahoo.elide.datastores.aggregation.metadata.models.Dimension;
import com.yahoo.elide.datastores.aggregation.metadata.models.MetricFunction;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.query.TimeDimensionProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metric.SQLMetricFunction;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLQuery;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLQueryConstructor;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLQueryTemplate;
import com.yahoo.elide.request.Pagination;
import com.yahoo.elide.utils.coerce.CoerceUtil;

import org.hibernate.jpa.QueryHints;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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

    private final SQLReferenceTable referenceTable;

    public SQLQueryEngine(MetaDataStore metaDataStore, EntityManagerFactory entityManagerFactory) {
        super(metaDataStore);
        this.entityManagerFactory = entityManagerFactory;
        this.referenceTable = new SQLReferenceTable(metaDataStore);
    }

    @Override
    protected Table constructTable(Class<?> entityClass, EntityDictionary metaDataDictionary) {
        return new SQLTable(entityClass, metaDataDictionary);
    }

    @Override
    public Iterable<Object> executeQuery(Query query) {
        EntityManager entityManager = null;
        EntityTransaction transaction = null;
        try {
            entityManager = entityManagerFactory.createEntityManager();

            // manually begin the transaction
            transaction = entityManager.getTransaction();
            if (!transaction.isActive()) {
                transaction.begin();
            }

            // Translate the query into SQL.
            SQLQuery sql = toSQL(query);
            log.debug("SQL Query: " + sql);

            javax.persistence.Query jpaQuery = entityManager.createNativeQuery(sql.toString());

            Pagination pagination = query.getPagination();
            if (pagination != null) {
                jpaQuery.setFirstResult(pagination.getOffset());
                jpaQuery.setMaxResults(pagination.getLimit());

                if (pagination.returnPageTotals()) {

                    SQLQuery paginationSQL = toPageTotalSQL(sql);
                    javax.persistence.Query pageTotalQuery =
                            entityManager.createNativeQuery(paginationSQL.toString())
                                    .setHint(QueryHints.HINT_READONLY, true);

                    //Supply the query parameters to the query
                    supplyFilterQueryParameters(query, pageTotalQuery);

                    //Run the Pagination query and log the time spent.
                    long total = new TimedFunction<>(
                            () -> CoerceUtil.coerce(pageTotalQuery.getSingleResult(), Long.class),
                            "Running Query: " + paginationSQL
                    ).get();

                    pagination.setPageTotals(total);
                }
            }

            // Supply the query parameters to the query
            supplyFilterQueryParameters(query, jpaQuery);

            // Run the primary query and log the time spent.
            List<Object> results = new TimedFunction<>(
                    () -> jpaQuery.setHint(QueryHints.HINT_READONLY, true).getResultList(),
                    "Running Query: " + sql).get();

            return new SQLEntityHydrator(results, query, getMetadataDictionary(), entityManager).hydrate();
        } finally {
            if (transaction != null && transaction.isActive()) {
                transaction.commit();
            }
            if (entityManager != null) {
                entityManager.close();
            }
        }
    }

    /**
     * Translates the client query into SQL.
     *
     * @param query the client query.
     * @return the SQL query.
     */
    protected SQLQuery toSQL(Query query) {
        Set<ColumnProjection> groupByDimensions = new LinkedHashSet<>(query.getGroupByDimensions());
        Set<TimeDimensionProjection> timeDimensions = new LinkedHashSet<>(query.getTimeDimensions());

        // TODO: handle the case of more than one time dimensions
        TimeDimensionProjection timeDimension = timeDimensions.stream().findFirst().orElse(null);

        SQLQueryTemplate queryTemplate = query.getMetrics().stream()
                .map(invocation -> {
                    MetricFunction function = invocation.getFunction();

                    if (!(function instanceof SQLMetricFunction)) {
                        throw new InvalidPredicateException("Non-SQL metric function on " + invocation.getAlias());
                    }

                    return ((SQLMetricFunction) function).resolve(
                            invocation.getArgumentMap(),
                            invocation.getAlias(),
                            groupByDimensions,
                            timeDimension);
                })
                .reduce(SQLQueryTemplate::merge)
                .orElse(new SQLQueryTemplate() {
                    @Override
                    public List<MetricFunctionInvocation> getMetrics() {
                        return Collections.emptyList();
                    }

                    @Override
                    public Set<ColumnProjection> getNonTimeDimensions() {
                        return groupByDimensions;
                    }

                    @Override
                    public TimeDimensionProjection getTimeDimension() {
                        return timeDimension;
                    }
                });

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
