/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.security.PermissionExecutor;
import com.yahoo.elide.security.checks.ExtractedChecks;
import com.yahoo.elide.security.checks.InlineCheck;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;

/**
 * Scope for filter processing.  Contains requestScope and checks.
 */
@Slf4j
public class FilterScope {

    @Getter private final RequestScope requestScope;
    @Getter private final ExtractedChecks.CheckMode checkMode;
    @Getter private final List<InlineCheck> inlineChecks;

    public FilterScope(RequestScope requestScope) {
        this.requestScope = requestScope;
        this.checkMode = ExtractedChecks.CheckMode.ALL;
        inlineChecks = Collections.emptyList();
    }

    public FilterScope(RequestScope requestScope, Class<?> resourceClass) {
        ExtractedChecks checks =
                PermissionExecutor.loadEntityChecks(ReadPermission.class, resourceClass, requestScope.getDictionary());
        this.requestScope = requestScope;
        this.checkMode = checks.getCheckMode();
        this.inlineChecks = checks.getInlineChecks();
    }

    public FilterScope(RequestScope requestScope, ExtractedChecks.CheckMode checkMode, List<InlineCheck> inlineChecks) {
        this.requestScope = requestScope;
        this.checkMode = checkMode;
        this.inlineChecks = inlineChecks;
    }

    /**
     * Determine whether or not the check mode is any.
     *
     * NOTE: This method is often used in transaction implementations.
     *
     * @return True if checkmode is any, false if all.
     */
    public boolean isAny() {
        return ExtractedChecks.CheckMode.ANY == checkMode;
    }

    /**

     * Returns true if pagination limits were added to this query
     *
     * NOTE: This method is often used in GET transaction implementations
     *
     * @return true if there is pagination filtering
     */
    public boolean hasSortingRules() {
        return !requestScope.getSorting().isDefaultInstance();
    }

    /**
     * Returns true if pagination limits were added to this query
     *
     * NOTE: This method is often used in GET transaction implementations
     *
     * @return true if there is pagination filtering
     */
    public boolean hasPagination() {
        return !requestScope.getPagination().isDefault();
    }

    /**
     * Returns true if filters are applied to this query.
     *
     * NOTE: This method is often used in transaction implementations.
     *
     * @return true if there are filters
     */
    public boolean hasPredicates() {
        return !requestScope.getPredicates().isEmpty();
    }
}
