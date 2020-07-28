/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.models.security;

import com.yahoo.elide.annotation.SecurityCheck;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.QueryStatus;
import com.yahoo.elide.security.ChangeSpec;
import com.yahoo.elide.security.RequestScope;
import com.yahoo.elide.security.User;
import com.yahoo.elide.security.checks.OperationCheck;
import com.yahoo.elide.security.checks.UserCheck;

import java.util.Optional;

/**
 * Operation Checks on the Async Query and Result objects.
 */
public class AsyncQueryInlineChecks {

    @SecurityCheck(AsyncQueryAdmin.PRINCIPAL_IS_ADMIN)
    public static class AsyncQueryAdmin extends UserCheck {

        public static final String PRINCIPAL_IS_ADMIN = "Principal is Admin";

        @Override
        public boolean ok(User user) {
            if (user != null && user.getPrincipal() != null) {
                return user.isInRole("admin");
            }
            return false;
        }
    }

    @SecurityCheck(AsyncQueryStatusValue.VALUE_IS_CANCELLED)
    public static class AsyncQueryStatusValue extends OperationCheck<AsyncQuery> {

        public static final String VALUE_IS_CANCELLED = "value is Cancelled";

        @Override
        public boolean ok(AsyncQuery object, RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
            return changeSpec.get().getModified().toString().equals(QueryStatus.CANCELLED.name());
        }
    }

    @SecurityCheck(AsyncQueryStatusQueuedValue.VALUE_IS_QUEUED)
    public static class AsyncQueryStatusQueuedValue extends OperationCheck<AsyncQuery> {
        public static final String VALUE_IS_QUEUED = "value is Queued";
        @Override
        public boolean ok(AsyncQuery object, RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
            return changeSpec.get().getModified().toString().equals(QueryStatus.QUEUED.name());
        }
    }
}
