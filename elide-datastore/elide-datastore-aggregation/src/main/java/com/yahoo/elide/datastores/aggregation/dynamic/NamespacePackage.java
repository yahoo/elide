/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.dynamic;

import com.yahoo.elide.annotation.ApiVersion;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.type.Package;
import com.yahoo.elide.datastores.aggregation.annotation.NamespaceMeta;
import com.yahoo.elide.modelconfig.model.NamespaceConfig;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

/**
 * A dynamic Elide model that wraps a deserialized HJSON Namespace.
 */
public class NamespacePackage implements Package {

    public static String DEFAULT = "default";
    public static NamespacePackage DEFAULT_NAMESPACE =
            new NamespacePackage(DEFAULT, "Default Namespace", DEFAULT);

    protected NamespaceConfig namespace;
    private Map<Class<? extends Annotation>, Annotation> annotations;

    public NamespacePackage(NamespaceConfig namespace) {
        this.namespace = namespace;
        this.annotations = buildAnnotations(namespace);
    }

    public NamespacePackage(String name, String description, String friendlyName) {
        this(NamespaceConfig.builder()
                .name(name)
                .friendlyName(friendlyName)
                .description(description)
                .apiVersion(EntityDictionary.NO_VERSION)
                .build());
    }

    @Override
    public <A extends Annotation> A getDeclaredAnnotation(Class<A> annotationClass) {
        if (annotations.containsKey(annotationClass)) {
            return (A) annotations.get(annotationClass);
        }
        return null;
    }

    @Override
    public String getName() {
        return namespace.getName();
    }

    @Override
    public Package getParentPackage() {
        return null;
    }

    private static Map<Class<? extends Annotation>, Annotation> buildAnnotations(NamespaceConfig namespace) {
        Map<Class<? extends Annotation>, Annotation> annotations = new HashMap<>();

        annotations.put(ReadPermission.class, new ReadPermission() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return ReadPermission.class;
            }

            @Override
            public String expression() {
                return namespace.getReadAccess();
            }

        });

        annotations.put(NamespaceMeta.class, new NamespaceMeta() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return NamespaceMeta.class;
            }

            @Override
            public String friendlyName() {
                return namespace.getFriendlyName();
            }

            @Override
            public String description() {
                return namespace.getDescription();
            }
        });

        annotations.put(ApiVersion.class, new ApiVersion() {
            @Override
            public String version() {
                return namespace.getApiVersion();
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return ApiVersion.class;
            }
        });

        return annotations;
    }
}
