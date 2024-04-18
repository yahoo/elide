/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.models.lifecycle;

import com.paiondata.elide.annotation.Include;
import com.paiondata.elide.annotation.LifeCycleHookBinding;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Include
@Data
@LifeCycleHookBinding(
        hook = OrderOneHook.class,
        operation = LifeCycleHookBinding.Operation.CREATE,
        phase = LifeCycleHookBinding.TransactionPhase.PRESECURITY
)
@LifeCycleHookBinding(
        hook = OrderTwoHook.class,
        operation = LifeCycleHookBinding.Operation.CREATE,
        phase = LifeCycleHookBinding.TransactionPhase.PRESECURITY
)
@LifeCycleHookBinding(
        hook = OrderTwoHook.class,
        operation = LifeCycleHookBinding.Operation.CREATE,
        phase = LifeCycleHookBinding.TransactionPhase.PRECOMMIT
)
@LifeCycleHookBinding(
        hook = OrderOneHook.class,
        operation = LifeCycleHookBinding.Operation.CREATE,
        phase = LifeCycleHookBinding.TransactionPhase.PRECOMMIT
)
public class HookOrderOnClass {
    @Id
    private String id;

    private String name;

    private String description;
}
