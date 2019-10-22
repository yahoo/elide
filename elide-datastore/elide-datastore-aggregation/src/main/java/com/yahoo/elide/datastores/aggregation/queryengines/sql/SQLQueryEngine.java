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
import com.yahoo.elide.datastores.aggregation.QueryEngine;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromSubquery;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromTable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.JoinTo;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.schema.SQLDimensionColumn;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.schema.SQLSchema;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.schema.SQLTimeDimensionColumn;
import com.yahoo.elide.datastores.aggregation.schema.Schema;
import com.yahoo.elide.datastores.aggregation.schema.dimension.DimensionColumn;
import com.yahoo.elide.datastores.aggregation.schema.dimension.TimeDimensionColumn;
import com.yahoo.elide.datastores.aggregation.schema.metric.Aggregation;
import com.yahoo.elide.datastores.aggregation.schema.metric.Metric;
import com.yahoo.elide.utils.coerce.CoerceUtil;

import com.google.common.base.Preconditions;
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

    private EntityManagerFactory emf;
    private EntityDictionary dictionary;

    @Getter
    private Map<Class<?>, SQLSchema> schemas;

    public SQLQueryEngine(EntityManagerFactory emf, EntityDictionary dictionary) {
        this.emf = emf;
        this.dictionary = dictionary;

        // Construct the list of schemas that will be managed by this query engine.
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

    @Override
    public Schema getSchema(Class<?> entityClass) {
        return schemas.get(entityClass);
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

            SQLSchema schema = schemas.get(query.getSchema().getEntityClass());

            //Make sure we actually manage this schema.
            Preconditions.checkNotNull(schema);

            //Translate the query into SQL.
            SQLQuery sql = toSQL(query, schema);
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

            //Supply the query parameters to the query
            supplyFilterQueryParameters(query, jpaQuery);

            //Run the primary query and log the time spent.
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
     * @param query the client query.
     * @param schema SQL schema.
     * @return the SQL query.
     */
    protected SQLQuery toSQL(Query query, SQLSchema schema) {
        Class<?> entityClass = schema.getEntityClass();

        SQLQuery.SQLQueryBuilder builder = SQLQuery.builder().clientQuery(query);

        String tableName = schema.getTableDefinition();
        String tableAlias = schema.getAlias();

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

        if (!query.getDimensions().isEmpty())  {
            builder.groupByClause(extractGroupBy(query));

            joinPredicates.addAll(extractPathElements(query
                    .getDimensions()
                    .stream()
                    .map(requestedDim -> requestedDim.toDimensionColumn(query.getSchema()))
                    .collect(Collectors.toSet())));
        }

        if (query.getSorting() != null) {
            Map<Path, Sorting.SortOrder> sortClauses = query.getSorting().getValidSortingRules(entityClass, dictionary);

            builder.orderByClause(extractOrderBy(entityClass, sortClauses));

            joinPredicates.addAll(extractPathElements(sortClauses));
        }

        String joinClause = joinPredicates.stream()
                .map(this::extractJoin)
                .collect(Collectors.joining(" "));

        builder.joinClause(joinClause);

        builder.fromClause(String.format("%s AS %s", tableName, tableAlias));

        return builder.build();
    }

    /**
     * Translates a filter expression into SQL.
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
     * @param groupByDims The list of dimensions we are grouping on.
     * @return A set of path elements that capture a relationship traversal.
     */
    private Set<Path.PathElement> extractPathElements(Set<DimensionColumn> groupByDims) {
        return groupByDims.stream()
            .map((SQLDimensionColumn.class::cast))
            .filter((dim) -> dim.getJoinPath() != null)
            .map(SQLDimensionColumn::getJoinPath)
            .map((path) -> extractPathElements(path))
            .flatMap((elements) -> elements.stream())
            .collect(Collectors.toSet());
    }

    /**
     * Given a filter expression, extracts any entity relationship traversals that require joins.
     * @param expression The filter expression
     * @return A set of path elements that capture a relationship traversal.
     */
    private Set<Path.PathElement> extractPathElements(FilterExpression expression) {
        Collection<FilterPredicate> predicates = expression.accept(new PredicateExtractionVisitor());

        return predicates.stream()
                .map(FilterPredicate::getPath)
                .map(this::expandJoinToPath)
                .filter(path -> path.getPathElements().size() > 1)
                .map((path) -> extractPathElements(path))
                .flatMap((elements) -> elements.stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Expands a predicate path (from a sort or filter predicate) to the path contained in
     * the JoinTo annotation.  If no JoinTo annotation is present, the original path is returned.
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
     * Given a path , extracts any entity relationship traversals that require joins.
     * @param path The path
     * @return A set of path elements that capture a relationship traversal.
     */
    private Set<Path.PathElement> extractPathElements(Path path) {
        return path.getPathElements().stream()
                .filter((p) -> dictionary.isRelation(p.getType(), p.getFieldName()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Given one component of the path taken to reach a particular field, extracts any table
     * joins that are required to perform the traversal to the field.
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
                SQLSchema.getTableOrSubselect(dictionary, relationshipClass),
                relationshipAlias,
                entityAlias,
                relationshipColumnName,
                relationshipAlias,
                relationshipIdField);
    }

    /**
     * Given a list of columns to sort on, extracts any entity relationship traversals that require joins.
     * @param sortClauses The list of sort columns and their sort order (ascending or descending).
     * @return A set of path elements that capture a relationship traversal.
     */
    private Set<Path.PathElement> extractPathElements(Map<Path, Sorting.SortOrder> sortClauses) {
        if (sortClauses.isEmpty()) {
            return new LinkedHashSet<>();
        }

        return sortClauses.entrySet().stream()
                .map(Map.Entry::getKey)
                .map(this::expandJoinToPath)
                .map((path) -> extractPathElements(path))
                .flatMap((elements) -> elements.stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Given a list of columns to sort on, constructs an ORDER BY clause in SQL.
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

                    return FilterPredicate.getTypeAlias(last.getType())
                            + "."
                            + getColumnName(entityClass, last.getFieldName())
                            + (order.equals(Sorting.SortOrder.desc) ? " DESC" : " ASC");
                }).collect(Collectors.joining(","));
    }

    /**
     * Given a JPA query, replaces any parameters with their values from client query.
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
     * Returns the physical database column name of an entity field.  Note - we can't use Schema here because
     * we need to look at both Dimension table and Fact table fields.
     * @param entityClass The JPA/Elide entity
     * @param fieldName The field name to lookup
     * @return
     */
    private String getColumnName(Class<?> entityClass, String fieldName) {
        return SQLSchema.getColumnName(dictionary, entityClass, fieldName);
    }

    /**
     * Takes a SQLQuery and creates a new clone that instead returns the total number of records of the original
     * query.
     * @param sql The original query
     * @return A new query that returns the total number of records.
     */
    private SQLQuery toPageTotalSQL(SQLQuery sql) {
        Query clientQuery = sql.getClientQuery();

        String groupByDimensions = clientQuery.getDimensions().stream()
                .map(requestedDim -> clientQuery.getSchema().getDimension(requestedDim.getName()))
                .map((SQLDimensionColumn.class::cast))
                .map(SQLDimensionColumn::getColumnName)
                .collect(Collectors.joining(","));

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
     * @param query The client query
     * @return A SQL fragment to use in the SELECT .. statement.
     */
    private String extractProjection(Query query) {
        List<String> metricProjections = query.getMetrics().entrySet().stream()
                .map((entry) -> {
                    Metric metric = entry.getKey();
                    Class<? extends Aggregation> agg = entry.getValue();
                    return metric.getMetricExpression(agg) + " AS " + metric.getName();
                })
                .collect(Collectors.toList());

        List<String> dimensionProjections = extractDimensionProjections(query);

        String projectionClause = metricProjections.stream()
                .collect(Collectors.joining(","));

        if (!dimensionProjections.isEmpty()) {
            projectionClause = projectionClause + "," + dimensionProjections.stream()
                    .collect(Collectors.joining(","));
        }

        return projectionClause;
    }

    /**
     * Extracts a GROUP BY SQL clause.
     * @param query A client query
     * @return The SQL GROUP BY clause
     */
    private String extractGroupBy(Query query) {
        List<String> dimensionStrings = query.getGroupDimensions().stream()
                .map(requestedDim -> query.getSchema().getDimension(requestedDim.getName()))
                .map((SQLDimensionColumn.class::cast))
                .map(SQLDimensionColumn::getColumnReference)
                .collect(Collectors.toList());

        dimensionStrings.addAll(query.getTimeDimensions().stream()
                .map(requestedDim -> {
                    SQLTimeDimensionColumn timeDim = (SQLTimeDimensionColumn)
                            query.getSchema().getTimeDimension(requestedDim.getName());

                    return timeDim.getColumnReference(requestedDim.getTimeGrain().grain());
                }).collect(Collectors.toList()));

        return "GROUP BY " + dimensionStrings.stream()
                .collect(Collectors.joining(","));
    }

    /**
     * extracts the SQL column references for the dimensions from the query.
     * @param query
     * @return
     */
    private List<String> extractDimensionProjections(Query query) {
        List<String> dimensionStrings = query.getGroupDimensions().stream()
                .map(requestedDim -> query.getSchema().getDimension(requestedDim.getName()))
                .map((SQLDimensionColumn.class::cast))
                .map(SQLDimensionColumn::getColumnReference)
                .collect(Collectors.toList());

        dimensionStrings.addAll(query.getTimeDimensions().stream()
                .map(requestedDim -> {
                    SQLTimeDimensionColumn timeDim = (SQLTimeDimensionColumn)
                            query.getSchema().getTimeDimension(requestedDim.getName());

                    return timeDim.getColumnReference(requestedDim.getTimeGrain().grain())
                            + " AS " + timeDim.getColumnName();
                }).collect(Collectors.toList()));

        return dimensionStrings;
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
     * @param path The predicate path to convert
     * @return A SQL fragment that references a database column
     */
    private String generateWhereClauseColumnReference(Path path) {
        Path.PathElement last = path.lastElement().get();
        Class<?> lastClass = last.getType();
        String fieldName = last.getFieldName();

        JoinTo joinTo = dictionary.getAttributeOrRelationAnnotation(lastClass, JoinTo.class, fieldName);

        if (joinTo == null) {
            return FilterPredicate.getTypeAlias(lastClass) + "." + getColumnName(lastClass, last.getFieldName());
        } else {
            return generateWhereClauseColumnReference(new Path(lastClass, dictionary, joinTo.path()));
        }
    }

    /**
     * Converts a filter predicate into a SQL HAVING clause column reference.
     * @param predicate The predicate to convert
     * @return A SQL fragment that references a database column
     */
    private String generateHavingClauseColumnReference(FilterPredicate predicate, Query query) {
        Path.PathElement last = predicate.getPath().lastElement().get();
        Class<?> lastClass = last.getType();

        if (!lastClass.equals(query.getSchema().getEntityClass())) {
            throw new InvalidPredicateException("The having clause can only reference fact table aggregations.");
        }

        Schema schema = schemas.get(lastClass);
        Metric metric = schema.getMetric(last.getFieldName());
        if (metric != null) {
            // if the having clause is applied on a metric field, should use aggregation expression
            Class<? extends Aggregation> agg = query.getMetrics().get(metric);

            return metric.getMetricExpression(agg);
        } else {
            // if the having clause is applied on a dimension field, should be the same as a where expression
            return generateWhereClauseColumnReference(predicate.getPath());
        }
    }
}
