/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.models.security;

import com.yahoo.elide.annotation.SecurityCheck;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.PrincipalOwned;
import com.yahoo.elide.async.models.QueryStatus;
import com.yahoo.elide.security.ChangeSpec;
import com.yahoo.elide.security.RequestScope;
import com.yahoo.elide.security.checks.OperationCheck;

import java.security.Principal;
import java.util.Optional;

/**
 * Operation Checks on the Async Query and Result objects.
 */
public class AsyncQueryOperationChecks {
    @SecurityCheck(AsyncQueryOwner.PRINCIPAL_IS_OWNER)
    public static class AsyncQueryOwner extends OperationCheck<Object> {

        public static final String PRINCIPAL_IS_OWNER = "Principal is Owner";

        @Override
        public boolean ok(Object object, RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
            Principal principal = requestScope.getUser().getPrincipal();
            boolean status = false;
            String principalName = ((PrincipalOwned) object).getPrincipalName();
            if (principalName == null && (principal == null || principal.getName() == null)) {
                status = true;
            } else if (principalName != null && principal != null && principal.getName() != null) {
                status = principalName.equals(principal.getName());
            }
            return status;
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
