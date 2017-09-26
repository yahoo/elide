/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.security.RequestScope;
import com.yahoo.elide.security.ChangeSpec;
import com.yahoo.elide.security.checks.OperationCheck;

import java.util.Optional;

/**
 * Useful for testing collection filter permissions.
 */
public class NegativeChildIdCheck extends OperationCheck<Child> {
    @Override
    public boolean ok(Child child, RequestScope requestScope, Optional<ChangeSpec> fChangeSpec) {
        return child.getId() >= 0;
    }

    @Override
    public String checkIdentifier() {
        return "negativeChildId";
    }
}
