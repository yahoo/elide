/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.datastore;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;

/**
 * Returns data loaded from a DataStore.   Wraps an iterable but also communicates to Elide
 * if the framework needs to filter, sort, or paginate the iterable in memory before returning to the client.
 * @param <T> The type being iterated over.
 */
public interface DataStoreIterable<T> extends Iterable<T> {

    /**
     * Returns the underlying iterable.
     * @return The underlying iterable.
     */
    Iterable<T> getWrappedIterable();

    @Override
    default Iterator<T> iterator() {
        return getWrappedIterable().iterator();
    }

    @Override
    default void forEach(Consumer<? super T> action) {
        getWrappedIterable().forEach(action);
    }

    @Override
    default Spliterator<T> spliterator() {
        return Spliterators.spliteratorUnknownSize(iterator(), 0);
    }

    /**
     * Whether the iterable should be filtered in memory.
     * @return true if the iterable needs sorting in memory.  false otherwise.
     */
    default boolean needsInMemoryFilter() {
        return false;
    }

    /**
     * Whether the iterable should be sorted in memory.
     * @return true if the iterable needs sorting in memory.  false otherwise.
     */
    default boolean needsInMemorySort() {
        return false;
    }

    /**
     * Whether the iterable should be paginated in memory.
     * @return true if the iterable needs pagination in memory.  false otherwise.
     */
    default boolean needsInMemoryPagination() {
        return false;
    }
}
