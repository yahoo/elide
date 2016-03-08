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
import example.blog.Post;

import java.util.Optional;

public class IsRevisionOwner extends OperationCheck<VersionedRecord<Post>> {
    @Override
    public boolean ok(VersionedRecord<Post> post, RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
        Blogger user = (Blogger) requestScope.getUser().getOpaqueUser();
        boolean isRevisionOwner = false;

        if (post.getNextRevision() != null) {
            isRevisionOwner = post.getNextRevision().getAuthor().equals(user);
        }

        if (post.getPreviousRevision() != null) {
            isRevisionOwner = isRevisionOwner || post.getPreviousRevision().getAuthor().equals(user);
        }

        return isRevisionOwner;
    }
}
