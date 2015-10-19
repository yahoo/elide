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
 * Single object treated as List.
 *
 * @param <T>  the type parameter
 */
@JsonSerialize(using = SingletonSerializer.class)
public class SingleElementSet<T> extends AbstractSet<T> {

    private final T value;

    /**
     * Instantiates a new Single element list.
     *
     * @param v the v
     */
    public SingleElementSet(T v) {
        value = v;
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public Iterator<T> iterator() {
        return Collections.singleton(value).iterator();
    }
}
