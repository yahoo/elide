/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.security.ChangeSpec;
import com.yahoo.elide.security.RequestScope;
import com.yahoo.elide.security.checks.OperationCheck;

import java.util.Optional;

/**
 * Contrived check to disallow updates on Child 4 through parent 10
 */
public class Child4Parent10Check extends OperationCheck<Child> {
    @Override
    public boolean ok(Child object, RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
        return object.getId() != 4L || object.getParents().stream().map(Parent::getId).noneMatch(id -> id == 10);
    }
}
