/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter;

import com.yahoo.elide.core.filter.expression.Expression;
import com.yahoo.elide.core.filter.expression.Visitor;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import java.util.List;

/**
 * Predicate class.
 */
@AllArgsConstructor
public class Predicate implements Expression {

    /**
     * The path taken through data model associations to
     * reference the field in the operator.
     * Eg: author.books.publisher.name
     */
    @AllArgsConstructor
    @ToString
    public static class PathElement {
        @Getter Class type;
        @Getter String typeName;
        @Getter Class fieldType;
        @Getter String fieldName;
    }

    @Getter @NonNull private List<PathElement> path;
    @Getter @NonNull private Operator operator;
    @Getter @NonNull private List<Object> values;


    public String getField() {
        PathElement last = path.get(path.size() - 1);
        return last.getFieldName();
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
    public String toString() {
        String formattedPath = path.get(0).getTypeName();

        for (PathElement element : path) {
            formattedPath = formattedPath + "." + element.getFieldName();
        }

        return String.format("%s %s %s", formattedPath, operator, values);
    }
}
