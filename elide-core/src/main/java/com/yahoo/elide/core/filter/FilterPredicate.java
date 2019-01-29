/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.Path.PathElement;
import com.yahoo.elide.core.RelationshipType;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpressionVisitor;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import org.apache.commons.lang3.StringUtils;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Predicate class.
 */
@EqualsAndHashCode
public class FilterPredicate implements FilterExpression, Function<RequestScope, Predicate> {
    private static final String UNDERSCORE = "_";
    private static final String PERIOD = ".";
    private static final PathElement[] ELEMENT_ARRAY = new PathElement[0];

    @Getter @NonNull private Path path;
    @Getter @NonNull private Operator operator;
    @Getter @NonNull private List<Object> values;
    @Getter @NonNull private String field;
    @Getter @NonNull private String fieldPath;

    public static boolean toManyInPath(EntityDictionary dictionary, Path path) {
        return path.getPathElements().stream()
                .map(element -> dictionary.getRelationshipType(element.getType(), element.getFieldName()))
                .anyMatch(RelationshipType::isToMany);
    }

    public FilterPredicate(PathElement pathElement, Operator op, List<Object> values) {
        this(new Path(Collections.singletonList(pathElement)), op, values);
    }

    public FilterPredicate(FilterPredicate copy) {
        this(copy.path, copy.operator, copy.values);
    }

    public FilterPredicate(Path path, Operator op, List<Object> values) {
        this.operator = op;
        this.path = new Path(path);
        this.values = ImmutableList.copyOf(values);
        this.field = path.lastElement()
                .map(PathElement::getFieldName)
                .orElse(null);
        this.fieldPath = path.getPathElements().stream()
                .map(PathElement::getFieldName)
                .collect(Collectors.joining(PERIOD));
    }

    /**
     * Compute the parameter value/name pairings.
     * @return the filter parameters for this predicate
     */
    public List<FilterParameter> getParameters() {
        String baseName = String.format("%s_%s_",
                getFieldPath().replace(PERIOD, UNDERSCORE),
                Integer.toHexString(hashCode()));
        return IntStream.range(0, values.size())
                .mapToObj(idx -> new FilterParameter(String.format("%s%d", baseName, idx), values.get(idx)))
                .collect(Collectors.toList());
    }

    /**
     * Create a copy of this filter that is scoped by scope. This is used in calculating page totals, we need to
     * scope this filter in the context of it's parent.
     *
     * @param scope the path element to add to the head of the path
     * @return the scoped filter expression.
     */
    public FilterPredicate scopedBy(PathElement scope) {
        List<PathElement> pathElements = Lists.asList(scope, path.getPathElements().toArray(ELEMENT_ARRAY));
        return new FilterPredicate(new Path(pathElements), operator, values);
    }

    /**
     * Returns an alias that uniquely identifies the last collection of entities in the path.
     * @return An alias for the path.
     */
    public String getAlias() {
        List<PathElement> elements = path.getPathElements();

        PathElement last = elements.get(elements.size() - 1);

        if (elements.size() == 1) {
            return getTypeAlias(last.getType());
        }

        PathElement previous = elements.get(elements.size() - 2);

        return getTypeAlias(previous.getType()) + UNDERSCORE + previous.getFieldName();
    }

    /**
     * Build an HQL friendly alias for a class.
     *
     * @param type The type to alias
     * @return type name alias that will likely not conflict with other types or with reserved keywords.
     */
    public static String getTypeAlias(Class<?> type) {
        return type.getCanonicalName().replace(PERIOD, UNDERSCORE);
    }

    public Class getEntityType() {
        List<PathElement> elements = path.getPathElements();
        PathElement first = elements.get(0);
        return first.getType();
    }

    @Override
    public <T> T accept(FilterExpressionVisitor<T> visitor) {
        return visitor.visitPredicate(this);
    }

    @Override
    public Predicate apply(RequestScope dictionary) {
        return operator.contextualize(getFieldPath(), values, dictionary);
    }

    public boolean isMatchingOperator() {
        return operator == Operator.INFIX
                || operator == Operator.INFIX_CASE_INSENSITIVE
                || operator == Operator.PREFIX
                || operator == Operator.PREFIX_CASE_INSENSITIVE
                || operator == Operator.POSTFIX
                || operator == Operator.POSTFIX_CASE_INSENSITIVE;
    }

    @Override
    public String toString() {
        List<PathElement> elements = path.getPathElements();
        StringBuilder formattedPath = new StringBuilder();
        if (!elements.isEmpty()) {
            formattedPath.append(StringUtils.uncapitalize(EntityDictionary.getSimpleName(elements.get(0).getType())));
        }

        for (PathElement element : elements) {
            formattedPath.append(PERIOD).append(element.getFieldName());

        }
        return formattedPath.append(' ').append(operator).append(' ').append(values).toString();
    }

    public FilterPredicate negate() {
        Operator newOp = operator.negate();
        return new FilterPredicate(this.path, newOp, this.values);
    }

    /**
     * A wrapper for filter parameters, for HQL injection.
     */
    @AllArgsConstructor
    public static class FilterParameter {
        @Getter private String name;
        @Getter private Object value;

        public String getPlaceholder() {
            return ":" + name;
        }

        public String escapeMatching() {
            // It is unclear why replaceAll here breaks our tests
            return value.toString().replace("%", "\\%");
        }
    }
}
