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
import example.blog.Comment;
import example.blog.Post;

import java.util.Optional;

public class HasWrittenComment extends OperationCheck<Post> {
    @Override
    public boolean ok(Post post, RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
        Blogger blogger = (Blogger) requestScope.getUser().getOpaqueUser();
        for (Comment c : post.getComments()) {
            if (c.getAuthor().equals(blogger)) {
                return true;
            }
        }

        return false;
    }
}
