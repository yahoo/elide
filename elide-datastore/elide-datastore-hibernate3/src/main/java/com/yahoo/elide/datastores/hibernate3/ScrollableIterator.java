/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.hibernate3;

import com.yahoo.elide.core.hibernate.ScrollableIteratorBase;

import org.hibernate.ScrollableResults;

/**
 * Wraps ScrollableResult as Iterator.
 *
 * @param <T> type of return object
 */
public class ScrollableIterator<T> extends ScrollableIteratorBase<T, ScrollableResults> {
    public ScrollableIterator(ScrollableResults results) {
        super(results, ScrollableResults::next, scroll -> (T) scroll.get()[0]);
    }
}
