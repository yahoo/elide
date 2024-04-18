/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.async.models.security;

import com.paiondata.elide.annotation.SecurityCheck;
import com.paiondata.elide.async.models.AsyncApi;
import com.paiondata.elide.async.models.QueryStatus;
import com.paiondata.elide.core.Path;
import com.paiondata.elide.core.filter.Operator;
import com.paiondata.elide.core.filter.expression.FilterExpression;
import com.paiondata.elide.core.filter.predicates.FilterPredicate;
import com.paiondata.elide.core.security.ChangeSpec;
import com.paiondata.elide.core.security.RequestScope;
import com.paiondata.elide.core.security.User;
import com.paiondata.elide.core.security.checks.FilterExpressionCheck;
import com.paiondata.elide.core.security.checks.OperationCheck;
import com.paiondata.elide.core.security.checks.UserCheck;
import com.paiondata.elide.core.type.ClassType;
import com.paiondata.elide.core.type.Type;

import java.security.Principal;
import java.util.Collections;
import java.util.Optional;

/**
 * Operation Checks on the Async API and Result objects.
 */
public class AsyncApiInlineChecks {
    private static final String PRINCIPAL_NAME = "principalName";

    private static FilterPredicate getPredicateOfPrincipalName(String principalName, Type entityClass) {
        Path.PathElement path = new Path.PathElement(entityClass, ClassType.STRING_TYPE, PRINCIPAL_NAME);
        return new FilterPredicate(path, Operator.IN, Collections.singletonList(principalName));
    }

    private static FilterPredicate getPredicateOfPrincipalNameNull(Type entityClass) {
        Path.PathElement path = new Path.PathElement(entityClass, ClassType.STRING_TYPE, PRINCIPAL_NAME);
        return new FilterPredicate(path, Operator.ISNULL, Collections.emptyList());
    }

    /**
     * Filter for principalName == Owner.
     * @param <T> type parameter
     */
    @SecurityCheck(AsyncApiOwner.PRINCIPAL_IS_OWNER)
    public static class AsyncApiOwner<T> extends FilterExpressionCheck<T> {
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

    @SecurityCheck(AsyncApiAdmin.PRINCIPAL_IS_ADMIN)
    public static class AsyncApiAdmin extends UserCheck {

        public static final String PRINCIPAL_IS_ADMIN = "Principal is Admin";

        @Override
        public boolean ok(User user) {
            if (user != null && user.getPrincipal() != null) {
                return user.isInRole("admin");
            }
            return false;
        }
    }

    @SecurityCheck(AsyncApiStatusValue.VALUE_IS_CANCELLED)
    public static class AsyncApiStatusValue extends OperationCheck<AsyncApi> {

        public static final String VALUE_IS_CANCELLED = "value is Cancelled";

        @Override
        public boolean ok(AsyncApi object, RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
            return changeSpec.get().getModified().toString().equals(QueryStatus.CANCELLED.name());
        }
    }

    @SecurityCheck(AsyncApiStatusQueuedValue.VALUE_IS_QUEUED)
    public static class AsyncApiStatusQueuedValue extends OperationCheck<AsyncApi> {
        public static final String VALUE_IS_QUEUED = "value is Queued";
        @Override
        public boolean ok(AsyncApi object, RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
            return changeSpec.get().getModified().toString().equals(QueryStatus.QUEUED.name());
        }
    }
}
