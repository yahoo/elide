/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.security;

import com.yahoo.elide.core.RequestScope;

import org.hibernate.criterion.Criterion;

/**
 * Extends Check to support Hibernate Criteria to limit SQL query responses.
 * @param <T> Type of record for Check
 */
public interface CriteriaCheck<T> extends Check<T> {
    /**
     * @see org.hibernate.criterion.Restrictions
     */
    Criterion getCriterion(RequestScope requestScope);
}
