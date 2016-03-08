/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.blog.security;

import com.yahoo.elide.security.ChangeSpec;
import com.yahoo.elide.security.RequestScope;
import com.yahoo.elide.security.checks.OperationCheck;
import example.blog.Blogger;

import java.util.Optional;

public class IsActiveUser extends OperationCheck<Blogger> {
    @Override
    public boolean ok(Blogger blogger, RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
        return blogger.isActive();
    }
}
