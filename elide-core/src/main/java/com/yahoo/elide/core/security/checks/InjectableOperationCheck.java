/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.security.checks;

import com.yahoo.elide.core.security.RequestScope;

import java.util.Optional;

/**
 * Operation check that allows the framework to override the implementation.
 */
public abstract class InjectableOperationCheck extends OperationCheck {

    @Override
    public boolean ok(Object object, RequestScope requestScope, Optional optional) {

        if (getActualCheck().isPresent()) {
            return getActualCheck().get().ok(object, requestScope, optional);
        }

        return true;
    }

    public abstract Optional<OperationCheck> getActualCheck();
}
