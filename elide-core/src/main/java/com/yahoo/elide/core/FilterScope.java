/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import com.yahoo.elide.core.filter.Predicate;
import com.yahoo.elide.security.Check;
import com.yahoo.elide.security.UserCheck.UserPermission;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.yahoo.elide.security.UserCheck.ALLOW;
import static com.yahoo.elide.security.UserCheck.DENY;
import static com.yahoo.elide.security.UserCheck.FILTER;

/**
 * Scope for filter processing.  Contains requestScope and checks.
 * @param <T> Filter type
 */
public class FilterScope<T> {

    @Getter private final RequestScope requestScope;
    @Getter private final boolean isAny;
    @Getter private final List<Check<T>> checks;
    private UserPermission filterUserPermission = null;

    public FilterScope(RequestScope requestScope) {
        this.requestScope = requestScope;
        this.isAny = false;
        checks = Collections.emptyList();
    }

    public FilterScope(RequestScope requestScope, boolean isAny, Class<? extends Check>[] checkClasses) {
        this.requestScope = requestScope;
        this.isAny = isAny;

        List<Check<T>> checks = new ArrayList<>(checkClasses.length);
        for (Class<? extends Check> checkClass : checkClasses) {
            try {
                checks.add(checkClass.newInstance());
            } catch (InstantiationException | IllegalAccessException e) {
                checks.add(null);
            }
        }
        this.checks = checks;
    }

    /**
     * Get the set of filter predicates applied to this request
     *
     * @return The set of predicates
     */
    public Set<Predicate> getPredicatesOfType(String type) {
        return requestScope.getPredicates().stream()
                .filter(predicate -> predicate.getType().equals(type))
                .collect(Collectors.toSet());
    }

    /**
     * Returns true if filters are applied to this query.
     *
     * @return true if there are filters
     */
    public boolean hasPredicates() {
        return !requestScope.getPredicates().isEmpty();
    }

    /**
     * Get User Permissions.
     *
     * @return composite UserPermission for this FilterScope
     */
    public UserPermission getUserPermission() {
        if (filterUserPermission != null) {
            return filterUserPermission;
        }

        UserPermission compositeUserPermission = null;
        for (Check check : checks) {
            UserPermission checkUserPermission = requestScope.getUser().checkUserPermission(check);

            // short-cut for ALLOW and ANY
            if (checkUserPermission == ALLOW && isAny) {
                compositeUserPermission = ALLOW;
                break;
            }

            // short-cut for DENY and ALL
            if (checkUserPermission == DENY && !isAny) {
                compositeUserPermission = DENY;
                break;
            }

            // if FILTER set as found and keep looking
            if (checkUserPermission == FILTER) {
                compositeUserPermission = FILTER;
                continue;
            }
        }

        // if still null, then all are DENY & ALL or ALLOW & ANY
        if (compositeUserPermission == null) {
            compositeUserPermission = isAny ? DENY : ALLOW;
        }
        return this.filterUserPermission = compositeUserPermission;
    }
}
