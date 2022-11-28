/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.dynamic;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;

import com.yahoo.elide.annotation.ApiVersion;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.core.type.Package;
import com.yahoo.elide.modelconfig.model.NamespaceConfig;

import lombok.EqualsAndHashCode;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

/**
 * A dynamic Elide model that wraps a deserialized HJSON Namespace.
 */
@EqualsAndHashCode
public class NamespacePackage implements Package {
    private static final long serialVersionUID = -7173317858416763972L;

    public static final String EMPTY = "";
    public static final String DEFAULT = "default";
    public static NamespacePackage DEFAULT_NAMESPACE =
            new NamespacePackage(EMPTY, "Default Namespace", DEFAULT, NO_VERSION);

    protected NamespaceConfig namespace;
    private transient Map<Class<? extends Annotation>, Annotation> annotations;

    public NamespacePackage(NamespaceConfig namespace) {
        if (namespace.getName().equals(DEFAULT)) {
            this.namespace = NamespaceConfig.builder()
                    .name(EMPTY)
                    .friendlyName(namespace.getFriendlyName())
                    .description(namespace.getDescription())
                    .apiVersion(namespace.getApiVersion())
                    .build() ;
        } else {
            this.namespace = namespace;
        }
        this.annotations = buildAnnotations(this.namespace);
    }

    public NamespacePackage(String name, String description, String friendlyName, String version) {
        this(NamespaceConfig.builder()
                .name(name)
                .friendlyName(friendlyName)
                .description(description)
                .apiVersion(version)
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

        annotations.put(Include.class, new Include() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return Include.class;
            }

            @Override
            public boolean rootLevel() {
                return true;
            }

            @Override
            public String description() {
                return namespace.getDescription();
            }

            @Override
            public String friendlyName() {
                return namespace.getFriendlyName();
            }

            @Override
            public String name() {
                return namespace.getName();
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
