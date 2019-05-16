/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter;

import static com.yahoo.elide.core.filter.Operator.FALSE;
import static com.yahoo.elide.core.filter.Operator.GE;
import static com.yahoo.elide.core.filter.Operator.GT;
import static com.yahoo.elide.core.filter.Operator.IN;
import static com.yahoo.elide.core.filter.Operator.INFIX;
import static com.yahoo.elide.core.filter.Operator.INFIX_CASE_INSENSITIVE;
import static com.yahoo.elide.core.filter.Operator.IN_INSENSITIVE;
import static com.yahoo.elide.core.filter.Operator.ISNULL;
import static com.yahoo.elide.core.filter.Operator.LE;
import static com.yahoo.elide.core.filter.Operator.LT;
import static com.yahoo.elide.core.filter.Operator.NOT;
import static com.yahoo.elide.core.filter.Operator.NOTNULL;
import static com.yahoo.elide.core.filter.Operator.NOT_INSENSITIVE;
import static com.yahoo.elide.core.filter.Operator.POSTFIX;
import static com.yahoo.elide.core.filter.Operator.POSTFIX_CASE_INSENSITIVE;
import static com.yahoo.elide.core.filter.Operator.PREFIX;
import static com.yahoo.elide.core.filter.Operator.PREFIX_CASE_INSENSITIVE;
import static com.yahoo.elide.core.filter.Operator.TRUE;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.exceptions.InvalidPredicateException;
import com.yahoo.elide.core.exceptions.InvalidValueException;
import com.yahoo.elide.core.filter.FilterPredicate.FilterParameter;
import com.yahoo.elide.core.filter.expression.AndFilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpressionVisitor;
import com.yahoo.elide.core.filter.expression.NotFilterExpression;
import com.yahoo.elide.core.filter.expression.OrFilterExpression;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.apache.commons.lang3.tuple.Triple;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Translates a filter predicate into a JPQL fragment.
 */
public class FilterTranslator implements FilterOperation<String> {
    private static final String FILTER_PATH_NOT_NULL = "Filtering field path cannot be empty.";
    private static final String FILTER_ALIAS_NOT_NULL = "Filtering alias cannot be empty.";
    public static final String PARAM_JOIN = ", ";
    public static final Function<FilterParameter, String> LOWERED_PARAMETER = p ->
            String.format("lower(%s)", p.getPlaceholder());
    /**
     * Converts a JPQL column alias and list of arguments into a JPQL filter predicate fragment.
     */
    @FunctionalInterface
    public interface JPQLPredicateGenerator {
        String generate(String columnAlias, List<FilterParameter> parameters);
    }

    public static Map<Operator, JPQLPredicateGenerator> operatorGenerators;
    public static Map<Triple<Operator, Class<?>, String>, JPQLPredicateGenerator> predicateOverrides;

    static {
        predicateOverrides = new HashMap<>();

        operatorGenerators = new HashMap<>();

        operatorGenerators.put(IN, (columnAlias, params) -> {
            Preconditions.checkState(! params.isEmpty());
            return String.format("%s IN (%s)", columnAlias, params.stream()
                        .map(FilterParameter::getPlaceholder)
                        .collect(Collectors.joining(PARAM_JOIN)));
        });

        operatorGenerators.put(IN_INSENSITIVE, (columnAlias, params) -> {
            Preconditions.checkState(!params.isEmpty());
            return String.format("lower(%s) IN (%s)", columnAlias, params.stream()
                    .map(LOWERED_PARAMETER)
                    .collect(Collectors.joining(PARAM_JOIN)));
        });

        operatorGenerators.put(NOT, (columnAlias, params) -> {
            Preconditions.checkState(!params.isEmpty());
            return String.format("%s NOT IN (%s)", columnAlias, params.stream()
                    .map(FilterParameter::getPlaceholder)
                    .collect(Collectors.joining(PARAM_JOIN)));

        });

        operatorGenerators.put(NOT_INSENSITIVE, (columnAlias, params) -> {
            Preconditions.checkState(!params.isEmpty());
            return String.format("lower(%s) NOT IN (%s)", columnAlias, params.stream()
                    .map(LOWERED_PARAMETER)
                    .collect(Collectors.joining(PARAM_JOIN)));
        });

        operatorGenerators.put(PREFIX, (columnAlias, params) -> {
            Preconditions.checkState(params.size() == 1);
            String paramPlaceholder = params.get(0).getPlaceholder();
            assertValidValues(columnAlias, paramPlaceholder);
            return String.format("%s LIKE CONCAT(%s, '%%')", columnAlias, paramPlaceholder);
        });

        operatorGenerators.put(PREFIX_CASE_INSENSITIVE, (columnAlias, params) -> {
            Preconditions.checkState(params.size() == 1);
            String paramPlaceholder = params.get(0).getPlaceholder();
            assertValidValues(columnAlias, paramPlaceholder);
            return String.format("lower(%s) LIKE CONCAT(lower(%s), '%%')", columnAlias, paramPlaceholder);
        });

        operatorGenerators.put(POSTFIX, (columnAlias, params) -> {
            Preconditions.checkState(params.size() == 1);
            String paramPlaceholder = params.get(0).getPlaceholder();
            assertValidValues(columnAlias, paramPlaceholder);
            return String.format("%s LIKE CONCAT('%%', %s)", columnAlias, paramPlaceholder);
        });

        operatorGenerators.put(POSTFIX_CASE_INSENSITIVE, (columnAlias, params) -> {
            Preconditions.checkState(params.size() == 1);
            String paramPlaceholder = params.get(0).getPlaceholder();
            assertValidValues(columnAlias, paramPlaceholder);
            return String.format("lower(%s) LIKE CONCAT('%%', lower(%s))", columnAlias, paramPlaceholder);
        });

        operatorGenerators.put(INFIX, (columnAlias, params) -> {
            Preconditions.checkState(params.size() == 1);
            String paramPlaceholder = params.get(0).getPlaceholder();
            assertValidValues(columnAlias, paramPlaceholder);
            return String.format("%s LIKE CONCAT('%%', %s, '%%')", columnAlias, paramPlaceholder);
        });

        operatorGenerators.put(INFIX_CASE_INSENSITIVE, (columnAlias, params) -> {
            Preconditions.checkState(params.size() == 1);
            String paramPlaceholder = params.get(0).getPlaceholder();
            assertValidValues(columnAlias, paramPlaceholder);
            return String.format("lower(%s) LIKE CONCAT('%%', lower(%s), '%%')", columnAlias, paramPlaceholder);
        });

        operatorGenerators.put(LT, (columnAlias, params) -> {
            Preconditions.checkState(!params.isEmpty());
            return String.format("%s < %s", columnAlias, params.size() == 1
                    ? params.get(0).getPlaceholder()
                    : leastClause(params));
        });

        operatorGenerators.put(LE, (columnAlias, params) -> {
            Preconditions.checkState(!params.isEmpty());
            return String.format("%s <= %s", columnAlias, params.size() == 1
                    ? params.get(0).getPlaceholder()
                    : leastClause(params));
        });

        operatorGenerators.put(GT, (columnAlias, params) -> {
            Preconditions.checkState(!params.isEmpty());
            return String.format("%s > %s", columnAlias, params.size() == 1
                    ? params.get(0).getPlaceholder()
                    : greatestClause(params));

        });

        operatorGenerators.put(GE, (columnAlias, params) -> {
            Preconditions.checkState(!params.isEmpty());
            return String.format("%s >= %s", columnAlias, params.size() == 1
                    ? params.get(0).getPlaceholder()
                    : greatestClause(params));
        });

        operatorGenerators.put(ISNULL, (columnAlias, params) -> {
            return String.format("%s IS NULL", columnAlias);
        });

        operatorGenerators.put(NOTNULL, (columnAlias, params) -> {
            return String.format("%s IS NOT NULL", columnAlias);
        });

        operatorGenerators.put(TRUE, (columnAlias, params) -> {
            return "(1 = 1)";
        });

        operatorGenerators.put(FALSE, (columnAlias, params) -> {
            return "(1 = 0)";
        });
    }

    /**
     * Overrides the default JPQL generator for a given operator.
     * @param op The filter predicate operator
     * @param generator The generator to resgister
     */
    public static void registerJPQLGenerator(Operator op,
                                             JPQLPredicateGenerator generator) {
        operatorGenerators.put(op, generator);
    }

    /**
     * Overrides the default JPQL generator for a given operator.
     * @param op The filter predicate operator
     * @param entityClass The entity class referenced in the predicate
     * @param fieldName The field name of the entity class referenced in the predicate.
     * @param generator The generator to resgister
     */
    public static void registerJPQLGenerator(Operator op,
                                             Class<?> entityClass,
                                             String fieldName,
                                             JPQLPredicateGenerator generator) {
        predicateOverrides.put(Triple.of(op, entityClass, fieldName), generator);
    }

    /**
     * Returns the registered JPQL generator for the given operator, class, and field.
     * @param op The filter predicate operator
     * @param entityClass The entity class referenced in the predicate
     * @param fieldName The field name of the entity class referenced in the predicate.
     * @return Returns null if no generator is registered.
     */
    public static JPQLPredicateGenerator lookupJPQLGenerator(Operator op,
                                           Class<?> entityClass,
                                           String fieldName) {
        return predicateOverrides.get(Triple.of(op, entityClass, fieldName));
    }

    /**
     * Returns the registered JPQL generator for the given operator.
     * @param op The filter predicate operator
     * @return Returns null if no generator is registered.
     */
    public static JPQLPredicateGenerator lookupJPQLGenerator(Operator op) {
        return operatorGenerators.get(op);
    }

    /**
     * Translates the filterPredicate to JPQL.
     * @param filterPredicate The predicate to translate
     * @return A JPQL query
     */
    @Override
    public String apply(FilterPredicate filterPredicate) {
        return apply(filterPredicate, false);
    }

    /**
     * Transforms a filter predicate into a HQL query fragment.
     * @param filterPredicate The predicate to transform.
     * @param prefixWithAlias Whether or not to append the entity type to the predicate.
     *                       This is useful for table aliases referenced in HQL for some kinds of joins.
     * @return The hql query fragment.
     */
    protected String apply(FilterPredicate filterPredicate, boolean prefixWithAlias) {
        String fieldPath = filterPredicate.getFieldPath();

        if (prefixWithAlias) {
            fieldPath = filterPredicate.getAlias() + "." + filterPredicate.getField();
        }

        Path.PathElement last = filterPredicate.getPath().lastElement().get();

        //HQL doesn't support 'this', but it does support aliases.
        fieldPath = fieldPath.replaceAll("\\.this", "");

        List<FilterParameter> params = filterPredicate.getParameters();

        Operator op = filterPredicate.getOperator();
        JPQLPredicateGenerator generator = lookupJPQLGenerator(op, last.getType(), last.getFieldName());

        if (generator == null) {
            generator = lookupJPQLGenerator(op);
        }

        if (generator == null) {
            throw new InvalidPredicateException("Operator not implemented: " + filterPredicate.getOperator());
        }

        return generator.generate(fieldPath, params);
    }

    private static String greatestClause(List<FilterParameter> params) {
        return String.format("greatest(%s)", params.stream()
                .map(FilterParameter::getPlaceholder)
                .collect(Collectors.joining(PARAM_JOIN)));
    }

    private static String leastClause(List<FilterParameter> params) {
        return String.format("least(%s)", params.stream()
                .map(FilterParameter::getPlaceholder)
                .collect(Collectors.joining(PARAM_JOIN)));
    }

    private static void assertValidValues(String fieldPath, String alias) {
        if (Strings.isNullOrEmpty(fieldPath)) {
            throw new InvalidValueException(FILTER_PATH_NOT_NULL);
        }
        if (Strings.isNullOrEmpty(alias)) {
            throw new IllegalStateException(FILTER_ALIAS_NOT_NULL);
        }
    }

    public String apply(FilterExpression filterExpression, boolean prefixWithAlias) {
        HQLQueryVisitor visitor = new HQLQueryVisitor(prefixWithAlias);
        return "WHERE " + filterExpression.accept(visitor);
    }

    /**
     * Filter expression visitor which builds an HQL query.
     */
    public class HQLQueryVisitor implements FilterExpressionVisitor<String> {
        public static final String TWO_NON_FILTERING_EXPRESSIONS =
                "Cannot build a filter from two non-filtering expressions";
        private boolean prefixWithAlias;

        public HQLQueryVisitor(boolean prefixWithAlias) {
            this.prefixWithAlias = prefixWithAlias;
        }

        @Override
        public String visitPredicate(FilterPredicate filterPredicate) {
            return apply(filterPredicate, prefixWithAlias);
        }

        @Override
        public String visitAndExpression(AndFilterExpression expression) {
            FilterExpression left = expression.getLeft();
            FilterExpression right = expression.getRight();
            return "(" + left.accept(this) + " AND " + right.accept(this) + ")";
        }

        @Override
        public String visitOrExpression(OrFilterExpression expression) {
            FilterExpression left = expression.getLeft();
            FilterExpression right = expression.getRight();
            return "(" + left.accept(this) + " OR " + right.accept(this) + ")";
        }

        @Override
        public String visitNotExpression(NotFilterExpression expression) {
            String negated = expression.getNegated().accept(this);
            return "NOT (" + negated + ")";
        }
    }
}
