/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.hibernate3;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;
import lombok.NonNull;
import org.hibernate.ScrollableResults;

import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;

/**
 * Wraps ScrollableResult as Iterator.
 *
 * @param <T> type of return object
 */
public class ScrollableIterator<T> implements Iterable<T>, Iterator<T> {
    final private ScrollableResults scroll;
    private boolean inUse = false;
    private boolean hasNext;

    public ScrollableIterator(ScrollableResults scroll) {
        this.scroll = scroll;
    }

    @Override
    public Iterator<T> iterator() {
        if (inUse) {
            throw new ConcurrentModificationException();
        }

        if (!scroll.first()) {
            return Collections.emptyListIterator();
        }

        inUse = true;
        hasNext = true;
        return Iterators.unmodifiableIterator(this);
    }

    @Override
    public boolean hasNext() {
        return hasNext;

    }

    @Override
    public @NonNull
    T next() {
        @SuppressWarnings("unchecked")
        @NonNull T row = (T) scroll.get()[0];
        Preconditions.checkNotNull(row);
        hasNext = scroll.next();
        return row;
    }
}
