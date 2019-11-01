/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.TimedFunction;
import com.yahoo.elide.core.exceptions.InvalidPredicateException;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.FilterTranslator;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.PredicateExtractionVisitor;
import com.yahoo.elide.core.pagination.Pagination;
import com.yahoo.elide.core.sort.Sorting;
import com.yahoo.elide.datastores.aggregation.AggregationDictionary;
import com.yahoo.elide.datastores.aggregation.QueryEngine;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.metadata.models.AnalyticView;
import com.yahoo.elide.datastores.aggregation.metadata.models.Metric;
import com.yahoo.elide.datastores.aggregation.metadata.models.MetricFunction;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromSubquery;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromTable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.JoinTo;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.schema.SQLDimension;
import com.yahoo.elide.utils.coerce.CoerceUtil;

import org.hibernate.annotations.Subselect;
import org.hibernate.jpa.QueryHints;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;

/**
 * QueryEngine for SQL backed stores.
 */
@Slf4j
public class SQLQueryEngine implements QueryEngine {

    private final EntityManagerFactory emf;
    private final AggregationDictionary dictionary;

    @Getter
    private Map<Class<?>, Table> tables;

    public SQLQueryEngine(EntityManagerFactory emf, AggregationDictionary dictionary, MetaDataStore metaDataStore) {
        this.emf = emf;
        this.dictionary = dictionary;

        Set<Table> tables = metaDataStore.getMetaData(Table.class);
        tables.addAll(metaDataStore.getMetaData(AnalyticView.class));

        this.tables = tables.stream().collect(Collectors.toMap(Table::getCls, Function.identity()));
    }

    @Override
    public Table getTable(Class<?> entityClass) {
        return tables.get(entityClass);
    }

    @Override
    public Iterable<Object> executeQuery(Query query) {
        EntityManager entityManager = null;
        EntityTransaction transaction = null;
        try {
            entityManager = emf.createEntityManager();

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

                if (pagination.isGenerateTotals()) {

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

            return new SQLEntityHydrator(results, query, dictionary, entityManager).hydrate();
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
        Table table = query.getTable();
        Class<?> tableCls = table.getCls();

        SQLQuery.SQLQueryBuilder builder = SQLQuery.builder().clientQuery(query);

        String tableStatement = tableCls.isAnnotationPresent(FromSubquery.class)
                ? "(" + tableCls.getAnnotation(FromSubquery.class).sql() + ")"
                : tableCls.isAnnotationPresent(FromTable.class)
                        ? tableCls.getAnnotation(FromTable.class).name()
                        : table.getName();
        String tableAlias = getClassAlias(tableCls);

        builder.projectionClause(extractProjection(query));

        Set<Path.PathElement> joinPredicates = new HashSet<>();

        if (query.getWhereFilter() != null) {
            joinPredicates.addAll(extractPathElements(query.getWhereFilter()));
            builder.whereClause("WHERE " + translateFilterExpression(
                    query.getWhereFilter(),
                    this::generateWhereClauseColumnReference));
        }

        if (query.getHavingFilter() != null) {
            joinPredicates.addAll(extractPathElements(query.getHavingFilter()));
            builder.havingClause("HAVING " + translateFilterExpression(
                    query.getHavingFilter(),
                    (predicate) -> generateHavingClauseColumnReference(predicate, query)));
        }

        if (!query.getDimensions().isEmpty()) {
            List<SQLDimension> groupByDimensions = extractSQLDimensions(query);
            builder.groupByClause(extractGroupBy(groupByDimensions));
            joinPredicates.addAll(extractPathElements(groupByDimensions));
        }

        if (query.getSorting() != null) {
            Map<Path, Sorting.SortOrder> sortClauses = query.getSorting().getValidSortingRules(tableCls, dictionary);
            builder.orderByClause(extractOrderBy(tableCls, sortClauses));
            joinPredicates.addAll(extractPathElements(sortClauses));
        }

        String joinClause = joinPredicates.stream()
                .map(this::extractJoin)
                .collect(Collectors.joining(" "));

        builder.joinClause(joinClause);

        builder.fromClause(String.format("%s AS %s", tableStatement, tableAlias));

        return builder.build();
    }

    /**
     * Translates a filter expression into SQL.
     *
     * @param expression The filter expression
     * @param columnGenerator A function which generates a column reference in SQL from a FilterPredicate.
     * @return A SQL expression
     */
    private String translateFilterExpression(FilterExpression expression,
                                             Function<FilterPredicate, String> columnGenerator) {
        FilterTranslator filterVisitor = new FilterTranslator();

        return filterVisitor.apply(expression, columnGenerator);
    }

    /**
     * Given the set of group by dimensions, extract any entity relationship traversals that require joins.
     *
     * @param groupbyDimensions The list of dimensions we are grouping on.
     * @return A set of path elements that capture a relationship traversal.
     */
    private Set<Path.PathElement> extractPathElements(List<SQLDimension> groupbyDimensions) {
        return groupbyDimensions.stream()
                .filter((dim) -> dim.getJoinPath() != null)
                .map(SQLDimension::getJoinPath)
                .map(this::extractPathElements)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    /**
     * Given a filter expression, extracts any entity relationship traversals that require joins.
     *
     * @param expression The filter expression
     * @return A set of path elements that capture a relationship traversal.
     */
    private Set<Path.PathElement> extractPathElements(FilterExpression expression) {
        Collection<FilterPredicate> predicates = expression.accept(new PredicateExtractionVisitor());

        return predicates.stream()
                .map(FilterPredicate::getPath)
                .map(this::expandJoinToPath)
                .filter(path -> path.getPathElements().size() > 1)
                .map(this::extractPathElements)
                .flatMap(Collection::stream)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Given a list of columns to sort on, extracts any entity relationship traversals that require joins.
     *
     * @param sortClauses The list of sort columns and their sort order (ascending or descending).
     * @return A set of path elements that capture a relationship traversal.
     */
    private Set<Path.PathElement> extractPathElements(Map<Path, Sorting.SortOrder> sortClauses) {
        return sortClauses.keySet().stream()
                .map(this::expandJoinToPath)
                .map(this::extractPathElements)
                .flatMap(Collection::stream)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }


    /**
     * Given a path , extracts any entity relationship traversals that require joins.
     *
     * @param path The path
     * @return A set of path elements that capture a relationship traversal.
     */
    private Set<Path.PathElement> extractPathElements(Path path) {
        return path.getPathElements().stream()
                .filter((p) -> dictionary.isRelation(p.getType(), p.getFieldName()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Expands a predicate path (from a sort or filter predicate) to the path contained in
     * the JoinTo annotation.  If no JoinTo annotation is present, the original path is returned.
     *
     * @param path The path to expand.
     * @return The expanded path.
     */
    private Path expandJoinToPath(Path path) {
        List<Path.PathElement> pathElements = path.getPathElements();
        Path.PathElement pathElement = pathElements.get(0);

        Class<?> type = pathElement.getType();
        String fieldName = pathElement.getFieldName();
        JoinTo joinTo = dictionary.getAttributeOrRelationAnnotation(type, JoinTo.class, fieldName);

        if (joinTo == null) {
            return path;
        }

        return new Path(pathElement.getType(), dictionary, joinTo.path());
    }

    /**
     * Given one component of the path taken to reach a particular field, extracts any table
     * joins that are required to perform the traversal to the field.
     *
     * @param pathElement A field or relationship traversal from an entity
     * @return A SQL expression
     */
    private String extractJoin(Path.PathElement pathElement) {
        //TODO - support composite join keys.
        //TODO - support joins where either side owns the relationship.
        //TODO - Support INNER and RIGHT joins.
        //TODO - Support toMany joins.
        Class<?> entityClass = pathElement.getType();
        String entityAlias = FilterPredicate.getTypeAlias(entityClass);

        Class<?> relationshipClass = pathElement.getFieldType();
        String relationshipAlias = FilterPredicate.getTypeAlias(relationshipClass);
        String relationshipName = pathElement.getFieldName();
        String relationshipIdField = getColumnName(relationshipClass, dictionary.getIdFieldName(relationshipClass));
        String relationshipColumnName = getColumnName(entityClass, relationshipName);

        return String.format("LEFT JOIN %s AS %s ON %s.%s = %s.%s",
                getTableOrSubselect(dictionary, relationshipClass),
                relationshipAlias,
                entityAlias,
                relationshipColumnName,
                relationshipAlias,
                relationshipIdField);
    }

    /**
     * Given a list of columns to sort on, constructs an ORDER BY clause in SQL.
     *
     * @param entityClass The class to sort.
     * @param sortClauses The list of sort columns and their sort order (ascending or descending).
     * @return A SQL expression
     */
    private String extractOrderBy(Class<?> entityClass, Map<Path, Sorting.SortOrder> sortClauses) {
        if (sortClauses.isEmpty()) {
            return "";
        }

        //TODO - Ensure that order by columns are also present in the group by.

        return " ORDER BY " + sortClauses.entrySet().stream()
                .map((entry) -> {
                    Path path = entry.getKey();
                    path = expandJoinToPath(path);
                    Sorting.SortOrder order = entry.getValue();

                    Path.PathElement last = path.lastElement().get();

                    return getClassAlias(last.getType())
                            + "."
                            + getColumnName(entityClass, last.getFieldName())
                            + (order.equals(Sorting.SortOrder.desc) ? " DESC" : " ASC");
                }).collect(Collectors.joining(","));
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
     * Returns the physical database column name of an entity field.
     *
     * @param entityClass The JPA/Elide entity
     * @param fieldName The field name to lookup
     * @return
     */
    private String getColumnName(Class<?> entityClass, String fieldName) {
        return dictionary.getColumnName(entityClass, fieldName);
    }

    /**
     * Takes a SQLQuery and creates a new clone that instead returns the total number of records of the original
     * query.
     *
     * @param sql The original query
     * @return A new query that returns the total number of records.
     */
    private SQLQuery toPageTotalSQL(SQLQuery sql) {
        String groupByDimensions = extractSQLDimensions(sql.getClientQuery()).stream()
                .map(SQLDimension::getColumnName)
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
     * Given a client query, constructs the list of columns to project from a database table.
     *
     * @param query The client query
     * @return A SQL fragment to use in the SELECT .. statement.
     */
    private String extractProjection(Query query) {
        List<String> metricProjections = query.getMetrics().entrySet().stream()
                .map((entry) -> getMetricExpression(entry.getValue(), entry.getKey().getName()))
                .collect(Collectors.toList());

        List<String> dimensionProjections = extractDimensionProjections(query);

        String projectionClause = String.join(",", metricProjections);

        if (!dimensionProjections.isEmpty()) {
            projectionClause = projectionClause + "," + String.join(",", dimensionProjections);
        }

        return projectionClause;
    }

    /**
     * Extract dimension projects in a query to sql dimensions
     * @param query requested query
     * @return sql dimensions in this query
     */
    private List<SQLDimension> extractSQLDimensions(Query query) {
        return query.getDimensions().stream()
                .map(dimensionProjection -> SQLDimension.constructSQLDimension(
                        dimensionProjection,
                        query.getTable(),
                        dictionary))
                .collect(Collectors.toList());
    }

    /**
     * Extracts a GROUP BY SQL clause.
     *
     * @param dimensions SQL dimensions to group by
     * @return The SQL GROUP BY clause
     */
    private String extractGroupBy(List<SQLDimension> dimensions) {
        return "GROUP BY " + dimensions.stream()
                .map(SQLDimension::getColumnReference)
                .collect(Collectors.joining(", "));
    }

    /**
     * extracts the SQL column references for the dimensions from the query.
     *
     * @param query request query
     * @return all dimension column reference
     */
    private List<String> extractDimensionProjections(Query query) {
        return query.getDimensions().stream()
                .map(dimensionProjection -> SQLDimension.constructSQLDimension(
                        dimensionProjection,
                        query.getTable(),
                        dictionary))
                .map(SQLDimension::getColumnReference)
                .collect(Collectors.toList());
    }

    /**
     * Converts a filter predicate into a SQL WHERE clause column reference.
     * @param predicate The predicate to convert
     * @return A SQL fragment that references a database column
     */
    private String generateWhereClauseColumnReference(FilterPredicate predicate) {
        return generateWhereClauseColumnReference(predicate.getPath());
    }

    /**
     * Converts a filter predicate path into a SQL WHERE clause column reference.
     *
     * @param path The predicate path to convert
     * @return A SQL fragment that references a database column
     */
    private String generateWhereClauseColumnReference(Path path) {
        Path.PathElement last = path.lastElement().get();
        Class<?> lastClass = last.getType();
        String fieldName = last.getFieldName();

        JoinTo joinTo = dictionary.getAttributeOrRelationAnnotation(lastClass, JoinTo.class, fieldName);

        if (joinTo == null) {
            return getClassAlias(lastClass) + "." + getColumnName(lastClass, last.getFieldName());
        } else {
            return generateWhereClauseColumnReference(new Path(lastClass, dictionary, joinTo.path()));
        }
    }

    /**
     * Converts a filter predicate into a SQL HAVING clause column reference.
     *
     * @param predicate The predicate to convert
     * @return A SQL fragment that references a database column
     */
    private String generateHavingClauseColumnReference(FilterPredicate predicate, Query query) {
        Path.PathElement last = predicate.getPath().lastElement().get();
        Class<?> lastClass = last.getType();

        if (!lastClass.equals(query.getTable().getCls())) {
            throw new InvalidPredicateException("The having clause can only reference fact table aggregations.");
        }

        Table table = tables.get(lastClass);
        Metric metric = table.getMetric(last.getFieldName());
        if (metric != null) {
            // if the having clause is applied on a metric field, should use aggregation expression
            MetricFunction function = query.getMetrics().get(metric);

            return getMetricExpression(function, metric.getName());
        } else {
            // if the having clause is applied on a dimension field, should be the same as a where expression
            return generateWhereClauseColumnReference(predicate.getPath());
        }
    }

    /**
     * Maps an entity class to a physical table of subselect query, if neither {@link javax.persistence.Table}
     * nor {@link Subselect} annotation is present on this class, use the class alias as default.
     *
     * @param entityDictionary The dictionary for this elide instance.
     * @param cls The entity class.
     * @return The physical SQL table or subselect query.
     */
    private static String getTableOrSubselect(EntityDictionary entityDictionary, Class<?> cls) {
        Subselect subselectAnnotation = entityDictionary.getAnnotation(cls, Subselect.class);

        if (subselectAnnotation == null) {
            javax.persistence.Table tableAnnotation =
                    entityDictionary.getAnnotation(cls, javax.persistence.Table.class);

            return (tableAnnotation == null)
                    ? entityDictionary.getJsonAliasFor(cls)
                    : tableAnnotation.name();
        } else {
            return "(" + subselectAnnotation.value() + ")";
        }
    }

    /**
     * Construct a sql metric expression for a metric function with given field name.
     *
     * @param function a metric function
     * @param fieldName metric field
     * @return constructed sql expression
     */
    private static String getMetricExpression(MetricFunction function, String fieldName) {
        String functionName = function.getName();
        switch (functionName) {
            case "sum":
                return String.format("SUM(%s)", fieldName);
            case "max":
                return String.format("MAX(%s)", fieldName);
            case "min":
                return String.format("MIN(%s)", fieldName);
            default:
                return "";
        }
    }

    // TODO: update this
    /**
     * An alias to assign this schema.
     * @return an alias that can be used in SQL.
     */
    public static String getClassAlias(Class<?> entityClass) {
        return FilterPredicate.getTypeAlias(entityClass);
    }
}
