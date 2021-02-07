/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.query;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.exceptions.BadRequestException;
import com.yahoo.elide.core.filter.FilterTranslator;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.PredicateExtractionVisitor;
import com.yahoo.elide.core.filter.predicates.FilterPredicate;
import com.yahoo.elide.core.request.Pagination;
import com.yahoo.elide.core.request.Sorting;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.query.QueryVisitor;
import com.yahoo.elide.datastores.aggregation.query.Queryable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromSubquery;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromTable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialect;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Translates a client query into a SQL query.
 */
public class QueryTranslator implements QueryVisitor<SQLQuery.SQLQueryBuilder> {

    private final SQLReferenceTable referenceTable;
    private final EntityDictionary dictionary;
    private final SQLDialect dialect;

    public QueryTranslator(SQLReferenceTable referenceTable, SQLDialect sqlDialect) {
        this.referenceTable = referenceTable;
        this.dictionary = referenceTable.getDictionary();
        this.dialect = sqlDialect;
    }

    @Override
    public SQLQuery.SQLQueryBuilder visitQuery(Query query) {
        SQLQuery.SQLQueryBuilder builder = query.getSource().accept(this);

        if (query.isNested()) {
            SQLQuery innerQuery = builder.build();

            builder = SQLQuery.builder().fromClause("(" + innerQuery.toString() + ") AS "
                    + applyQuotes(query.getSource().getAlias()));
        }

        Set<String> joinExpressions = new LinkedHashSet<>();

        builder.projectionClause(constructProjectionWithReference(query));

        //Handles join for all type of column projects - dimensions, metrics and time dimention
        joinExpressions.addAll(extractJoinExpressions(query.getColumnProjections(), query.getSource()));

        Set<ColumnProjection> groupByDimensions = query.getAllDimensionProjections();

        if (!groupByDimensions.isEmpty()) {
            if (!query.getMetricProjections().isEmpty()) {
                builder.groupByClause("GROUP BY " + groupByDimensions.stream()
                        .map(SQLColumnProjection.class::cast)
                        .map((column) -> column.toSQL(referenceTable))
                        .collect(Collectors.joining(", ")));
            }
        }

        if (query.getWhereFilter() != null) {
            builder.whereClause("WHERE " + translateFilterExpression(
                    query.getWhereFilter(),
                    filterPredicate -> generatePredicatePathReference(filterPredicate.getPath(), query)));

            joinExpressions.addAll(extractJoinExpressions(query.getSource(), query.getWhereFilter()));
        }

        if (query.getHavingFilter() != null) {
            builder.havingClause("HAVING " + translateFilterExpression(
                    query.getHavingFilter(),
                    (predicate) -> constructHavingClauseWithReference(predicate, query)));

            joinExpressions.addAll(extractJoinExpressions(query.getSource(), query.getHavingFilter()));
        }

        if (query.getSorting() != null) {
            Map<Path, Sorting.SortOrder> sortClauses = query.getSorting().getSortingPaths();
            builder.orderByClause(extractOrderBy(sortClauses, query));

            joinExpressions.addAll(extractJoinExpressions(query.getSource(), sortClauses));
        }

        Pagination pagination = query.getPagination();
        if (pagination != null) {
            builder.offsetLimitClause(dialect.generateOffsetLimitClause(pagination.getOffset(), pagination.getLimit()));
        }

        return builder.joinClause(String.join(" ", joinExpressions));
    }

    @Override
    public SQLQuery.SQLQueryBuilder visitQueryable(Queryable table) {
        SQLQuery.SQLQueryBuilder builder = SQLQuery.builder();

        Type<?> tableCls = dictionary.getEntityClass(table.getName(), table.getVersion());
        String tableAlias = applyQuotes(table.getAlias());

        String tableStatement = tableCls.isAnnotationPresent(FromSubquery.class)
                ? "(" + tableCls.getAnnotation(FromSubquery.class).sql() + ")"
                : tableCls.isAnnotationPresent(FromTable.class)
                ? applyQuotes(tableCls.getAnnotation(FromTable.class).name())
                : applyQuotes(table.getName());

        return builder.fromClause(String.format("%s AS %s", tableStatement, tableAlias));
    }

    /**
     * Construct HAVING clause filter using physical column references. Metric fields need to be aggregated in HAVING.
     *
     * @param predicate a filter predicate in HAVING clause
     * @param query query
     * @return an filter/constraint expression that can be put in HAVING clause
     */
    private String constructHavingClauseWithReference(FilterPredicate predicate, Query query) {
        Path.PathElement last = predicate.getPath().lastElement().get();
        String fieldName = last.getFieldName();

        if (predicate.getPath().getPathElements().size() > 1) {
            throw new BadRequestException("The having clause can only reference fact table aggregations.");
        }

        SQLMetricProjection metric = query.getMetricProjections().stream()
                .map(SQLMetricProjection.class::cast)
                // TODO: filter predicate should support alias
                .filter(invocation -> invocation.getAlias().equals(fieldName))
                .findFirst()
                .orElse(null);

        if (metric != null) {
            return metric.toSQL(referenceTable);
        } else {
            return generatePredicatePathReference(predicate.getPath(), query);
        }
    }

    /**
     * Construct SELECT statement expression with metrics and dimensions directly using physical table column
     * references.
     *
     * @param query query
     * @return <code>SELECT function(metric1) AS alias1, tb1.dimension1 AS alias2</code>
     */
    private String constructProjectionWithReference(Query query) {
        // TODO: project metric field using table column reference
        List<String> metricProjections = query.getMetricProjections().stream()
                .map(SQLMetricProjection.class::cast)
                .map(invocation -> invocation.toSQL(referenceTable) + " AS "
                                + applyQuotes(invocation.getSQLAlias()))
                .collect(Collectors.toList());

        List<String> dimensionProjections = query.getAllDimensionProjections().stream()
                .map(SQLColumnProjection.class::cast)
                .map(dimension -> dimension.toSQL(referenceTable) + " AS "
                                + applyQuotes(dimension.getSQLAlias()))
                .collect(Collectors.toList());

        if (metricProjections.isEmpty()) {
            return "DISTINCT " + String.join(",", dimensionProjections);
        }

        return Stream.concat(metricProjections.stream(), dimensionProjections.stream())
                .collect(Collectors.joining(","));
    }

    /**
     * Given a list of columns to sort on, constructs an ORDER BY clause in SQL.
     * @param sortClauses The list of sort columns and their sort order (ascending or descending).
     * @return A SQL expression
     */
    private String extractOrderBy(Map<Path, Sorting.SortOrder> sortClauses, Query plan) {
        if (sortClauses.isEmpty()) {
            return "";
        }

        //TODO - Ensure that order by columns are also present in the group by.

        return " ORDER BY " + sortClauses.entrySet().stream()
                .map((entry) -> {
                    Path path = entry.getKey();
                    Sorting.SortOrder order = entry.getValue();

                    Path.PathElement last = path.lastElement().get();

                    SQLColumnProjection projection = fieldToColumnProjection(plan, last.getAlias());
                    String orderByClause = (plan.getColumnProjections().contains(projection)
                            && dialect.useAliasForOrderByClause())
                            ? applyQuotes(projection.getSQLAlias())
                            : projection.toSQL(referenceTable);

                    return orderByClause + (order.equals(Sorting.SortOrder.desc) ? " DESC" : " ASC");
                })
                .collect(Collectors.joining(","));
    }

    /**
     * Coverts a Path from a table to a join path.
     * @param source The table being queried.
     * @param path The path object from the table that may contain a join.
     * @return
     */
    private Set<String> extractJoinExpressions(Queryable source, Path path) {
        Path.PathElement last = path.lastElement().get();
        return referenceTable.getResolvedJoinExpressions(source, last.getFieldName());
    }

    /**
     * Given a filter expression, extracts any entity relationship traversals that require joins.
     *
     * @param source The table that is being queried.
     * @param expression The filter expression
     * @return A set of Join expressions that capture a relationship traversal.
     */
    private Set<String> extractJoinExpressions(Queryable source, FilterExpression expression) {
        Collection<FilterPredicate> predicates = expression.accept(new PredicateExtractionVisitor());

        return predicates.stream()
                .map(FilterPredicate::getPath)
                .map(path -> extractJoinExpressions(source, path))
                .flatMap(Set::stream)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Given a list of columns to sort on, extracts any entity relationship traversals that require joins.
     *
     * @param source The table that is being queried.
     * @param sortClauses The list of sort columns and their sort order (ascending or descending).
     * @return A set of Join expressions that capture a relationship traversal.
     */
    private Set<String> extractJoinExpressions(Queryable source, Map<Path, Sorting.SortOrder> sortClauses) {
        return sortClauses.keySet().stream()
                .map(path -> extractJoinExpressions(source, path))
                .flatMap(Set::stream)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Given the set of group by dimensions or projection metrics,
     * extract any entity relationship traversals that require joins.
     * This method takes in a {@link Table} because the sql join path meta data is stored in it.
     *
     * @param columnProjections The list of dimensions we are grouping on.
     * @param source queried table
     * @return A set of Join expressions that capture a relationship traversal.
     */
    private Set<String> extractJoinExpressions(Set<ColumnProjection> columnProjections,
                                           Queryable source) {
        return columnProjections.stream()
                .map(column -> referenceTable.getResolvedJoinExpressions(source, column.getName()))
                .flatMap(Collection::stream)
                .collect(Collectors.toCollection(LinkedHashSet::new));
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
     * Converts a path into a SQL WHERE/HAVING clause column reference.
     *
     * @param path path to a field
     * @param query query plan
     * @return A SQL fragment that references a database column
     */
    private String generatePredicatePathReference(Path path, Query query) {
        Path.PathElement last = path.lastElement().get();

        SQLColumnProjection projection = fieldToColumnProjection(query, last.getAlias());
        return projection.toSQL(referenceTable);
    }

    private SQLColumnProjection fieldToColumnProjection(Query query, String fieldName) {
        ColumnProjection projection = query.getColumnProjection(fieldName);
        if (projection == null) {
            projection = query.getSource().getColumnProjection(fieldName);
        }
        return (SQLColumnProjection) projection;
    }

    /**
     * Quote column / table aliases using dialect specific quote characters.
     *
     * @param str alias
     * @return quoted alias
     */
    private String applyQuotes(String str) {
        return SQLReferenceTable.applyQuotes(str, dialect);
    }
}
