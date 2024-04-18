/*
 * Copyright 2023, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.utils;

import com.paiondata.elide.annotation.Include;
import com.paiondata.elide.annotation.SecurityCheck;
import com.paiondata.elide.core.utils.coerce.converters.ElideTypeConverter;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * Caches a set of classes with certain annotations.
 *
 * For the native code path this will be initialized at build time.
 */
public class ClassScannerCache {
    private static final Map<String, Set<Class<?>>> INSTANCE;

    private static final String [] CACHE_ANNOTATIONS  = {
        //Elide Core Annotations
        Include.class.getCanonicalName(),
        SecurityCheck.class.getCanonicalName(),
        ElideTypeConverter.class.getCanonicalName(),

        //GraphQL annotations.  Strings here to avoid dependency.
        "com.paiondata.elide.graphql.subscriptions.annotations.Subscription",

        //Aggregation Store Annotations.  Strings here to avoid dependency.
        "com.paiondata.elide.datastores.aggregation.queryengines.sql.annotation.FromTable",
        "com.paiondata.elide.datastores.aggregation.queryengines.sql.annotation.FromSubquery",
        "org.hibernate.annotations.Subselect",

        //JPA
        "jakarta.persistence.Entity",
        "jakarta.persistence.Table"
    };

    static {
        Map<String, Set<Class<?>>> result = new HashMap<>();
        try (ScanResult scanResult = new ClassGraph().enableClassInfo().enableAnnotationInfo().scan()) {
            for (String annotationName : CACHE_ANNOTATIONS) {
                result.put(annotationName, scanResult.getClassesWithAnnotation(annotationName)
                        .stream()
                        .map(ClassInfo::loadClass)
                        .collect(Collectors.toCollection(LinkedHashSet::new)));
            }
        }
        INSTANCE = result;
    }

    private ClassScannerCache() {
    }

    public static Map<String, Set<Class<?>>> getInstance() {
        return INSTANCE;
    }
}
