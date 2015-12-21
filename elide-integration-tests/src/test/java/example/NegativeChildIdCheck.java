/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.security.ChangeSpec;
import com.yahoo.elide.security.Check;

import java.util.Optional;

/**
 * Useful for testing collection filter permissions.
 */
public class NegativeChildIdCheck implements Check<Child> {
    @Override
    public boolean ok(Child child, RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
        return child.getId() >= 0;
    }

    @Override
    public boolean ok(RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
        return true;
    }
}
