/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.core.datastore.inmemory;

import com.paiondata.elide.core.RequestScope;
import com.paiondata.elide.core.filter.expression.FilterExpression;
import com.paiondata.elide.core.filter.expression.InMemoryFilterExecutor;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

/**
 * An iterator which filters another iterator by an Elide filter expression.
 * @param <T> The type being iterated over.
 */
public class FilteredIterator<T> implements Iterator<T> {

    private Iterator<T> wrapped;
    private Predicate<T> predicate;

    private T next;

    /**
     * Constructor.
     * @param filterExpression The filter expression to filter on.
     * @param scope Request scope.
     * @param wrapped The wrapped iterator.
     */
    public FilteredIterator(FilterExpression filterExpression, RequestScope scope, Iterator<T> wrapped) {
        this.wrapped = wrapped;
        InMemoryFilterExecutor executor = new InMemoryFilterExecutor(scope);

        predicate = filterExpression.accept(executor);
    }

    @Override
    public boolean hasNext() {
        try {
            next = next();
        } catch (NoSuchElementException e) {
            return false;
        }

        return true;
    }

    @Override
    public T next() {
        if (next != null) {
            T result = next;
            next = null;
            return result;
        }

        while (next == null && wrapped.hasNext()) {
            try {
                next = wrapped.next();
            } catch (NoSuchElementException e) {
                next = null;
            }
            if (next == null || ! predicate.test(next)) {
                next = null;
            }
        }

        if (next == null) {
            throw new NoSuchElementException();
        }

        return next;
    }
}
