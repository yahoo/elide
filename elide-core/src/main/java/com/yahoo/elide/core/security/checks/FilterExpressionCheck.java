/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.security.checks;

import com.yahoo.elide.annotation.FilterExpressionPath;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.predicates.FilterPredicate;
import com.yahoo.elide.core.filter.visitors.FilterExpressionCheckEvaluationVisitor;
import com.yahoo.elide.core.security.ChangeSpec;
import com.yahoo.elide.core.security.RequestScope;
import com.yahoo.elide.core.type.Type;

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
public abstract class FilterExpressionCheck<T> extends OperationCheck<T> {

    /**
     * Returns a FilterExpression from FilterExpressionCheck.
     *
     * @param entityClass entity type
     * @param requestScope Request scope object
     * @return FilterExpression for FilterExpressionCheck.
     */
    public abstract FilterExpression getFilterExpression(Type<?> entityClass, RequestScope requestScope);

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
        EntityDictionary dictionary = coreScope(requestScope).getDictionary();
        Type<?> entityClass = dictionary.lookupBoundClass(EntityDictionary.getType(object));
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
            com.yahoo.elide.core.RequestScope scope = coreScope(requestScope);
            Predicate<T> fn = filterPredicate.getOperator()
                    .contextualize(filterPredicate.getPath(), filterPredicate.getValues(), scope);
            return fn.test(object);
        } catch (Exception e) {
            log.error("Failed to apply predicate {}", filterPredicate, e);
            return false;
        }
    }

    @Override
    public final boolean runAtCommit() {
        return false;
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
    protected Path getFieldPath(Type<?> type, RequestScope requestScope, String method, String defaultPath) {
        EntityDictionary dictionary = coreScope(requestScope).getDictionary();
        try {
            FilterExpressionPath fep = getFilterExpressionPath(type, method, dictionary);
            return new Path(type, dictionary, fep == null ? defaultPath : fep.value());
        } catch (NoSuchMethodException | SecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static FilterExpressionPath getFilterExpressionPath(
            Type<?> type,
            String method,
            EntityDictionary dictionary) throws NoSuchMethodException {
        FilterExpressionPath path = dictionary.lookupBoundClass(type)
                .getMethod(method)
                .getAnnotation(FilterExpressionPath.class);
        return path;
    }

    protected static com.yahoo.elide.core.RequestScope coreScope(RequestScope requestScope) {
        return (com.yahoo.elide.core.RequestScope) requestScope;
    }
}
