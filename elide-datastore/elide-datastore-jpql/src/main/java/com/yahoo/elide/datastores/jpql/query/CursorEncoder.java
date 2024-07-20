/*
 * Copyright 2024, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.jpql.query;

import java.util.Map;

/**
 * Cursor encoder.
 */
public interface CursorEncoder {
    /**
     * Encode the cursor.
     *
     * @param keys the keys
     * @return the encoded cursor
     */
    String encode(Map<String, String> keys);

    /**
     * Decode the cursor.
     *
     * @param cursor the encoded cursor
     * @return the keys
     */
    Map<String, String> decode(String cursor);
}
