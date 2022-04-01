/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.models.security;

import com.yahoo.elide.annotation.SecurityCheck;
import com.yahoo.elide.async.models.AsyncAPI;
import com.yahoo.elide.async.models.QueryStatus;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.predicates.FilterPredicate;
import com.yahoo.elide.core.security.ChangeSpec;
import com.yahoo.elide.core.security.RequestScope;
import com.yahoo.elide.core.security.User;
import com.yahoo.elide.core.security.checks.FilterExpressionCheck;
import com.yahoo.elide.core.security.checks.OperationCheck;
import com.yahoo.elide.core.security.checks.UserCheck;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.core.type.Type;

import java.security.Principal;
import java.util.Collections;
import java.util.Optional;

/**
 * Operation Checks on the Async API and Result objects.
 */
public class AsyncAPIInlineChecks {
    private static final String PRINCIPAL_NAME = "principalName";

    static private FilterPredicate getPredicateOfPrincipalName(String principalName, Type entityClass) {
        Path.PathElement path = new Path.PathElement(entityClass, ClassType.STRING_TYPE, PRINCIPAL_NAME);
        return new FilterPredicate(path, Operator.IN, Collections.singletonList(principalName));
    }

    static private FilterPredicate getPredicateOfPrincipalNameNull(Type entityClass) {
        Path.PathElement path = new Path.PathElement(entityClass, ClassType.STRING_TYPE, PRINCIPAL_NAME);
        return new FilterPredicate(path, Operator.ISNULL, Collections.emptyList());
    }

    /**
     * Filter for principalName == Owner.
     * @param <T> type parameter
     */
    @SecurityCheck(AsyncAPIOwner.PRINCIPAL_IS_OWNER)
    static public class AsyncAPIOwner<T> extends FilterExpressionCheck<T> {
        public static final String PRINCIPAL_IS_OWNER = "Principal is Owner";
        /**
         * query principalName == owner.
         */
        @Override
        public FilterExpression getFilterExpression(Type entityClass, RequestScope requestScope) {
            Principal principal = requestScope.getUser().getPrincipal();
            if (principal == null || principal.getName() == null) {
                 return getPredicateOfPrincipalNameNull(entityClass);
            }
            return getPredicateOfPrincipalName(principal.getName(), entityClass);
        }
    }

    @SecurityCheck(AsyncAPIAdmin.PRINCIPAL_IS_ADMIN)
    public static class AsyncAPIAdmin extends UserCheck {

        public static final String PRINCIPAL_IS_ADMIN = "Principal is Admin";

        @Override
        public boolean ok(User user) {
            if (user != null && user.getPrincipal() != null) {
                return user.isInRole("admin");
            }
            return false;
        }
    }

    @SecurityCheck(AsyncAPIStatusValue.VALUE_IS_CANCELLED)
    public static class AsyncAPIStatusValue extends OperationCheck<AsyncAPI> {

        public static final String VALUE_IS_CANCELLED = "value is Cancelled";

        @Override
        public boolean ok(AsyncAPI object, RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
            return changeSpec.get().getModified().toString().equals(QueryStatus.CANCELLED.name());
        }
    }

    @SecurityCheck(AsyncAPIStatusQueuedValue.VALUE_IS_QUEUED)
    public static class AsyncAPIStatusQueuedValue extends OperationCheck<AsyncAPI> {
        public static final String VALUE_IS_QUEUED = "value is Queued";
        @Override
        public boolean ok(AsyncAPI object, RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
            return changeSpec.get().getModified().toString().equals(QueryStatus.QUEUED.name());
        }
    }
}
