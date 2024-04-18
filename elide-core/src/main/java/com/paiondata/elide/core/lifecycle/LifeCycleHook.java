/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.lifecycle;

import com.paiondata.elide.annotation.LifeCycleHookBinding;
import com.paiondata.elide.core.security.ChangeSpec;
import com.paiondata.elide.core.security.RequestScope;

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
    void execute(LifeCycleHookBinding.Operation operation,
                 LifeCycleHookBinding.TransactionPhase phase,
                 T elideEntity,
                 RequestScope requestScope,
                 Optional<ChangeSpec> changes);

    /**
     * Base method of life cycle hook invoked by Elide.  Includes access to the underlying
     * CRUDEvent and the PersistentResource.
     * @param operation CREATE, READ, UPDATE, or DELETE
     * @param phase PRESECURITY, PRECOMMIT or POSTCOMMIT
     * @param event The CRUD Event that triggered this hook.
     */
    default void execute(
            LifeCycleHookBinding.Operation operation,
            LifeCycleHookBinding.TransactionPhase phase,
            CRUDEvent event) {
        this.execute(
                operation,
                phase,
                (T) event.getResource().getObject(),
                event.getResource().getRequestScope(),
                event.getChanges());
    }
}
