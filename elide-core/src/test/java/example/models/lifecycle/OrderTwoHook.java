/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.models.lifecycle;

import com.paiondata.elide.annotation.LifeCycleHookBinding;
import com.paiondata.elide.core.lifecycle.LifeCycleHook;
import com.paiondata.elide.core.security.ChangeSpec;
import com.paiondata.elide.core.security.RequestScope;

import java.util.Optional;

public class OrderTwoHook implements LifeCycleHook<HookOrder> {
    @Override
    public void execute(LifeCycleHookBinding.Operation operation, LifeCycleHookBinding.TransactionPhase phase, HookOrder elideEntity, RequestScope requestScope, Optional<ChangeSpec> changes) {

    }

    @Override
    public int hashCode() {
        return 2;
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }
}
