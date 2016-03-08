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

public class IsPostOwner extends OperationCheck<PostRelatedRecord> {
    @Override
    public boolean ok(PostRelatedRecord record, RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
        Blogger blogger = (Blogger) requestScope.getUser().getOpaqueUser();
        return record.getPost().getAuthor().equals(blogger);
    }
}
