/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import com.google.common.collect.UnmodifiableIterator;

import java.util.AbstractSet;
import java.util.Iterator;

/**
 * Stream iterable list as a set of PersistentResource.
 * @param <T> type of resource
 */
public class PersistentResourceSet<T> extends AbstractSet<PersistentResource<T>> {

    final private PersistentResource<?> parent;
    final private Iterable<T> list;
    final private RequestScope requestScope;

    public PersistentResourceSet(PersistentResource<?> parent, Iterable<T> list, RequestScope requestScope) {
        this.parent = parent;
        this.list = list;
        this.requestScope = requestScope;
    }

    public PersistentResourceSet(Iterable<T> list, RequestScope requestScope) {
        this(null, list, requestScope);
    }

    @Override
    public Iterator<PersistentResource<T>> iterator() {
        final Iterator<T> iterator = list.iterator();
        return new UnmodifiableIterator<PersistentResource<T>>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public PersistentResource<T> next() {
                T obj = iterator.next();
                return new PersistentResource<>(obj, parent, requestScope.getUUIDFor(obj), requestScope);
            }
        };
    }

    @Override
    public int size() {
        throw new UnsupportedOperationException();
    }
}
