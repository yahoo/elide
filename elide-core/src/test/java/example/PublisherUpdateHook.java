/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package example;

import com.yahoo.elide.annotation.LifeCycleHookBinding;
import com.yahoo.elide.functions.LifeCycleHook;
import com.yahoo.elide.security.ChangeSpec;
import com.yahoo.elide.security.RequestScope;

import java.util.Optional;

/**
 * Used to tests life cycle hook invocations.
 */
public class PublisherUpdateHook implements LifeCycleHook<Publisher> {

    @Override
    public void execute(LifeCycleHookBinding.Operation operation, Publisher elideEntity,
                        RequestScope requestScope, Optional<ChangeSpec> changes) {
        elideEntity.setUpdateHookInvoked(true);
    }
}
