/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.hibernate3.security;

import com.yahoo.elide.core.RequestScope;

import com.yahoo.elide.security.Check;
import org.hibernate.criterion.Criterion;

/**
 * Extends Check to support Hibernate Criteria to limit SQL query responses.
 * @param <T> Type of record for Check
 */
public interface CriteriaCheck<T> extends Check<T> {
    /**
     * Get criterion for request scope.
     *
     * @param requestScope the requestSCope
     * @return the criterion
     */
    Criterion getCriterion(RequestScope requestScope);
}
