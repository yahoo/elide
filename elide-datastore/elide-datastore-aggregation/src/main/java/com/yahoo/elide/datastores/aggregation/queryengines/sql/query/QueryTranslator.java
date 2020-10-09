/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.query;

import static com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore.isTableJoin;
import static com.yahoo.elide.utils.TypeHelper.appendAlias;
import static com.yahoo.elide.utils.TypeHelper.getTypeAlias;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.exceptions.InvalidPredicateException;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.FilterTranslator;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.PredicateExtractionVisitor;
import com.yahoo.elide.datastores.aggregation.annotation.Join;
import com.yahoo.elide.datastores.aggregation.core.JoinPath;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.query.QueryVisitor;
import com.yahoo.elide.datastores.aggregation.query.Queryable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromSubquery;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromTable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialect;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable;
import com.yahoo.elide.request.Pagination;
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

        Set<JoinPath> joinPaths = new HashSet<>();

        builder.projectionClause(constructProjectionWithReference(query));

        Set<ColumnProjection> groupByDimensions = query.getAllDimensionProjections();

        if (!groupByDimensions.isEmpty()) {
            if (!query.getMetricProjections().isEmpty()) {
                builder.groupByClause("GROUP BY " + groupByDimensions.stream()
                        .map(SQLColumnProjection.class::cast)
                        .map((column) -> column.toSQL(query))
                        .collect(Collectors.joining(", ")));
            }

            joinPaths.addAll(extractJoinPaths(groupByDimensions, query.getSource()));
        }

        if (query.getWhereFilter() != null) {
            builder.whereClause("WHERE " + translateFilterExpression(
                    query.getWhereFilter(),
                    filterPredicate -> generatePredicatePathReference(filterPredicate.getPath(), query)));

            joinPaths.addAll(extractJoinPaths(query.getSource(), query.getWhereFilter()));
        }

        if (query.getHavingFilter() != null) {
            builder.havingClause("HAVING " + translateFilterExpression(
                    query.getHavingFilter(),
                    (predicate) -> constructHavingClauseWithReference(predicate, query)));

            joinPaths.addAll(extractJoinPaths(query.getSource(), query.getHavingFilter()));
        }

        if (query.getSorting() != null) {
            Map<Path, Sorting.SortOrder> sortClauses = query.getSorting().getSortingPaths();
            builder.orderByClause(extractOrderBy(sortClauses, query));

            joinPaths.addAll(extractJoinPaths(query.getSource(), sortClauses));
        }

        Pagination pagination = query.getPagination();
        if (pagination != null) {
            builder.offsetLimitClause(dialect.generateOffsetLimitClause(pagination.getOffset(), pagination.getLimit()));
        }

        return builder.joinClause(extractJoin(joinPaths));
    }

    @Override
    public SQLQuery.SQLQueryBuilder visitQueryable(Queryable table) {
        SQLQuery.SQLQueryBuilder builder = SQLQuery.builder();

        Class<?> tableCls = dictionary.getEntityClass(table.getName(), table.getVersion());
        String tableAlias = table.getAlias();

        String tableStatement = tableCls.isAnnotationPresent(FromSubquery.class)
                ? "(" + tableCls.getAnnotation(FromSubquery.class).sql() + ")"
                : tableCls.isAnnotationPresent(FromTable.class)
                ? tableCls.getAnnotation(FromTable.class).name()
                : table.getName();

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
            throw new InvalidPredicateException("The having clause can only reference fact table aggregations.");
        }

        SQLMetricProjection metric = query.getMetricProjections().stream()
                .map(SQLMetricProjection.class::cast)
                // TODO: filter predicate should support alias
                .filter(invocation -> invocation.getAlias().equals(fieldName))
                .findFirst()
                .orElse(null);

        if (metric != null) {
            return metric.toSQL(query);
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
                .map(invocation -> invocation.toSQL(query) + " AS " + invocation.getAlias())
                .collect(Collectors.toList());

        List<String> dimensionProjections = query.getAllDimensionProjections().stream()
                .map(SQLColumnProjection.class::cast)
                .map(dimension -> dimension.toSQL(query) + " AS " + dimension.getAlias())
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
    private String extractJoin(Set<JoinPath> joinPaths) {
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
    private void addJoinClauses(JoinPath joinPath, Set<String> alreadyJoined) {
        String parentAlias = getTypeAlias(joinPath.getPathElements().get(0).getType());

        for (Path.PathElement pathElement : joinPath.getPathElements()) {
            String fieldName = pathElement.getFieldName();
            Class<?> parentClass = pathElement.getType();

            // Nothing left to join.
            if (!dictionary.isRelation(parentClass, fieldName) && !isTableJoin(parentClass, fieldName, dictionary)) {
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
     * Build a single dimension join clause for joining a relationship/join table to the parent table.
     *
     * @param fromClass parent class
     * @param fromAlias parent table alias
     * @param joinClass relationship/join class
     * @param joinField relationship/join field name
     * @return built join clause i.e. <code>LEFT JOIN table1 AS dimension1 ON table0.dim_id = dimension1.id</code>
     */
    private String extractJoinClause(Class<?> fromClass,
                                     String fromAlias,
                                     Class<?> joinClass,
                                     String joinField) {
        //TODO - support composite join keys.
        //TODO - support joins where either side owns the relationship.
        //TODO - Support INNER and RIGHT joins.
        //TODO - Support toMany joins.

        String joinAlias = appendAlias(fromAlias, joinField);
        String joinColumnName = dictionary.getAnnotatedColumnName(fromClass, joinField);

        // resolve the right hand side of JOIN
        String joinSource = constructTableOrSubselect(joinClass);

        Join join = dictionary.getAttributeOrRelationAnnotation(
                fromClass,
                Join.class,
                joinField);

        String joinClause = join == null
                ? String.format(
                        "%s.%s = %s.%s",
                        fromAlias,
                        joinColumnName,
                        joinAlias,
                        dictionary.getAnnotatedColumnName(
                                joinClass,
                                dictionary.getIdFieldName(joinClass)))
                : extractJoinExpression(join.value(), fromAlias, joinAlias);

        return String.format("LEFT JOIN %s AS %s ON %s",
                joinSource,
                joinAlias,
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

                    SQLColumnProjection projection = fieldToColumnProjection(plan, last.getFieldName());
                    String orderByClause = (plan.getColumnProjections().contains(projection)
                            && dialect.useAliasForOrderByClause())
                            ? projection.getAlias()
                            : projection.toSQL(plan);

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
    private Set<JoinPath> extractJoinPaths(Queryable source, Path path) {
        Path.PathElement last = path.lastElement().get();

        return referenceTable.getResolvedJoinPaths(source, last.getFieldName());
    }

    /**
     * Given a filter expression, extracts any entity relationship traversals that require joins.
     *
     * @param source The table that is being queried.
     * @param expression The filter expression
     * @return A set of path elements that capture a relationship traversal.
     */
    private Set<JoinPath> extractJoinPaths(Queryable source, FilterExpression expression) {
        Collection<FilterPredicate> predicates = expression.accept(new PredicateExtractionVisitor());

        return predicates.stream()
                .map(FilterPredicate::getPath)
                .map(path -> extractJoinPaths(source, path))
                .flatMap(Set::stream)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Given a list of columns to sort on, extracts any entity relationship traversals that require joins.
     *
     * @param source The table that is being queried.
     * @param sortClauses The list of sort columns and their sort order (ascending or descending).
     * @return A set of path elements that capture a relationship traversal.
     */
    private Set<JoinPath> extractJoinPaths(Queryable source, Map<Path, Sorting.SortOrder> sortClauses) {
        return sortClauses.keySet().stream()
                .map(path -> extractJoinPaths(source, path))
                .flatMap(Set::stream)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Given the set of group by dimensions, extract any entity relationship traversals that require joins.
     * This method takes in a {@link Table} because the sql join path meta data is stored in it.
     *
     * @param groupByDimensions The list of dimensions we are grouping on.
     * @param source queried table
     * @return A set of path elements that capture a relationship traversal.
     */
    private Set<JoinPath> extractJoinPaths(Set<ColumnProjection> groupByDimensions,
                                           Queryable source) {
        return groupByDimensions.stream()
                .map(column -> referenceTable.getResolvedJoinPaths(source, column.getName()))
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

        SQLColumnProjection projection = fieldToColumnProjection(query, last.getFieldName());
        return projection.toSQL(query);
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

    private SQLColumnProjection fieldToColumnProjection(Query query, String fieldName) {
        ColumnProjection projection = query.getColumnProjection(fieldName);
        if (projection == null) {
            projection = query.getSource().getColumnProjection(fieldName);
        }
        return (SQLColumnProjection) projection;
    }
}
