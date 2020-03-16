/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package graphqlEndpointTestModels.security;

import com.yahoo.elide.security.RequestScope;
import com.yahoo.elide.security.checks.OperationCheck;

import java.security.Principal;
import java.util.Optional;

public abstract class CommitChecks {
    public static final String IS_NOT_USER_3 = "isnt user three";

    public static class IsNotUser3 extends OperationCheck {

        @Override
        public boolean ok(Object object, RequestScope requestScope, Optional optional) {
            Principal principal = requestScope.getUser().getPrincipal();
            return !"3".equals(principal.getName());
        }
    }
}
