/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.lifecycle;

import com.yahoo.elide.annotation.LifeCycleHookBinding;
import com.yahoo.elide.annotation.OnCreatePostCommit;
import com.yahoo.elide.annotation.OnCreatePreCommit;
import com.yahoo.elide.annotation.OnCreatePreSecurity;
import com.yahoo.elide.annotation.OnDeletePostCommit;
import com.yahoo.elide.annotation.OnDeletePreCommit;
import com.yahoo.elide.annotation.OnDeletePreSecurity;
import com.yahoo.elide.annotation.OnUpdatePostCommit;
import com.yahoo.elide.annotation.OnUpdatePreCommit;
import com.yahoo.elide.annotation.OnUpdatePreSecurity;
import com.yahoo.elide.core.security.ChangeSpec;
import com.yahoo.elide.core.security.RequestScope;

import com.google.common.base.Throwables;

import java.lang.annotation.Annotation;
import java.util.Optional;

/**
 * Hook to aid in porting from Elide 4 life cycle hooks to Elide 5.
 * This hook will not work for Elide 4 read hooks or "*" hooks.
 */
public class Elide4MigrationHook implements LifeCycleHook<Object> {

    @Override
    public void execute(LifeCycleHookBinding.Operation operation, LifeCycleHookBinding.TransactionPhase phase,
                        Object elideEntity,
                        RequestScope requestScope,
                        Optional<ChangeSpec> changes) {

        String optionalFieldName = changes.map(ChangeSpec::getFieldName).orElse(null);
        java.lang.reflect.Method method = null;

        if (operation == LifeCycleHookBinding.Operation.CREATE) {
            if (phase == LifeCycleHookBinding.TransactionPhase.POSTCOMMIT) {
                method = findAnnotation(elideEntity, OnCreatePostCommit.class, optionalFieldName);
            } else if (phase == LifeCycleHookBinding.TransactionPhase.PRECOMMIT) {
                method = findAnnotation(elideEntity, OnCreatePreCommit.class, optionalFieldName);
            } else {
                method = findAnnotation(elideEntity, OnCreatePreSecurity.class, optionalFieldName);
            }
        } else if (operation == LifeCycleHookBinding.Operation.DELETE) {
            if (phase == LifeCycleHookBinding.TransactionPhase.POSTCOMMIT) {
                method = findAnnotation(elideEntity, OnDeletePostCommit.class, null);
            } else if (phase == LifeCycleHookBinding.TransactionPhase.PRECOMMIT) {
                method = findAnnotation(elideEntity, OnDeletePreCommit.class, null);
            } else {
                method = findAnnotation(elideEntity, OnDeletePreSecurity.class, null);
            }
        } else if (operation == LifeCycleHookBinding.Operation.UPDATE) {
            if (phase == LifeCycleHookBinding.TransactionPhase.POSTCOMMIT) {
                method = findAnnotation(elideEntity, OnUpdatePostCommit.class, optionalFieldName);
            } else if (phase == LifeCycleHookBinding.TransactionPhase.PRECOMMIT) {
                method = findAnnotation(elideEntity, OnUpdatePreCommit.class, optionalFieldName);
            } else {
                method = findAnnotation(elideEntity, OnUpdatePreSecurity.class, optionalFieldName);
            }
        }

        if (method == null) {
            throw new IllegalStateException(String.format("No legacy hook found for: %s %s %s",
                    operation, phase, elideEntity.getClass().getSimpleName()));
        }

        invokeHook(elideEntity, method, changes, requestScope);
    }

    private <T extends Annotation> java.lang.reflect.Method findAnnotation(Object model,
                                                                           Class<T> annotationClass,
                                                                           String field) {
        Class cls = model.getClass();

        for (java.lang.reflect.Method method: cls.getMethods()) {
            Annotation annotation = method.getDeclaredAnnotation(annotationClass);
            String annotationField = null;
            if (annotation instanceof OnCreatePostCommit) {
                annotationField = ((OnCreatePostCommit) annotation).value();
            } else if (annotation instanceof OnCreatePreCommit) {
                annotationField = ((OnCreatePreCommit) annotation).value();
            } else if (annotation instanceof OnCreatePreSecurity) {
                annotationField = ((OnCreatePreSecurity) annotation).value();
            } else if (annotation instanceof OnUpdatePostCommit) {
                annotationField = ((OnUpdatePostCommit) annotation).value();
            } else if (annotation instanceof OnUpdatePreCommit) {
                annotationField = ((OnUpdatePreCommit) annotation).value();
            } else if (annotation instanceof OnUpdatePreSecurity) {
                annotationField = ((OnUpdatePreSecurity) annotation).value();
            }

            if (field == annotationField
                    || field.equals(annotationField)) {
                return method;
            }
        }
        return null;
    }

    private void invokeHook(Object model, java.lang.reflect.Method method,
                            Optional<ChangeSpec> changes, RequestScope scope) {
        try {
            int paramCount = method.getParameterCount();
            Class<?>[] paramTypes = method.getParameterTypes();

            if (changes.isPresent() && paramCount == 2
                    && paramTypes[0].isInstance(scope)
                    && paramTypes[1].isInstance(changes.get())) {
                method.invoke(model, scope, changes.get());
            } else if (paramCount == 1 && paramTypes[0].isInstance(scope)) {
                method.invoke(model, scope);
            } else if (paramCount == 0) {
                method.invoke(model);
            } else {
                throw new IllegalArgumentException();
            }
        } catch (ReflectiveOperationException e) {
            Throwables.propagateIfPossible(e.getCause());
            throw new IllegalArgumentException(e);
        }
    }
}
