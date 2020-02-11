/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql;

import static com.yahoo.elide.core.filter.FilterPredicate.appendAlias;
import static com.yahoo.elide.core.filter.FilterPredicate.getTypeAlias;
import static com.yahoo.elide.datastores.aggregation.queryengines.sql.SQLQueryEngine.generateColumnReference;
import static com.yahoo.elide.datastores.aggregation.queryengines.sql.SQLQueryEngine.getClassAlias;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.exceptions.InvalidPredicateException;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.FilterTranslator;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.PredicateExtractionVisitor;
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
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLColumn;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLQueryTemplate;
import com.yahoo.elide.request.Sorting;

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
 * Class to construct query template into real sql query.
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
        SQLTable table = (SQLTable) clientQuery.getTable();
        Class<?> tableCls = dictionary.getEntityClass(clientQuery.getTable().getName());
        String tableAlias = getClassAlias(tableCls);

        SQLQuery.SQLQueryBuilder builder = SQLQuery.builder().clientQuery(clientQuery);

        Set<Path> joinPaths = new HashSet<>();

        String tableStatement = tableCls.isAnnotationPresent(FromSubquery.class)
                ? "(" + tableCls.getAnnotation(FromSubquery.class).sql() + ")"
                : tableCls.isAnnotationPresent(FromTable.class)
                ? tableCls.getAnnotation(FromTable.class).name()
                : table.getName();

        builder.fromClause(String.format("%s AS %s", tableStatement, tableAlias));

        builder.projectionClause(constructProjectionWithReference(template, table));

        Set<ColumnProjection> groupByDimensions = template.getGroupByDimensions();

        if (!groupByDimensions.isEmpty()) {
            if (!clientQuery.getMetrics().isEmpty()) {
                builder.groupByClause(constructGroupByWithReference(groupByDimensions, table));
            }

            joinPaths.addAll(extractJoinPaths(groupByDimensions, table));
        }

        if (whereClause != null) {
            builder.whereClause("WHERE " + translateFilterExpression(whereClause, this::generatePredicateReference));

            joinPaths.addAll(extractJoinPaths(whereClause));
        }

        if (havingClause != null) {
            builder.havingClause("HAVING " + translateFilterExpression(
                    havingClause,
                    (predicate) -> constructHavingClauseWithReference(predicate, table, template)));

            joinPaths.addAll(extractJoinPaths(havingClause));
        }

        if (sorting != null) {
            Map<Path, Sorting.SortOrder> sortClauses = sorting.getSortingPaths();
            builder.orderByClause(extractOrderBy(sortClauses, template));

            joinPaths.addAll(extractJoinPaths(sortClauses));
        }

        builder.joinClause(extractJoin(joinPaths));

        return builder.build();
    }

    /**
     * Construct directly projection GROUP BY clause using column reference.
     *
     * @param groupByDimensions columns to project out
     * @param table queried table
     * @return <code>GROUP BY tb1.col1, tb2.col2, ...</code>
     */
    private String constructGroupByWithReference(Set<ColumnProjection> groupByDimensions,
                                                 SQLTable table) {
        return "GROUP BY " + groupByDimensions.stream()
                .map(dimension -> resolveSQLColumnReference(dimension, table))
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

        if (!lastClass.equals(dictionary.getEntityClass(table.getName()))) {
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
            return generatePredicateReference(predicate);
        }
    }

    /**
     * Construct SELECT statement expression with metrics and dimensions directly using physical table column
     * references.
     *
     * @param template query template with nested subquery
     * @param table queried table
     * @return <code>SELECT function(metric1) AS alias1, tb1.dimension1 AS alias2</code>
     */
    private String constructProjectionWithReference(SQLQueryTemplate template, SQLTable table) {
        // TODO: project metric field using table column reference
        List<String> metricProjections = template.getMetrics().stream()
                .map(invocation -> invocation.getFunctionExpression() + " AS " + invocation.getAlias())
                .collect(Collectors.toList());

        Class<?> tableClass = dictionary.getEntityClass(table.getName());

        List<String> dimensionProjections = template.getGroupByDimensions().stream()
                .map(dimension -> {
                    String fieldName = dimension.getColumn().getName();

                    // relation to Non-JPA Entities object can't be projected
                    if (dictionary.isRelation(tableClass, fieldName)) {
                        Class<?> relationshipClass = dictionary.getParameterizedType(tableClass, fieldName);
                        if (!dictionary.isJPAEntity(relationshipClass)) {
                            throw new InvalidPredicateException(
                                    "Can't query on non-JPA relationship field: " + dimension.getColumn().getName());
                        }
                    }

                    return resolveSQLColumnReference(dimension, table) + " AS " + dimension.getAlias();
                })
                .collect(Collectors.toList());

        if (metricProjections.isEmpty()) {
            return "DISTINCT " + String.join(",", dimensionProjections);
        }

        return Stream.concat(metricProjections.stream(), dimensionProjections.stream())
                .collect(Collectors.joining(","));
    }

    /**
     * Build full join clause for all join paths.
     *
     * @param joinPaths paths that require joins
     * @return built join clause that contains all needed relationship dimension joins for this query.
     */
    private String extractJoin(Set<Path> joinPaths) {
        Set<String> joinClauses = new LinkedHashSet<>();

        joinPaths.forEach(path -> addJoinClauses(path, joinClauses));

        return String.join(" ", joinClauses);
    }

    /**
     * Add a join clause to a set of join clauses.
     *
     * @param joinPath join path
     * @param alreadyJoined A set of joins that have already been computed.
     */
    private void addJoinClauses(Path joinPath, Set<String> alreadyJoined) {
        String parentAlias = getTypeAlias(joinPath.getPathElements().get(0).getType());

        for (Path.PathElement pathElement : joinPath.getPathElements()) {
            String fieldName = pathElement.getFieldName();
            Class<?> parentClass = pathElement.getType();

            // Nothing left to join.
            if (! dictionary.isRelation(parentClass, fieldName)) {
                return;
            }

            String joinFragment = extractJoinClause(
                    parentClass,
                    parentAlias,
                    pathElement.getFieldType(),
                    fieldName);

            alreadyJoined.add(joinFragment);

            parentAlias = appendAlias(parentAlias, fieldName);
        }
    }

    /**
     * Build a single dimension join clause for joining a relationship table to the parent table.
     *
     * @param parentClass parent class
     * @param parentAlias parent table alias
     * @param relationshipClass relationship class
     * @param relationshipName relationship field name
     * @return built join clause i.e. <code>LEFT JOIN table1 AS dimension1 ON table0.dim_id = dimension1.id</code>
     */
    private String extractJoinClause(Class<?> parentClass,
                                     String parentAlias,
                                     Class<?> relationshipClass,
                                     String relationshipName) {
        //TODO - support composite join keys.
        //TODO - support joins where either side owns the relationship.
        //TODO - Support INNER and RIGHT joins.
        //TODO - Support toMany joins.

        String relationshipAlias = appendAlias(parentAlias, relationshipName);
        String relationshipColumnName = dictionary.getAnnotatedColumnName(parentClass, relationshipName);

        // resolve the right hand side of JOIN
        String joinSource = constructTableOrSubselect(relationshipClass);

        JoinTo joinTo = dictionary.getAttributeOrRelationAnnotation(
                parentClass,
                JoinTo.class,
                relationshipColumnName);

        String joinClause = joinTo == null
                ? String.format("%s.%s = %s.%s",
                parentAlias,
                relationshipColumnName,
                relationshipAlias,
                dictionary.getAnnotatedColumnName(
                        relationshipClass,
                        dictionary.getIdFieldName(relationshipClass)))
                : extractJoinExpression(joinTo.joinClause(), parentAlias, relationshipAlias);

        return String.format("LEFT JOIN %s AS %s ON %s",
                joinSource,
                relationshipAlias,
                joinClause);
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
     * @param sortClauses The list of sort columns and their sort order (ascending or descending).
     * @return A SQL expression
     */
    private String extractOrderBy(Map<Path, Sorting.SortOrder> sortClauses, SQLQueryTemplate template) {
        if (sortClauses.isEmpty()) {
            return "";
        }

        //TODO - Ensure that order by columns are also present in the group by.

        return " ORDER BY " + sortClauses.entrySet().stream()
                .map((entry) -> {
                    Path expandedPath = expandJoinToPath(entry.getKey());
                    Sorting.SortOrder order = entry.getValue();

                    Path.PathElement last = expandedPath.lastElement().get();

                    MetricFunctionInvocation metric = template.getMetrics().stream()
                            // TODO: filter predicate should support alias
                            .filter(invocation -> invocation.getAlias().equals(last.getFieldName()))
                            .findFirst()
                            .orElse(null);

                    String orderByClause = metric == null
                            ? generateColumnReference(expandedPath, dictionary)
                            : metric.getFunctionExpression();

                    return orderByClause + (order.equals(Sorting.SortOrder.desc) ? " DESC" : " ASC");
                })
                .collect(Collectors.joining(","));
    }

    /**
     * Expands a predicate path (from a sort or filter predicate) to the path contained in
     * the JoinTo annotation.  If no JoinTo annotation is present, the original path is returned.
     *
     * @param path The path to expand.
     * @return The expanded path.
     */
    private Path expandJoinToPath(Path path) {
        Path.PathElement pathRoot = path.getPathElements().get(0);

        Class<?> entityClass = pathRoot.getType();
        String fieldName = pathRoot.getFieldName();

        JoinTo joinTo = dictionary.getAttributeOrRelationAnnotation(entityClass, JoinTo.class, fieldName);

        if (joinTo == null || joinTo.path().equals("")) {
            return path;
        }

        return new Path(entityClass, dictionary, joinTo.path());
    }

    /**
     * Given a filter expression, extracts any entity relationship traversals that require joins.
     *
     * @param expression The filter expression
     * @return A set of path elements that capture a relationship traversal.
     */
    private Set<Path> extractJoinPaths(FilterExpression expression) {
        Collection<FilterPredicate> predicates = expression.accept(new PredicateExtractionVisitor());

        return predicates.stream()
                .map(FilterPredicate::getPath)
                .map(this::expandJoinToPath)
                .filter(path -> path.getPathElements().size() > 1)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Given a list of columns to sort on, extracts any entity relationship traversals that require joins.
     *
     * @param sortClauses The list of sort columns and their sort order (ascending or descending).
     * @return A set of path elements that capture a relationship traversal.
     */
    private Set<Path> extractJoinPaths(Map<Path, Sorting.SortOrder> sortClauses) {
        return sortClauses.keySet().stream()
                .map(this::expandJoinToPath)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Given the set of group by dimensions, extract any entity relationship traversals that require joins.
     * This method takes in a {@link SQLTable} because the sql join path meta data is stored in it.
     *
     * @param groupByDimensions The list of dimensions we are grouping on.
     * @param table queried table
     * @return A set of path elements that capture a relationship traversal.
     */
    private Set<Path> extractJoinPaths(Set<ColumnProjection> groupByDimensions,
                                       SQLTable table) {
        return resolveSQLColumns(groupByDimensions, table).stream()
                .filter((dim) -> dim.getJoinPath() != null)
                .map(SQLColumn::getJoinPath)
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
    private String generatePredicateReference(FilterPredicate predicate) {
        return generateColumnReference(predicate.getPath(), dictionary);
    }

    /**
     * Resolve all projected sql column from a queried table.
     *
     * @param columnProjections projections
     * @param table sql table
     * @return projected columns
     */
    private Set<SQLColumn> resolveSQLColumns(Set<ColumnProjection> columnProjections, SQLTable table) {
        return columnProjections.stream()
                .map(colProjection -> table.getSQLColumn(colProjection.getColumn().getName()))
                .collect(Collectors.toSet());
    }

    /**
     * Resolve projected sql column as column reference from a queried table.
     * If the projection is {@link TimeDimensionProjection}, the correct time grain expression would be used.
     *
     * @param columnProjection projection
     * @param table sql table
     * @return projected columns
     */
    private String resolveSQLColumnReference(ColumnProjection columnProjection, SQLTable table) {
        SQLColumn sqlColumn = table.getSQLColumn(columnProjection.getColumn().getName());

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
     * nor {@link Subselect} annotation is present on this class, try {@link FromTable} and {@link FromSubquery}.
     *
     * @param cls The entity class.
     * @return The physical SQL table or subselect query.
     */
    private static String resolveTableOrSubselect(EntityDictionary dictionary, Class<?> cls) {
        if (isSubselect(cls)) {
            if (cls.isAnnotationPresent(FromSubquery.class)) {
                return dictionary.getAnnotation(cls, FromSubquery.class).sql();
            } else {
                return dictionary.getAnnotation(cls, Subselect.class).value();
            }
        } else {
            javax.persistence.Table table = dictionary.getAnnotation(cls, javax.persistence.Table.class);

            if (table != null) {
                return resolveTableAnnotation(table);
            } else {
                FromTable fromTable = dictionary.getAnnotation(cls, FromTable.class);

                return fromTable != null ? fromTable.name() : dictionary.getJsonAliasFor(cls);
            }
        }
    }

    /**
     * Get the full table name from JPA {@link javax.persistence.Table} annotation.
     *
     * @param table table annotation
     * @return <code>catalog.schema.name</code>
     */
    private static String resolveTableAnnotation(javax.persistence.Table table) {
        StringBuilder fullTableName = new StringBuilder();

        if (!"".equals(table.catalog())) {
            fullTableName.append(table.catalog()).append(".");
        }
        if (!"".equals(table.schema())) {
            fullTableName.append(table.schema()).append(".");
        }
        fullTableName.append(table.name());

        return fullTableName.toString();
    }

    /**
     * Construct a join on clause based on given constraint expression, replace "%from" with from table alias
     * and "%join" with join table alias.
     *
     * @param joinClause sql join constraint
     * @param fromAlias from table alias
     * @param joinToAlias join to table alias
     * @return sql string that represents a full join condition
     */
    private String extractJoinExpression(String joinClause, String fromAlias, String joinToAlias) {
        return joinClause.replace("%from", fromAlias).replace("%join", joinToAlias);
    }

    /**
     * Check whether a class is mapped to a subselect query instead of a physical table.
     *
     * @param cls The entity class
     * @return True if the class has {@link Subselect} annotation
     */
    private static boolean isSubselect(Class<?> cls) {
        return cls.isAnnotationPresent(Subselect.class) || cls.isAnnotationPresent(FromSubquery.class);
    }
}
