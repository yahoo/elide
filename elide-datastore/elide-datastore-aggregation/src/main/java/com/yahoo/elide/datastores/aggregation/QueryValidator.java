/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation;

import com.yahoo.elide.core.exceptions.HttpStatusException;
import com.yahoo.elide.datastores.aggregation.query.Query;

public interface QueryValidator {

    default void validate(Query query) throws HttpStatusException {
        validateWhereClause(query);
        validateHavingClause(query);
        validateSorting(query);
    }

    void validateWhereClause(Query query);
    void validateHavingClause(Query query);
    void validateSorting(Query query);
}
