/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import com.yahoo.elide.core.exceptions.InvalidValueException;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
        this.pathElements = new ArrayList(pathElements);
    }

    public Path(Class<?> entityClass, EntityDictionary dictionary, String dotSeparatedPath) {
        pathElements = new ArrayList<>();
        String[] fieldNames = dotSeparatedPath.split("\\.");

        Class<?> currentClass = entityClass;
        for (String fieldName : fieldNames) {
            if (dictionary.isRelation(currentClass, fieldName)) {
                Class<?> relationClass = dictionary.getParameterizedType(currentClass, fieldName);
                pathElements.add(new PathElement(currentClass, relationClass, fieldName));
                currentClass = relationClass;
            } else if (dictionary.isAttribute(currentClass, fieldName)
                    || fieldName.equals(dictionary.getIdFieldName(entityClass))) {
                Class<?> attributeClass = dictionary.getType(currentClass, fieldName);
                pathElements.add(new PathElement(currentClass, attributeClass, fieldName));
            } else {
                throw new InvalidValueException(dictionary.getJsonAliasFor(currentClass)
                        + " doesn't contain the field "
                        + fieldName);
            }
        }
    }

    public Optional<PathElement> lastElement() {
        return pathElements.isEmpty() ? Optional.empty() : Optional.of(pathElements.get(pathElements.size() - 1));
    }

    public String getFieldPath() {
        StringBuilder fieldPath = new StringBuilder();
        for (PathElement pathElement : pathElements) {
            if (fieldPath.length() != 0) {
                fieldPath.append(PERIOD);
            }
            fieldPath.append(pathElement.getFieldName());
        }
        return fieldPath.toString();
    }

    /**
     * Returns an alias that uniquely identifies the last collection of entities in the path.
     * @return An alias for the path.
     */
    public String getAlias() {
        PathElement last = pathElements.get(pathElements.size() - 1);

        if (pathElements.size() == 1) {
            return getTypeAlias(last.getType());
        }

        PathElement previous = pathElements.get(pathElements.size() - 2);

        return getTypeAlias(previous.getType()) + UNDERSCORE + previous.getFieldName();
    }

    /**
     * @param type The type to alias
     * @return type name alias that will likely not conflict with other types or with reserved keywords.
     */
    public static String getTypeAlias(Class<?> type) {
        return type.getCanonicalName().replace(PERIOD, UNDERSCORE);
    }
}
