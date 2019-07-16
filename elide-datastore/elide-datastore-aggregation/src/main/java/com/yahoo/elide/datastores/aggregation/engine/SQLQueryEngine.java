/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.engine;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.exceptions.InvalidPredicateException;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.HQLFilterOperation;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.PredicateExtractionVisitor;
import com.yahoo.elide.core.pagination.Pagination;
import com.yahoo.elide.core.sort.Sorting;
import com.yahoo.elide.datastores.aggregation.Query;
import com.yahoo.elide.datastores.aggregation.QueryEngine;
import com.yahoo.elide.datastores.aggregation.Schema;
import com.yahoo.elide.datastores.aggregation.dimension.Dimension;
import com.yahoo.elide.datastores.aggregation.dimension.DimensionType;
import com.yahoo.elide.datastores.aggregation.engine.annotation.FromSubquery;
import com.yahoo.elide.datastores.aggregation.engine.annotation.FromTable;
import com.yahoo.elide.datastores.aggregation.engine.schema.SQLSchema;
import com.yahoo.elide.datastores.aggregation.metric.Aggregation;
import com.yahoo.elide.datastores.aggregation.metric.Metric;
import com.yahoo.elide.utils.coerce.CoerceUtil;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.mutable.MutableInt;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.persistence.Column;
import javax.persistence.EntityManager;
import javax.persistence.JoinColumn;
import javax.persistence.Table;

/**
 * QueryEngine for SQL backed stores.
 */
@Slf4j
public class SQLQueryEngine implements QueryEngine {

    private EntityManager entityManager;
    private EntityDictionary dictionary;
    @Getter
    private Map<Class<?>, SQLSchema> schemas;

    private static final String SUBQUERY = "__SUBQUERY__";

    public SQLQueryEngine(EntityManager entityManager, EntityDictionary dictionary) {
        this.entityManager = entityManager;
        this.dictionary = dictionary;
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
    public Iterable<Object> executeQuery(Query query) {
        SQLSchema schema = schemas.get(query.getSchema().getEntityClass());

        //Make sure we actually manage this schema.
        Preconditions.checkNotNull(schema);

        SQLQuery sql = toSQL(query);

        log.debug("Running native SQL query: {}", sql);

        javax.persistence.Query jpaQuery = entityManager.createNativeQuery(sql.toString());

        Pagination pagination = query.getPagination();
        if (pagination != null) {
            jpaQuery.setFirstResult(pagination.getOffset());
            jpaQuery.setMaxResults(pagination.getLimit());

            if (pagination.isGenerateTotals()) {
                javax.persistence.Query pageTotalQuery =
                        entityManager.createNativeQuery(toPageTotalSQL(sql).toString());

                supplyFilterQueryParameters(query, pageTotalQuery);

                pagination.setPageTotals(CoerceUtil.coerce(pageTotalQuery.getSingleResult(), Long.class));
            }
        }

        supplyFilterQueryParameters(query, jpaQuery);

        List<Object> results = jpaQuery.getResultList();

        MutableInt counter = new MutableInt(0);

        return results.stream()
                .map((result) -> { return result instanceof Object[] ? (Object []) result : new Object[] { result }; })
                .map((result) -> coerceObjectToEntity(query, result, counter))
                .collect(Collectors.toList());
    }

    protected SQLQuery toSQL(Query query) {
        SQLSchema schema = schemas.get(query.getSchema().getEntityClass());
        Class<?> entityClass = schema.getEntityClass();

        Preconditions.checkNotNull(schema);

        SQLQuery.SQLQueryBuilder builder = SQLQuery.builder().clientQuery(query);

        String tableName = schema.getTableDefinition();
        String tableAlias = schema.getAlias();

        String joinClause = "";
        builder.projectionClause(extractProjection(query));

        if (query.getWhereFilter() != null) {
            joinClause = extractJoin(query.getWhereFilter());
            builder.whereClause("WHERE " + translateFilterExpression(schema, query.getWhereFilter(),
                    this::generateWhereClauseColumnReference));
        }

        if (query.getHavingFilter() != null) {
            builder.havingClause("HAVING " + translateFilterExpression(schema, query.getHavingFilter(),
                    (predicate) -> { return generateHavingClauseColumnReference(predicate, query); }));
        }

        if (! query.getDimensions().isEmpty())  {
            builder.groupByClause(extractGroupBy(query));
        }

        if (query.getSorting() != null) {
            Map<Path, Sorting.SortOrder> sortClauses = query.getSorting().getValidSortingRules(entityClass, dictionary);

            builder.orderByClause(extractOrderBy(entityClass, sortClauses));
            joinClause += extractJoin(sortClauses);
        }

        builder.joinClause(joinClause);

        builder.fromClause(String.format("%s AS %s", tableName, tableAlias));

        return builder.build();
    }

    protected Object coerceObjectToEntity(Query query, Object[] result, MutableInt counter) {
        Class<?> entityClass = query.getSchema().getEntityClass();
        List<String> projections = query.getMetrics().entrySet().stream()
                .map(Map.Entry::getKey)
                .map(Metric::getName)
                .collect(Collectors.toList());

        projections.addAll(query.getDimensions().stream()
                .map(Dimension::getName)
                .collect(Collectors.toList()));

        SQLSchema schema = schemas.get(entityClass);

        Preconditions.checkArgument(result.length == projections.size());

        SQLSchema schema = (SQLSchema) query.getSchema();

        //Construct the object.
        Object entityInstance;
        try {
            entityInstance = entityClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }

        //Populate all of the fields.
        for (int idx = 0; idx < result.length; idx++) {
            Object value = result[idx];
            String fieldName = projections.get(idx);

            Dimension dim = schema.getDimension(fieldName);
            if (dim != null && dim.getDimensionType() == DimensionType.ENTITY) {
                //We don't hydrate relationships here.
                continue;
            }

            dictionary.setValue(entityInstance, fieldName, value);
        }

        //Set the ID (it must be coerced from an integer)
        dictionary.setValue(entityInstance, dictionary.getIdFieldName(entityClass), counter.getAndIncrement());

        return entityInstance;
    }

    private String translateFilterExpression(SQLSchema schema,
                                             FilterExpression expression,
                                             Function<FilterPredicate, String> columnGenerator) {
        HQLFilterOperation filterVisitor = new HQLFilterOperation();

        return filterVisitor.apply(expression, columnGenerator);
    }

    private String extractJoin(FilterExpression expression) {
        Collection<FilterPredicate> predicates = expression.accept(new PredicateExtractionVisitor());

        return predicates.stream()
                .filter(predicate -> predicate.getPath().getPathElements().size() > 1)
                .map(FilterPredicate::getPath)
                .flatMap((path) -> path.getPathElements().stream())
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
        String relationshipName = pathElement.getFieldName();
        Class<?> relationshipClass = pathElement.getFieldType();
        String relationshipAlias = FilterPredicate.getTypeAlias(relationshipClass);
        Class<?> entityClass = pathElement.getType();
        String entityAlias = FilterPredicate.getTypeAlias(entityClass);

        Table tableAnnotation = dictionary.getAnnotation(relationshipClass, Table.class);

        String relationshipTableName = (tableAnnotation == null)
                ? dictionary.getJsonAliasFor(relationshipClass)
                : tableAnnotation.name();

        String relationshipIdField = getColumnName(relationshipClass, dictionary.getIdFieldName(relationshipClass));

        return String.format("LEFT JOIN %s AS %s ON %s.%s = %s.%s",
                relationshipTableName,
                relationshipAlias,
                entityAlias,
                getColumnName(entityClass, relationshipName),
                relationshipAlias,
                relationshipIdField);
    }

    private String extractJoin(Map<Path, Sorting.SortOrder> sortClauses) {
        if (sortClauses.isEmpty()) {
            return "";
        }

        return sortClauses.entrySet().stream()
                .map(Map.Entry::getKey)
                .flatMap((path) -> path.getPathElements().stream())
                .filter((predicate) -> dictionary.isRelation(predicate.getType(), predicate.getFieldName()))
                .map(this::extractJoin)
                .collect(Collectors.joining(" "));
    }

    private String extractOrderBy(Class<?> entityClass, Map<Path, Sorting.SortOrder> sortClauses) {
        if (sortClauses.isEmpty()) {
            return "";
        }

        return " ORDER BY " + sortClauses.entrySet().stream()
                .map((entry) -> {
                    Path path = entry.getKey();
                    Sorting.SortOrder order = entry.getValue();

                    Path.PathElement last = path.lastElement().get();

                    return FilterPredicate.getTypeAlias(last.getType())
                            + "."
                            + getColumnName(entityClass, last.getFieldName())
                            + (order.equals(Sorting.SortOrder.desc) ? " DESC" : " ASC");
                }).collect(Collectors.joining(","));
    }

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
        Column[] column = dictionary.getAttributeOrRelationAnnotations(entityClass, Column.class, fieldName);

        JoinColumn[] joinColumn = dictionary.getAttributeOrRelationAnnotations(entityClass,
                JoinColumn.class, fieldName);

        if (column == null || column.length == 0) {
            if (joinColumn == null || joinColumn.length == 0) {
                return fieldName;
            } else {
                return joinColumn[0].name();
            }
        } else {
            return column[0].name();
        }
    }

    private SQLQuery toPageTotalSQL(SQLQuery sql) {
        Query clientQuery = sql.getClientQuery();

        String groupByDimensions = clientQuery.getDimensions().stream()
            .map(Dimension::getName)
            .map((name) -> getColumnName(clientQuery.getSchema().getEntityClass(), name))
            .collect(Collectors.joining(","));

        String projectionClause = String.format("SELECT COUNT(DISTINCT(%s))", groupByDimensions);

        return SQLQuery.builder()
                .clientQuery(sql.getClientQuery())
                .projectionClause(projectionClause)
                .fromClause(sql.getFromClause())
                .joinClause(sql.getJoinClause())
                .whereClause(sql.getWhereClause())
                .build();
    }

    private String extractProjection(Query query) {
        List<String> metricProjections = query.getMetrics().entrySet().stream()
                .map((entry) -> {
                    Metric metric = entry.getKey();
                    Class<? extends Aggregation> agg = entry.getValue();
                    return metric.getMetricExpression(Optional.of(agg)) + " AS " + metric.getName();
                })
                .collect(Collectors.toList());

        List<String> dimensionProjections = query.getDimensions().stream()
                .map(Dimension::getName)
                .map((name) -> getColumnName(query.getSchema().getEntityClass(), name))
                .collect(Collectors.toList());

        String projectionClause = metricProjections.stream()
                .collect(Collectors.joining(","));

        if (!dimensionProjections.isEmpty()) {
            projectionClause = projectionClause + "," + dimensionProjections.stream()
                    .map((name) -> query.getSchema().getAlias() + "." + name)
                    .collect(Collectors.joining(","));
        }

        return projectionClause;
    }

    private String extractGroupBy(Query query) {
        List<String> dimensionProjections = query.getDimensions().stream()
                .map(Dimension::getName)
                .map((name) -> getColumnName(query.getSchema().getEntityClass(), name))
                .collect(Collectors.toList());

        return "GROUP BY " + dimensionProjections.stream()
                    .map((name) -> query.getSchema().getAlias() + "." + name)
                    .collect(Collectors.joining(","));

    }

    //Converts a filter predicate into a SQL WHERE clause column reference
    private String generateWhereClauseColumnReference(FilterPredicate predicate) {
        Path.PathElement last = predicate.getPath().lastElement().get();
        Class<?> lastClass = last.getType();

        return FilterPredicate.getTypeAlias(lastClass) + "." + getColumnName(lastClass, last.getFieldName());
    }

    //Converts a filter predicate into a SQL HAVING clause column reference
    private String generateHavingClauseColumnReference(FilterPredicate predicate, Query query) {
        Path.PathElement last = predicate.getPath().lastElement().get();
        Class<?> lastClass = last.getType();

        if (! lastClass.equals(query.getSchema().getEntityClass())) {
            throw new InvalidPredicateException("The having clause can only reference fact table aggregations.");
        }

        Schema schema = schemas.get(lastClass);
        Metric metric = schema.getMetric(last.getFieldName());
        Class<? extends Aggregation> agg = query.getMetrics().get(metric);

        return metric.getMetricExpression(Optional.of(agg));
    }
}
