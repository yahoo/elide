/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.models.security;

import com.yahoo.elide.annotation.SecurityCheck;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.security.FilterExpressionCheck;
import com.yahoo.elide.security.RequestScope;

import java.security.Principal;
import java.util.Collections;

/**
 * FilterExpressions for AsyncQuery.
 */
public class AsyncQueryFilterExpression {
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
    @SecurityCheck(AsyncQueryPrincipalOwner.PRINCIPAL_IS_OWNER)
    static public class AsyncQueryPrincipalOwner<T> extends FilterExpressionCheck<T> {
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
}
