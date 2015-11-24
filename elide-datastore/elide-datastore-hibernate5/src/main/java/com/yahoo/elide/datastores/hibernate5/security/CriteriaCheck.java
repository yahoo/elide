/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.hibernate5.security;

import com.yahoo.elide.core.RequestScope;

import com.yahoo.elide.security.Check;
import org.hibernate.criterion.Criterion;

/**
 * Extends Check to support Hibernate Criteria to limit SQL query responses.
 *
 * @param <T> Type of record for Check
 */
public interface CriteriaCheck<T> extends Check<T> {
    /**
     * Gets criterion.
     *
     * @param requestScope the request scope
     * @return the criterion
     * @see org.hibernate.criterion.Restrictions
     */
    Criterion getCriterion(RequestScope requestScope);
}
