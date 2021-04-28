/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.security.checks;

import com.yahoo.elide.core.security.ChangeSpec;
import com.yahoo.elide.core.security.RequestScope;

import lombok.Getter;
import lombok.Setter;

import java.util.Optional;

public class DatastoreEvalFilterExpressionCheck<T> extends OperationCheck<T> {
    @Getter @Setter
    private boolean executedInMemory;
    private boolean negated = false;
    @Getter
    private FilterExpressionCheck<T> wrappedFilterExpressionCheck;

    public DatastoreEvalFilterExpressionCheck(FilterExpressionCheck<T> wrappedFilterExpressionCheck) {
        this.wrappedFilterExpressionCheck = wrappedFilterExpressionCheck;
        this.executedInMemory = false;
    }

    @Override
    public boolean ok(T object, RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
        if (executedInMemory) {
            return negated ^ wrappedFilterExpressionCheck.ok(object, requestScope, changeSpec);
        }
        return true;
    }

    public void negate() {
        negated = !negated;
    }

    @Override
    public String toString() {
        return "DatastoreEvalFilterExpressionCheck{" +
                "executedInMemory=" + executedInMemory +
                ", negated=" + negated +
                ", wrappedFilterExpressionCheck=" + wrappedFilterExpressionCheck.getClass() +
                '}';
    }
}
