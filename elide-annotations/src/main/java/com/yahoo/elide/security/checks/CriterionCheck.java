/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.security.checks;

import com.yahoo.elide.security.RequestScope;

/**
 * Extends Check to support Hibernate Criteria to limit SQL query responses.
 * @param <R> Type of the criterion to return
 * @param <T> Type of the record for the Check
 */
public interface CriterionCheck<R, T> extends Check<T> {
    /**
     * Get criterion for request scope.
     *
     * @param requestScope the requestSCope
     * @return the criterion
     */
    R getCriterion(RequestScope requestScope);
}
