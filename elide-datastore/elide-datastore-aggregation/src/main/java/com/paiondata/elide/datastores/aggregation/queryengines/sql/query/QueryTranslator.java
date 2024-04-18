/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.aggregation.queryengines.sql.query;

import com.paiondata.elide.core.Path;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.exceptions.BadRequestException;
import com.paiondata.elide.core.filter.expression.FilterExpression;
import com.paiondata.elide.core.filter.expression.PredicateExtractionVisitor;
import com.paiondata.elide.core.filter.predicates.FilterPredicate;
import com.paiondata.elide.core.request.Argument;
import com.paiondata.elide.core.request.Pagination;
import com.paiondata.elide.core.request.Sorting;
import com.paiondata.elide.core.type.Type;
import com.paiondata.elide.datastores.aggregation.metadata.ColumnContext;
import com.paiondata.elide.datastores.aggregation.metadata.MetaDataStore;
import com.paiondata.elide.datastores.aggregation.metadata.TableContext;
import com.paiondata.elide.datastores.aggregation.metadata.enums.ValueType;
import com.paiondata.elide.datastores.aggregation.query.ColumnProjection;
import com.paiondata.elide.datastores.aggregation.query.Query;
import com.paiondata.elide.datastores.aggregation.query.QueryVisitor;
import com.paiondata.elide.datastores.aggregation.query.Queryable;
import com.paiondata.elide.datastores.aggregation.query.TableSQLMaker;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.annotation.FromSubquery;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.annotation.FromTable;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialect;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.expression.ExpressionParser;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.expression.JoinExpressionExtractor;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.expression.Reference;
import com.paiondata.elide.datastores.jpql.filter.FilterTranslator;

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
public class QueryTranslator implements QueryVisitor<NativeQuery.NativeQueryBuilder> {

    private final MetaDataStore metaDataStore;
    private final EntityDictionary dictionary;
    private final SQLDialect dialect;
    private FilterTranslator filterTranslator;
    private final ExpressionParser parser;
    private final Query clientQuery;

    public QueryTranslator(MetaDataStore metaDataStore, SQLDialect sqlDialect, Query clientQuery) {
        this.metaDataStore = metaDataStore;
        this.dictionary = metaDataStore.getMetadataDictionary();
        this.dialect = sqlDialect;
        this.filterTranslator = new FilterTranslator(dictionary, sqlDialect.getPredicateGeneratorOverrides());
        this.parser = new ExpressionParser(metaDataStore);
        this.clientQuery = clientQuery;
    }

    @Override
    public NativeQuery.NativeQueryBuilder visitQuery(Query query) {
        NativeQuery.NativeQueryBuilder builder = query.getSource().accept(this);

        if (query.isNested()) {
            NativeQuery innerQuery = builder.build();

            builder = NativeQuery.builder().fromClause(getFromClause("(" + innerQuery + ")",
                                                                     applyQuotes(query.getSource().getAlias()),
                                                                     dialect));
        }

        Set<String> joinExpressions = new LinkedHashSet<>();

        builder.projectionClause(constructProjectionWithReference(query));

        //Handles join for all type of column projects - dimensions, metrics and time dimention
        joinExpressions.addAll(extractJoinExpressions(query));

        Set<ColumnProjection> groupByDimensions = query.getAllDimensionProjections().stream()
                .map(SQLColumnProjection.class::cast)
                .filter(SQLColumnProjection::isProjected)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (!groupByDimensions.isEmpty()) {
            if (!query.getMetricProjections().isEmpty()) {
                builder.groupByClause("GROUP BY " + groupByDimensions.stream()
                        .map(SQLColumnProjection.class::cast)
                        .map((column) -> column.toSQL(query, metaDataStore))
                        .collect(Collectors.joining(", ")));
            }
        }

        if (query.getWhereFilter() != null) {
            builder.whereClause("WHERE " + translateFilterExpression(
                    query.getWhereFilter(),
                    path -> generatePredicatePathReference(path, query)));

            joinExpressions.addAll(extractJoinExpressions(query, query.getWhereFilter()));
        }

        if (query.getHavingFilter() != null) {
            builder.havingClause("HAVING " + translateFilterExpression(
                    query.getHavingFilter(),
                    (path) -> constructHavingClauseWithReference(path, query)));

            joinExpressions.addAll(extractJoinExpressions(query, query.getHavingFilter()));
        }

        if (query.getSorting() != null) {
            Map<Path, Sorting.SortOrder> sortClauses = query.getSorting().getSortingPaths();
            builder.orderByClause(extractOrderBy(sortClauses, query));

            joinExpressions.addAll(extractJoinExpressions(query, sortClauses));
        }

        Pagination pagination = query.getPagination();
        if (pagination != null) {
            builder.offsetLimitClause(dialect.generateOffsetLimitClause(pagination.getOffset(), pagination.getLimit()));
        }

        return builder.joinClause(String.join(" ", joinExpressions));
    }

    @Override
    public NativeQuery.NativeQueryBuilder visitQueryable(Queryable table) {
        NativeQuery.NativeQueryBuilder builder = NativeQuery.builder();

        Type<?> tableCls = dictionary.getEntityClass(table.getName(), table.getVersion());
        String tableAlias = applyQuotes(table.getAlias());

        TableContext context = TableContext.builder().tableArguments(clientQuery.getArguments()).build();

        String tableStatement;
        if (tableCls.isAnnotationPresent(FromSubquery.class)) {
            FromSubquery fromSubquery = tableCls.getAnnotation(FromSubquery.class);
            Class<? extends TableSQLMaker> makerClass = fromSubquery.maker();
            if (makerClass != null) {
                TableSQLMaker maker = dictionary.getInjector().instantiate(makerClass);
                tableStatement = "(" + context.resolve(maker.make(clientQuery)) + ")";
            } else {
                tableStatement = "(" + context.resolve(fromSubquery.sql()) + ")";
            }
        } else {
            tableStatement = tableCls.isAnnotationPresent(FromTable.class)
                    ? applyQuotes(tableCls.getAnnotation(FromTable.class).name())
                    : applyQuotes(table.getName());
        }

        return builder.fromClause(getFromClause(tableStatement, tableAlias, dialect));
    }

    /**
     * Construct HAVING clause filter using physical column references. Metric fields need to be aggregated in HAVING.
     *
     * @param path a filter predicate path in HAVING clause
     * @param query query
     * @return an filter/constraint expression that can be put in HAVING clause
     */
    private String constructHavingClauseWithReference(Path path, Query query) {
        Path.PathElement last = path.lastElement().get();
        String fieldName = last.getFieldName();

        if (path.getPathElements().size() > 1) {
            throw new BadRequestException("The having clause can only reference fact table aggregations.");
        }

        SQLMetricProjection metric = query.getMetricProjections().stream()
                .map(SQLMetricProjection.class::cast)
                // TODO: filter predicate should support alias
                .filter(invocation -> invocation.getAlias().equals(fieldName))
                .findFirst()
                .orElse(null);

        if (metric != null) {
            return metric.toSQL(query, metaDataStore);
        }
        return generatePredicatePathReference(path, query);
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
                .filter(SQLColumnProjection::isProjected)
                .filter(projection -> ! projection.getValueType().equals(ValueType.ID))
                .map(invocation -> invocation.toSQL(query, metaDataStore) + " AS "
                                + applyQuotes(invocation.getSafeAlias()))
                .collect(Collectors.toList());

        List<String> dimensionProjections = query.getAllDimensionProjections().stream()
                .map(SQLColumnProjection.class::cast)
                .filter(SQLColumnProjection::isProjected)
                .map(dimension -> dimension.toSQL(query, metaDataStore) + " AS "
                                + applyQuotes(dimension.getSafeAlias()))
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
    private String extractOrderBy(Map<Path, Sorting.SortOrder> sortClauses, Query query) {
        if (sortClauses.isEmpty()) {
            return "";
        }

        //TODO - Ensure that order by columns are also present in the group by.

        return " ORDER BY " + sortClauses.entrySet().stream()
                .map((entry) -> {
                    Path path = entry.getKey();
                    Sorting.SortOrder order = entry.getValue();

                    Path.PathElement last = path.lastElement().get();

                    SQLColumnProjection projection = fieldToColumnProjection(query, last.getAlias());
                    String orderByClause = (query.getColumnProjections().contains(projection)
                            && dialect.useAliasForOrderByClause())
                            ? applyQuotes(projection.getSafeAlias())
                            : projection.toSQL(query, metaDataStore);

                    return orderByClause + (order.equals(Sorting.SortOrder.desc) ? " DESC" : " ASC");
                })
                .collect(Collectors.joining(","));
    }

    /**
     * Coverts a Path from a table to a join path.
     * @param query query
     * @param path The path object from the table that may contain a join.
     * @return
     */
    private Set<String> extractJoinExpressions(Query query, Path path) {
        SQLColumnProjection columnProj = pathToColumnProjection(path, query);
        return extractJoinExpressions(columnProj, query);
    }

    /**
     * Given a filter expression, extracts any entity relationship traversals that require joins.
     *
     * @param query query
     * @param expression The filter expression
     * @return A set of Join expressions that capture a relationship traversal.
     */
    private Set<String> extractJoinExpressions(Query query, FilterExpression expression) {
        Collection<FilterPredicate> predicates = expression.accept(new PredicateExtractionVisitor());

        return predicates.stream()
                .map(FilterPredicate::getPath)
                .map(path -> extractJoinExpressions(query, path))
                .flatMap(Set::stream)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Given a list of columns to sort on, extracts any entity relationship traversals that require joins.
     *
     * @param query query
     * @param sortClauses The list of sort columns and their sort order (ascending or descending).
     * @return A set of Join expressions that capture a relationship traversal.
     */
    private Set<String> extractJoinExpressions(Query query, Map<Path, Sorting.SortOrder> sortClauses) {
        return sortClauses.keySet().stream()
                .map(path -> extractJoinExpressions(query, path))
                .flatMap(Set::stream)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Get required join expressions for all the projected columns in given Query.
     * @param query Expanded Query.
     * @return A set of Join expressions that capture a relationship traversal.
     */
    private Set<String> extractJoinExpressions(Query query) {
        return query.getColumnProjections().stream()
                .filter(column -> column.isProjected())
                .map(column -> extractJoinExpressions(column, query))
                .flatMap(Collection::stream)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Get required join expressions for given column in given Query.
     * @param column {@link ColumnProjection}
     * @param query Expanded Query.
     * @return A set of Join expressions that capture a relationship traversal.
     */
    private Set<String> extractJoinExpressions(ColumnProjection column, Query query) {
        Set<String> joinExpressions = new LinkedHashSet<>();

        ColumnContext context = ColumnContext.builder()
                        .queryable(query)
                        .alias(query.getSource().getAlias())
                        .metaDataStore(metaDataStore)
                        .column(column)
                        .tableArguments(query.getArguments())
                        .build();

        JoinExpressionExtractor visitor = new JoinExpressionExtractor(context, clientQuery);
        List<Reference> references = parser.parse(query.getSource(), column);
        references.forEach(ref -> joinExpressions.addAll(ref.accept(visitor)));
        return joinExpressions;
    }

    /**
     * Translates a filter expression into SQL.
     *
     * @param expression The filter expression
     * @param aliasGenerator A function which generates a column reference in SQL from a Path.
     * @return A SQL expression
     */
    private String translateFilterExpression(FilterExpression expression,
                                             Function<Path, String> aliasGenerator) {
        return filterTranslator.apply(expression, aliasGenerator);
    }

    /**
     * Converts a path into a SQL WHERE/HAVING clause column reference.
     *
     * @param path path to a field
     * @param query query plan
     * @return A SQL fragment that references a database column
     */
    private String generatePredicatePathReference(Path path, Query query) {
        SQLColumnProjection projection = pathToColumnProjection(path, query);
        return projection.toSQL(query, metaDataStore);
    }

    private SQLColumnProjection pathToColumnProjection(Path path, Query query) {
        Path.PathElement last = path.lastElement().get();

        Map<String, Argument> arguments = last.getArguments().stream()
                        .collect(Collectors.toMap(Argument::getName, Function.identity()));

        return fieldToColumnProjection(query, last.getAlias(), arguments);
    }

    private SQLColumnProjection fieldToColumnProjection(Query query, String fieldName) {
        ColumnProjection projection = query.getColumnProjection(fieldName);

        if (projection == null) {
            projection = query.getSource().getColumnProjection(fieldName);
        }
        return (SQLColumnProjection) projection;
    }

    private SQLColumnProjection fieldToColumnProjection(Query query, String fieldName,
                    Map<String, Argument> arguments) {

        ColumnProjection projection = query.getColumnProjection(fieldName, arguments);

        if (projection == null) {
            projection = query.getSource().getColumnProjection(fieldName, arguments);
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
        return ColumnContext.applyQuotes(str, dialect);
    }

    /**
     * Generates from clause with provided statement and alias.
     * @param fromStatement table name / subquery.
     * @param fromAlias alias for table name / subquery.
     * @param sqlDialect SQLDialect.
     * @return Generated from clause with or without "AS" before alias.
     */
    public static String getFromClause(String fromStatement, String fromAlias, SQLDialect sqlDialect) {

        if (sqlDialect.useASBeforeTableAlias()) {
            return String.format("%s AS %s", fromStatement, fromAlias);
        }
        return String.format("%s %s", fromStatement, fromAlias);
    }
}
