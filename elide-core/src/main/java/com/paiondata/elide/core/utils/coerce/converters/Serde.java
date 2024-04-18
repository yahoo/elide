/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.utils.coerce.converters;

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
     * Deserialize an instance of type S to type T.
     * @param type The type to deserialize
     * @param val The thing to deserialize
     * @return The deserialized value
     */
    default T deserialize(Class<?> type, S val) {
        return deserialize(val);
    }

    /**
     * Serializes an instance of type T as type S.
     * @param val The thing to serialize
     * @return The serialized value
     */
    S serialize(T val);
}
