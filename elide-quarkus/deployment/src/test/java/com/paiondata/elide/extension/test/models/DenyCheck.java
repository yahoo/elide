/*
 * Copyright 2024, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.extension.test.models;

import com.paiondata.elide.annotation.SecurityCheck;
import com.paiondata.elide.core.security.ChangeSpec;
import com.paiondata.elide.core.security.RequestScope;
import com.paiondata.elide.core.security.checks.OperationCheck;
import java.util.Optional;

/**
 * A security check that denies access unconditionally.
 *
 */
@SecurityCheck("Deny")
public class DenyCheck extends OperationCheck<Supplier> {

    @Override
    public boolean ok(Supplier object, RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
        return false;
    }
}
