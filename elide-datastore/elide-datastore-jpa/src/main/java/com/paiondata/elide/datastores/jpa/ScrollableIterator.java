/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.jpa;

import com.paiondata.elide.datastores.jpql.porting.ScrollableIteratorBase;

import java.util.Iterator;

/**
 * Wraps ScrollableResult as Iterator.
 *
 * @param <T> type of return object
 */
public class ScrollableIterator<T> extends ScrollableIteratorBase<T, Iterator<T>> {
    public ScrollableIterator(Iterator<T> results) {
        super(results, Iterator::hasNext, Iterator::next);
    }
}
