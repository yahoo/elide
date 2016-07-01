/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.security;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.filter.Predicate;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.parsers.expression.FilterExpressionCheckEvaluationVisitor;
import com.yahoo.elide.security.checks.InlineCheck;

import java.util.Optional;

/**
 * Check for FilterExpression. This is a super class for user defined FilterExpression check. The subclass should
 * override getFilterExpression function and return a FilterExpression which will be passed down to datastore.
 * @param <T> Type of class
 */
public abstract class FilterExpressionCheck<T> extends InlineCheck<T> {

    /**
     * Returns a FilterExpression from FilterExpressionCheck.
     * @param requestScope Request scope object
     * @return FilterExpression for FilterExpressionCheck.
     */
    public abstract FilterExpression getFilterExpression(RequestScope requestScope);


    /**
     * The filter expression is evaluated in memory if it cannot be pushed to the data store by elide for any reason.
     * @param object object returned from datastore
     * @param requestScope Request scope object
     * @param changeSpec Summary of modifications
     * @return true if the object pass evaluation against FilterExpression.
     */
    @Override
    public final boolean ok(T object, RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
        FilterExpression filterExpression = getFilterExpression(requestScope);
        return filterExpression.accept(new FilterExpressionCheckEvaluationVisitor(object, this, requestScope));
    }

    /**
     *
     * @param object object returned from datastore
     * @param predicate A predicate from filterExpressionCheck
     * @param requestScope Request scope object
     * @return true if the object pass evaluation against Predicate.
     */
    public boolean applyPredicateToObject(T object, Predicate predicate, RequestScope requestScope) {
        Predicate.PathElement path = predicate.getPath().get(predicate.getPath().size() - 1);
        Class type = path.getType();
        String fieldName = path.getFieldName();
        if (fieldName == null) {
            return false;
        }
        EntityDictionary dictionary = ((com.yahoo.elide.core.RequestScope) requestScope).getDictionary();
        java.util.function.Predicate fn = predicate.getOperator()
                .contextualize(fieldName, predicate.getValues(), dictionary);
        return fn.test(object);
    }
}
