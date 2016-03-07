/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.jsonapi.models;

import com.yahoo.elide.jsonapi.serialization.SingletonSerializer;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.AbstractSet;
import java.util.Collections;
import java.util.Iterator;

/**
 * Single object treated as a Set.
 *
 * @param <T>  the type of element to treat as a set
 */
@JsonSerialize(using = SingletonSerializer.class)
public class SingleElementSet<T> extends AbstractSet<T> {

    private final T value;

    public SingleElementSet(T v) {
        value = v;
    }

    public T getValue() {
        return value;
    }

    @Override
    public int size() {
        return value == null ? 0 : 1;
    }

    @Override
    public Iterator<T> iterator() {
        return value == null
                ? Collections.emptyIterator()
                : Collections.singleton(value).iterator();
    }
}
