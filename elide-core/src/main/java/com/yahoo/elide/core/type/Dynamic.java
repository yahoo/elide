/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.type;

/**
 * All objects created or loaded by a DataStore that are not associated with a ClassType
 * must inherit from this interface.
 */
public interface Dynamic {

    /**
     * Get the underlying Elide type associated with this object.
     * @return The Elide type.
     */
    Type getType();
}
