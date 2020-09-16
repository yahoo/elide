/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.query;

import static com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore.isTableJoin;
import static com.yahoo.elide.datastores.aggregation.queryengines.sql.SQLQueryEngine.getClassAlias;
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
import com.yahoo.elide.datastores.aggregation.metadata.models.Column;
import com.yahoo.elide.datastores.aggregation.metadata.models.Dimension;
import com.yahoo.elide.datastores.aggregation.metadata.models.Metric;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimension;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromSubquery;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromTable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialect;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable;
import com.yahoo.elide.request.Argument;
import com.yahoo.elide.request.Pagination;
import com.yahoo.elide.request.Sorting;

import org.hibernate.annotations.Subselect;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
    private final SQLDialect dialect;

    public SQLQueryConstructor(SQLReferenceTable referenceTable, SQLDialect sqlDialect) {
        this.referenceTable = referenceTable;
        this.dictionary = referenceTable.getDictionary();
        this.dialect = sqlDialect;
    }

    /**
     * Construct sql query with a template and sorting, where and having clause.
     *
     * @param clientQuery original query object
     * @param template query template constructed from client query
     * @param sorting sorting clause
     * @param whereClause where clause
     * @param havingClause having clause
     * @param pagination limit/offset clause
     * @return constructed SQLQuery object contains all information above
     */
    public SQLQuery resolveTemplate(Query clientQuery,
                                    SQLQueryTemplate template,
                                    Sorting sorting,
                                    FilterExpression whereClause,
                                    FilterExpression havingClause,
                                    Pagination pagination) {
        Table table = template.getTable();
        Class<?> tableCls = dictionary.getEntityClass(table.getName(), table.getVersion());
        String tableAlias = getClassAlias(tableCls);

        SQLQuery.SQLQueryBuilder builder = SQLQuery.builder().clientQuery(clientQuery);

        Set<JoinPath> joinPaths = new HashSet<>();

        String tableStatement = tableCls.isAnnotationPresent(FromSubquery.class)
                ? "(" + tableCls.getAnnotation(FromSubquery.class).sql() + ")"
                : tableCls.isAnnotationPresent(FromTable.class)
                ? tableCls.getAnnotation(FromTable.class).name()
                : table.getName();

        builder.fromClause(String.format("%s AS %s", tableStatement, tableAlias));

        builder.projectionClause(constructProjectionWithReference(template));

        Set<SQLColumnProjection> groupByDimensions = template.getGroupByDimensions();

        if (!groupByDimensions.isEmpty()) {
            if (!template.getMetrics().isEmpty()) {
                builder.groupByClause("GROUP BY " + groupByDimensions.stream()
                        .map((column) -> column.toSQL(template))
                        .collect(Collectors.joining(", ")));
            }

            joinPaths.addAll(extractJoinPaths(groupByDimensions, table));
        }

        if (whereClause != null) {
            builder.whereClause("WHERE " + translateFilterExpression(
                    whereClause,
                    filterPredicate -> generatePredicatePathReference(filterPredicate.getPath(), template)));

            joinPaths.addAll(extractJoinPaths(template.getTable(), whereClause));
        }

        if (havingClause != null) {
            builder.havingClause("HAVING " + translateFilterExpression(
                    havingClause,
                    (predicate) -> constructHavingClauseWithReference(predicate, template)));

            joinPaths.addAll(extractJoinPaths(template.getTable(), havingClause));
        }

        if (sorting != null) {
            Map<Path, Sorting.SortOrder> sortClauses = sorting.getSortingPaths();
            builder.orderByClause(extractOrderBy(sortClauses, template));

            joinPaths.addAll(extractJoinPaths(template.getTable(), sortClauses));
        }

        if (pagination != null) {
            builder.offsetLimitClause(dialect.appendOffsetLimit(pagination.getOffset(), pagination.getLimit()));
        }

        builder.joinClause(extractJoin(joinPaths));

        return builder.build();
    }

    /**
     * Construct HAVING clause filter using physical column references. Metric fields need to be aggregated in HAVING.
     *
     * @param predicate a filter predicate in HAVING clause
     * @param template query template
     * @return an filter/constraint expression that can be put in HAVING clause
     */
    private String constructHavingClauseWithReference(FilterPredicate predicate, SQLQueryTemplate template) {
        Path.PathElement last = predicate.getPath().lastElement().get();
        Class<?> lastClass = last.getType();
        String fieldName = last.getFieldName();

        Table table = template.getTable();
        if (!lastClass.equals(dictionary.getEntityClass(table.getName(), table.getVersion()))) {
            throw new InvalidPredicateException("The having clause can only reference fact table aggregations.");
        }

        SQLMetricProjection metric = template.getMetrics().stream()
                // TODO: filter predicate should support alias
                .filter(invocation -> invocation.getAlias().equals(fieldName))
                .findFirst()
                .orElse(null);

        if (metric != null) {
            return metric.toSQL(template);
        } else {
            return generatePredicatePathReference(predicate.getPath(), template);
        }
    }

    /**
     * Construct SELECT statement expression with metrics and dimensions directly using physical table column
     * references.
     *
     * @param template query template with nested subquery
     * @return <code>SELECT function(metric1) AS alias1, tb1.dimension1 AS alias2</code>
     */
    private String constructProjectionWithReference(SQLQueryTemplate template) {
        // TODO: project metric field using table column reference
        List<String> metricProjections = template.getMetrics().stream()
                .map(invocation -> invocation.toSQL(template) + " AS " + invocation.getAlias())
                .collect(Collectors.toList());

        List<String> dimensionProjections = template.getGroupByDimensions().stream()
                .map(dimension -> dimension.toSQL(template) + " AS " + dimension.getAlias())
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
                    Path path = entry.getKey();
                    Sorting.SortOrder order = entry.getValue();

                    Path.PathElement last = path.lastElement().get();

                    SQLColumnProjection projection = fieldToColumnProjection(template, last.getFieldName());
                    String orderByClause = (template.getColumnProjections().contains(projection)
                            && dialect.useAliasForOrderByClause())
                            ? projection.getAlias()
                            : projection.toSQL(template);

                    return orderByClause + (order.equals(Sorting.SortOrder.desc) ? " DESC" : " ASC");
                })
                .collect(Collectors.joining(","));
    }

    /**
     * Coverts a Path from a table to a join path.
     * @param table The table being queried.
     * @param path The path object from the table that may contain a join.
     * @return
     */
    private Set<JoinPath> extractJoinPaths(Table table, Path path) {
        Path.PathElement last = path.lastElement().get();

        return referenceTable.getResolvedJoinPaths(table, last.getFieldName());
    }

    /**
     * Given a filter expression, extracts any entity relationship traversals that require joins.
     *
     * @param table The table that is being queried.
     * @param expression The filter expression
     * @return A set of path elements that capture a relationship traversal.
     */
    private Set<JoinPath> extractJoinPaths(Table table, FilterExpression expression) {
        Collection<FilterPredicate> predicates = expression.accept(new PredicateExtractionVisitor());

        return predicates.stream()
                .map(FilterPredicate::getPath)
                .map(path -> extractJoinPaths(table, path))
                .flatMap(Set::stream)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Given a list of columns to sort on, extracts any entity relationship traversals that require joins.
     *
     * @param table The table that is being queried.
     * @param sortClauses The list of sort columns and their sort order (ascending or descending).
     * @return A set of path elements that capture a relationship traversal.
     */
    private Set<JoinPath> extractJoinPaths(Table table, Map<Path, Sorting.SortOrder> sortClauses) {
        return sortClauses.keySet().stream()
                .map(path -> extractJoinPaths(table, path))
                .flatMap(Set::stream)
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
    private Set<JoinPath> extractJoinPaths(Set<SQLColumnProjection> groupByDimensions,
                                           Table table) {
        return resolveProjectedDimensions(groupByDimensions, table).stream()
                .map(column -> referenceTable.getResolvedJoinPaths(table, column.getName()))
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
     * @param template query template
     * @return A SQL fragment that references a database column
     */
    private String generatePredicatePathReference(Path path, SQLQueryTemplate template) {
        Path.PathElement last = path.lastElement().get();

        SQLColumnProjection projection = fieldToColumnProjection(template, last.getFieldName());
        return projection.toSQL(template);
    }

    /**
     * Resolve all projected sql column from a queried table.
     *
     * @param columnProjections projections
     * @param table sql table
     * @return projected columns
     */
    private Set<Dimension> resolveProjectedDimensions(Set<SQLColumnProjection> columnProjections, Table table) {
        return columnProjections.stream()
                .map(colProjection -> table.getDimension(colProjection.getColumn().getName()))
                .collect(Collectors.toSet());
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

    private SQLColumnProjection fieldToColumnProjection(SQLQueryTemplate queryTemplate, String fieldName) {
        SQLColumnProjection projection = queryTemplate.getColumnProjections()
                .stream()
                .filter(columnProjection -> fieldName.equals(columnProjection.getAlias()))
                .findFirst()
                .orElse(null);

        if (projection != null) {
            return projection;
        }

        Table table = queryTemplate.getTable();

        Metric metric = table.getMetric(fieldName);
        if (metric != null) {
            return new SQLMetricProjection(metric, referenceTable, metric.getName(), new LinkedHashMap<>());
        }
        TimeDimension timeDimension = table.getTimeDimension(fieldName);
        if (timeDimension != null) {
            return new SQLTimeDimensionProjection(timeDimension, referenceTable);
        }

        Dimension dimension = table.getDimension(fieldName);

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
                return dimension.getName();
            }

            @Override
            public Map<String, Argument> getArguments() {
                return new LinkedHashMap<>();
            }
        };
    }
}
