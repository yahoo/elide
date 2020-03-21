/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package hooks;

import com.yahoo.elide.annotation.LifeCycleHookBinding;
import com.yahoo.elide.functions.LifeCycleHook;
import com.yahoo.elide.graphql.GraphQLEndpointTest;
import com.yahoo.elide.security.ChangeSpec;
import com.yahoo.elide.security.RequestScope;
import graphqlEndpointTestModels.Book;

import java.util.Optional;

/**
 * Test post-commit hook for updates to Book.title.
 */
public class BookUpdatePostCommitHook implements LifeCycleHook<Book> {
    @Override
    public void execute(LifeCycleHookBinding.Operation operation, Book elideEntity,
                        RequestScope requestScope, Optional<ChangeSpec> changes) {
        GraphQLEndpointTest.User user = (GraphQLEndpointTest.User) requestScope.getUser().getPrincipal();
        user.appendLog("On Title Update Post Commit\n");
    }
}
