/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.security.executors;

import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.security.ChangeSpec;
import com.yahoo.elide.security.PermissionExecutor;
import com.yahoo.elide.security.PersistentResource;

import java.lang.annotation.Annotation;
import java.util.Optional;

/**
 * Permission executor intended to bypass all security checks. I.e. this is effectively a no-op.
 */
public class BypassPermissionExecutor implements PermissionExecutor {
    @Override
    public <A extends Annotation> void checkPermission(Class<A> annotationClass,
                                                       PersistentResource resource) {

    }

    @Override
    public <A extends Annotation> void checkPermission(Class<A> annotationClass,
                                                       PersistentResource resource,
                                                       ChangeSpec changeSpec) {

    }

    @Override
    public <A extends Annotation> void checkSpecificFieldPermissions(PersistentResource<?> resource,
                                                                     ChangeSpec changeSpec,
                                                                     Class<A> annotationClass, String field) {

    }

    @Override
    public <A extends Annotation> void checkSpecificFieldPermissionsDeferred(PersistentResource<?> resource,
                                                                             ChangeSpec changeSpec,
                                                                             Class<A> annotationClass, String field) {

    }

    @Override
    public <A extends Annotation> void checkUserPermissions(PersistentResource<?> resource, Class<A> annotationClass,
                                                            String field) {

    }

    @Override
    public <A extends Annotation> void checkUserPermissions(Class<?> resourceClass,
                                                            Class<A> annotationClass) {

    }

    @Override
    public Optional<FilterExpression> getReadPermissionFilter(Class<?> resourceClass) {
        return Optional.empty();
    }

    @Override
    public void executeCommitChecks() {

    }
}
