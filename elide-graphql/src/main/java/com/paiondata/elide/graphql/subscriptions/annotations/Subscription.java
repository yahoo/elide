/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.graphql.subscriptions.annotations;

import static com.paiondata.elide.graphql.subscriptions.annotations.Subscription.Operation.CREATE;
import static com.paiondata.elide.graphql.subscriptions.annotations.Subscription.Operation.DELETE;
import static com.paiondata.elide.graphql.subscriptions.annotations.Subscription.Operation.UPDATE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marks an Elide model as a root level subscription topic.
 */
@Target({TYPE})
@Retention(RUNTIME)
public @interface Subscription {
    enum Operation {
        CREATE,
        UPDATE,
        DELETE
    };

    /**
     * Notify subscribers whenever a model is manipulated by the given operations.
     * @return operation topics to post on.
     */
    Operation[] operations() default { CREATE, UPDATE, DELETE };
}
