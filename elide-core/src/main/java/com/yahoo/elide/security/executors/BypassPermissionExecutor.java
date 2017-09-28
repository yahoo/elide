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
import com.yahoo.elide.security.permissions.ExpressionResult;

import java.lang.annotation.Annotation;
import java.util.Optional;

/**
 * Permission executor intended to bypass all security checks. (i.e. this is effectively a no-op)
 */
public class BypassPermissionExecutor implements PermissionExecutor {
    @Override
    public ExpressionResult checkPermission(Class<? extends Annotation> annotationClass, PersistentResource resource) {
        return ExpressionResult.PASS;
    }

    @Override
    public ExpressionResult checkSpecificFieldPermissions(PersistentResource<?> resource, ChangeSpec changeSpec,
                                                          Class<? extends Annotation> annotationClass, String field) {
        return ExpressionResult.PASS;
    }

    @Override
    public ExpressionResult checkSpecificFieldPermissionsDeferred(PersistentResource<?> resource,
                                                                  ChangeSpec changeSpec,
                                                                  Class<? extends Annotation> annotationClass,
                                                                  String field) {
        return ExpressionResult.PASS;
    }

    @Override
    public ExpressionResult checkUserPermissions(Class<?> resourceClass, Class<? extends Annotation> annotationClass) {
        return ExpressionResult.PASS;
    }

    @Override
    public Optional<FilterExpression> getReadPermissionFilter(Class<?> resourceClass) {
        return Optional.empty();
    }

    @Override
    public void executeCommitChecks() {

    }
}
