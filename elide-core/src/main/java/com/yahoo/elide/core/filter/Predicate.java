/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.Visitor;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.NonNull;


import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * Predicate class.
 */
@AllArgsConstructor
@EqualsAndHashCode
public class Predicate implements FilterExpression, Function<EntityDictionary, java.util.function.Predicate> {

    /**
     * The path taken through data model associations to
     * reference the field in the operator.
     * Eg: author.books.publisher.name
     */
    @AllArgsConstructor
    @ToString
    @EqualsAndHashCode
    public static class PathElement {
        @Getter Class type;
        @Getter String typeName;
        @Getter Class fieldType;
        @Getter String fieldName;
    }

    @Getter @NonNull private List<PathElement> path;
    @Getter @NonNull private Operator operator;
    @Getter @NonNull private List<Object> values;

    public Predicate(PathElement pathElement, Operator op, List<Object> values) {
        this(Collections.singletonList(pathElement), op, values);
    }

    public Predicate(PathElement pathElement, Operator op) {
        this(Collections.singletonList(pathElement), op, Collections.emptyList());
    }

    public Predicate(List<PathElement> path, Operator op) {
        this(path, op, Collections.emptyList());
    }

    public String getField() {
        PathElement last = path.get(path.size() - 1);
        return last.getFieldName();
    }

    public String getFieldPath() {
        StringBuilder fieldPath = new StringBuilder();
        for (PathElement pathElement : path) {
            if (fieldPath.length() != 0) {
                fieldPath.append('.');
            }
            fieldPath.append(pathElement.getFieldName());
        }
        return fieldPath.toString();
    }

    /**
     * get a unique name for this predicate to be used as a parameter name
     * @return unique name
     */
    public String getParameterName() {
        return getFieldPath().replace('.', '_') + '_' + Integer.toHexString(hashCode());
    }

    public String getEntityType() {
        PathElement last = path.get(path.size() - 1);
        return last.getTypeName();
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitPredicate(this);
    }

    @Override
    public java.util.function.Predicate apply(EntityDictionary dictionary) {
        return operator.contextualize(getFieldPath(), values, dictionary);
    }

    @Override
    public String toString() {
        String formattedPath = path.isEmpty() ? "" : path.get(0).getTypeName();

        for (PathElement element : path) {
            formattedPath = formattedPath + "." + element.getFieldName();
        }

        return String.format("%s %s %s", formattedPath, operator, values);
    }
}
