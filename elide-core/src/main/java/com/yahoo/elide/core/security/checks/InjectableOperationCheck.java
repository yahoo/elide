/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.security.checks;

import com.yahoo.elide.core.security.RequestScope;

import java.util.Optional;
import javax.inject.Inject;

/**
 * Operation check that allows the framework to override the implementation.
 */
public class InjectableOperationCheck extends OperationCheck {

    @Inject
    OperationCheck actualCheck;

    @Override
    public boolean ok(Object object, RequestScope requestScope, Optional optional) {
        if (actualCheck != null) {
            return actualCheck.ok(object, requestScope, optional);
        }

        return true;
    }
}
