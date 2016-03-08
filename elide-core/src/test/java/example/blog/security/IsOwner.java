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


public class IsOwner extends OperationCheck<UserAssociatedRecord> {
    @Override
    public boolean ok(UserAssociatedRecord record, RequestScope scope, Optional<ChangeSpec> changeSpec) {
        Blogger currentBlogger = (Blogger) scope.getUser().getOpaqueUser();
        return record.getUser().equals(currentBlogger);
    }
}
