/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.lifecycle;

import com.yahoo.elide.annotation.LifeCycleHookBinding;
import com.yahoo.elide.core.security.ChangeSpec;
import com.yahoo.elide.core.security.RequestScope;

import java.util.Optional;

/**
 * Function which will be invoked for Elide lifecycle triggers.
 * @param <T> The elide entity type associated with this callback.
 */
@FunctionalInterface
public interface LifeCycleHook<T> {
    /**
     * Run for a lifecycle event.
     * @param operation CREATE, READ, UPDATE, or DELETE
     * @param phase PRESECURITY, PRECOMMIT or POSTCOMMIT
     * @param elideEntity The entity that triggered the event
     * @param requestScope The request scope
     * @param changes Optionally, the changes that were made to the entity
     */
    public abstract void execute(LifeCycleHookBinding.Operation operation,
                                 LifeCycleHookBinding.TransactionPhase phase,
                                 T elideEntity,
                                 RequestScope requestScope,
                                 Optional<ChangeSpec> changes);
}
