/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package example;

import com.paiondata.elide.annotation.LifeCycleHookBinding;
import com.paiondata.elide.core.lifecycle.LifeCycleHook;
import com.paiondata.elide.core.security.ChangeSpec;
import com.paiondata.elide.core.security.RequestScope;

import java.util.Optional;

/**
 * Used to tests life cycle hook invocations.
 */
public class PublisherUpdateHook implements LifeCycleHook<Publisher> {

    @Override
    public void execute(LifeCycleHookBinding.Operation operation, LifeCycleHookBinding.TransactionPhase phase,
                        Publisher elideEntity, RequestScope requestScope, Optional<ChangeSpec> changes) {
        elideEntity.setUpdateHookInvoked(true);
    }
}
