/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.core.datastore;

import java.util.ArrayList;

/**
 * Constructs DataStoreIterables.
 * @param <T>
 */
public class DataStoreIterableBuilder<T> {

    private boolean filterInMemory = false;
    private boolean sortInMemory = false;
    private boolean paginateInMemory = false;
    private final Iterable<T> wrapped;

    /**
     * Constructor.
     */
    public DataStoreIterableBuilder() {
        this.wrapped = new ArrayList<>();
    }

    /**
     * Constructor.
     * @param wrapped Required iterable to wrap.
     */
    public DataStoreIterableBuilder(Iterable<T> wrapped) {
        if (wrapped == null) {
            this.wrapped = new ArrayList<>();
        } else {
            this.wrapped = wrapped;
        }
    }

    /**
     * Filter the iterable in memory.
     * @param filterInMemory true to filter in memory.
     * @return the builder.
     */
    public DataStoreIterableBuilder filterInMemory(boolean filterInMemory) {
        this.filterInMemory = filterInMemory;
        return this;
    }

    /**
     * Sorts the iterable in memory.
     * @param sortInMemory true to sort in memory.
     * @return the builder.
     */
    public DataStoreIterableBuilder sortInMemory(boolean sortInMemory) {
        this.sortInMemory = sortInMemory;
        return this;
    }

    /**
     * Paginates the iterable in memory.
     * @param paginateInMemory true to paginate in memory.
     * @return the builder.
     */
    public DataStoreIterableBuilder paginateInMemory(boolean paginateInMemory) {
        this.paginateInMemory = paginateInMemory;
        return this;
    }

    /**
     * Filter, sort, and paginate in memory.
     * @return the builder.
     */
    public DataStoreIterableBuilder allInMemory() {
        this.filterInMemory = true;
        this.sortInMemory = true;
        this.paginateInMemory = true;
        return this;
    }

    /**
     * Constructs the DataStoreIterable.
     * @return the new iterable.
     */
    public DataStoreIterable<T> build() {
        return new DataStoreIterable<T>() {
            @Override
            public Iterable<T> getWrappedIterable() {
                return wrapped;
            }

            @Override
            public boolean needsInMemoryFilter() {
                return filterInMemory;
            }

            @Override
            public boolean needsInMemorySort() {
                return sortInMemory;
            }

            @Override
            public boolean needsInMemoryPagination() {
                return paginateInMemory;
            }
        };
    }
}
