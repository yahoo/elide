package com.yahoo.elide.datastores.aggregation.queryengines.sql;

import static com.yahoo.elide.datastores.aggregation.AggregationDictionary.getClassAlias;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.exceptions.InvalidPredicateException;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.FilterTranslator;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.PredicateExtractionVisitor;
import com.yahoo.elide.core.sort.Sorting;
import com.yahoo.elide.datastores.aggregation.AggregationDictionary;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import com.yahoo.elide.datastores.aggregation.query.DimensionProjection;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromSubquery;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromTable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.JoinTo;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metric.SQLMetricFunctionInvocation;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.schema.SQLDimensionProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.templates.SQLQueryTemplate;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SQLQueryConstructor {
    private final AggregationDictionary dictionary;

    public SQLQueryConstructor(AggregationDictionary dictionary) {
        this.dictionary = dictionary;
    }

    public SQLQuery resolveTemplate(Query clientQuery,
                                    SQLQueryTemplate template,
                                    Sorting sorting,
                                    FilterExpression whereClause,
                                    FilterExpression havingClause) {
        SQLQuery.SQLQueryBuilder builder = SQLQuery.builder().clientQuery(clientQuery);
        Class<?> tableCls = clientQuery.getTable().getCls();
        String tableAlias = getClassAlias(tableCls);

        Set<Path.PathElement> joinPredicates = new HashSet<>();

        if (template.getLevel() > 1) {
            builder.fromClause(
                    String.format(
                            "(%s) AS %s",
                            resolveTemplate(
                                    clientQuery,
                                    template.getSubQuery(),
                                    null,
                                    whereClause,
                                    null).toString(),
                            tableAlias));

            builder.projectionClause(directlyProject(template));

            if (!template.getGroupByDimensions().isEmpty()) {
                List<SQLDimensionProjection> groupByDimensions = directlyProjectDimensions(template);
                builder.groupByClause(directlyProjectGroupBy(groupByDimensions));
            }

            if (havingClause != null) {
                joinPredicates.addAll(extractPathElements(havingClause));
                builder.havingClause("HAVING " + translateFilterExpression(
                        havingClause,
                        (predicate) -> directlyProjectHavingFilter(predicate, clientQuery, template)));
            }

        } else {
            Table table = clientQuery.getTable();

            String tableStatement = tableCls.isAnnotationPresent(FromSubquery.class)
                    ? "(" + tableCls.getAnnotation(FromSubquery.class).sql() + ")"
                    : tableCls.isAnnotationPresent(FromTable.class)
                    ? tableCls.getAnnotation(FromTable.class).name()
                    : table.getName();

            builder.fromClause(String.format("%s AS %s", tableStatement, tableAlias));

            builder.projectionClause(referenceProject(template, table));

            if (!template.getGroupByDimensions().isEmpty()) {
                List<SQLDimensionProjection> groupByDimensions = referenceProjectDimensions(template, table);
                builder.groupByClause(referenceProjectGroupBy(groupByDimensions));
                joinPredicates.addAll(extractPathElements(groupByDimensions));
            }

            if (whereClause != null) {
                joinPredicates.addAll(extractPathElements(whereClause));
                builder.whereClause("WHERE " + translateFilterExpression(
                        whereClause,
                        this::generateWhereClauseColumnReference));
            }

            if (havingClause != null) {
                joinPredicates.addAll(extractPathElements(havingClause));
                builder.havingClause("HAVING " + translateFilterExpression(
                        havingClause,
                        (predicate) -> referenceProjectHavingFilter(predicate, clientQuery, template)));
            }
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
     * Project dimension fields from subquery directly using alias.
     *
     * @param template sql query template with subquery
     * @return projected columns
     */
    private List<SQLDimensionProjection> directlyProjectDimensions(SQLQueryTemplate template) {
        Set<DimensionProjection> dimensions = template.getGroupByDimensions();
        dimensions.add(template.getTimeDimension());

        return dimensions.stream()
                .map(dimensionProjection -> new SQLDimensionProjection(
                        dimensionProjection.getAlias(), null, dimensionProjection.getAlias(), null))
                .collect(Collectors.toList());
    }

    /**
     * Construct directly projection GROUP BY clause using col aliases.
     *
     * @param dimensions columns to project out
     * @return <code>GROUP BY col1, col2, ...</code>
     */
    private String directlyProjectGroupBy(List<SQLDimensionProjection> dimensions) {
        return "GROUP BY " + dimensions.stream()
                .map(SQLDimensionProjection::getColumnName)
                .collect(Collectors.joining(", "));
    }

    /**
     *
     */
    private String directlyProjectHavingFilter(FilterPredicate predicate, Query query, SQLQueryTemplate template) {
        Path.PathElement last = predicate.getPath().lastElement().get();
        Class<?> lastClass = last.getType();
        String fieldName = last.getFieldName();

        if (!lastClass.equals(query.getTable().getCls())) {
            throw new InvalidPredicateException("The having clause can only reference fact table aggregations.");
        }

        SQLMetricFunctionInvocation metric = template.getMetrics().stream()
                // TODO: filter predicate should support alias
                .filter(invocation -> invocation.getAggregatedField().getFieldName().equals(fieldName))
                .findFirst()
                .orElse(null);

        if (metric != null) {
            return metric.toSQLExpression();
        } else {
            DimensionProjection dimension = template.getGroupByDimensions().stream()
                    .filter(projection -> projection.getAlias().equals(fieldName))
                    .findFirst()
                    .orElse(null);

            if (dimension == null) {
                if (template.getTimeDimension().getAlias().equals(fieldName)) {
                    return template.getTimeDimension().getAlias();
                }

                throw new InvalidPredicateException("Having clause field not found " + fieldName);
            } else {
                return dimension.getAlias();
            }
        }
    }

    private String directlyProject(SQLQueryTemplate template) {
        List<String> metricProjections = template.getMetrics().stream()
                .map(invocation -> invocation.toSQLExpression() + " AS " + invocation.getAlias())
                .collect(Collectors.toList());

        List<String> dimensionProjections = directlyProjectDimensions(template).stream()
                .map(projection -> projection.getColumnName() + " AS " + projection.getColumnAlias())
                .collect(Collectors.toList());

        String projectionClause = String.join(",", metricProjections);

        if (!dimensionProjections.isEmpty()) {
            projectionClause = projectionClause + "," + String.join(",", dimensionProjections);
        }

        return projectionClause;
    }

    /**
     *
     */
    private List<SQLDimensionProjection> referenceProjectDimensions(SQLQueryTemplate template, Table table) {
        Set<DimensionProjection> dimensions = template.getGroupByDimensions();

        if (template.getTimeDimension() != null) {
            dimensions.add(template.getTimeDimension());
        }

        return dimensions.stream()
                .map(dimensionProjection -> SQLDimensionProjection.constructSQLDimension(dimensionProjection, table, dictionary))
                .collect(Collectors.toList());
    }

    /**
     * Extracts a GROUP BY SQL clause.
     *
     * @param dimensions SQL dimensions to group by
     * @return The SQL GROUP BY clause
     */
    private String referenceProjectGroupBy(List<SQLDimensionProjection> dimensions) {
        return "GROUP BY " + dimensions.stream()
                .map(SQLDimensionProjection::getColumnReference)
                .collect(Collectors.joining(", "));
    }

    /**
     *
     */
    private String referenceProjectHavingFilter(FilterPredicate predicate, Query query, SQLQueryTemplate template) {
        Path.PathElement last = predicate.getPath().lastElement().get();
        Class<?> lastClass = last.getType();
        String fieldName = last.getFieldName();

        if (!lastClass.equals(query.getTable().getCls())) {
            throw new InvalidPredicateException("The having clause can only reference fact table aggregations.");
        }

        SQLMetricFunctionInvocation metric = template.getMetrics().stream()
                // TODO: filter predicate should support alias
                .filter(invocation -> invocation.getAggregatedField().getFieldName().equals(fieldName))
                .findFirst()
                .orElse(null);

        if (metric != null) {
            return metric.toSQLExpression();
        } else {
            return generateWhereClauseColumnReference(predicate);
        }
    }

    /**
     * Project metric and
     *
     * @param template
     * @param table
     * @return
     */
    private String referenceProject(SQLQueryTemplate template, Table table) {
        // TODO: project metric field using reference
        List<String> metricProjections = template.getMetrics().stream()
                .map(invocation -> invocation.toSQLExpression() + " AS " + invocation.getAlias())
                .collect(Collectors.toList());

        List<String> dimensionProjections = referenceProjectDimensions(template, table).stream()
                .map(dim -> dim.getColumnReference() + " AS " + dim.getColumnAlias())
                .collect(Collectors.toList());

        String projectionClause = String.join(",", metricProjections);

        if (!dimensionProjections.isEmpty()) {
            projectionClause = projectionClause + "," + String.join(",", dimensionProjections);
        }

        return projectionClause;
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

        String relationshipIdField = dictionary.getColumnName(
                relationshipClass,
                dictionary.getIdFieldName(relationshipClass));
        String relationshipColumnName = dictionary.getColumnName(
                entityClass,
                relationshipName);

        return String.format("LEFT JOIN %s AS %s ON %s.%s = %s.%s",
                dictionary.getTableOrSubselect(relationshipClass),
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

                    return getClassAlias(last.getType()) + "."
                            + dictionary.getColumnName(entityClass, last.getFieldName())
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
     *
     * @param groupByDimensions The list of dimensions we are grouping on.
     * @return A set of path elements that capture a relationship traversal.
     */
    private Set<Path.PathElement> extractPathElements(List<SQLDimensionProjection> groupByDimensions) {
        return groupByDimensions.stream()
                .filter((dim) -> dim.getJoinPath() != null)
                .map(SQLDimensionProjection::getJoinPath)
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
            return getClassAlias(lastClass) + "." + dictionary.getColumnName(lastClass, last.getFieldName());
        } else {
            return generateWhereClauseColumnReference(new Path(lastClass, dictionary, joinTo.path()));
        }
    }
}
