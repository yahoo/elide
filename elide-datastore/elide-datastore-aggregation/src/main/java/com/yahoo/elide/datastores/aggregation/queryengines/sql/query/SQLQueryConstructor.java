/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.query;

import static com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore.isTableJoin;
import static com.yahoo.elide.datastores.aggregation.queryengines.sql.SQLQueryEngine.getClassAlias;
import static com.yahoo.elide.utils.TypeHelper.appendAlias;
import static com.yahoo.elide.utils.TypeHelper.getPathAlias;
import static com.yahoo.elide.utils.TypeHelper.getTypeAlias;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.exceptions.InvalidPredicateException;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.FilterTranslator;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.PredicateExtractionVisitor;
import com.yahoo.elide.datastores.aggregation.annotation.Join;
import com.yahoo.elide.datastores.aggregation.annotation.JoinTo;
import com.yahoo.elide.datastores.aggregation.core.JoinPath;
import com.yahoo.elide.datastores.aggregation.metadata.metric.MetricFunctionInvocation;
import com.yahoo.elide.datastores.aggregation.metadata.models.Dimension;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimension;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimensionGrain;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.query.TimeDimensionProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromSubquery;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromTable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable;
import com.yahoo.elide.request.Sorting;

import com.google.common.collect.Streams;
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
    private final SQLReferenceTable referenceTable;
    private final EntityDictionary dictionary;

    public SQLQueryConstructor(SQLReferenceTable referenceTable) {
        this.referenceTable = referenceTable;
        this.dictionary = referenceTable.getDictionary();
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
        Table table = clientQuery.getTable();
        Class<?> tableCls = dictionary.getEntityClass(clientQuery.getTable().getId());
        String tableAlias = getClassAlias(tableCls);

        SQLQuery.SQLQueryBuilder builder = SQLQuery.builder().clientQuery(clientQuery);

        Set<JoinPath> joinPaths = new HashSet<>();

        String tableStatement = tableCls.isAnnotationPresent(FromSubquery.class)
                ? "(" + tableCls.getAnnotation(FromSubquery.class).sql() + ")"
                : tableCls.isAnnotationPresent(FromTable.class)
                ? tableCls.getAnnotation(FromTable.class).name()
                : table.getId();

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
            builder.whereClause("WHERE " + translateFilterExpression(
                    whereClause,
                    filterPredicate -> generatePredicatePathReference(filterPredicate.getPath())));

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
                                                 Table table) {
        return "GROUP BY " + groupByDimensions.stream()
                .map(dimension -> resolveDimensionReference(dimension, table))
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

        if (!lastClass.equals(dictionary.getEntityClass(table.getId()))) {
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
            return generatePredicatePathReference(predicate.getPath());
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
    private String constructProjectionWithReference(SQLQueryTemplate template, Table table) {
        // TODO: project metric field using table column reference
        List<String> metricProjections = template.getMetrics().stream()
                .map(invocation -> invocation.getFunctionExpression() + " AS " + invocation.getAlias())
                .collect(Collectors.toList());

        List<String> dimensionProjections = template.getGroupByDimensions().stream()
                .map(dimension -> resolveDimensionReference(dimension, table) + " AS " + dimension.getAlias())
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
    private String extractOrderBy(Map<Path, Sorting.SortOrder> sortClauses, SQLQueryTemplate template) {
        if (sortClauses.isEmpty()) {
            return "";
        }

        //TODO - Ensure that order by columns are also present in the group by.

        return " ORDER BY " + sortClauses.entrySet().stream()
                .map((entry) -> {
                    JoinPath expandedPath = extendToJoinToPath(entry.getKey());
                    Sorting.SortOrder order = entry.getValue();

                    Path.PathElement last = expandedPath.lastElement().get();

                    MetricFunctionInvocation metric = template.getMetrics().stream()
                            // TODO: filter predicate should support alias
                            .filter(invocation -> invocation.getAlias().equals(last.getFieldName()))
                            .findFirst()
                            .orElse(null);

                    String orderByClause = metric == null
                            ? referenceTable.resolveReference(expandedPath, getPathAlias(expandedPath))
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
    private JoinPath extendToJoinToPath(Path path) {
        Path.PathElement pathRoot = path.getPathElements().get(0);

        Class<?> entityClass = pathRoot.getType();
        String fieldName = pathRoot.getFieldName();

        JoinTo joinTo = dictionary.getAttributeOrRelationAnnotation(entityClass, JoinTo.class, fieldName);

        return joinTo == null || joinTo.path().equals("")
                ? new JoinPath(path)
                : new JoinPath(entityClass, dictionary, joinTo.path());

    }

    /**
     * Given a filter expression, extracts any entity relationship traversals that require joins.
     *
     * @param expression The filter expression
     * @return A set of path elements that capture a relationship traversal.
     */
    private Set<JoinPath> extractJoinPaths(FilterExpression expression) {
        Collection<FilterPredicate> predicates = expression.accept(new PredicateExtractionVisitor());

        return predicates.stream()
                .map(FilterPredicate::getPath)
                .map(this::extendToJoinToPath)
                .filter(path -> path.getPathElements().size() > 1)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Given a list of columns to sort on, extracts any entity relationship traversals that require joins.
     *
     * @param sortClauses The list of sort columns and their sort order (ascending or descending).
     * @return A set of path elements that capture a relationship traversal.
     */
    private Set<JoinPath> extractJoinPaths(Map<Path, Sorting.SortOrder> sortClauses) {
        return sortClauses.keySet().stream()
                .map(this::extendToJoinToPath)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Given the set of group by dimensions, extract any entity relationship traversals that require joins.
     * This method takes in a {@link Table} because the sql join path meta data is stored in it.
     *
     * @param groupByDimensions The list of dimensions we are grouping on.
     * @param table queried table
     * @return A set of path elements that capture a relationship traversal.
     */
    private Set<JoinPath> extractJoinPaths(Set<ColumnProjection> groupByDimensions,
                                           Table table) {
        Class<?> tableClass = dictionary.getEntityClass(table.getId());

        return resolveProjectedDimensions(groupByDimensions, table).stream()
                .map(column -> referenceTable.getResolvedJoinPaths(table, column.getName()))
                .map(Collection::stream)
                .reduce(Stream.empty(), (s1, s2) -> Streams.concat(s1, s2))
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
     * @return A SQL fragment that references a database column
     */
    private String generatePredicatePathReference(Path path) {
        return referenceTable.resolveReference(path, getClassAlias(path.getPathElements().get(0).getType()));
    }

    /**
     * Resolve all projected sql column from a queried table.
     *
     * @param columnProjections projections
     * @param table sql table
     * @return projected columns
     */
    private Set<Dimension> resolveProjectedDimensions(Set<ColumnProjection> columnProjections, Table table) {
        return columnProjections.stream()
                .map(colProjection -> table.getDimension(colProjection.getColumn().getName()))
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
    private String resolveDimensionReference(ColumnProjection columnProjection, Table table) {
        Class<?> tableClass = dictionary.getEntityClass(table.getId());
        String fieldName = columnProjection.getColumn().getName();

        if (columnProjection instanceof TimeDimensionProjection) {
            TimeDimension timeDimension = ((TimeDimensionProjection) columnProjection).getTimeDimension();
            TimeDimensionGrain grainInfo = timeDimension.getSupportedGrains().stream()
                    .filter(g -> g.getGrain().equals(((TimeDimensionProjection) columnProjection).getGrain()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Requested time grain not supported."));

            //TODO - We will likely migrate to a templating language when we support parameterized metrics.
            return String.format(
                    grainInfo.getExpression(),
                    referenceTable.getResolvedReference(table, fieldName));
        } else {
            return referenceTable.getResolvedReference(table, fieldName);
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
