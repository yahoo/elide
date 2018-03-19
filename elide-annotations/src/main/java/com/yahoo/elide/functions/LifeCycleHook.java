/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.functions;

import com.yahoo.elide.security.ChangeSpec;
import com.yahoo.elide.security.RequestScope;

import java.util.Optional;

/**
 * Function which will be invoked for Elide lifecycle triggers
 * @param <T> The elide entity type associated with this callback.
 */
@FunctionalInterface
public interface LifeCycleHook<T> {
    /**
     * Run for a lifecycle event
     * @param elideEntity The entity that triggered the event
     * @param requestScope The request scope
     * @param changes Optionally, the changes that were made to the entity
     */
    public abstract void execute(T elideEntity,
                                 RequestScope requestScope,
                                 Optional<ChangeSpec> changes);
}
