/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.blog.security;

import com.yahoo.elide.security.ChangeSpec;
import com.yahoo.elide.security.RequestScope;
import com.yahoo.elide.security.checks.OperationCheck;
import example.blog.Post;

import java.util.Optional;


public class IsPublished extends OperationCheck<Post> {
    @Override
    public boolean ok(Post post, RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
        return post.isPublished();
    }
}
