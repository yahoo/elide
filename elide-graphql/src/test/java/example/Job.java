/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package example;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.LifeCycleHookBinding;

import hooks.JobLifeCycleHook;
import jakarta.persistence.Id;
import lombok.Data;

/**
 * Tests lifecycle hooks in GraphQL that update model state.
 */
@Include
@Data
@LifeCycleHookBinding(
        hook = JobLifeCycleHook.class,
        operation = LifeCycleHookBinding.Operation.CREATE,
        phase = LifeCycleHookBinding.TransactionPhase.PREFLUSH
)
@LifeCycleHookBinding(
        hook = JobLifeCycleHook.class,
        operation = LifeCycleHookBinding.Operation.CREATE,
        phase = LifeCycleHookBinding.TransactionPhase.PRESECURITY
)
@LifeCycleHookBinding(
        hook = JobLifeCycleHook.class,
        operation = LifeCycleHookBinding.Operation.UPDATE,
        phase = LifeCycleHookBinding.TransactionPhase.PREFLUSH
)
@LifeCycleHookBinding(
        hook = JobLifeCycleHook.class,
        operation = LifeCycleHookBinding.Operation.DELETE,
        phase = LifeCycleHookBinding.TransactionPhase.PREFLUSH
)
public class Job {
    @Id
    private long id;

    private int status = 0;

    private String result;
}
