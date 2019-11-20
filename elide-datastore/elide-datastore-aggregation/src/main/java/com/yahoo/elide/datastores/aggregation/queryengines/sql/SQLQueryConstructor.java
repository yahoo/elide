/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql;

import static com.yahoo.elide.datastores.aggregation.queryengines.sql.SQLQueryEngine.getClassAlias;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.exceptions.InvalidPredicateException;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.FilterTranslator;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.PredicateExtractionVisitor;
import com.yahoo.elide.core.sort.Sorting;
import com.yahoo.elide.datastores.aggregation.metadata.metric.MetricFunctionInvocation;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimension;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimensionGrain;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.query.TimeDimensionProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromSubquery;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromTable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.JoinTo;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLAnalyticView;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLColumn;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLQueryTemplate;

import org.hibernate.annotations.Subselect;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Class to construct query template into real sql query
 */
public class SQLQueryConstructor {
    private final EntityDictionary dictionary;

    public SQLQueryConstructor(EntityDictionary dictionary) {
        this.dictionary = dictionary;
    }

    /**
     * Construct sql query with a template and sorting, where and having clause.
     *
     * @param clientQuery original query object
     * @param template query template constructed from client query
     * @param sorting sorting clause
     * @param whereClause where clause
     * @param havingClause having clause
     * @return constructed SQLQuery object contains all information above
     */
    public SQLQuery resolveTemplate(Query clientQuery,
                                    SQLQueryTemplate template,
                                    Sorting sorting,
                                    FilterExpression whereClause,
                                    FilterExpression havingClause) {
        SQLAnalyticView queriedTable = (SQLAnalyticView) clientQuery.getAnalyticView();
        Class<?> tableCls = clientQuery.getAnalyticView().getCls();
        String tableAlias = getClassAlias(tableCls);

        SQLQuery.SQLQueryBuilder builder = SQLQuery.builder().clientQuery(clientQuery);

        Set<Path.PathElement> joinPredicates = new HashSet<>();

        String tableStatement = tableCls.isAnnotationPresent(FromSubquery.class)
                ? "(" + tableCls.getAnnotation(FromSubquery.class).sql() + ")"
                : tableCls.isAnnotationPresent(FromTable.class)
                ? tableCls.getAnnotation(FromTable.class).name()
                : queriedTable.getName();

        builder.fromClause(String.format("%s AS %s", tableStatement, tableAlias));

        builder.projectionClause(constructProjectionWithReference(template, queriedTable));

        Set<ColumnProjection> groupByDimensions = template.getGroupByDimensions();

        if (!groupByDimensions.isEmpty()) {
            builder.groupByClause(constructGroupByWithReference(groupByDimensions, queriedTable));
            joinPredicates.addAll(extractPathElements(groupByDimensions, queriedTable));
        }

        if (whereClause != null) {
            joinPredicates.addAll(extractPathElements(whereClause));
            builder.whereClause("WHERE " + translateFilterExpression(
                    whereClause,
                    this::generateColumnReference));
        }

        if (havingClause != null) {
            joinPredicates.addAll(extractPathElements(havingClause));
            builder.havingClause("HAVING " + translateFilterExpression(
                    havingClause,
                    (predicate) -> constructHavingClauseWithReference(predicate, queriedTable, template)));
        }

        if (sorting != null) {
            Map<Path, Sorting.SortOrder> sortClauses = sorting.getValidSortingRules(tableCls, dictionary);
            builder.orderByClause(extractOrderBy(tableCls, sortClauses));
            joinPredicates.addAll(extractPathElements(sortClauses));
        }

        String joinClause = joinPredicates.stream()
                .map(this::extractJoin)
                .collect(Collectors.joining(" "));

        builder.joinClause(joinClause);

        return builder.build();
    }

    /**
     * Construct directly projection GROUP BY clause using column reference.
     *
     * @param groupByDimensions columns to project out
     * @param queriedTable queried analytic view
     * @return <code>GROUP BY tb1.col1, tb2.col2, ...</code>
     */
    private String constructGroupByWithReference(Set<ColumnProjection> groupByDimensions,
                                                 SQLAnalyticView queriedTable) {
        return "GROUP BY " + groupByDimensions.stream()
                .map(dimension -> resolveSQLColumnReference(dimension, queriedTable))
                .collect(Collectors.joining(", "));
    }

    /**
     * Construct HAVING clause filter using physical column references. Metric fields need to be aggregated in HAVING.
     *
     * @param predicate a filter predicate in HAVING clause
     * @param table Elide logical table this query is querying
     * @param template query template
     * @return an filter/constraint expression that can be put in HAVING clause
     */
    private String constructHavingClauseWithReference(FilterPredicate predicate,
                                                      Table table,
                                                      SQLQueryTemplate template) {
        Path.PathElement last = predicate.getPath().lastElement().get();
        Class<?> lastClass = last.getType();
        String fieldName = last.getFieldName();

        if (!lastClass.equals(table.getCls())) {
            throw new InvalidPredicateException("The having clause can only reference fact table aggregations.");
        }

        MetricFunctionInvocation metric = template.getMetrics().stream()
                // TODO: filter predicate should support alias
                .filter(invocation -> invocation.getAlias().equals(fieldName))
                .findFirst()
                .orElse(null);

        if (metric != null) {
            return metric.getFunctionExpression();
        } else {
            return generateColumnReference(predicate);
        }
    }

    /**
     * Construct SELECT statement expression with metrics and dimensions directly using physical table column
     * references.
     *
     * @param template query template with nested subquery
     * @param queriedTable queried analytic view
     * @return <code>SELECT function(metric1) AS alias1, tb1.dimension1 AS alias2</code>
     */
    private String constructProjectionWithReference(SQLQueryTemplate template, SQLAnalyticView queriedTable) {
        // TODO: project metric field using table column reference
        List<String> metricProjections = template.getMetrics().stream()
                .map(invocation -> invocation.getFunctionExpression() + " AS " + invocation.getAlias())
                .collect(Collectors.toList());

        List<String> dimensionProjections = template.getGroupByDimensions().stream()
                .map(dimension ->  resolveSQLColumnReference(dimension, queriedTable) + " AS " + dimension.getAlias())
                .collect(Collectors.toList());

        return Stream.concat(metricProjections.stream(), dimensionProjections.stream())
                .collect(Collectors.joining(","));
    }

    /**
     * Given one component of the path taken to reach a particular field, extracts any table
     * joins that are required to perform the traversal to the field.
     *
     * @param pathElement A field or relationship traversal from an entity
     * @return A SQL JOIN expression
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

        String relationshipIdField = dictionary.getAnnotatedColumnName(
                relationshipClass,
                dictionary.getIdFieldName(relationshipClass));

        String relationshipColumnName = dictionary.getAnnotatedColumnName(entityClass, relationshipName);

        return String.format("LEFT JOIN %s AS %s ON %s.%s = %s.%s",
                constructTableOrSubselect(relationshipClass),
                relationshipAlias,
                entityAlias,
                relationshipColumnName,
                relationshipAlias,
                relationshipIdField);
    }


    /**
     * Make a select statement for a table a sub select query.
     *
     * @param cls entity class
     * @return <code>tableName</code> or <code>(subselect query)</code>
     */
    private String constructTableOrSubselect(Class<?> cls) {
        return isSubselect(cls)
                ? "(" + resolveTableOrSubselect(dictionary, cls) + ")"
                : resolveTableOrSubselect(dictionary, cls);
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

                    return getClassAlias(last.getType()) + "."
                            + dictionary.getAnnotatedColumnName(entityClass, last.getFieldName())
                            + (order.equals(Sorting.SortOrder.desc) ? " DESC" : " ASC");
                }).collect(Collectors.joining(","));
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
     * Given the set of group by dimensions, extract any entity relationship traversals that require joins.
     * This method takes in a {@link SQLAnalyticView} because the sql join path meta data is stored in it.
     *
     * @param groupByDimensions The list of dimensions we are grouping on.
     * @param queriedTable queried analytic view
     * @return A set of path elements that capture a relationship traversal.
     */
    private Set<Path.PathElement> extractPathElements(Set<ColumnProjection> groupByDimensions,
                                                      SQLAnalyticView queriedTable) {
        return resolveSQLColumns(groupByDimensions, queriedTable).stream()
                .filter((dim) -> dim.getJoinPath() != null)
                .map(SQLColumn::getJoinPath)
                .map(this::extractPathElements)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
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
     * Converts a filter predicate into a SQL WHERE/HAVING clause column reference.
     *
     * @param predicate The predicate to convert
     * @return A SQL fragment that references a database column
     */
    private String generateColumnReference(FilterPredicate predicate) {
        return generateColumnReference(predicate.getPath());
    }

    /**
     * Converts a filter predicate path into a SQL WHERE/HAVING clause column reference.
     *
     * @param path The predicate path to convert
     * @return A SQL fragment that references a database column
     */
    private String generateColumnReference(Path path) {
        Path.PathElement last = path.lastElement().get();
        Class<?> lastClass = last.getType();
        String fieldName = last.getFieldName();

        JoinTo joinTo = dictionary.getAttributeOrRelationAnnotation(lastClass, JoinTo.class, fieldName);

        if (joinTo == null) {
            return getClassAlias(lastClass) + "." + dictionary.getAnnotatedColumnName(lastClass, last.getFieldName());
        } else {
            return generateColumnReference(new Path(lastClass, dictionary, joinTo.path()));
        }
    }

    /**
     * Resolve all projected sql column from a queried table.
     *
     * @param columnProjections projections
     * @param queriedTable sql analytic view
     * @return projected columns
     */
    private Set<SQLColumn> resolveSQLColumns(Set<ColumnProjection> columnProjections, SQLAnalyticView queriedTable) {
        return columnProjections.stream()
                .map(colProjection -> queriedTable.getColumn(colProjection.getColumn().getName()))
                .collect(Collectors.toSet());
    }

    /**
     * Resolve projected sql column as column reference from a queried table.
     * If the projection is {@link TimeDimensionProjection}, the correct time grain expression would be used.
     *
     * @param columnProjection projection
     * @param queriedTable sql analytic view
     * @return projected columns
     */
    private String resolveSQLColumnReference(ColumnProjection columnProjection, SQLAnalyticView queriedTable) {
        SQLColumn sqlColumn = queriedTable.getColumn(columnProjection.getColumn().getName());

        if (columnProjection instanceof TimeDimensionProjection) {
            TimeDimension timeDimension = ((TimeDimensionProjection) columnProjection).getTimeDimension();
            TimeDimensionGrain grainInfo = timeDimension.getSupportedGrains().stream()
                    .filter(g -> g.getGrain().equals(((TimeDimensionProjection) columnProjection).getGrain()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Requested time grain not supported."));

            //TODO - We will likely migrate to a templating language when we support parameterized metrics.
            return String.format(grainInfo.getExpression(), sqlColumn.getReference());
        } else {
            return sqlColumn.getReference();
        }
    }

    /**
     * Maps an entity class to a physical table of subselect query, if neither {@link javax.persistence.Table}
     * nor {@link Subselect} annotation is present on this class, use the class alias as default.
     *
     * @param cls The entity class.
     * @return The physical SQL table or subselect query.
     */
    private static String resolveTableOrSubselect(EntityDictionary dictionary, Class<?> cls) {
        if (isSubselect(cls)) {
            return dictionary.getAnnotation(cls, Subselect.class).value();
        } else {
            javax.persistence.Table tableAnnotation =
                    dictionary.getAnnotation(cls, javax.persistence.Table.class);

            return (tableAnnotation == null)
                    ? dictionary.getJsonAliasFor(cls)
                    : tableAnnotation.name();
        }
    }

    /**
     * Check whether a class is mapped to a subselect query instead of a physical table.
     *
     * @param cls The entity class
     * @return True if the class has {@link Subselect} annotation
     */
    private static boolean isSubselect(Class<?> cls) {
        return cls.isAnnotationPresent(Subselect.class);
    }
}
