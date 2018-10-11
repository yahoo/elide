/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.hibernate3;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;

import org.hibernate.ScrollableResults;

import lombok.NonNull;

import java.util.ConcurrentModificationException;
import java.util.Iterator;

/**
 * Wraps ScrollableResult as Iterator.
 *
 * @param <T> type of return object
 */
public class ScrollableIterator<T> implements Iterable<T>, Iterator<T> {
    private final ScrollableResults scroll;
    private boolean inUse = false;
    private boolean hasNext = false;

    public ScrollableIterator(ScrollableResults scroll) {
        this.scroll = scroll;

        hasNext = scroll.next();
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
        return hasNext;
    }

    @Override
    @NonNull public T next() {
        @SuppressWarnings("unchecked")
        @NonNull T row = (T) scroll.get()[0];
        Preconditions.checkNotNull(row);
        hasNext = scroll.next();
        return row;
    }
}
