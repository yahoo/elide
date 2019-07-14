/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.engine;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.HQLFilterOperation;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.PredicateExtractionVisitor;
import com.yahoo.elide.datastores.aggregation.Query;
import com.yahoo.elide.datastores.aggregation.QueryEngine;
import com.yahoo.elide.datastores.aggregation.dimension.Dimension;
import com.yahoo.elide.datastores.aggregation.dimension.DimensionType;
import com.yahoo.elide.datastores.aggregation.engine.annotation.FromSubquery;
import com.yahoo.elide.datastores.aggregation.engine.annotation.FromTable;
import com.yahoo.elide.datastores.aggregation.engine.schema.SQLSchema;
import com.yahoo.elide.datastores.aggregation.metric.Metric;

import com.google.common.base.Preconditions;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.dialect.CalciteSqlDialect;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParser;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    SqlDialect dialect;

    private EntityManager entityManager;
    private EntityDictionary dictionary;
    private Map<Class<?>, SQLSchema> schemas;

    private static final String SUBQUERY = "__SUBQUERY__";

    //Function to return the alias to apply to a filter predicate expression.
    private static final Function<FilterPredicate, String> ALIAS_PROVIDER = (predicate) -> {
        List<Path.PathElement> elements = predicate.getPath().getPathElements();

        Path.PathElement last = elements.get(elements.size() - 1);

        return FilterPredicate.getTypeAlias(last.getType());
    };

    public SQLQueryEngine(EntityManager entityManager, EntityDictionary dictionary, SqlDialect dialect) {
        this.entityManager = entityManager;
        this.dictionary = dictionary;
        this.dialect = dialect;
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

    public SQLQueryEngine(EntityManager entityManager, EntityDictionary dictionary) {
        this(entityManager, dictionary, CalciteSqlDialect.DEFAULT);
    }

    @Override
    public Iterable<Object> executeQuery(Query query) {
        SQLSchema schema = schemas.get(query.getEntityClass());

        Preconditions.checkNotNull(schema);

        String tableName = schema.getTableDefinition();
        String tableAlias = schema.getAlias();

        List<String> projections = query.getMetrics().stream()
                .map(Metric::getName)
                .collect(Collectors.toList());

        projections.addAll(query.getGroupDimensions().stream()
                .map(Dimension::getName)
                .collect(Collectors.toList()));

        projections.addAll(query.getTimeDimensions().stream()
                .map(Dimension::getName)
                .collect(Collectors.toList()));

        String projectionClause = projections.stream()
                .map((name) -> tableAlias + "." + name)
                .collect(Collectors.joining(","));

        String sql = String.format("SELECT %s FROM %s AS %s",
                projectionClause, tableName, tableAlias);

        if (query.getWhereFilter() != null) {
            sql += " " + extractJoin(query.getWhereFilter());
            sql += " " + translateFilterExpression(schema, query.getWhereFilter());
        }

        String nativeSql = translateSqlToNative(sql, dialect);

        log.debug("Running native SQL query: {}", nativeSql);

        javax.persistence.Query jpaQuery = entityManager.createNativeQuery(nativeSql);

        if (query.getWhereFilter() != null) {
            supplyFilterQueryParameters(query.getWhereFilter(), jpaQuery);
        }

        List<Object> results = jpaQuery.getResultList();

        return results.stream()
                .map((result) -> { return result instanceof Object[] ? (Object []) result : new Object[] { result }; })
                .map((result) -> coerceObjectToEntity(query.getEntityClass(), projections, result))
                .collect(Collectors.toList());
    }

    protected String translateSqlToNative(String sqlStatement, SqlDialect dialect) {
        log.debug("Parsing SQL {}", sqlStatement);

        SqlParser parser = SqlParser.create(sqlStatement);

        try {
            SqlNode ast = parser.parseQuery();
            String translated = ast.toSqlString(dialect).getSql();
            translated = translated.replaceAll("\'(:[a-zA-Z0-9_]+)\'", "$1");

            log.debug("TRANSLATED: {}", translated);
            return translated;

        } catch (SqlParseException e) {
            throw new IllegalStateException(e);
        }
    }

    protected Object coerceObjectToEntity(Class<?> entityClass, List<String> projections, Object[] result) {
        SQLSchema schema = schemas.get(entityClass);

        Preconditions.checkNotNull(schema);
        Preconditions.checkArgument(result.length == projections.size());

        Object entityInstance;
        try {
            entityInstance = entityClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }

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

        return entityInstance;
    }

    private String translateFilterExpression(SQLSchema schema, FilterExpression expression) {
        HQLFilterOperation filterVisitor = new HQLFilterOperation();

        String whereClause = filterVisitor.apply(expression, Optional.of(ALIAS_PROVIDER));

        return whereClause.replaceAll("(:[a-zA-Z0-9_]+)", "\'$1\'");
    }

    private String extractJoin(FilterExpression expression) {
        Collection<FilterPredicate> predicates = expression.accept(new PredicateExtractionVisitor());

        return predicates.stream()
                .filter(predicate -> predicate.getPath().getPathElements().size() > 1)
                .map(this::extractJoin)
                .collect(Collectors.joining(" "));
    }

    private String extractJoin(FilterPredicate predicate) {
        return predicate.getPath().getPathElements().stream()
                .filter((p) -> dictionary.isRelation(p.getType(), p.getFieldName()))
                .map(this::extractJoin)
                .collect(Collectors.joining(" "));
    }

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

    private void supplyFilterQueryParameters(FilterExpression expression,
                                             javax.persistence.Query query) {
        Collection<FilterPredicate> predicates = expression.accept(new PredicateExtractionVisitor());

        for (FilterPredicate filterPredicate : predicates) {
            if (filterPredicate.getOperator().isParameterized()) {
                boolean shouldEscape = filterPredicate.isMatchingOperator();
                filterPredicate.getParameters().forEach(param -> {
                    query.setParameter(param.getName(), shouldEscape ? param.escapeMatching() : param.getValue());
                });
            }
        }
    }

    /**
     * Returns the physical database column name of an entity field.  Note - we can't use Schema here because
     * we need to look at Dimension table and Fact table fields.
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
}
