/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter;

/**
 * Interface for filter operations.
 * @param <T> the return type for apply
 */
public interface FilterOperation<T> {
    T apply(FilterPredicate expression);
}
