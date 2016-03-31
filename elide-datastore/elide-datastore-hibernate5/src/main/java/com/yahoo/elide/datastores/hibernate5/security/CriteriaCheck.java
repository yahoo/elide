/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.hibernate5.security;

import com.yahoo.elide.security.RequestScope;
import com.yahoo.elide.security.checks.CriterionCheck;
import org.hibernate.criterion.Criterion;

/**
 * Extends Check to support Hibernate Criteria to limit SQL query responses.
 * @param <T> Type of record for Check
 * @deprecated As of 2.1, replaced by {@link com.yahoo.elide.security.checks.CriterionCheck}
 */
@Deprecated
public interface CriteriaCheck<T> extends CriterionCheck<Criterion, T> {
    /**
     * Gets criterion.
     *
     * @param requestScope the request scope
     * @return the criterion
     * @see org.hibernate.criterion.Restrictions
     */
    Criterion getCriterion(RequestScope requestScope);
}
