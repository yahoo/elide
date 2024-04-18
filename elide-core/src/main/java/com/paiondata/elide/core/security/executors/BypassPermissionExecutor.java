/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.security.executors;

import com.paiondata.elide.core.PersistentResource;
import com.paiondata.elide.core.filter.expression.FilterExpression;
import com.paiondata.elide.core.security.ChangeSpec;
import com.paiondata.elide.core.security.PermissionExecutor;
import com.paiondata.elide.core.security.permissions.ExpressionResult;
import com.paiondata.elide.core.type.Type;

import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.Set;

/**
 * Permission executor intended to bypass all security checks. I.e. this is effectively a no-op.
 */
public class BypassPermissionExecutor implements PermissionExecutor {
    @Override
    public <A extends Annotation> ExpressionResult checkPermission(Class<A> annotationClass,
                                                                   PersistentResource resource,
                                                                   Set<String> requestedFields) {
        return ExpressionResult.PASS;
    }

    @Override
    public <A extends Annotation> ExpressionResult checkSpecificFieldPermissions(
            PersistentResource<?> resource, ChangeSpec changeSpec, Class<A> annotationClass, String field) {
        return ExpressionResult.PASS;
    }

    @Override
    public <A extends Annotation> ExpressionResult checkSpecificFieldPermissionsDeferred(
            PersistentResource<?> resource, ChangeSpec changeSpec, Class<A> annotationClass, String field) {
        return ExpressionResult.PASS;
    }

    @Override
    public <A extends Annotation> ExpressionResult checkUserPermissions(Type<?> resourceClass,
                                                                        Class<A> annotationClass,
                                                                        Set<String> requestedFields) {
        return ExpressionResult.PASS;
    }

    @Override
    public Optional<FilterExpression> getReadPermissionFilter(Type<?> resourceClass, Set<String> requestedFields) {
        return Optional.empty();
    }

    @Override
    public <A extends Annotation> ExpressionResult checkUserPermissions(Type<?> resourceClass,
                                                                        Class<A> annotationClass,
                                                                        String field) {
        return ExpressionResult.PASS;
    }

    @Override
    public void executeCommitChecks() {

    }
}
