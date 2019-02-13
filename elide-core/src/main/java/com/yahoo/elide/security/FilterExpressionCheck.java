/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.security;

import com.yahoo.elide.annotation.FilterExpressionPath;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.parsers.expression.FilterExpressionCheckEvaluationVisitor;
import com.yahoo.elide.security.checks.InlineCheck;

import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * Check for FilterExpression. This is a super class for user defined FilterExpression check. The subclass should
 * override getFilterExpression function and return a FilterExpression which will be passed down to datastore.
 *
 * @param <T> Type of class
 */
@Slf4j
public abstract class FilterExpressionCheck<T> extends InlineCheck<T> {

    /**
     * Returns a FilterExpression from FilterExpressionCheck.
     *
     * @param entityClass entity type
     * @param requestScope Request scope object
     * @return FilterExpression for FilterExpressionCheck.
     */
    public abstract FilterExpression getFilterExpression(Class<?> entityClass, RequestScope requestScope);

    /* NOTE: Filter Expression checks and user checks are intended to be _distinct_ */
    @Override
    public final boolean ok(User user) {
        throw new UnsupportedOperationException();
    }


    /**
     * The filter expression is evaluated in memory if it cannot be pushed to the data store by elide for any reason.
     *
     * @param object object returned from datastore
     * @param requestScope Request scope object
     * @param changeSpec Summary of modifications
     * @return true if the object pass evaluation against FilterExpression.
     */
    @Override
    public final boolean ok(T object, RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
        Class entityClass = coreScope(requestScope).getDictionary().lookupEntityClass(object.getClass());
        FilterExpression filterExpression = getFilterExpression(entityClass, requestScope);
        return filterExpression.accept(new FilterExpressionCheckEvaluationVisitor(object, this, requestScope));
    }

    /**
     * Applies a filter predicate to the object in question.
     *
     * @param object object returned from datastore
     * @param filterPredicate A predicate from filterExpressionCheck
     * @param requestScope Request scope object
     * @return true if the object pass evaluation against Predicate.
     */
    public boolean applyPredicateToObject(T object, FilterPredicate filterPredicate, RequestScope requestScope) {
        try {
            String fieldPath = filterPredicate.getFieldPath();
            com.yahoo.elide.core.RequestScope scope = coreScope(requestScope);
            Predicate fn = filterPredicate.getOperator().contextualize(fieldPath, filterPredicate.getValues(), scope);
            return fn.test(object);
        } catch (Exception e) {
            log.error("Failed to apply predicate {}", filterPredicate, e);
            return false;
        }
    }

    /**
     * Converts FieldExpressionPath value to corresponding list of Predicates.
     *
     * @param type         entity
     * @param requestScope request scope
     * @param method       associated check method name containing FieldExpressionPath
     * @param defaultPath  path to use if no FieldExpressionPath defined
     * @return Predicates
     */
    protected static Path getFieldPath(Class<?> type, RequestScope requestScope, String method, String defaultPath) {
        EntityDictionary dictionary = coreScope(requestScope).getDictionary();
        try {
            FilterExpressionPath fep = getFilterExpressionPath(type, method, dictionary);
            return new Path(type, dictionary, fep == null ? defaultPath : fep.value());
        } catch (NoSuchMethodException | SecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static FilterExpressionPath getFilterExpressionPath(
            Class<?> type,
            String method,
            EntityDictionary dictionary) throws NoSuchMethodException {
        FilterExpressionPath path = dictionary.lookupEntityClass(type)
                .getMethod(method)
                .getAnnotation(FilterExpressionPath.class);
        return path;
    }

    protected static com.yahoo.elide.core.RequestScope coreScope(RequestScope requestScope) {
        return (com.yahoo.elide.core.RequestScope) requestScope;
    }
}
