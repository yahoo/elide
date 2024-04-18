/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.paiondata.elide.core.security.ChangeSpec;
import com.paiondata.elide.core.security.RequestScope;
import com.paiondata.elide.core.security.checks.OperationCheck;

import java.util.Optional;

/**
 * Useful for testing collection filter permissions.
 */
public class NegativeChildIdCheck extends OperationCheck<Child> {
    @Override
    public boolean ok(Child child, RequestScope requestScope, Optional<ChangeSpec> fChangeSpec) {
        return child.getId() >= 0;
    }
}
