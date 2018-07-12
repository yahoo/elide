/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.utils.coerce.converters;

/**
 * Bidirectional conversion from one type to another.
 * @param <S>
 * @param <T>
 */
public interface Serde<S, T> {
    /**
     * Foo
     * @param val
     * @return
     */
    public T serialize(S val);

    /**
     * Bar
     * @param val
     * @return
     */
    public S deserialize(T val);
}
