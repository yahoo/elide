/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.hibernate3.security;

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
     * Get criterion for request scope.
     *
     * @param requestScope the requestSCope
     * @return the criterion
     */
    Criterion getCriterion(RequestScope requestScope);
}
