/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import static com.yahoo.elide.core.dictionary.EntityDictionary.getSimpleName;
import static com.yahoo.elide.core.utils.TypeHelper.appendAlias;
import static com.yahoo.elide.core.utils.TypeHelper.getTypeAlias;

import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.exceptions.InvalidValueException;
import com.yahoo.elide.core.request.Argument;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.core.type.Type;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.commons.collections4.CollectionUtils;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents a path in the entity relationship graph.
 */
@EqualsAndHashCode
public class Path {
    private static final String PERIOD = ".";

    @Getter protected List<PathElement> pathElements;
    /**
     * The path taken through data model associations to reference a given field.
     * eg. author.books.publisher.name
     */
    @AllArgsConstructor
    @ToString
    @EqualsAndHashCode
    public static class PathElement {
        @Getter private Type type;
        @Getter private Type fieldType;
        @Getter private String fieldName;
        @Getter private String alias;
        @Getter private Set<Argument> arguments;

        public PathElement(Class<?> type, Class<?> fieldType, String fieldName) {
            this(ClassType.of(type), ClassType.of(fieldType), fieldName);
        }

        public PathElement(Type type, Type fieldType, String fieldName) {
            this.type = type;
            this.fieldType = fieldType;
            this.fieldName = fieldName;
            this.alias = fieldName;
            this.arguments = Collections.emptySet();
        }
    }

    protected Path() {
    }

    public Path(Path copy) {
        this(copy.pathElements);
    }

    public Path(List<PathElement> pathElements) {
        this.pathElements = ImmutableList.copyOf(pathElements);
    }

    public Path(Class<?> entityClass, EntityDictionary dictionary, String dotSeparatedPath) {
        this(ClassType.of(entityClass), dictionary, dotSeparatedPath);
    }

    public Path(Type<?> entityClass, EntityDictionary dictionary, String dotSeparatedPath) {
        pathElements = resolvePathElements(entityClass, dictionary, dotSeparatedPath);
    }

    public Path(Class<?> entityClass, EntityDictionary dictionary, String fieldName,
                String alias, Set<Argument> arguments) {
        this(ClassType.of(entityClass), dictionary, fieldName, alias, arguments);
    }

    public Path(Type<?> entityClass, EntityDictionary dictionary, String fieldName,
                String alias, Set<Argument> arguments) {
        pathElements = Lists.newArrayList(resolvePathAttribute(entityClass, fieldName, alias, arguments, dictionary));
    }

    public boolean isComputed(EntityDictionary dictionary) {
        for (Path.PathElement pathElement : getPathElements()) {
            Type<?> entityClass = pathElement.getType();
            String fieldName = pathElement.getFieldName();

            if (dictionary.isComputed(entityClass, fieldName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Resolve a dot separated path into list of path elements.
     *
     * @param entityClass root class e.g. "foo"
     * @param dictionary dictionary
     * @param dotSeparatedPath path e.g. "bar.baz"
     * @return list of path elements e.g. ["foo.bar", "bar.baz"]
     */
    protected List<PathElement> resolvePathElements(Type<?> entityClass,
                                                    EntityDictionary dictionary,
                                                    String dotSeparatedPath) {
        List<PathElement> elements = new ArrayList<>();
        String[] fieldNames = dotSeparatedPath.split("\\.");

        Type<?> currentClass = entityClass;
        for (String fieldName : fieldNames) {
            if (needNavigation(currentClass, fieldName, dictionary)) {
                Type<?> joinClass = dictionary.getParameterizedType(currentClass, fieldName);
                elements.add(new PathElement(currentClass, joinClass, fieldName));
                currentClass = joinClass;
            } else {
                elements.add(resolvePathAttribute(currentClass, fieldName,
                        fieldName, Collections.emptySet(), dictionary));
            }
        }

        return ImmutableList.copyOf(elements);
    }

    protected PathElement resolvePathAttribute(Type<?> entityClass,
                                               String fieldName,
                                               String alias,
                                               Set<Argument> arguments,
                                               EntityDictionary dictionary) {
        if (dictionary.isAttribute(entityClass, fieldName)
                || fieldName.equals(dictionary.getIdFieldName(entityClass))) {
            Type<?> attributeClass = dictionary.getType(entityClass, fieldName);
            return new PathElement(entityClass, attributeClass, fieldName, alias, arguments);
        }
        if ("this".equals(fieldName)) {
            return new PathElement(entityClass, null, fieldName);
        }
        String entityAlias = dictionary.getJsonAliasFor(entityClass);
        throw new InvalidValueException(entityAlias + " does not contain the field " + fieldName);
    }

    /**
     * Check whether a field need navigation to another entity.
     *
     * @param entityClass entity class
     * @param fieldName field name
     * @param dictionary dictionary
     * @return True if the field requires navigation.
     */
    protected boolean needNavigation(Type<?> entityClass, String fieldName, EntityDictionary dictionary) {
        return dictionary.isRelation(entityClass, fieldName) || dictionary.isComplexAttribute(entityClass, fieldName);
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
        return CollectionUtils.isEmpty(pathElements) ? "EMPTY"
                : pathElements.stream()
                        .map(e -> '[' + getSimpleName(e.getType()) + ']' + PERIOD + e.getFieldName())
                .collect(Collectors.joining("/"));
    }
}
