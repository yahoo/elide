/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.jpql.porting;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;

import lombok.NonNull;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Wraps ScrollableResult as Iterator.
 *
 * @param <T> type of return object
 * @param <R> Result object
 */
public class ScrollableIteratorBase<T, R> implements Iterable<T>, Iterator<T> {
    private final R scroll;
    private final Predicate<R> nextFun;
    private final Function<R, T> getFun;
    private boolean inUse = false;
    private boolean hasNext = false;
    private T first;

    public ScrollableIteratorBase(R scroll, Predicate<R> nextFun, Function<R, T> getFun) {
        this.scroll = scroll;
        this.nextFun = nextFun;
        this.getFun = getFun;

        hasNext = nextFun.test(scroll);
        // extract the first element to see if it is singleton
        if (hasNext) {
            first = getFun.apply(scroll);
            hasNext = nextFun.test(scroll);
        }
    }

    @Override
    public Iterator<T> iterator() {
        if (inUse) {
            throw new ConcurrentModificationException();
        }

        inUse = true;
        return Iterators.unmodifiableIterator(this);
    }

    @Override
    public boolean hasNext() {
        return hasNext || first != null;
    }

    @Override
    @NonNull public T next() {
        if (!hasNext && first == null) {
            throw new NoSuchElementException();
        }
        @NonNull T row;
        if (first != null) {
            row = first;
            first = null;
        } else {
            row = getFun.apply(scroll);
            hasNext = nextFun.test(scroll);
        }
        Preconditions.checkNotNull(row);
        return row;
    }

    /**
     * Returns element if a singleton.
     * @return singleton element
     */
    public Optional<T> singletonElement() {
        if (first != null && !hasNext) {
            return Optional.of(first);
        }
        return Optional.empty();
    }
}
