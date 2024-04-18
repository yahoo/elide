/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.datastores.aggregation;

import com.paiondata.elide.core.exceptions.HttpStatusException;
import com.paiondata.elide.datastores.aggregation.query.Query;

public interface QueryValidator {

    default void validate(Query query) throws HttpStatusException {
        validateWhereClause(query);
        validateHavingClause(query);
        validateSorting(query);
        validateProjectedColumns(query);
        validateQueryArguments(query);
    }

    /**
     * Ensures that no filter predicates tries not navigate a relationship.
     * @param query The client query to validate.
     */
    void validateWhereClause(Query query);


    /**
     * Validate the having clause before execution. Having clause is not as flexible as where clause,
     * the fields in having clause must be either or these two:
     * 1. A grouped by dimension in this query
     * 2. An aggregated metric in this query
     *
     * All grouped by dimensions are defined in the entity bean, so the last entity class of a filter path
     * must match entity class of the query.
     *
     * @param query The client query to validate.
     */
    void validateHavingClause(Query query);

    /**
     * Method to verify that all the sorting options provided by the user are valid and supported.
     * @param query The client query to validate.
     */
    void validateSorting(Query query);

    /**
     * Validates that the requested set of columns can be queried together.
     * @param query The client query to validate.
     */
    void validateProjectedColumns(Query query);

    /**
     * Validates that the arguments sent in the query.
     * @param query The client query to validate.
     */
    void validateQueryArguments(Query query);
}
