/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter;

import static com.yahoo.elide.core.filter.Operator.BETWEEN;
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
import static com.yahoo.elide.core.filter.Operator.NOTBETWEEN;
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
import com.yahoo.elide.core.dictionary.EntityDictionary;
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

import java.util.EnumMap;
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

    private static Map<Operator, JPQLPredicateGenerator> globalOperatorGenerators;
    private static Map<Triple<Operator, Type<?>, String>, JPQLPredicateGenerator> globalPredicateOverrides;

    private Map<Operator, JPQLPredicateGenerator> operatorGenerators;
    private Map<Triple<Operator, Type<?>, String>, JPQLPredicateGenerator> predicateOverrides;

    static {
        globalPredicateOverrides = new HashMap<>();

        globalOperatorGenerators = new EnumMap<>(Operator.class);

        globalOperatorGenerators.put(IN, new CaseAwareJPQLGenerator(
                "%s IN (%s)",
                CaseAwareJPQLGenerator.Case.NONE,
                CaseAwareJPQLGenerator.ArgumentCount.MANY)
        );

        globalOperatorGenerators.put(IN_INSENSITIVE, new CaseAwareJPQLGenerator(
                "%s IN (%s)",
                CaseAwareJPQLGenerator.Case.LOWER,
                CaseAwareJPQLGenerator.ArgumentCount.MANY)
        );

        globalOperatorGenerators.put(NOT, new CaseAwareJPQLGenerator(
                "%s NOT IN (%s)",
                CaseAwareJPQLGenerator.Case.NONE,
                CaseAwareJPQLGenerator.ArgumentCount.MANY)
        );

        globalOperatorGenerators.put(NOT_INSENSITIVE, new CaseAwareJPQLGenerator(
                "%s NOT IN (%s)",
                CaseAwareJPQLGenerator.Case.LOWER,
                CaseAwareJPQLGenerator.ArgumentCount.MANY)
        );

        globalOperatorGenerators.put(PREFIX, new CaseAwareJPQLGenerator(
                "%s LIKE CONCAT(%s, '%%')",
                CaseAwareJPQLGenerator.Case.NONE,
                CaseAwareJPQLGenerator.ArgumentCount.ONE)
        );

        globalOperatorGenerators.put(PREFIX_CASE_INSENSITIVE, new CaseAwareJPQLGenerator(
                "%s LIKE CONCAT(%s, '%%')",
                CaseAwareJPQLGenerator.Case.LOWER,
                CaseAwareJPQLGenerator.ArgumentCount.ONE)
        );

        globalOperatorGenerators.put(POSTFIX, new CaseAwareJPQLGenerator(
                "%s LIKE CONCAT('%%', %s)",
                CaseAwareJPQLGenerator.Case.NONE,
                CaseAwareJPQLGenerator.ArgumentCount.ONE)
        );

        globalOperatorGenerators.put(POSTFIX_CASE_INSENSITIVE,  new CaseAwareJPQLGenerator(
                "%s LIKE CONCAT('%%', %s)",
                CaseAwareJPQLGenerator.Case.LOWER,
                CaseAwareJPQLGenerator.ArgumentCount.ONE)
        );

        globalOperatorGenerators.put(INFIX, new CaseAwareJPQLGenerator(
                "%s LIKE CONCAT('%%', CONCAT(%s, '%%'))",
                CaseAwareJPQLGenerator.Case.NONE,
                CaseAwareJPQLGenerator.ArgumentCount.ONE)
        );

        globalOperatorGenerators.put(INFIX_CASE_INSENSITIVE, new CaseAwareJPQLGenerator(
                "%s LIKE CONCAT('%%', CONCAT(%s, '%%'))",
                CaseAwareJPQLGenerator.Case.LOWER,
                CaseAwareJPQLGenerator.ArgumentCount.ONE)
        );

        globalOperatorGenerators.put(LT, (predicate, aliasGenerator) -> {
            Preconditions.checkState(!predicate.getParameters().isEmpty());
            return String.format("%s < %s", aliasGenerator.apply(predicate.getPath()),
                    predicate.getParameters().size() == 1
                    ? predicate.getParameters().get(0).getPlaceholder()
                    : leastClause(predicate.getParameters()));
        });

        globalOperatorGenerators.put(LE, (predicate, aliasGenerator) -> {
            Preconditions.checkState(!predicate.getParameters().isEmpty());
            return String.format("%s <= %s", aliasGenerator.apply(predicate.getPath()),
                    predicate.getParameters().size() == 1
                    ? predicate.getParameters().get(0).getPlaceholder()
                    : leastClause(predicate.getParameters()));
        });

        globalOperatorGenerators.put(GT, (predicate, aliasGenerator) -> {
            Preconditions.checkState(!predicate.getParameters().isEmpty());
            return String.format("%s > %s", aliasGenerator.apply(predicate.getPath()),
                    predicate.getParameters().size() == 1
                    ? predicate.getParameters().get(0).getPlaceholder()
                    : greatestClause(predicate.getParameters()));

        });

        globalOperatorGenerators.put(GE, (predicate, aliasGenerator) -> {
            Preconditions.checkState(!predicate.getParameters().isEmpty());
            return String.format("%s >= %s", aliasGenerator.apply(predicate.getPath()),
                    predicate.getParameters().size() == 1
                    ? predicate.getParameters().get(0).getPlaceholder()
                    : greatestClause(predicate.getParameters()));
        });

        globalOperatorGenerators.put(ISNULL, (predicate, aliasGenerator) -> {
            return String.format("%s IS NULL", aliasGenerator.apply(predicate.getPath()));
        });

        globalOperatorGenerators.put(NOTNULL, (predicate, aliasGenerator) -> {
            return String.format("%s IS NOT NULL", aliasGenerator.apply(predicate.getPath()));
        });

        globalOperatorGenerators.put(TRUE, (predicate, aliasGenerator) -> {
            return "(1 = 1)";
        });

        globalOperatorGenerators.put(FALSE, (predicate, aliasGenerator) -> {
            return "(1 = 0)";
        });

        globalOperatorGenerators.put(ISEMPTY, (predicate, aliasGenerator) -> {
            return String.format("%s IS EMPTY", aliasGenerator.apply(predicate.getPath()));
        });

        globalOperatorGenerators.put(NOTEMPTY, (predicate, aliasGenerator) -> {
            return String.format("%s IS NOT EMPTY", aliasGenerator.apply(predicate.getPath()));
        });

        globalOperatorGenerators.put(BETWEEN, (predicate, aliasGenerator) -> {
            List<FilterPredicate.FilterParameter> parameters = predicate.getParameters();
            Preconditions.checkState(!parameters.isEmpty());
            Preconditions.checkArgument(parameters.size() == 2);
            return String.format("%s BETWEEN %s AND %s",
                    aliasGenerator.apply(predicate.getPath()),
                    parameters.get(0).getPlaceholder(),
                    parameters.get(1).getPlaceholder());
        });

        globalOperatorGenerators.put(NOTBETWEEN, (predicate, aliasGenerator) -> {
            List<FilterPredicate.FilterParameter> parameters = predicate.getParameters();
            Preconditions.checkState(!parameters.isEmpty());
            Preconditions.checkArgument(parameters.size() == 2);
            return String.format("%s NOT BETWEEN %s AND %s",
                    aliasGenerator.apply(predicate.getPath()),
                    parameters.get(0).getPlaceholder(),
                    parameters.get(1).getPlaceholder());
        });
    }

    /**
     * Overrides the default JPQL generator for a given operator.
     * @param op The filter predicate operator
     * @param generator The generator to resgister
     */
    public static void registerJPQLGenerator(Operator op,
                                             JPQLPredicateGenerator generator) {
        globalOperatorGenerators.put(op, generator);
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
        globalPredicateOverrides.put(Triple.of(op, entityClass, fieldName), generator);
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
        return globalPredicateOverrides.get(Triple.of(op, entityClass, fieldName));
    }

    /**
     * Returns the registered JPQL generator for the given operator.
     * This operation should only be performed at service startup.
     * @param op The filter predicate operator
     * @return Returns null if no generator is registered.
     */
    public static JPQLPredicateGenerator lookupJPQLGenerator(Operator op) {
        return globalOperatorGenerators.get(op);
    }

    private final EntityDictionary dictionary;

    /**
     * Constructor.
     * @param dictionary Model dictionary
     */
    public FilterTranslator(EntityDictionary dictionary) {
        this.dictionary = dictionary;
        if (! globalOperatorGenerators.containsKey(HASMEMBER)) {
            globalOperatorGenerators.put(HASMEMBER, new HasMemberJPQLGenerator(dictionary));
        }

        if (! globalOperatorGenerators.containsKey(HASNOMEMBER)) {
            globalOperatorGenerators.put(HASNOMEMBER, new HasMemberJPQLGenerator(dictionary, true));
        }
        this.operatorGenerators = new HashMap<>(globalOperatorGenerators);
        this.predicateOverrides = new HashMap<>(globalPredicateOverrides);
    }

    /**
     * Constructor.
     * @param dictionary Model dictionary.
     * @param operationOverrides Contains JPQL generators that override the system defaults.
     */
    public FilterTranslator(
            EntityDictionary dictionary,
            Map<Operator, JPQLPredicateGenerator> operationOverrides) {
        this(dictionary);
        this.operatorGenerators.putAll(operationOverrides);
    }

    /**
     * Translates the filterPredicate to JPQL.
     * @param filterPredicate The predicate to translate
     * @return A JPQL query
     */
    @Override
    public String apply(FilterPredicate filterPredicate) {
        return apply(filterPredicate, this::expandPathNoAlias);
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

        JPQLPredicateGenerator generator = predicateOverrides.get(Triple.of(op, last.getType(), last.getFieldName()));

        if (generator == null) {
            generator = operatorGenerators.get(op);
        }

        if (generator == null) {
            throw new BadRequestException("Operator not implemented: " + filterPredicate.getOperator());
        }

        return generator.generate(filterPredicate, removeThisFromAlias);
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
        Function<Path, String> aliasGenerator = this::expandPathNoAlias;
        if (prefixWithAlias) {
            aliasGenerator = this::expandPathWithAlias;
        }

        return apply(filterExpression, aliasGenerator);
    }

    public String expandPathNoAlias(Path path) {
        return path.getPathElements().stream()
                .map(Path.PathElement::getFieldName)
                .collect(Collectors.joining("."));
    }

    public String expandPathWithAlias(Path path) {
        return getFieldAlias(getPathAlias(path, dictionary),
                        path.lastElement().map(Path.PathElement::getFieldName).orElse(null));
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
