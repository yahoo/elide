/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import com.yahoo.elide.core.exceptions.InvalidValueException;

import com.google.common.collect.ImmutableList;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Represents a path in the entity relationship graph.
 */
@EqualsAndHashCode
public class Path {
    private static final String PERIOD = ".";
    private static final String UNDERSCORE = "_";

    @Getter private List<PathElement> pathElements;
    /**
     * The path taken through data model associations to reference a given field.
     * eg. author.books.publisher.name
     */
    @AllArgsConstructor
    @ToString
    @EqualsAndHashCode
    public static class PathElement {
        @Getter private Class type;
        @Getter private Class fieldType;
        @Getter private String fieldName;
    }

    public Path(Path copy) {
        this(copy.pathElements);
    }

    public Path(List<PathElement> pathElements) {
        this.pathElements = ImmutableList.copyOf(pathElements);
    }

    public Path(Class<?> entityClass, EntityDictionary dictionary, String dotSeparatedPath) {
        List<PathElement> elements = new ArrayList<>();
        String[] fieldNames = dotSeparatedPath.split("\\.");

        Class<?> currentClass = entityClass;
        for (String fieldName : fieldNames) {
            if (dictionary.isRelation(currentClass, fieldName)) {
                Class<?> relationClass = dictionary.getParameterizedType(currentClass, fieldName);
                elements.add(new PathElement(currentClass, relationClass, fieldName));
                currentClass = relationClass;
            } else if (dictionary.isAttribute(currentClass, fieldName)
                    || fieldName.equals(dictionary.getIdFieldName(entityClass))) {
                Class<?> attributeClass = dictionary.getType(currentClass, fieldName);
                elements.add(new PathElement(currentClass, attributeClass, fieldName));
            } else if ("this".equals(fieldName)) {
                elements.add(new PathElement(currentClass, null, fieldName));
            } else {
                String alias = dictionary.getJsonAliasFor(currentClass);
                throw new InvalidValueException(alias + " doesn't contain the field " + fieldName);
            }
        }
        pathElements = ImmutableList.copyOf(elements);
    }

    public Optional<PathElement> lastElement() {
        return pathElements.isEmpty() ? Optional.empty() : Optional.of(pathElements.get(pathElements.size() - 1));
    }

    public String getFieldPath() {
        return pathElements.stream()
                .map(PathElement::getFieldName)
                .collect(Collectors.joining(PERIOD));
    }

    /**
     * Returns an alias that uniquely identifies the last collection of entities in the path.
     * @return An alias for the path.
     */
    public String getAlias() {
        if (pathElements.size() < 2) {
            return lastElement()
                    .map(e -> getTypeAlias(e.getType()))
                    .orElse(null);
        }

        PathElement previous = pathElements.get(pathElements.size() - 2);
        return getTypeAlias(previous.getType()) + UNDERSCORE + previous.getFieldName();
    }

    @Override
    public String toString() {
        return pathElements.size() == 0 ? "EMPTY"
                : pathElements.stream()
                        .map(e -> '[' + EntityDictionary.getSimpleName(e.getType()) + ']' + PERIOD + e.getFieldName())
                .collect(Collectors.joining("/"));
    }

    /**
     * Convert a class name into a hibernate friendly name.
     * @param type The type to alias
     * @return type name alias that will likely not conflict with other types or with reserved keywords.
     */
    public static String getTypeAlias(Class<?> type) {
        return type.getCanonicalName().replace(PERIOD, UNDERSCORE);
    }
}
