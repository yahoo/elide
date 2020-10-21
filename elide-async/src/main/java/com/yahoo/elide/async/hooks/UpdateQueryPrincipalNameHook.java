/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.hooks;

import com.yahoo.elide.annotation.LifeCycleHookBinding;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.functions.LifeCycleHook;
import com.yahoo.elide.security.ChangeSpec;
import com.yahoo.elide.security.RequestScope;

import java.util.Optional;

public class UpdateQueryPrincipalNameHook extends AsyncHook implements LifeCycleHook<AsyncQuery> {

    @Override
    public void execute(LifeCycleHookBinding.Operation operation, LifeCycleHookBinding.TransactionPhase phase,
                        AsyncQuery query, RequestScope requestScope, Optional<ChangeSpec> changes) {
        updatePrincipalName(query, requestScope);
    }
}
