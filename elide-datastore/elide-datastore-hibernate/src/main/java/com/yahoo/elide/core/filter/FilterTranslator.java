/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter;

import static com.yahoo.elide.core.filter.Operator.FALSE;
import static com.yahoo.elide.core.filter.Operator.GE;
import static com.yahoo.elide.core.filter.Operator.GT;
import static com.yahoo.elide.core.filter.Operator.HASMEMBER;
import static com.yahoo.elide.core.filter.Operator.HASNOMEMBER;
import static com.yahoo.elide.core.filter.Operator.IN;
import static com.yahoo.elide.core.filter.Operator.INFIX;
import static com.yahoo.elide.core.filter.Operator.INFIX_CASE_INSENSITIVE;
import static com.yahoo.elide.core.filter.Operator.IN_INSENSITIVE;
import static com.yahoo.elide.core.filter.Operator.ISEMPTY;
import static com.yahoo.elide.core.filter.Operator.ISNULL;
import static com.yahoo.elide.core.filter.Operator.LE;
import static com.yahoo.elide.core.filter.Operator.LT;
import static com.yahoo.elide.core.filter.Operator.NOT;
import static com.yahoo.elide.core.filter.Operator.NOTEMPTY;
import static com.yahoo.elide.core.filter.Operator.NOTNULL;
import static com.yahoo.elide.core.filter.Operator.NOT_INSENSITIVE;
import static com.yahoo.elide.core.filter.Operator.POSTFIX;
import static com.yahoo.elide.core.filter.Operator.POSTFIX_CASE_INSENSITIVE;
import static com.yahoo.elide.core.filter.Operator.PREFIX;
import static com.yahoo.elide.core.filter.Operator.PREFIX_CASE_INSENSITIVE;
import static com.yahoo.elide.core.filter.Operator.TRUE;
import static com.yahoo.elide.core.utils.TypeHelper.getFieldAlias;
import static com.yahoo.elide.core.utils.TypeHelper.getPathAlias;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.exceptions.BadRequestException;
import com.yahoo.elide.core.filter.expression.AndFilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpressionVisitor;
import com.yahoo.elide.core.filter.expression.NotFilterExpression;
import com.yahoo.elide.core.filter.expression.OrFilterExpression;
import com.yahoo.elide.core.filter.predicates.FilterPredicate;
import com.yahoo.elide.core.filter.predicates.FilterPredicate.FilterParameter;
import com.yahoo.elide.core.type.Type;
import com.google.common.base.Preconditions;
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
    private static final String COMMA = ", ";

    private static Map<Operator, JPQLPredicateGenerator> operatorGenerators;
    private static Map<Triple<Operator, Type<?>, String>, JPQLPredicateGenerator> predicateOverrides;

    public static final Function<Path, String> GENERATE_HQL_COLUMN_NO_ALIAS = (path) -> path.getPathElements().stream()
            .map(Path.PathElement::getFieldName)
            .collect(Collectors.joining("."));

    public static final Function<Path, String> GENERATE_HQL_COLUMN_WITH_ALIAS =
            (path) -> getFieldAlias(getPathAlias(path),
                    path.lastElement().map(Path.PathElement::getFieldName).orElse(null));

    static {
        predicateOverrides = new HashMap<>();

        operatorGenerators = new HashMap<>();

        operatorGenerators.put(IN, new CaseAwareJPQLGenerator(
                "%s IN (%s)",
                CaseAwareJPQLGenerator.Case.NONE,
                CaseAwareJPQLGenerator.ArgumentCount.MANY)
        );

        operatorGenerators.put(IN_INSENSITIVE, new CaseAwareJPQLGenerator(
                "%s IN (%s)",
                CaseAwareJPQLGenerator.Case.LOWER,
                CaseAwareJPQLGenerator.ArgumentCount.MANY)
        );

        operatorGenerators.put(NOT, new CaseAwareJPQLGenerator(
                "%s NOT IN (%s)",
                CaseAwareJPQLGenerator.Case.NONE,
                CaseAwareJPQLGenerator.ArgumentCount.MANY)
        );

        operatorGenerators.put(NOT_INSENSITIVE, new CaseAwareJPQLGenerator(
                "%s NOT IN (%s)",
                CaseAwareJPQLGenerator.Case.LOWER,
                CaseAwareJPQLGenerator.ArgumentCount.MANY)
        );

        operatorGenerators.put(PREFIX, new CaseAwareJPQLGenerator(
                "%s LIKE CONCAT(%s, '%%')",
                CaseAwareJPQLGenerator.Case.NONE,
                CaseAwareJPQLGenerator.ArgumentCount.ONE)
        );

        operatorGenerators.put(PREFIX_CASE_INSENSITIVE, new CaseAwareJPQLGenerator(
                "%s LIKE CONCAT(%s, '%%')",
                CaseAwareJPQLGenerator.Case.LOWER,
                CaseAwareJPQLGenerator.ArgumentCount.ONE)
        );

        operatorGenerators.put(POSTFIX, new CaseAwareJPQLGenerator(
                "%s LIKE CONCAT('%%', %s)",
                CaseAwareJPQLGenerator.Case.NONE,
                CaseAwareJPQLGenerator.ArgumentCount.ONE)
        );

        operatorGenerators.put(POSTFIX_CASE_INSENSITIVE,  new CaseAwareJPQLGenerator(
                "%s LIKE CONCAT('%%', %s)",
                CaseAwareJPQLGenerator.Case.LOWER,
                CaseAwareJPQLGenerator.ArgumentCount.ONE)
        );

        operatorGenerators.put(INFIX, new CaseAwareJPQLGenerator(
                "%s LIKE CONCAT('%%', %s, '%%')",
                CaseAwareJPQLGenerator.Case.NONE,
                CaseAwareJPQLGenerator.ArgumentCount.ONE)
        );

        operatorGenerators.put(INFIX_CASE_INSENSITIVE, new CaseAwareJPQLGenerator(
                "%s LIKE CONCAT('%%', %s, '%%')",
                CaseAwareJPQLGenerator.Case.LOWER,
                CaseAwareJPQLGenerator.ArgumentCount.ONE)
        );

        operatorGenerators.put(LT, (predicate, aliasGenerator) -> {
            Preconditions.checkState(!predicate.getParameters().isEmpty());
            return String.format("%s < %s", aliasGenerator.apply(predicate.getPath()),
                    predicate.getParameters().size() == 1
                    ? predicate.getParameters().get(0).getPlaceholder()
                    : leastClause(predicate.getParameters()));
        });

        operatorGenerators.put(LE, (predicate, aliasGenerator) -> {
            Preconditions.checkState(!predicate.getParameters().isEmpty());
            return String.format("%s <= %s", aliasGenerator.apply(predicate.getPath()),
                    predicate.getParameters().size() == 1
                    ? predicate.getParameters().get(0).getPlaceholder()
                    : leastClause(predicate.getParameters()));
        });

        operatorGenerators.put(GT, (predicate, aliasGenerator) -> {
            Preconditions.checkState(!predicate.getParameters().isEmpty());
            return String.format("%s > %s", aliasGenerator.apply(predicate.getPath()),
                    predicate.getParameters().size() == 1
                    ? predicate.getParameters().get(0).getPlaceholder()
                    : greatestClause(predicate.getParameters()));

        });

        operatorGenerators.put(GE, (predicate, aliasGenerator) -> {
            Preconditions.checkState(!predicate.getParameters().isEmpty());
            return String.format("%s >= %s", aliasGenerator.apply(predicate.getPath()),
                    predicate.getParameters().size() == 1
                    ? predicate.getParameters().get(0).getPlaceholder()
                    : greatestClause(predicate.getParameters()));
        });

        operatorGenerators.put(ISNULL, (predicate, aliasGenerator) -> {
            return String.format("%s IS NULL", aliasGenerator.apply(predicate.getPath()));
        });

        operatorGenerators.put(NOTNULL, (predicate, aliasGenerator) -> {
            return String.format("%s IS NOT NULL", aliasGenerator.apply(predicate.getPath()));
        });

        operatorGenerators.put(TRUE, (predicate, aliasGenerator) -> {
            return "(1 = 1)";
        });

        operatorGenerators.put(FALSE, (predicate, aliasGenerator) -> {
            return "(1 = 0)";
        });

        operatorGenerators.put(ISEMPTY, (predicate, aliasGenerator) -> {
            return String.format("%s IS EMPTY", aliasGenerator.apply(predicate.getPath()));
        });

        operatorGenerators.put(NOTEMPTY, (predicate, aliasGenerator) -> {
            return String.format("%s IS NOT EMPTY", aliasGenerator.apply(predicate.getPath()));
        });

        operatorGenerators.put(HASMEMBER, (predicate, aliasGenerator) -> {
            Preconditions.checkArgument(predicate.getParameters().size() == 1);
            return String.format("%s MEMBER OF %s",
                    predicate.getParameters().get(0).getPlaceholder(),
                    aliasGenerator.apply(predicate.getPath()));
        });

        operatorGenerators.put(HASNOMEMBER, (predicate, aliasGenerator) -> {
            Preconditions.checkArgument(predicate.getParameters().size() == 1);
            return String.format("%s NOT MEMBER OF %s",
                    predicate.getParameters().get(0).getPlaceholder(),
                    aliasGenerator.apply(predicate.getPath()));
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
                                             Type<?> entityClass,
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
                                                             Type<?> entityClass,
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
        return apply(filterPredicate, GENERATE_HQL_COLUMN_NO_ALIAS);
    }

    /**
     * Transforms a filter predicate into a JPQL query fragment.
     * @param filterPredicate The predicate to transform.
     * @param aliasGenerator Function which supplies a HQL fragment which represents the column in the predicate.
     * @return The hql query fragment.
     */
    protected String apply(FilterPredicate filterPredicate, Function<Path, String> aliasGenerator) {

        Function<Path, String> removeThisFromAlias = (path) -> {
            String fieldPath = aliasGenerator.apply(path);

            //JPQL doesn't support 'this', but it does support aliases.
            return fieldPath.replaceAll("\\.this", "");
        };

        Path.PathElement last = filterPredicate.getPath().lastElement().get();

        Operator op = filterPredicate.getOperator();
        JPQLPredicateGenerator generator = lookupJPQLGenerator(op, last.getType(), last.getFieldName());

        if (generator == null) {
            generator = lookupJPQLGenerator(op);
        }

        if (generator == null) {
            throw new BadRequestException("Operator not implemented: " + filterPredicate.getOperator());
        }

        return generator.generate(filterPredicate, aliasGenerator);
    }

    private static String greatestClause(List<FilterParameter> params) {
        return String.format("greatest(%s)", params.stream()
                .map(FilterParameter::getPlaceholder)
                .collect(Collectors.joining(COMMA)));
    }

    private static String leastClause(List<FilterParameter> params) {
        return String.format("least(%s)", params.stream()
                .map(FilterParameter::getPlaceholder)
                .collect(Collectors.joining(COMMA)));
    }

    /**
     * Translates the filterExpression to a JPQL filter fragment.
     * @param filterExpression The filterExpression to translate
     * @param prefixWithAlias If true, use the default alias provider to append the predicates with an alias.
     *                        Otherwise, don't append aliases.
     * @return A JPQL filter fragment.
     */
    public String apply(FilterExpression filterExpression, boolean prefixWithAlias) {
        Function<Path, String> aliasGenerator = GENERATE_HQL_COLUMN_NO_ALIAS;
        if (prefixWithAlias) {
            aliasGenerator = GENERATE_HQL_COLUMN_WITH_ALIAS;
        }

        return apply(filterExpression, aliasGenerator);
    }

    /**
     * Translates the filterExpression to a JPQL filter fragment.
     * @param filterExpression The filterExpression to translate
     * @param aliasGenerator Generates a HQL fragment that represents a column in the predicate
     * @return A JPQL filter fragment.
     */
    public String apply(FilterExpression filterExpression, Function<Path, String> aliasGenerator) {
        JPQLQueryVisitor visitor = new JPQLQueryVisitor(aliasGenerator);
        return filterExpression.accept(visitor);
    }

    /**
     * Filter expression visitor which builds an JPQL query.
     */
    public class JPQLQueryVisitor implements FilterExpressionVisitor<String> {
        private Function<Path, String> aliasGenerator;

        public JPQLQueryVisitor(Function<Path, String> aliasGenerator) {
            this.aliasGenerator = aliasGenerator;
        }

        @Override
        public String visitPredicate(FilterPredicate filterPredicate) {
            return apply(filterPredicate, aliasGenerator);
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
