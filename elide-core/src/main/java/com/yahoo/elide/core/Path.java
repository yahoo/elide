/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import static com.yahoo.elide.core.EntityDictionary.getSimpleName;
import static com.yahoo.elide.utils.TypeHelper.appendAlias;
import static com.yahoo.elide.utils.TypeHelper.getTypeAlias;

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
        pathElements = resolvePathElements(entityClass, dictionary, dotSeparatedPath);
    }

    /**
     * Resolve a dot separated path into list of path elements.
     *
     * @param entityClass root class e.g. "foo"
     * @param dictionary dictionary
     * @param dotSeparatedPath path e.g. "bar.baz"
     * @return list of path elements e.g. ["foo.bar", "bar.baz"]
     */
    private List<PathElement> resolvePathElements(Class<?> entityClass,
                                                  EntityDictionary dictionary,
                                                  String dotSeparatedPath) {
        List<PathElement> elements = new ArrayList<>();
        String[] fieldNames = dotSeparatedPath.split("\\.");

        Class<?> currentClass = entityClass;
        for (String fieldName : fieldNames) {
            if (needNavigation(currentClass, fieldName, dictionary)) {
                Class<?> joinClass = dictionary.getParameterizedType(currentClass, fieldName);
                elements.add(new PathElement(currentClass, joinClass, fieldName));
                currentClass = joinClass;
            } else if (dictionary.isAttribute(currentClass, fieldName)
                    || fieldName.equals(dictionary.getIdFieldName(entityClass))) {
                Class<?> attributeClass = dictionary.getType(currentClass, fieldName);
                elements.add(new PathElement(currentClass, attributeClass, fieldName));
            } else if ("this".equals(fieldName)) {
                elements.add(new PathElement(currentClass, null, fieldName));
            } else {
                String alias = dictionary.getJsonAliasFor(currentClass);
                throw new InvalidValueException(alias + " does not contain the field " + fieldName);
            }
        }

        return ImmutableList.copyOf(elements);
    }

    /**
     * Check whether a field need navigation to another entity.
     *
     * @param entityClass entity class
     * @param fieldName field name
     * @param dictionary dictionary
     * @return True if the field requires navigation.
     */
    protected boolean needNavigation(Class<?> entityClass, String fieldName, EntityDictionary dictionary) {
        return dictionary.isRelation(entityClass, fieldName);
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
        return appendAlias(getTypeAlias(previous.getType()), previous.getFieldName());
    }

    @Override
    public String toString() {
        return pathElements.size() == 0 ? "EMPTY"
                : pathElements.stream()
                        .map(e -> '[' + getSimpleName(e.getType()) + ']' + PERIOD + e.getFieldName())
                .collect(Collectors.joining("/"));
    }
}
