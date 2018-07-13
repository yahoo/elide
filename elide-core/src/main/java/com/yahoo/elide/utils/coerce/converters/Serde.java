/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.utils.coerce.converters;

/**
 * Bidirectional conversion from one type to another.
 * @param <S> The serialized type
 * @param <T> The deserialized type
 */
public interface Serde<S, T> {
    /**
     * Deserialize an instance of type S to type T.
     * @param val The thing to deserialize
     * @return The deserialized value
     */
    T deserialize(S val);

    /**
     * Serializes an instance of type T as type S.
     * @param val The thing to serialize
     * @return The serialized value
     */
    S serialize(T val);
}
