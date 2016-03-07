/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.security.checks;

import com.yahoo.elide.audit.InvalidSyntaxException;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.PersistentResource;
import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

/**
 * Extracted checks.
 */
@Slf4j
public class ExtractedChecks {
    private final Class<? extends Check>[] anyChecks;
    private final Class<? extends Check>[] allChecks;

    /**
     * Constructor.
     *
     * @param cls Resource
     * @param dictionary Entity dictionary
     * @param annotationClass Annotation class
     * @param <A> Type parameter
     */
    public <A extends Annotation> ExtractedChecks(final Class<?> cls,
                                                  final EntityDictionary dictionary,
                                                  final Class<A> annotationClass) {
        this(cls, dictionary, annotationClass, null);
    }

    /**
     * Constructor.
     *
     * @param resource Resource
     * @param annotationClass Annotation class
     * @param field Field
     * @param <A> Type parameter
     */
    public <A extends Annotation> ExtractedChecks(final PersistentResource resource,
                                                  final Class<A> annotationClass,
                                                  final String field) {
        this(resource.getResourceClass(), resource.getDictionary(), annotationClass, field);
    }

    /**
     * Constructor.
     *
     * @param cls Entity class
     * @param dictionary Entity dictionary
     * @param annotationClass annotation class
     * @param field Field
     * @param <A> type parameter
     */
    public <A extends Annotation> ExtractedChecks(final Class<?> cls,
                                                  final EntityDictionary dictionary,
                                                  final Class<A> annotationClass,
                                                  final String field) {
        final A annotation = (field == null) ? dictionary.getAnnotation(cls, annotationClass)
                                             : dictionary.getAttributeOrRelationAnnotation(cls, annotationClass, field);
        // No checks specified
        if (annotation == null) {
            anyChecks = allChecks = null;
        } else {
            try {
                anyChecks = (Class<? extends Check>[]) annotationClass.getMethod("any")
                                                                      .invoke(annotation, (Object[]) null);
                allChecks = (Class<? extends Check>[]) annotationClass.getMethod("all")
                                                                      .invoke(annotation, (Object[]) null);
            } catch (ReflectiveOperationException e) {
                log.warn("Unknown permission: {}, {}", annotationClass.getName(), e);
                throw new InvalidSyntaxException("Unknown permission '" + annotationClass.getName() + "'", e);
            }
            if (anyChecks.length <= 0 && allChecks.length <= 0) {
                log.warn("Unknown permission: {}, {}", annotationClass.getName());
                throw new InvalidSyntaxException("Unknown permission '" + annotationClass.getName() + "'");
            }
        }
    }

    public CheckMode getCheckMode() {
        return (anyChecks != null && anyChecks.length > 0) ? CheckMode.ANY : CheckMode.ALL;
    }

    public Class<? extends Check>[] getChecks() {
        return (anyChecks != null && anyChecks.length > 0) ? anyChecks : allChecks;
    }

    public List<InlineCheck> getInlineChecks() {
        return arrayToList(getChecks(), InlineCheck.class);
    }

    private static <T extends Check> ArrayList<T> arrayToList(Class<? extends Check>[] array, Class<T> cls) {
        ArrayList<T> result = new ArrayList<>();
        if (array == null) {
            return result;
        }
        for (Class<? extends Check> checkClass : array) {
            if (cls.isAssignableFrom(checkClass)) {
                try {
                    Check instance = checkClass.newInstance();
                    result.add(cls.cast(instance));
                } catch (InstantiationException | IllegalAccessException e) {
                    log.error("Could not access check. Exception: {}", e);
                    throw new InvalidSyntaxException("Could not instantiate specified check.");
                }
            }
        }
        return result;
    }

    /**
     * Check mode.
     */
    public enum CheckMode {
        ANY,
        ALL
    }
}
