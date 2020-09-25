/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.models.security;

import com.yahoo.elide.annotation.SecurityCheck;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.QueryStatus;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.security.ChangeSpec;
import com.yahoo.elide.security.checks.FilterExpressionCheck;
import com.yahoo.elide.security.RequestScope;
import com.yahoo.elide.security.User;
import com.yahoo.elide.security.checks.OperationCheck;
import com.yahoo.elide.security.checks.UserCheck;

import java.security.Principal;
import java.util.Collections;
import java.util.Optional;

/**
 * Operation Checks on the Async Query and Result objects.
 */
public class AsyncQueryInlineChecks {
    private static final String PRINCIPAL_NAME = "principalName";

    static private FilterPredicate getPredicateOfPrincipalName(String principalName) {
        Path.PathElement path = new Path.PathElement(AsyncQuery.class, String.class, PRINCIPAL_NAME);
        return new FilterPredicate(path, Operator.IN, Collections.singletonList(principalName));
    }

    static private FilterPredicate getPredicateOfPrincipalNameNull() {
        Path.PathElement path = new Path.PathElement(AsyncQuery.class, String.class, PRINCIPAL_NAME);
        return new FilterPredicate(path, Operator.ISNULL, Collections.emptyList());
    }

    /**
     * Filter for principalName == Owner.
     * @param <T> type parameter
     */
    @SecurityCheck(AsyncQueryOwner.PRINCIPAL_IS_OWNER)
    static public class AsyncQueryOwner<T> extends FilterExpressionCheck<T> {
        public static final String PRINCIPAL_IS_OWNER = "Principal is Owner";
        /**
         * query principalName == owner.
         */
        @Override
        public FilterExpression getFilterExpression(Class entityClass, RequestScope requestScope) {
            Principal principal = requestScope.getUser().getPrincipal();
            if (principal == null || principal.getName() == null) {
                 return getPredicateOfPrincipalNameNull();
            } else {
                return getPredicateOfPrincipalName(principal.getName());
            }
        }
    }

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
