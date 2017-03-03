/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.RelationshipType;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.Visitor;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Predicate class.
 */
@AllArgsConstructor
@EqualsAndHashCode
public class FilterPredicate implements FilterExpression, Function<RequestScope, Predicate> {

    public static boolean toManyInPath(EntityDictionary dictionary, List<PathElement> path) {
        return path.stream()
                .map(element -> dictionary.getRelationshipType(element.getType(), element.getFieldName()))
                .anyMatch(RelationshipType::isToMany);
    }

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

    public FilterPredicate(PathElement pathElement, Operator op, List<Object> values) {
        this(Collections.singletonList(pathElement), op, values);
    }

    public FilterPredicate(PathElement pathElement, Operator op) {
        this(Collections.singletonList(pathElement), op, Collections.emptyList());
    }

    public FilterPredicate(List<PathElement> path, Operator op) {
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
     * Get a unique name for this predicate to be used as a parameter name.
     * @return unique name
     */
    public String getParameterName() {
        return getFieldPath().replace('.', '_') + '_' + Integer.toHexString(hashCode());
    }

    public String getLeafEntityType() {
        return path.get(path.size() - 1).getTypeName();
    }

    public String getRootEntityType() {
        return path.get(0).getTypeName();
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitPredicate(this);
    }

    @Override
    public Predicate apply(RequestScope dictionary) {
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
