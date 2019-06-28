/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.search.constraints;

import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.exceptions.HttpStatusException;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.Operator;

/**
 * Constraint on whether or not the predicate operator is supported.
 */
public class OperatorConstraint implements SearchConstraint {

    private DataStoreTransaction.FeatureSupport supportsInfix;
    private DataStoreTransaction.FeatureSupport supportsPrefix;

    public OperatorConstraint(DataStoreTransaction.FeatureSupport supportsInfix,
                              DataStoreTransaction.FeatureSupport supportsPrefix) {
        this.supportsInfix = supportsInfix;
        this.supportsPrefix = supportsPrefix;
    }

    @Override
    public DataStoreTransaction.FeatureSupport canSearch(Class<?> entityClass, FilterPredicate predicate)
            throws HttpStatusException {

        Operator op = predicate.getOperator();

        /* We only support INFIX & PREFIX */
        switch (op) {
            case INFIX:
            case INFIX_CASE_INSENSITIVE:
                return supportsInfix;
            case PREFIX:
            case PREFIX_CASE_INSENSITIVE:
                return supportsPrefix;
            default:
                return DataStoreTransaction.FeatureSupport.NONE;
        }
    }
}
