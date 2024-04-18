/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.annotation;

import com.paiondata.elide.core.lifecycle.LifeCycleHook;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Executes arbitrary logic (a lifecycle hook) when an Elide model is read or written.
 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(LifeCycleHookBindings.class)
public @interface LifeCycleHookBinding {

    Operation [] ALL_OPERATIONS = {
            Operation.CREATE,
            Operation.UPDATE,
            Operation.DELETE
    };

    enum Operation {
        CREATE,
        UPDATE,
        DELETE
    };

    enum TransactionPhase {
        PRESECURITY,
        PREFLUSH,
        PRECOMMIT,
        POSTCOMMIT
    }

    /**
     * The function to invoke when this life cycle triggers.
     * @return the function class.
     */
    Class<? extends LifeCycleHook> hook();

    /**
     * Which CRUD operation to trigger on.
     * @return CREATE, READ, UPDATE, or DELETE
     */
    Operation operation();

    /**
     * Which transaction phase to trigger on.
     * @return PRESECURITY, PRECOMMIT, or POSTCOMMIT
     */
    TransactionPhase phase() default TransactionPhase.PRECOMMIT;

    /**
     * Controls how often the hook is invoked:
     * A hook is invoked once per class per request (when bound to the model).
     * A hook is invoked once per field per request (when bound to a model field or method).
     * A hook is invoked one or more times per class per request (when bound to a model and oncePerRequest is false).
     * @return true or false.
     */
    boolean oncePerRequest() default true;
}
