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
import com.yahoo.elide.annotation.OnReadPostCommit;
import com.yahoo.elide.annotation.OnReadPreCommit;
import com.yahoo.elide.annotation.OnReadPreSecurity;
import com.yahoo.elide.annotation.OnUpdatePostCommit;
import com.yahoo.elide.annotation.OnUpdatePreCommit;
import com.yahoo.elide.annotation.OnUpdatePreSecurity;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.security.RequestScope;
import com.yahoo.elide.core.type.Method;

import com.google.common.base.Throwables;

import java.util.Optional;

/**
 * Migrates Elide 4 hooks to Elide 5.
 */
public class Elide4MigrationHelper {

    public static void bindLegacyHooks(EntityDictionary dictionary) {
        dictionary.getBindings().forEach(binding -> {
            binding.getAllMethods().stream()
                    .forEach(field -> {
                        String annotationField = null;
                        LifeCycleHookBinding.TransactionPhase phase = null;
                        LifeCycleHookBinding.Operation operation = null;
                        if (field.isAnnotationPresent(OnCreatePostCommit.class)) {
                            annotationField = field.getAnnotation(OnCreatePostCommit.class).value();
                            phase = LifeCycleHookBinding.TransactionPhase.POSTCOMMIT;
                            operation = LifeCycleHookBinding.Operation.CREATE;
                        } else if (field.isAnnotationPresent(OnCreatePreCommit.class)) {
                            annotationField = field.getAnnotation(OnCreatePreCommit.class).value();
                            phase = LifeCycleHookBinding.TransactionPhase.PRECOMMIT;
                            operation = LifeCycleHookBinding.Operation.CREATE;
                        } else if (field.isAnnotationPresent(OnCreatePreSecurity.class)) {
                            annotationField = field.getAnnotation(OnCreatePreSecurity.class).value();
                            phase = LifeCycleHookBinding.TransactionPhase.PRESECURITY;
                            operation = LifeCycleHookBinding.Operation.CREATE;
                        } else if (field.isAnnotationPresent(OnUpdatePostCommit.class)) {
                            annotationField = field.getAnnotation(OnUpdatePostCommit.class).value();
                            phase = LifeCycleHookBinding.TransactionPhase.POSTCOMMIT;
                            operation = LifeCycleHookBinding.Operation.UPDATE;
                        } else if (field.isAnnotationPresent(OnUpdatePreCommit.class)) {
                            annotationField = field.getAnnotation(OnUpdatePreCommit.class).value();
                            phase = LifeCycleHookBinding.TransactionPhase.PRECOMMIT;
                            operation = LifeCycleHookBinding.Operation.UPDATE;
                        } else if (field.isAnnotationPresent(OnUpdatePreSecurity.class)) {
                            annotationField = field.getAnnotation(OnUpdatePreSecurity.class).value();
                            phase = LifeCycleHookBinding.TransactionPhase.PRESECURITY;
                            operation = LifeCycleHookBinding.Operation.UPDATE;
                        } else if (field.isAnnotationPresent(OnReadPostCommit.class)) {
                            annotationField = field.getAnnotation(OnReadPostCommit.class).value();
                            phase = LifeCycleHookBinding.TransactionPhase.POSTCOMMIT;
                            operation = LifeCycleHookBinding.Operation.READ;
                        } else if (field.isAnnotationPresent(OnReadPreCommit.class)) {
                            annotationField = field.getAnnotation(OnReadPreCommit.class).value();
                            phase = LifeCycleHookBinding.TransactionPhase.PRECOMMIT;
                            operation = LifeCycleHookBinding.Operation.READ;
                        } else if (field.isAnnotationPresent(OnReadPreSecurity.class)) {
                            annotationField = field.getAnnotation(OnReadPreSecurity.class).value();
                            phase = LifeCycleHookBinding.TransactionPhase.PRESECURITY;
                            operation = LifeCycleHookBinding.Operation.READ;
                        } else if (field.isAnnotationPresent(OnDeletePostCommit.class)) {
                            phase = LifeCycleHookBinding.TransactionPhase.POSTCOMMIT;
                            operation = LifeCycleHookBinding.Operation.DELETE;
                        } else if (field.isAnnotationPresent(OnDeletePreCommit.class)) {
                            phase = LifeCycleHookBinding.TransactionPhase.PRECOMMIT;
                            operation = LifeCycleHookBinding.Operation.DELETE;
                        } else if (field.isAnnotationPresent(OnDeletePreSecurity.class)) {
                            phase = LifeCycleHookBinding.TransactionPhase.PRESECURITY;
                            operation = LifeCycleHookBinding.Operation.DELETE;
                        }

                        if (operation != null) {
                            if (annotationField != null) {
                                if (annotationField.equals("*")) {
                                    dictionary.bindTrigger(binding.entityClass, operation, phase,
                                            generateHook((Method) field), true);
                                } else {
                                    dictionary.bindTrigger(binding.entityClass, annotationField, operation, phase,
                                            generateHook((Method) field));
                                }
                            } else {
                                dictionary.bindTrigger(binding.entityClass, operation, phase,
                                        generateHook((Method) field), false);
                            }
                        }

                    });
        });
    }

    private static LifeCycleHook generateHook(Method method) {
        return new LifeCycleHook() {
            @Override
            public void execute(LifeCycleHookBinding.Operation operation,
                                LifeCycleHookBinding.TransactionPhase phase,
                                Object model,
                                RequestScope scope,
                                Optional changes) {
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
        };
    }
}
